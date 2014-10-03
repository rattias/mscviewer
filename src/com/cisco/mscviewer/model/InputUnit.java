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

public enum InputUnit {
    NS("ns"),
    US("us"),
    MS("ms"),
    S("s"),
    M("m"),
    H("h"),
    T("ticks");

    private final String str;

    private InputUnit(String s){
        str = s;
    }

    @Override
    public String toString() {
        return str;
    }

    public long getNsCount() {
        switch(this) {
        case T:
        case NS:
            return 1L;
        case US:
            return 1000L;
        case MS:
            return 1000000L;
        case S:
            return 1000000000L;
        case M:
            return 1000000000L*60;
        case H:
            return 1000000000L*60*60;
        default:
            return -1;
        }
    }
}
