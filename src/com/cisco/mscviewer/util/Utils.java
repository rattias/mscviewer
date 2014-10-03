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
package com.cisco.mscviewer.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import com.cisco.mscviewer.gui.MainFrame;

public class Utils {
    private static final HashSet<String> en = new HashSet<String>(); 
    private static final boolean tracing = false;
    public final static String EVENTS = "events";
    public static JFileChooser jfc;


    public static void setEnabled(String cl, boolean enabled) {
        if (enabled)
            en.add(cl);
        else
            en.remove(cl);
    }

    public static void trace(String group, String msg) {
        if (tracing) {
            StackTraceElement[] frames = Thread.currentThread().getStackTrace();
            int cnt;
            for(cnt=2; cnt<frames.length; cnt++) {
                if (! frames[cnt].getClass().getPackage().getName().startsWith("com.cisco"))
                    break;
            }
            String ind = "                                                                                            ".substring(2, cnt);
            String cl = frames[2].getClassName();
            cl = cl.substring(cl.lastIndexOf('.')+1);
            String m = frames[2].getMethodName();
            if (en.contains(group))
                System.out.println(ind+cl+"."+m+":"+msg);
        }
    }

    public static void dispatchOnAWTThreadNow(Runnable r) {
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }		
    }

    public static void dispatchOnAWTThreadLater(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }

    }

    public static String stringToHTML(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        // true if last char was blank
        boolean lastWasBlankChar = false;
        char c;

        string = string.trim();
        int len = string.length();
        for (int i = 0; i < len; i++)
        {
            c = string.charAt(i);
            if (c == ' ') {
                // blank gets extra work,
                // this solves the problem you get if you replace all
                // blanks with &nbsp;, if you do that you loose 
                // word breaking
                if (lastWasBlankChar) {
                    lastWasBlankChar = false;
                    sb.append("&nbsp;");
                }
                else {
                    lastWasBlankChar = true;
                    sb.append(' ');
                }
            }
            else {
                lastWasBlankChar = false;
                //
                // HTML Special Chars
                if (c == '"')
                    sb.append("&quot;");
                else if (c == '&')
                    sb.append("&amp;");
                else if (c == '<')
                    sb.append("&lt;");
                else if (c == '>')
                    sb.append("&gt;");
                else if (c == '\n')
                    // Handle Newline
                    sb.append("<br/>");
                else {
                    int ci = 0xffff & c;
                    if (ci < 160 )
                        // nothing special only 7 Bit
                        sb.append(c);
                    else {
                        // Not 7 Bit use the unicode system
                        sb.append("&#");
                        sb.append(Integer.toString(ci));
                        sb.append(';');
                    }
                }
            }
        }
        return sb.toString();
    }


    private static void getPNGSnapshotTargetsInternal(Component c, ArrayList<PNGSnapshotTarget> arr) {
        if (c instanceof PNGSnapshotTarget)
            arr.add((PNGSnapshotTarget)c);
        if (c instanceof Container) {
            Container cc = (Container)c;
            int cnt = cc.getComponentCount();
            for(int i=0; i<cnt; i++) {
                getPNGSnapshotTargetsInternal(cc.getComponent(i), arr);
            }
        }
    }

    public static PNGSnapshotTarget[] getPNGSnapshotTargets(Component c) {
        ArrayList<PNGSnapshotTarget> al = new ArrayList<PNGSnapshotTarget>();
        while(c.getParent() != null)
            c = c.getParent();
        getPNGSnapshotTargetsInternal(c, al);
        return al.toArray(new PNGSnapshotTarget[al.size()]);        
    }

    public static void getPNGSnapshot(String compName, String path) {
        PNGSnapshotTarget[] tgt = getPNGSnapshotTargets(MainFrame.getInstance());
        for (PNGSnapshotTarget tgt1 : tgt) {
            if (tgt1.getName().equals(compName)) {
                getPNGSnapshot(tgt1, path);
                return;
            }
        }
        throw new Error("component "+compName+" not found");
    }

    public static void getPNGSnapshot(final PNGSnapshotTarget t) {
        if (jfc == null)
            jfc = new JFileChooser();
        int result = jfc.showSaveDialog(null);
        switch (result) {
        case JFileChooser.APPROVE_OPTION:
            File f = jfc.getSelectedFile();
            getPNGSnapshot(t, f.getAbsolutePath());
            break;
        case JFileChooser.CANCEL_OPTION:
            break;
        case JFileChooser.ERROR_OPTION:
            System.out.println("Error");
        }
    }

    public static void getPNGSnapshot(final PNGSnapshotTarget t, final String path) {
        dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                Component c = t.getPNGSnapshotClient();
                if (c == null)
                    c = (Component)t;
                Rectangle rec = c.getBounds(); 
                BufferedImage capture = new BufferedImage(rec.width, rec.height, BufferedImage.TYPE_INT_ARGB);
                c.paint(capture.getGraphics());
                try {
                    ImageIO.write(capture, "png", new File(path));
                    System.out.println(" written to file "+path);
                } catch (IOException ioe) {
                    throw new Error(ioe);
                }
            }
        });
    }   
}
