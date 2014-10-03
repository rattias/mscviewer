/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  May 2012
 */
package com.cisco.mscviewer.gui;

import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
class HelpFrame extends JFrame {
    private final JScrollPane scrollPane;  
    private final JEditorPane editorPane;  

    public HelpFrame(){  
        setBounds(100, 100, 600, 400);
        setTitle("MSCViewer Help");

        editorPane=new JEditorPane();  
        try {  
            URL url = ClassLoader.getSystemResource("com/cisco/mscviewer/doc/help_home.html");
            System.out.println("URL: "+url);
            editorPane.setPage(url);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  

        scrollPane=new JScrollPane(editorPane);
        setContentPane(scrollPane);
    }  
}
