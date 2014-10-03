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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@SuppressWarnings("serial")
class FindSourcePanel extends JPanel {
    private final JList<String> list;   

    public FindSourcePanel(final JList<String> list) {
        this.list = list;
        setLayout(new BorderLayout());
        JLabel jl = new JLabel("Find:");
        add(jl, BorderLayout.WEST);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        add(p, BorderLayout.CENTER);
        final JButton prev = new JButton("<");
        final JButton next = new JButton(">");
        final JTextField tf = new JTextField();

        p.add(prev, BorderLayout.WEST);
        p.add(tf, BorderLayout.CENTER);
        p.add(next, BorderLayout.EAST);

        tf.addActionListener(new ActionListener() {			
            @Override
            public void actionPerformed(ActionEvent e) {
                String expr = tf.getText();
                findNext(expr);
            }
        });

        prev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String  txt = tf.getText();
                findPrev(txt);
            }
        });		

        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String txt = tf.getText();
                findNext(txt);
            }
        });		
    }

    private Pattern getPattern(String txt) {
        try {
            return Pattern.compile(txt);
        }catch(PatternSyntaxException ex) {
            String msg = ex.getMessage();
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void findNext(String txt) {
        if (txt != null) {
            Pattern p = getPattern(txt);
            ListModel<String> m = list.getModel();
            int idx = list.getSelectedIndex()+1;
            while(idx<m.getSize()) {
                if (p.matcher(m.getElementAt(idx)).find()) {
                    list.setSelectedIndex(idx);
                    list.ensureIndexIsVisible(idx);
                    return;
                }
                idx++;
            }
            JOptionPane.showMessageDialog(this, "No more elements matching expression", "Error", JOptionPane.ERROR_MESSAGE);
        }		
    }

    private void findPrev(String txt) {
        if (txt != null) {
            Pattern p = getPattern(txt);
            ListModel<String> m = list.getModel();
            int idx = list.getSelectedIndex();
            if (idx<0)
                idx = m.getSize()-1;
            else
                idx--;
            while(idx >= 0) {
                if (p.matcher(m.getElementAt(idx)).find()) {
                    list.setSelectedIndex(idx);
                    list.ensureIndexIsVisible(idx);
                    return;
                }
                idx--;
            }
            JOptionPane.showMessageDialog(this, "No more elements matching expression", "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

}
