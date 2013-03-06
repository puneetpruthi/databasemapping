/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.mybeans.factory.BeanFactoryException;
import org.mybeans.factory.DuplicateKeyException;
import org.mybeans.factory.MatchArg;
import org.mybeans.factory.RollbackException;
import org.mybeans.factory.Transaction;
import org.mybeans.nonmodifiable.NMDate;
import org.mybeans.nonmodifiable.NMSQLDate;
import org.mybeans.nonmodifiable.NMTime;

public class CSVFactory<B> extends AbstractFactory<B> implements OutcomeListener {

	private static ReentrantLock tableLock = new ReentrantLock();
    private static ArrayList<CSVFactory<?>> involvedCSVFactories = null;

	// Instance variables protected by tableLock
	private Map<PrimaryKey<B>,Object[]> dbBeans = new TreeMap<PrimaryKey<B>,Object[]>();
	private Map<PrimaryKey<B>,BeanTrackerRec<B>> changedBeans = null;
	private long maxId = 0;

	// Instance variables initialized by constructor and then protected by tableLock
    private int backupNumber;
    private List<Integer> backupNums;

	// Instance variables initialized by constructor
	private File csvFile;
	private int maxBackups;
    private String backupFileNamePrefix;

    // Other instance variables
    private Writer debug = null;

	public CSVFactory(Class<B> beanClass, File csvFile, int maxBackups, String[] primaryKeyNames, AbstractFactory<?>[] referencedFactories) {
		super(beanClass,primaryKeyNames,referencedFactories);
		this.csvFile = csvFile;
		this.maxBackups = maxBackups;
		checkFileColumns(beanClass);
		initDateFormats();
		loadFile();
		initBackupFileNameInfo();
	}

    public B create(Object...primaryKeyValues) throws RollbackException {
        if (!Transaction.isActive()) {
            // No big performance benefit in this implementation to not using transactions...
            Transaction.begin();
            B answer = create(primaryKeyValues);
            Transaction.commit();
            return answer;
        }

        Property[] priKeyProps = primaryKeyInfo.getProperties();
        boolean autoIncrement;
        if (primaryKeyValues.length == 0 && priKeyProps.length == 1 &&
                (priKeyProps[0].getType() == int.class || priKeyProps[0].getType() == long.class)) {
            autoIncrement = true;
        } else {
            autoIncrement = false;
            validatePrimaryKeyValues(primaryKeyValues);
        }
        lockTable();

        Object[] priKeyDBValues;
        if (!autoIncrement) {
            priKeyDBValues = DBValues.makeDBValues(primaryKeyInfo.getProperties(),primaryKeyValues);
        } else if (primaryKeyInfo.getProperties()[0].getType() == long.class) {
            // long auto_increment case
            maxId++;
            priKeyDBValues = new Object[] { maxId };
        } else {
            // int auto_increment case
            maxId++;
            int i = (int) maxId;
            priKeyDBValues = new Object[] { i };
        }

        PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,priKeyDBValues);
        BeanTrackerRec<B> rec = changedBeans.get(key);
        if (rec != null && rec.getBean() != null) {
            TranImpl.rollbackAndThrow(new DuplicateKeyException("A record already has this primary key value+: "+key+" (which you have already read or created)"));
        }

        // If we got here, we're not tracking this key, so let's check the DB
        Object[] dbValues = dbBeans.get(key);
        if (dbValues != null) TranImpl.rollbackAndThrow(new DuplicateKeyException("A record already has this primary key value+: "+key));

        // If we got here, it doesn't already exist
        B answer = newBean(priKeyDBValues);
        changedBeans.put(key,new BeanTrackerRec<B>(key,answer,null));

        if (!autoIncrement && primaryKeyInfo.getProperties().length == 1) {
            if (primaryKeyValues[0] instanceof Integer) {
                int id = (Integer) primaryKeyValues[0];
                if (id > maxId) maxId = id;
            }
            if (primaryKeyValues[0] instanceof Long) {
                long id = (Long) primaryKeyValues[0];
                if (id > maxId) maxId = id;
            }
        }

        return answer;
    }

    public void delete(Object...primaryKeyValues) throws RollbackException {
        if (!Transaction.isActive()) {
            // No big performance benefit in this implementation to not using transactions...
            Transaction.begin();
            delete(primaryKeyValues);
            Transaction.commit();
            return;
        }

        validatePrimaryKeyValues(primaryKeyValues);

        lockTable();

        Object[] priKeyDBValues = DBValues.makeDBValues(primaryKeyInfo.getProperties(),primaryKeyValues);
        PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,priKeyDBValues);
        BeanTrackerRec<B> rec = changedBeans.get(key);
        if (rec != null && rec.getBean() == null) {
            TranImpl.rollbackAndThrow("This record was already deleted by the current transaction: "+key);
        }

        // If we got here, we're not tracking this key, so let's check the DB
        Object[] dbValues = dbBeans.get(key);
        if (dbValues == null) TranImpl.rollbackAndThrow("This record does not exist: "+key);

        // In this implemenation, deleted beans are tracked as "null" until transaction commit
        // At commit time they are removed from the DB (unless the have been re-created later in the transaction)
        rec = new BeanTrackerRec<B>(key,null,null);
        changedBeans.put(key,rec);
    }

    public int getBeanCount() throws RollbackException {
        if (!Transaction.isActive()) {
            // No big performance benefit in this implementation to not using transactions...
            Transaction.begin();
            int answer = getBeanCount();
            Transaction.commit();
            return answer;
        }

        lockTable();

        int answer = dbBeans.size();

        for (Object key : changedBeans.keySet()) {
            BeanTrackerRec<B> rec = changedBeans.get(key);
            B bean = rec.getBean();
            Object[] values = dbBeans.get(key);

            if (bean == null && values != null) {
                // This transaction removed the bean
                // (and the bean previously existed)
                answer--;
            }

            if (bean != null && values == null) {
                // This transaction created the bean
                // (and has not subsequently removed it)
                answer++;
            }
        }

        return answer;
    }

	public B lookup(Object...primaryKeyValues) throws RollbackException {
		if (!Transaction.isActive()) {
			// No big performance benefit in this implementation to not using transactions...
			Transaction.begin();
			B answer = lookup(primaryKeyValues);
			Transaction.commit();
			return answer;
		}

        validatePrimaryKeyValues(primaryKeyValues);

        lockTable();

        Object[] priKeyDBValues = DBValues.makeDBValues(primaryKeyInfo.getProperties(),primaryKeyValues);
        PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,priKeyDBValues);
        BeanTrackerRec<B> rec = changedBeans.get(key);
        if (rec != null) return rec.getBean();

        // If we got here, we're not tracking this key, so let's check the DB
        Object[] dbValues = dbBeans.get(key);
        if (dbValues == null) return null;

        B answer = makeBean(dbValues);
        rec = new BeanTrackerRec<B>(key,answer,dbValues);
        changedBeans.put(key,rec);
        return answer;
	}

    public B[] match(MatchArg...constraints) throws RollbackException {
        if (!Transaction.isActive()) {
            // No big performance benefit in this implementation to not using transactions...
            Transaction.begin();
            B[] answer = match(constraints);
            Transaction.commit();
            return answer;
        }

        MatchArgTree argTree = MatchArgTree.buildTree(properties,MatchArg.and(constraints));

    	fixMaxMin(argTree);  // Calls lockTable() if necessary

        lockTable();

        List<B> answerBeans = new ArrayList<B>();
        for (BeanTrackerRec<B> rec : changedBeans.values()) {
        	B changedBean = rec.getBean();
        	if (changedBean != null) {
	            Object[] newDBValues = makeDBValues(changedBean);
	            if (argTree.satisfied(newDBValues)) {
	                answerBeans.add(changedBean);
	            }
        	}
        }

        for (Object[] dbValues : dbBeans.values()) {
            PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,primaryKeyInfo.getPrimaryKeyDBValues(dbValues));
            if (!changedBeans.containsKey(key) && argTree.satisfied(dbValues)) {
                B bean = makeBean(dbValues);
                answerBeans.add(bean);
                changedBeans.put(key,new BeanTrackerRec<B>(key,bean,dbValues));
            }
        }
        return toArray(answerBeans);
    }

    protected void setDebugOutput(Writer writer) { debug = writer; }

    public void prepare() throws RollbackException {
        for (CSVFactory<?> f : involvedCSVFactories) {
            f.doPrepare();
        }
    }

	private void doPrepare() throws RollbackException {
		// Prepare just makes sure that there are no beans in the changedBeans
		// map that have had their primary keys changed
		for (BeanTrackerRec<B> rec : changedBeans.values()) {
            PrimaryKey<B> key = rec.getKey();
			B bean = rec.getBean();
            if (bean == null) {
                // This bean was deleted in the transaction, so skip the check
                continue;
            }

            Object[] newDBValues = makeDBValues(bean);
            if (!key.keyEquals(primaryKeyInfo.getPrimaryKeyDBValues(newDBValues))) {
                TranImpl.rollbackAndThrow("Bean "+bean+" has changed primary key value: key should be \""+
                        DBValues.toString(rec.getKey().getDBValues())+
                        "\" but instead is \""+
                        DBValues.toString(primaryKeyInfo.getPrimaryKeyDBValues(newDBValues))+"\"");
            }
		}
	}

	public void commit() {
        for (CSVFactory<?> f : involvedCSVFactories) {
            f.doCommit();
        }
        involvedCSVFactories = null;
        tableLock.unlock();
    }

    public void doCommit() {
		boolean changedDB = false;

		for (BeanTrackerRec<B> rec : changedBeans.values()) {
            PrimaryKey<B> key = rec.getKey();
			B bean = rec.getBean();

			if (bean == null) {
				// This transaction removed the bean
				dbBeans.remove(key);
				changedDB = true;
                continue;
			}

            Object[] oldDBValues = dbBeans.get(key);
            Object[] newDBValues = makeDBValues(bean);

            if (rec.getDBValues() == null) {
				// This transaction created the bean
				// (and hasn't subsequently removed it)
				dbBeans.put(key,newDBValues);
				changedDB = true;
                continue;
			}

            if (!DBValues.equalDBValues(properties,oldDBValues,newDBValues)) {
				// We're tracking this bean and it was changed
				dbBeans.put(key,newDBValues);
				changedDB = true;
			}
		}

		if (changedDB) flush();
		changedBeans = null;
	}

	public void rollback() {
        for (CSVFactory<?> f : involvedCSVFactories) {
            f.changedBeans = null;
        }
        involvedCSVFactories = null;
		tableLock.unlock();
	}

	private void lockTable() throws RollbackException {
        if (!tableLock.isHeldByCurrentThread()) {
            tableLock.lock();
            if (changedBeans != null) throw new AssertionError("New transaction (for this CSVFactory), but changed beans already exist");
            if (involvedCSVFactories != null) throw new AssertionError("New transaction (for this CSVFactory), but involved factories already exist");
            involvedCSVFactories = new ArrayList<CSVFactory<?>>();
            TranImpl.join(this);
        }
        if (changedBeans == null) {
            changedBeans = new HashMap<PrimaryKey<B>,BeanTrackerRec<B>>();
            involvedCSVFactories.add(this);
        }
	}

	// Private variables used for formatting

    private static final String NULL_STRING_REP = "\\null";

	// Date & time formatting -- used only for reading & writing the file
	private static final String dateFormatStr               = "yyyy-MM-dd";
	private static final String dateTimeFormatWithSecStr    = "yyyy-MM-dd HH:mm:ss";
	private static final String dateTimeFormatWithoutSecStr = "yyyy-MM-dd HH:mm";
	private static final String timeFormatWithSecStr        = "HH:mm:ss";
	private static final String timeFormatWithoutSecStr     = "HH:mm";

	// These formatters are not thread safe...they are only used when
	// reading the file (in the constructor, so not multi-threaded)
	// and when writing out the file (synchronized on myHash)
	private SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatStr);
	private SimpleDateFormat dateTimeFormatWithSec = new SimpleDateFormat(dateTimeFormatWithSecStr);
	private SimpleDateFormat dateTimeFormatWithoutSec = new SimpleDateFormat(dateTimeFormatWithoutSecStr);
	private SimpleDateFormat timeFormatWithSec = new SimpleDateFormat(timeFormatWithSecStr);
	private SimpleDateFormat timeFormatWithoutSec = new SimpleDateFormat(timeFormatWithSecStr);

	// Private instance methods, in alphabetical order

	private void checkFileColumns(Class<B> beanClass) {
		Column[] fileColumns = loadHeaders(csvFile,beanClass);

        checkForDuplicateColumnNames(fileColumns);

        Iterator<Column> columnIter = new MyArrayIterator<Column>(fileColumns);
        for (Property prop : properties) {
            String[]   columnNames = prop.getColumnNames();
            Class<?>[] columnTypes = prop.getColumnTypes();
            for (int i=0; i<columnNames.length; i++) {
                if (!columnIter.hasNext()) {
                    throw new BeanFactoryException("Too few columns in the CSV file: no column "+columnNames[i]+",file="+csvFile);
                }
                
                Column fileColumn = columnIter.next();
                
                if (!columnNames[i].equals(fileColumn.name)) {
                    throw new BeanFactoryException("Mismatched column names: found \""+fileColumn.name+
                            "\" expecting \""+columnNames[i]);
                }
                
                if (columnTypes[i].isEnum() && fileColumn.type != String.class) {
                    throw new BeanFactoryException("Mismatched column type: columnName="+columnNames[i]+" found \""+fileColumn.type+
                            "\" expecting \""+String.class+"\" (file="+csvFile+")");
                }
                
                if (!columnTypes[i].isEnum() && columnTypes[i] != fileColumn.type) {
                    throw new BeanFactoryException("Mismatched column type: columnName="+columnNames[i]+" found \""+fileColumn.type+
                            "\" expecting \""+columnTypes[i]+"\" (file="+csvFile+")");
                }
                
                if (prop.isPrimaryKeyProperty() && !fileColumn.isPrimaryKey) {
                    throw new BeanFactoryException("Mismatched column type: must be marked as part of the primary key: columnName="+columnNames[i]+",file="+csvFile);
                }
                
                if (!prop.isPrimaryKeyProperty() && fileColumn.isPrimaryKey) {
                    throw new BeanFactoryException("Mismatched column type: must NOT be marked as part of the primary key: columnName="+columnNames[i]+",file="+csvFile);
                }
                
                if (prop.isArray() && !fileColumn.isArray) {
                    throw new BeanFactoryException("Mismatched column type: must be an array: columnName="+columnNames[i]+",file="+csvFile);
                }
                
                if (!prop.isArray() && fileColumn.isArray) {
                    throw new BeanFactoryException("Mismatched column type: must be an array: columnName="+columnNames[i]+",file="+csvFile);
                }
			}
		}

        if (columnIter.hasNext()) {
            throw new BeanFactoryException("Too many columns in the CSV file: column"+columnIter.next().name+",file="+csvFile);
        }
	}

    private void deleteExtraBackups() {
        while (backupNums.size() > maxBackups) {
            int firstNum = backupNums.remove(0);
            String firstFileName = backupFileNamePrefix + firstNum + ".csv";
            File f = new File(firstFileName);
            boolean b = f.delete();
            if (!b) throw new BeanFactoryException("Could not delete backup file: "+f);
        }
    }

	private void flush() {
        try {
			backupNumber = backupNumber + 1;
            File backupFile = new File(backupFileNamePrefix + backupNumber + ".csv");
			File newFile    = new File(backupFileNamePrefix + "new.csv");

			if (newFile.exists()) {
				boolean b = newFile.delete();
				if (!b) throw new AssertionError("An old "+newFile+" exists.  Could not delete it!");
			}

            FileWriter fw = new FileWriter(newFile);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(generateHeader(properties));

            for (Object[] values : dbBeans.values()) {
                for (int i=0; i<properties.length; i++) {
                    if (i>0) bw.write(',');
                    // System.out.println("flush: value="+DBValues.toString(values[i])+", prop="+properties[i]);
                    if (properties[i].isArray()) {
                        flushArrayValue(bw,values[i],properties[i]);
                    } else {
                        flushNonArrayValue(bw,values[i],properties[i]);
                    }
                }
                bw.write('\n');
            }
            bw.close();
            fw.close();

			boolean b = csvFile.renameTo(backupFile);
            if (!b) throw new AssertionError("Could not rename old "+csvFile+" to "+backupFile);

			b = newFile.renameTo(csvFile);
            if (!b) throw new AssertionError("Could not rename old "+newFile+" to "+csvFile);
            backupNums.add(backupNumber);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        deleteExtraBackups();
    }

	private void flushArrayValue(BufferedWriter bw, Object value, Property property) throws IOException {
        if (value == null) {
            bw.write(NULL_STRING_REP);
            return;
        }

        if (value instanceof Object[]) {
            Object[] values = (Object[]) value;
            bw.write(String.valueOf(values.length));
            for (Object v : values) {
                bw.write(',');
                flushNonArrayValue(bw,v,property);
            }
            return;
        }
    }

    private void flushNonArrayValue(BufferedWriter bw, Object value, Property property) throws IOException {
        if (property instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) property;
            Property[] priKeyProps = refProp.getRefBeanPrimaryKeyProperties();
            Object[] priKeyVals = (Object[]) value;
            for (int i=0; i<priKeyProps.length; i++) {
                if (priKeyVals == null) {
                    flushNonArrayValue(bw,null,priKeyProps[i]);
                } else {
                    flushNonArrayValue(bw,priKeyVals[i],priKeyProps[i]);
                }
            }
            return;
        }

        if (value == null) {
            bw.write(NULL_STRING_REP);
            return;
        }

        Class<?> type = property.getBaseType();
        if (type == String.class) {
            bw.write('"');
            bw.write(fixBadChars((String)value));
            bw.write('"');
            return;
        }

        if (type == byte[].class) {
        	bw.write(HexHelpers.bytesToHex((byte[])value));
            return;
        }

        if (type == java.sql.Date.class || type == NMSQLDate.class) {
            bw.write(dateFormat.format((java.sql.Date)value));
            return;
        }

        if (type == java.util.Date.class || type == NMDate.class) {
            bw.write(dateTimeFormatWithSec.format((java.util.Date)value));
            return;
        }

        if (type == java.sql.Time.class || type == NMTime.class) {
            bw.write(timeFormatWithSec.format((java.sql.Time)value));
            return;
        }

        bw.write(value.toString());
	}

	private void initBackupFileNameInfo() {
        // Note: we're guaranteed that file name ends with .csv
		String csvFileName = csvFile.getPath();
		backupFileNamePrefix = csvFileName.substring(0,csvFileName.length()-4)
								+ "-backup-";
		backupNumber = 0;

		File parentDir = csvFile.getAbsoluteFile().getParentFile();
		String prefix = csvFile.getName().substring(0,csvFile.getName().length()-4)+"-backup-";
        backupNums = new ArrayList<Integer>();
		String[] list = parentDir.list();
		for (int i=0; i<list.length; i++) {
			String f = list[i];
			if (f.startsWith(prefix)) {
				String numStr = f.substring(f.lastIndexOf('-')+1,f.length()-4);
				try {
					backupNums.add(new Integer(numStr));
				} catch (NumberFormatException e) {
					System.out.println("Found backup file with invalid version number: "+f);
				}
			}
		}

        if (backupNums.size() == 0) return;

        // set backupNumber to highest one seen
        Collections.sort(backupNums);
        Integer lastNumObj = backupNums.get(backupNums.size()-1);
        if (lastNumObj != null) backupNumber = lastNumObj.intValue();

        deleteExtraBackups();
	}

	private void initDateFormats() {
		dateTimeFormatWithSec.setLenient(false);
		dateTimeFormatWithoutSec.setLenient(false);
		dateFormat.setLenient(false);
		timeFormatWithSec.setLenient(false);
		timeFormatWithoutSec.setLenient(false);
	}

	private void loadFile() {
		// No synchronization required...only called from constructor
		try {
			FileReader fr = new FileReader(csvFile);
			CSVReader  cr = new CSVReader(fr);

			cr.readCSVLine();  // skip names line
			cr.readCSVLine();  // skip types line

			// Skip the checks of the first two lines.  They were done when
			// the loadHeader() call was previously made.

			String[] strValues = cr.readCSVLine();
			int lineNum = 3;
			while (strValues != null) {
				if (debug != null) debug.write("loadFile: Reading line="+lineNum);
				Object[] dbValues = parseRow(lineNum,strValues);
                Object[] priKeyDBValues = primaryKeyInfo.getPrimaryKeyDBValues(dbValues);
                PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,priKeyDBValues);
				dbBeans.put(key,dbValues);
                if (primaryKeyInfo.getProperties().length == 1) {
    				if (dbValues[0] instanceof Integer) {
    					int id = (Integer) dbValues[0];
    					if (id > maxId) maxId = id;
    				}
    				if (dbValues[0] instanceof Long) {
    					long id = (Long) dbValues[0];
    					if (id > maxId) maxId = id;
    				}
                }
				strValues = cr.readCSVLine();
				lineNum++;
			}

			cr.close();
			fr.close();
		} catch (Exception e) {
			throw new BeanFactoryException(e);
		}
	}

	private Object[] parseRow(int lineNum, String[] strValues) {
		Object[] dbValues = new Object[properties.length];
        Iterator<String> iter = new MyArrayIterator<String>(strValues);
		for (int i=0; i<properties.length; i++) {
            dbValues[i] = parseProperty(lineNum,iter,properties[i]);
        }
        return dbValues;
    }

    private Object parseProperty(int lineNum, Iterator<String> iter, Property property) {
        if (property instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) property;
            Property[] priKeyProps = refProp.getRefBeanPrimaryKeyProperties();
            Object[] dbValues = new Object[priKeyProps.length];
            for (int i=0; i<priKeyProps.length; i++) {
                dbValues[i] = parseProperty(lineNum,iter,priKeyProps[i]);
            }
            return dbValues;
        }

        try {
    		if (!iter.hasNext()) throw new BeanFactoryException("Too few values");

            String value = iter.next();
            if (!property.isArray()) {
                Object answer = parseValue(value,property);
                if (answer == null && !property.isNullable()) throw new BeanFactoryException("Value cannot be NULL");
                return answer;
            }

            if (value.equals(NULL_STRING_REP)) return null;

            int count;
    		try {
    			count = Integer.parseInt(value);
    		} catch (NumberFormatException e) {
    			count = -1;
    		}
    		if (count < 0) throw new BeanFactoryException("Invalid array length: "+value);

    		Object[] array = new Object[count];
    		for (int j=0; j<count; j++) {
                if (!iter.hasNext()) throw new BeanFactoryException("Too few values to fill array");
    			array[j] = parseValue(iter.next(),property);
    		}

            return array;
        } catch (BeanFactoryException e) {
            String newMessage = e.getMessage()+": line="+lineNum+", prop="+property+", file="+csvFile;
            throw new BeanFactoryException(newMessage,e);
        }
	}


    private Object parseValue(String s, Property prop) {
		if (s.equals(NULL_STRING_REP)) return null;

		Class<?> type = prop.getType();
		if (type == String.class) return unfixBadChars(s);

		if (type == boolean.class) {
			if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
			if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
			throw new BeanFactoryException("Invalid BOOLEAN (not \"true\" or \"false\")");
		}

		if (type == byte[].class) {
			try {
				return HexHelpers.hexToBytes(s);
			} catch (NumberFormatException e) {
				throw new BeanFactoryException("Could not parse hex string",e);
			}
		}

		if (type == float.class) {
			try {
				return new Float(s);
			} catch (NumberFormatException e) {
				throw new BeanFactoryException("Could not parse FLOAT",e);
			}
		}

		if (type == double.class) {
			try {
				return new Double(s);
			} catch (NumberFormatException e) {
				throw new BeanFactoryException("Could not parse DOUBLE",e);
			}
		}

		if (type == int.class) {
			try {
				return new Integer(s);
			} catch (NumberFormatException e) {
				throw new BeanFactoryException("Could not parse INT",e);
			}
		}

		if (type == long.class) {
			try {
				return new Long(s);
			} catch (NumberFormatException e) {
				throw new BeanFactoryException("Could not parse LONG",e);
			}
		}

		if (type == java.sql.Date.class || type == NMSQLDate.class) {
			try {
				java.util.Date d = dateFormat.parse(s);
				if (type == NMSQLDate.class) return new NMSQLDate(d.getTime());
				return new java.sql.Date(d.getTime());
			} catch (ParseException e) {
				throw new BeanFactoryException("Could not parse DATE (not \""+dateFormatStr+"\")",e);
			}
		}

		if (type == java.util.Date.class || type == NMDate.class) {
			try {
				java.util.Date d;
				if (s.length() == dateTimeFormatWithSecStr.length()) {
					d = dateTimeFormatWithSec.parse(s);
				} else {
					d = dateTimeFormatWithoutSec.parse(s);
				}
				if (type == NMDate.class) return new NMDate(d.getTime());
				return d;
			} catch (ParseException e) {
				throw new BeanFactoryException("Could not parse DATETIME (not \""+dateTimeFormatWithoutSecStr+
						"\" or \""+dateTimeFormatWithSecStr+"\")",e);
			}
		}

		if (type == java.sql.Time.class || type == NMTime.class) {
			try {
				java.util.Date d;
				if (s.length() == timeFormatWithSecStr.length()) {
					d = timeFormatWithSec.parse(s);
				} else {
					d = timeFormatWithoutSec.parse(s);
				}
				if (type == NMTime.class) return new NMTime(d.getTime());
				return new java.sql.Time(d.getTime());
			} catch (ParseException e) {
				throw new BeanFactoryException("Could not parse TIME (not \""+timeFormatWithoutSecStr+
						"\" or \""+timeFormatWithSecStr+"\")",e);
			}
		}

		throw new BeanFactoryException("Unknown type");
	}

	// private static methods, in alphabetical order

	private static String fixBadChars(String s) {
		if (s.indexOf('\\') == -1 && s.indexOf('"') == -1 && s.indexOf('\n') == -1) {
			return s;
		}

		StringBuffer b = new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\') {
				b.append("\\\\");
			} else if (c == '"') {
				b.append("\\\"");
			} else if (c == '\n') {
				b.append("\\n");
			} else if (c == '\r') {
				// Do not include carriage returns in data files
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}

    private void fixMaxMin(MatchArgTree parsedMatchArg) throws RollbackException {
    	Iterator<MatchArgLeafNode> iter = parsedMatchArg.leafIterator();
    	while (iter.hasNext()) {
    		MatchArgLeafNode arg = iter.next();
			MatchOp op = arg.getOp();
			
    		if (op != MatchOp.MAX && op == MatchOp.MIN) {
    			Property prop = arg.getProperty();

    			lockTable();

    			Object matchValue = getMaxMinValueOfTrackedBeans(changedBeans,prop,op);

    	        for (Object[] dbValues : dbBeans.values()) {
    	            PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,primaryKeyInfo.getPrimaryKeyDBValues(dbValues));
    	            if (!changedBeans.containsKey(key)) {
    	            	Object dbValue = dbValues[prop.getPropertyNum()];
    	            	matchValue = matchMaxMin(prop,op,matchValue,dbValue);
    	            }
    	        }

    	        if (matchValue == null) {
    	        	// If there is no match from some max or min op, we set the constraint's op
    	        	// to null.  This causes this constraint to evaluate to false.
    	        	arg.fixConstraint(null,null);
    	        } else {
    	        	arg.fixConstraint(MatchOp.EQUALS,matchValue);
    	        }
    		}
    	}
    }

    protected static String generateHeader(Property[] properties) {
		StringBuffer b = new StringBuffer();
		for (int i=0; i<properties.length; i++) {
			if (i > 0) b.append(',');
            String[] columnNames = properties[i].getColumnNames();
            for (int j=0; j<columnNames.length; j++) {
                if (j > 0) b.append(',');
                if (properties[i].isPrimaryKeyProperty()) b.append('*');
                b.append(columnNames[j]);
            }
		}
		b.append('\n');

		for (int i=0; i<properties.length; i++) {
			if (i > 0) b.append(',');
            Class<?>[] columnTypes = properties[i].getColumnTypes();
            for (int j=0; j<columnTypes.length; j++) {
                if (j > 0) b.append(',');
                Class<?> myType = columnTypes[j];
                if (myType == byte[].class) {
                	b.append("byte[]");
                } else if (myType.isEnum()) {
                	b.append("java.lang.String");
                } else {
    				b.append(myType.getCanonicalName());
    				if (properties[i].isArray()) b.append("[]");
    			}
            }
		}
		b.append('\n');
		return b.toString();
	}

    private static class Column {
        String   name;
        Class<?> type;
        boolean  isPrimaryKey;
        boolean  isArray;
        int      position;

        Column(String name, String strType, boolean isPrimary, int pos) {
            this.name = name;
            isPrimaryKey = isPrimary;
            position = pos;
            isArray = strType.endsWith("[]") && !strType.equals("byte[]");

            if (strType.equals("byte[]")) {
                type = byte[].class;
            } else if (strType.startsWith("boolean")) {
                type = boolean.class;
            } else if (strType.startsWith("double")) {
                type = double.class;
            } else if (strType.startsWith("float")) {
                type = float.class;
            } else if (strType.startsWith("int")) {
                type = int.class;
            } else if (strType.startsWith("long")) {
                type = long.class;
            } else if (strType.startsWith("java.lang.String")) {
                type = String.class;
            } else if (strType.startsWith("java.util.Date")) {
                type = java.util.Date.class;
            } else if (strType.startsWith("java.sql.Date")) {
                type = java.sql.Date.class;
            } else if (strType.startsWith("java.sql.Time")) {
                type = java.sql.Time.class;
            } else if (strType.startsWith(NMDate.class.getCanonicalName())) {
                type = NMDate.class;
            } else if (strType.startsWith(NMSQLDate.class.getCanonicalName())) {
                type = NMSQLDate.class;
            } else if (strType.startsWith(NMTime.class.getCanonicalName())) {
                type = NMTime.class;
            } else {
                throw new BeanFactoryException("Unknown type for column "+pos+"("+name+"): "+strType);
            }
        }
    }

    private void checkForDuplicateColumnNames(Column[] columns) {
        for (int i=1; i<columns.length; i++) {
            for(int j=0; j<i; j++) {
                if (columns[i].name.equals(columns[j].name)) {
                    throw new BeanFactoryException("Duplicate column names: name="+columns[i].name+
                            " in columns "+(j+1)+" and "+(i+1));
                }
            }
        }
    }

    private Column[] loadHeaders(File csvFile, Class<B> beanClass) {
        try {
            FileReader fr = new FileReader(csvFile);
            CSVReader  cr = new CSVReader(fr);

            String[] names = cr.readCSVLine();
            if (names == null || names.length == 0) {
                throw new BeanFactoryException("First line of file must contain column names: file="+csvFile);
            }

            String[] types  = cr.readCSVLine();
            if (types == null || types.length == 0) {
                throw new BeanFactoryException("Second line of file must contain column types: file="+csvFile);
            }

            if (names.length != types.length) {
                throw new BeanFactoryException("Number of column names and types do not match: file="+csvFile);
            }

            Column[] columns = new Column[names.length];
            for (int i=0; i<columns.length; i++) {
                String name = names[i];
                if (name.length() < 1) throw new BeanFactoryException("Column name has zero length: column="+(i+1)+", file="+csvFile);
                boolean isPrimaryKey = false;
                if (name.charAt(0) == '*') {
                    isPrimaryKey = true;
                    name = name.substring(1);
                    if (name.length() < 1) throw new BeanFactoryException("Primary key column name has zero length: column="+(i+1)+", file="+csvFile);
                }
                columns[i] = new Column(name,types[i],isPrimaryKey,i+1);
            }

            cr.close();
            fr.close();
            return columns;
        } catch (IOException e) {
            throw new BeanFactoryException(e);
        }
    }

	private static String unfixBadChars(String s) {
		if (s.indexOf('\\') == -1) {
			return s;
		}

		StringBuffer b = new StringBuffer();
		int i = 0;
		while (i<s.length()) {
			char c = s.charAt(i);
			if (c == '\\' && i < s.length()-1 &&
			    (s.charAt(i+1) == '"' || s.charAt(i+1) == '\\')) {
					b.append(s.charAt(i+1));
					i += 2;
			} else if (c == '\\' && i < s.length()-1 && s.charAt(i+1) == 'n') {
					b.append('\n');
					i += 2;
			} else {
				b.append(c);
				i++;
			}
		}
		return b.toString();
	}
}
