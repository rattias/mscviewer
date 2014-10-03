/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.model;

public enum OutputUnit {
    H("h"),
    H_M("h:m"),
    H_M_S("h:m:s"),
    H_M_S_MS("h:m:s:ms"),
    H_M_S_MS_US("h:m:s:ms:us"),
    H_M_S_MS_US_NS("h:m:s:ms:us:ns"),
    H_M_S_US("h:m:s:us"),
    H_M_S_NS("h:m:s:ns"),
    T("ticks");

    private final String str;

    private OutputUnit(String s){
        str = s;
    }

    public boolean contains(String el) {
        if (el.equals("h"))
            return str.startsWith("h");
        else
            return str.contains(":"+el); 
    }

    @Override
    public String toString() {
        return str;
    }
}
