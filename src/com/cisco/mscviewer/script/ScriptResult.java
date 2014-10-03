/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Nov 2013
 */
package com.cisco.mscviewer.script;


public class ScriptResult {
    final static public int STATUS_COMPLETED = 1;
    final static public int STATUS_EXCEPTION = 2; 
    private Object result;
    private Throwable t;

    public void setResult(Object o) {
        result = o;
    }

    public Object getResult() {
        return result;
    }

    public void setException(Throwable t) {
        this.t = t;
    }

    public Throwable getException() {
        return t;
    }

    public void done(int status) {
    }
}
