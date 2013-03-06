/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import org.mybeans.factory.MatchArg;

public class UnaryMatchArg extends MatchArg {
    private String  keyName;
    private MatchOp op;

    public UnaryMatchArg(String keyName, MatchOp op) {
        this.keyName  = keyName;
        this.op       = op;
    }

    public String  getKeyName()  { return keyName;  }
    public MatchOp getOp()       { return op;       }
}
