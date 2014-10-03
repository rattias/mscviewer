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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.MSCDataModel;

public class ConfigSaver {

    public void save(String fname, MSCDataModel mod) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(fname));
        String path = mod.getFilePath();
        File f = new File(path);
        long dateTime = f.lastModified();
        String date = Config.getDateString(dateTime);
        pw.println("<"+Config.ATTR_MSCCONFIG_LOG+"\""+mod.getFilePath()+" "+Config.ATTR_MSCCONFIG_DATE+"=\""+date+"\">");
        emitFilterConfig(pw, mod);
        emitMarkerConfig(pw, mod);
        pw.println("</"+Config.EL_MSCCONFIG+">");
        pw.close();
    }

    public void emitFilterConfig(PrintWriter pw, MSCDataModel mod) {
        pw.println("  <"+Config.EL_FILTER+">");
        pw.println("  </"+Config.EL_FILTER+">");
    }

    public void emitMarkerConfig(PrintWriter pw, MSCDataModel mod) {
        pw.println("  <"+Config.EL_MARKERS+">");
        for(int i=0; i<mod.getEventCount(); i++) {
            Event ev = mod.getEventAt(i);
            Marker m = ev.getMarker();
            pw.print("    <"+Config.EL_MARKER+" ");
            pw.print(Config.ATTR_MARKER_LINE +"=\""+i+" ");
            pw.print(Config.ATTR_MARKER_COLOR+"=\""+m.toString()+" ");
            pw.println(Config.ATTR_MARKER_NOTE +"=\""+ev.getNote()+"\">");
        }
        pw.println("  </"+Config.EL_MARKERS+">");		
    }
}
