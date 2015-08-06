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

public class  OutputUnit {
    public static int WEEK_DAY = 1;
    public static int LONG_YEAR = 2;
    public static int TIME_UNIT = 4;
    
    public static enum DateMode 
    {
        NO_DATE("No date"), D_M_Y("Day/Month/Year"), Y_M_D("Year/Month/Day"), M_D_Y("Month day, Year");
        String v;
        private DateMode(String s) {
            v = s;
        }
        
        @Override
        public String toString() {
            return v;
        }
        
        public static OutputUnit.DateMode byName(String name) {
            for(OutputUnit.DateMode  dm: values()) {
                if (dm.v.equals(name))
                    return dm;
            }
            return null;
        }

    }; 
    public static enum TimeMode {
        RAW_TIME("Raw timestamp"), H_M_S("hh:mm:ss"), H_M_S_MS("hh:mm:ss,ms"), H_M_S_US("hh:mm:ss,us"), H_M_S_NS("hh:mm:ss,ns"), H_M_S_MS_US_NS("hh:mm:ss,us,ns"); 
        String v;
        private TimeMode(String s) {
            v = s;
        }

        @Override
        public String toString() {
            return v;
        }

        public static OutputUnit.TimeMode byName(String name) {
            for(OutputUnit.TimeMode  tm: values()) {
                if (tm.v.equals(name))
                    return tm;
            }
            return null;
        }
}    

    private SimpleDateFormat fmt;
    private  DateMode dateMode;
    private  TimeMode timeMode;
    private int flags;
    
    public OutputUnit() {
        this(DateMode.NO_DATE, TimeMode.H_M_S_MS, 0);
    }
    
    public OutputUnit(
            DateMode dateMode,
            TimeMode timeMode,
            int flags) {
        this.timeMode = timeMode;
        this.dateMode = dateMode;
        this.flags = flags;
        computeBaseFormat();
    }

    
    public String format(long nsSinceEpoch) {
        long msSinceEpoch = TimeUnit.NANOSECONDS.toMillis(nsSinceEpoch);
        String prefix = fmt.format(new Date(msSinceEpoch));
        switch(timeMode) {
            case RAW_TIME: return nsSinceEpoch+((flags & TIME_UNIT) != 0 ? "ns" : "");
            case H_M_S:
            case H_M_S_MS:
                return prefix;
        case H_M_S_US:
            long leftoverNs = nsSinceEpoch - TimeUnit.MILLISECONDS.toNanos(msSinceEpoch);
            long us = TimeUnit.NANOSECONDS.toMicros(leftoverNs);
            return String.format("%s:%06d", prefix, us);
        case H_M_S_NS:
            leftoverNs = nsSinceEpoch - TimeUnit.MILLISECONDS.toNanos(msSinceEpoch);
            return String.format("%s:%09d", prefix, leftoverNs);
        case H_M_S_MS_US_NS:
            leftoverNs = nsSinceEpoch - TimeUnit.MILLISECONDS.toNanos(msSinceEpoch);
            us = TimeUnit.NANOSECONDS.toMicros(leftoverNs);
            final long ns = leftoverNs - TimeUnit.MICROSECONDS.toNanos(us);
            return String.format("%s:%03d:%03d", prefix, us, ns);
        default:
            return prefix;
        }
    }
    
    public void setTimeMode(OutputUnit.TimeMode tm) {
        timeMode = tm;
    }
    
    public OutputUnit.TimeMode getTimeMode() {
        return timeMode;
    }
    
    public void setDateMode(OutputUnit.DateMode dm) {
        dateMode = dm;
        computeBaseFormat();
    }
    
    public OutputUnit.DateMode getDateMode() {
        return dateMode;
    }
    
    public void setFlags(int flags) {
        this.flags = flags;
        computeBaseFormat();
    }
    
    public int getFlags() {
        return flags;
    }
    
    private void computeBaseFormat() {
        StringBuilder sb = new StringBuilder();
        String yearFmt = (flags & LONG_YEAR) != 0 ? "YYYY" : "YY";
        String weekDay = (flags & WEEK_DAY) != 0 ? "E " : "";
        switch(dateMode) {
            case NO_DATE: break;
            case D_M_Y: sb.append(weekDay + "dd/MM/" + yearFmt); break;
            case Y_M_D: sb.append(yearFmt + "/MM/dd" +  weekDay); break;
            case M_D_Y: sb.append(weekDay + "MMM dd, " + yearFmt); break;
            default:
                throw new Error("Invalid DateMode "+dateMode);
        }
        switch(timeMode) {
            case RAW_TIME: break;
            case H_M_S:
            case H_M_S_US:
            case H_M_S_NS:
                sb.append((flags & TIME_UNIT) != 0 ? " HH'h':mm'm':ss's'" : " HH:mm:ss");
                break;
            case H_M_S_MS: 
            case H_M_S_MS_US_NS: 
                sb.append((flags & TIME_UNIT) != 0 ? " HH'h':mm'm':ss's',SSS'ms'" : " HH:mm:ss,SSS"); 
                break;
            default:
                throw new Error("Invalid TimeMode "+timeMode);
        }
        fmt = new SimpleDateFormat(sb.toString());        
    }
}
