/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

/**
 * An implementation of connection pool for JDBC.
 *
 * Rather than allocating a new connection every time we access the database,
 * we ask the connection pool for a connection and return it to the connection
 * pool when we're finished.  The connection pool will save the connection for
 * for subsequent reuse.  If there are no open connections to hand out, the
 * connection pool opens another one.  With some JDBC implementations, idle
 * connections eventually fail.  So, this connection pool closes idle
 * connections.  (See implementation for the current settings to determine
 * how long idle connections remain open.)
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class ConnectionPool {
	// Map of connection pools.  Hashed on driver+url+user.
    private static HashMap<String,ConnectionPool> poolMap = new HashMap<String,ConnectionPool>();

    /**
     * Starts (or shuts down) a "cleaner" thread which will close idle database connections.
     *
     * By default, idle connections are not closed.  The reason is that scanning for idle connections
     * leave a "cleaner" thread running.  Leave a thread running prevents educational demos from exiting
     * once the main thread exits.  If you using this class in a long running server,
     * you should enable the closing of idle database connections.
     * @param enable
     * @see getMaxIdleTime, setMaxIdleTime
     */
    public synchronized void setIdleConnectionCleanup(boolean enable) {
    	if (enable && cleaner == null) {
			cleaner = new Cleaner(this,maxIdleTime);
			cleaner.start();
		}

    	if (!enable && cleaner != null) {
    		cleaner.stop();
    		cleaner = null;
    	}
    }

    /**
     * Get the connection pool for a given driver/URL combination.
     *
     * This methods tests the connection pool by getting and then releasing a connection.
     * If the driver or URL are bad, we'll get an error right away rather than having
     * to wait until we're deep in some bean factory.
     *
     * @param driver
     * @param URL
     * @return
     * @throws ConnectionException if getting the test connection fails
     */
    public static synchronized ConnectionPool getInstance(String driver, String URL, String user, String password) throws ConnectionException {
        String hashString = driver+URL+"==>"+user;
    	synchronized (poolMap) {
	        ConnectionPool existingPool = poolMap.get(hashString);
	        if (existingPool != null) return existingPool;
    	}

    	ConnectionPool newPool = new ConnectionPool(driver,URL,user,password);
        Connection test = newPool.getConnection();
        newPool.releaseConnection(test);

    	synchronized (poolMap) {
    		poolMap.put(hashString,newPool);
    	}

        return newPool;
    }

    /**
     * Get the maximum time that after which idle database connections are closed
     * @return maximum idle time in milliseconds
     * @see closeIdleConnections, setMaxIdleTime
     */
    public long getMaxIdleTime() {
    	return maxIdleTime;
    }

    /**
     * Changes the time after which idle database connections are closed.
     * The default time is 20 minutes (20*60*1000 milliseconds)
     * @param millis
     * @see closeIdleConnections, getMaxIdleTime
     */
    public void setMaxIdleTime(long millis) {
    	maxIdleTime = millis;
    }

    private String jdbcDriverName;
	private String jdbcURL;
    private String user;
    private String password;

    // Default time connection is allowed to be idle (20 minutes)
    private long maxIdleTime = 20 * 60 * 1000;

    // Ref to Cleaner that closes idle connections (defined below)
    private Cleaner cleaner = null;



    // A helper class to keep track of connections in the pool and the last time they were used.
    private static class MyConnTime {
    	Connection conn;
    	long       lastUsed;  // time in millis
    }

    private ArrayList<MyConnTime> connections;

    private ConnectionPool(String driver, String URL, String user, String password) {
        jdbcDriverName = driver;
        jdbcURL = URL;
        this.user = user;
        this.password = password;
        connections = new ArrayList<MyConnTime>();
    }

	public Connection getConnection() throws ConnectionException {
		// If there is already a connection in the pool, return it
		synchronized (connections) {
			if (connections.size() > 0) {
				MyConnTime myConn = connections.get(connections.size()-1);
                connections.remove(connections.size()-1);
                return myConn.conn;
			}
		}

		// Otherwise, make a new connection and return it

		try {
			Class.forName(jdbcDriverName);
		} catch (ClassNotFoundException e) {
			throw new ConnectionException("Could not load database driver: " + e.toString());
		}

		try {
            if (user == null) return DriverManager.getConnection(jdbcURL);
            return DriverManager.getConnection(jdbcURL,user,password);
		} catch (SQLException e) {
			throw new ConnectionException("Could not get connection: " + e.toString());
		}
	}

	private Connection getIdleConnection() {
		synchronized (connections) {
			long now = System.currentTimeMillis();
			for (int i=0; i<connections.size(); i++) {
				MyConnTime myConn = connections.get(i);
				long idleTime = now - myConn.lastUsed;
				if (idleTime > maxIdleTime) {
					connections.remove(i);
					return myConn.conn;
				}
			}
			return null;
		}
	}

	private long getMostIdleTime() {
		synchronized (connections) {
			long answer = 0;
			long now = System.currentTimeMillis();
			for (MyConnTime myConn : connections) {
				long idleTime = now - myConn.lastUsed;
				if (idleTime > answer) answer = idleTime;
			}
			return answer;
		}
	}

	public void releaseConnection(Connection c) {
		MyConnTime myConn = new MyConnTime();
		myConn.conn = c;
		myConn.lastUsed = System.currentTimeMillis();
		synchronized (connections) {
            connections.add(myConn);
		}
	}

	private static class Cleaner implements Runnable {
		private boolean keepRunning = false;
		private ConnectionPool pool;
        private long maxIdleTime;

		public Cleaner(ConnectionPool pool, long maxIdleTime) {
			this.pool = pool;
            this.maxIdleTime = maxIdleTime;
		}

		public void run() {
			while (keepRunning) {
				Connection con = pool.getIdleConnection();
				while (con != null) {
					try {
						con.close();
					} catch (SQLException e) {
						System.err.println("ConnectionPool.Cleaner.run: error closing connection: " + e);
					}
					con = pool.getIdleConnection();
				}

				long sleepTime = maxIdleTime;
				long idle = pool.getMostIdleTime();
				if (maxIdleTime - idle < sleepTime) {
					sleepTime = maxIdleTime - idle;
				}

				try {
					if (sleepTime > 0) Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// Do nothing
				}
			}
		}

		public void start() {
			keepRunning = true;
			Thread t = new Thread(this);
			t.start();
		}

		public void stop() {
			keepRunning = false;
		}
	}
}
