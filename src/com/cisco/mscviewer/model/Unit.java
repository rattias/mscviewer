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

public enum Unit {
    NS("ns"),
    US("us"),
    MS("ms"),
    S("s"),
    M("m"),
    H("h"),
    D("d");

    private final String str;

    private Unit(String s){
        str = s;
    }

    @Override
    public String toString() {
        return str;
    }
}
