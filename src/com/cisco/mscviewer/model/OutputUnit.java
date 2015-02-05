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

import java.text.SimpleDateFormat;
import java.util.Date;

public enum OutputUnit {
    H_M_S(new SimpleDateFormat("HH:mm:ss")),
    H_M_S_MS(new SimpleDateFormat("HH:mm:ss:SSS")),
    H_M_S_MS_US(new SimpleDateFormat("HH:mm:ss:SSS")),
    H_M_S_MS_US_NS(new SimpleDateFormat("HH:mm:ss:SSS")),
    H_M_S_US(new SimpleDateFormat("HH:mm:ss")),
    H_M_S_NS(new SimpleDateFormat("HH:mm:ss")),
    T(null);

    private final SimpleDateFormat fmt;

    private OutputUnit(SimpleDateFormat f){
        fmt = f;
    }

    public String format(long nsSinceEpoch) {
        long msSinceEpoch = nsSinceEpoch/1000000;
        String s = fmt.format(new Date(msSinceEpoch));
        long leftoverNs = nsSinceEpoch - msSinceEpoch*1000000;
        switch(this) {
            case H_M_S_MS_US:
                long us = leftoverNs/1000;
                return s + ":" + us;
            case H_M_S_MS_US_NS:
                us = leftoverNs/1000;
                long ns = leftoverNs - us*1000;
                return s + ":" + us + ":" + ns;
            case H_M_S_US:
                long sSinceEpoch = nsSinceEpoch/1000000000;
                long leftoverUs = nsSinceEpoch - sSinceEpoch*1000000000;
                return s + ":" + leftoverUs;
            case H_M_S_NS:
                return s + ":" + leftoverNs;
        }
        return s;
    }
    
//    public boolean contains(String el) {
//        if (el.equals("h"))
//            return str.startsWith("h");
//        else
//            return str.contains(":"+el); 
//    }

//    @Override
//    public String toString() {
//        return str;
//    }
}
