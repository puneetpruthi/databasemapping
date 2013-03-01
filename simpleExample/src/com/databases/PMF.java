package com.databases;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

/**
* @author Max Ross <maxr@google.com>
*/
public final class PMF {

private static final PersistenceManagerFactory INSTANCE =
   JDOHelper.getPersistenceManagerFactory("transactional");

public static PersistenceManagerFactory get() {
 return INSTANCE;
}

private PMF() {}
}
