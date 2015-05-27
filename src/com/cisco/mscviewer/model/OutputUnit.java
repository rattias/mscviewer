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
import java.util.concurrent.TimeUnit;

public enum OutputUnit {
    H_M_S(new SimpleDateFormat("HH:mm:ss")), H_M_S_MS(new SimpleDateFormat(
            "HH:mm:ss:SSS")), H_M_S_MS_US(new SimpleDateFormat("HH:mm:ss:SSS")), H_M_S_MS_US_NS(
            new SimpleDateFormat("HH:mm:ss:SSS")), H_M_S_US(
            new SimpleDateFormat("HH:mm:ss")), H_M_S_NS(new SimpleDateFormat(
            "HH:mm:ss")), T(null);

    private final SimpleDateFormat fmt;

    private OutputUnit(SimpleDateFormat f) {
        fmt = f;
    }

    public String format(long nsSinceEpoch) {
        final long msSinceEpoch = TimeUnit.NANOSECONDS.toMillis(nsSinceEpoch);
        final String prefix = fmt.format(new Date(msSinceEpoch));
        final long leftoverNs = nsSinceEpoch
                - TimeUnit.MILLISECONDS.toNanos(msSinceEpoch);
        switch (this) {
        case H_M_S_MS_US:
            long us = TimeUnit.NANOSECONDS.toMicros(leftoverNs);
            return String.format("%s:%.3d", prefix, us);
        case H_M_S_MS_US_NS:
            us = TimeUnit.NANOSECONDS.toMicros(leftoverNs);
            final long ns = leftoverNs - TimeUnit.MICROSECONDS.toNanos(us);
            return String.format("%s:%.3:%.3", prefix, us, ns);
        case H_M_S_US:
            us = TimeUnit.NANOSECONDS.toMicros(leftoverNs);
            return String.format("%s:%.6d", prefix, us);
        case H_M_S_NS:
            return String.format("%s:%.9d", prefix, leftoverNs);
        default:
            return prefix;
        }
    }

    // public boolean contains(String el) {
    // if (el.equals("h"))
    // return str.startsWith("h");
    // else
    // return str.contains(":"+el);
    // }

    // @Override
    // public String toString() {
    // return str;
    // }
}
