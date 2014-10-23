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

import com.cisco.mscviewer.Main;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.MSCDataModel;

public class Report {
    public static void exception(Throwable t) {
        if (Main.batchMode())
           t.printStackTrace();
        else {
            Font font = new Font("Courier", Font.PLAIN, 12);
            JSplitPane jsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            JTextArea jta = new JTextArea();
            jta.setFont(font);
            jta.setLineWrap(true);
            jta.setWrapStyleWord(true);
            jta.setText(t.toString());
            jta.setEditable(false);
            jta.setPreferredSize(new Dimension(400, 300));
            final JScrollPane jsp = new JScrollPane(jta);
            jsp.setBorder(new TitledBorder("Error"));
            jsplit.setTopComponent(jsp);
            
            JTextArea jta_1 = new JTextArea();
            jta_1.setFont(font);
            jta_1.setLineWrap(true);
            jta_1.setWrapStyleWord(true);
            if (t.getCause() != null)
                jta_1.setText(t.getCause().toString());
            jta_1.setEditable(false);
            jta_1.setPreferredSize(new Dimension(50, 300));
            final JScrollPane jsp_1 = new JScrollPane(jta_1);
            jsp_1.setBorder(new TitledBorder("Details"));
            jsplit.setBottomComponent(jsp_1);
            
            jsplit.addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    Window window = SwingUtilities.getWindowAncestor(jsp);
                    if (window instanceof Dialog) {
                        Dialog dialog = (Dialog) window;
                        if (!dialog.isResizable()) {
                            dialog.setResizable(true);
                        }
                    }
                }
            });
            JOptionPane.showMessageDialog(
                    Main.getMainFrame(), 
                    jsplit, 
                    "Exception", 
                    JOptionPane.ERROR_MESSAGE);
            //t.printStackTrace();
        }
    }
    
    public static void error(Event ev, String msg) {
        MSCDataModel m = Main.getModel(); 
        String finfo = m.getFilePath()+":"+ev.getLineIndex();
        if (Main.batchMode()) {
           System.err.println(finfo+": "+msg);
        } else {
            JOptionPane.showMessageDialog(Main.getMainFrame(), finfo+"\n"+msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void error(Entity en, String msg) {
        MSCDataModel m = Main.getModel(); 
        if (Main.batchMode()) {
           System.err.println(m.getFilePath()+": entity "+en.getPath()+": "+msg);
        } else {
            JOptionPane.showMessageDialog(Main.getMainFrame(), msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}