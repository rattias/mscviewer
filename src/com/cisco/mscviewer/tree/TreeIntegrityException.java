/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Aug 2014
 */
package com.cisco.mscviewer.tree;


/**
 *
 * @author rattias
 */
@SuppressWarnings("serial")
public class TreeIntegrityException extends Exception {
    private String path;
    int value;
    
    public TreeIntegrityException(String msg, String path, int v) {
        super(msg);
        this.path = path;
        value = v;
    }
    
    public String getTreePath() {
        return path;
    }

    public int getTreeValue() {
        return value;
    }
}
