/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jan 2012
 */
package com.cisco.mscviewer.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Config {
    static String EL_MSCCONFIG = "mscconfig";
    static String ATTR_MSCCONFIG_LOG = "log";
    static String ATTR_MSCCONFIG_DATE = "date";
    static String EL_FILTER = "filter";
    static String EL_MARKERS = "markers";
    static String EL_MARKER = "marker";
    static String ATTR_MARKER_LINE = "line";
    static String ATTR_MARKER_COLOR = "color";
    static String ATTR_MARKER_NOTE = "note";
    static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss"); 

    public static String getDateString(long t) {
        Date d = new Date(t);
        String dateString = sdf.format(d);
        return dateString;
    }

    public static long getTimeSinceEpoch(String dateString) throws ParseException {
        return sdf.parse(dateString).getTime();
    }
}
