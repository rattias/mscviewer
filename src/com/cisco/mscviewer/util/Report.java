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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.MSCDataModel;

public class Report {
    private static JComponent buildTextPane(String content, Font f, boolean descr) {
        final JTextArea jta = new JTextArea();
        
        // workaround for JDK bug. See http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7027598
        jta.setDropTarget(null);
        
        jta.setName(descr ? "TOP" : "BOTTOM");
        jta.setText(content);
        jta.setEditable(false);
        jta.setFont(f);
        if (descr) {
            jta.setOpaque(false);
            jta.setLineWrap(true);
            jta.setWrapStyleWord(true);
            jta.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 4));
            jta.setBackground(new Color(0,0,0,0));
            jta.setMaximumSize(new Dimension(1024, 768));
            return jta;
        }
        final JScrollPane jsp = new JScrollPane(jta);
        jsp.setPreferredSize(new Dimension(400, 300));
        jsp.setMaximumSize(new Dimension(1024, 768));
        jsp.setOpaque(false);
        return jsp;
    }
    
    public static void exception(String message, Throwable t) {
        if (Main.batchMode()) {
            if (message != null)
                System.err.println(message);
            t.printStackTrace();
            Throwable cause = t.getCause();
            if (cause != null) {
                System.err.println("Caused by:");
                cause.printStackTrace();
            }
        } else {
            Font font = new Font(Font.SERIF, Font.PLAIN, 20);
            JComponent top, btm;
            top = buildTextPane(message, font, true);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            t = t.getCause();
            if (t != null) {
                pw.println("Caused by:");
                t.printStackTrace(pw);
            }
            font = new Font("Courier", Font.PLAIN, 12);
            btm = buildTextPane(sw.toString(), font, false);
            JPanel container = new JPanel();
            container.setMaximumSize(new Dimension(1000,1000));
            container.setLayout(new BorderLayout());
            container.add(top, BorderLayout.NORTH);
            container.add(btm, BorderLayout.CENTER);

            container.addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    final Window window = SwingUtilities.getWindowAncestor(container);
                    if (window instanceof Dialog) {
                        final Dialog dialog = (Dialog) window;
                        if (!dialog.isResizable()) {
                            dialog.setResizable(true);
                        }
                    }
                }
            });
            JOptionPane.showMessageDialog(MainFrame.getInstance(), container,
                    "Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void error(Event ev, String msg) {
        final MSCDataModel m = MSCDataModel.getInstance();
        final String finfo = m.getFilePath() + ":" + ev.getLineIndex();
        if (Main.batchMode()) {
            System.err.println(finfo + ": " + msg);
        } else {
            JOptionPane.showMessageDialog(MainFrame.getInstance(), finfo + "\n"
                    + msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void error(Entity en, String msg) {
        final MSCDataModel m = MSCDataModel.getInstance();
        if (Main.batchMode()) {
            System.err.println(m.getFilePath() + ": entity " + en.getPath()
                    + ": " + msg);
        } else {
            JOptionPane.showMessageDialog(MainFrame.getInstance(), msg,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
