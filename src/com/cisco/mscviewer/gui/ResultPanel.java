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
package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.MSCDataModel;

@SuppressWarnings("serial")
class ResultPanel extends JPanel implements MouseListener {
    private final JEditorPane editor;

    public ResultPanel(final MSCDataModel model) {        
        setLayout(new BorderLayout());
        Font font = new Font("courier", Font.PLAIN, 12);
        String CSS = "<style> " +
        "body { "+
        "font-family: " + font.getFamily() + "; " +
        "font-size: " + font.getSize() + "pt; "+
        "}" +
        ".match {  color: #00A000; }" +
        ".mismatch {  color: #FF0000; }" +
        "</style>";
        editor = new JEditorPane("text/html", "<html><head>"+CSS+"</head><body>"); 
        //((HTMLDocument)editor.getDocument()).getStyleSheet().addRule(bodyRule);
        editor.setEditable(false);
        editor.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent he) {
                HyperlinkEvent.EventType type = he.getEventType();
                if (type == HyperlinkEvent.EventType.ACTIVATED) {
                    String descr = he.getDescription();
                    if (descr.startsWith("msc_event://")) {
                        String ids = descr.substring("msc_event://".length());
                        String[] idarr = ids.split(",");
                        for (String idarr1 : idarr) {
                            if (!idarr1.equals("")) {
                                Event ev = model.getEventAt(Integer.parseInt(idarr1));
                                Main.open(ev);
                            }
                        }
                    }
                }
            }
        });
        add(new JScrollPane(editor), BorderLayout.CENTER);
        editor.addMouseListener(this);
    }

    public void append(String txt) {
        HTMLEditorKit edkit = (HTMLEditorKit) editor.getEditorKit();
        StringReader reader = new StringReader(txt);         
        try {
            edkit.read(reader, editor.getDocument(), editor.getDocument().getLength());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void clear() {
        Document doc = editor.getDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent me) {
        if (me.isPopupTrigger()) {
            JPopupMenu jpm = new JPopupMenu();
            JMenuItem it = new JMenuItem("Clear");
            it.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    editor.setText("");                
                }
            });
            jpm.add(it);
            jpm.show((Component)me.getSource(), me.getX(), me.getY());
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

}
