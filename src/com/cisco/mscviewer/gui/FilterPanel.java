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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.cisco.mscviewer.expression.ParsedExpression;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@SuppressWarnings("serial")
class FilterPanel extends JPanel {
    final private MainFrame mf;
    private JToggleButton enable;
    private RuleComponent fc;
    public FilterPanel(MainFrame mf) {
        this.mf = mf;
        setLayout(new BorderLayout());

        JLabel jl = new JLabel("Filter:");
        add(jl,BorderLayout.WEST);

        fc = new RuleComponent();
        add(fc, BorderLayout.CENTER);

        enable = new JToggleButton("!");
        add(enable, BorderLayout.EAST);

        enable.addActionListener(new ActionListener() {			
            @Override
            public void actionPerformed(ActionEvent e) {
                FilterPanel.this.mf.getMainPanel().updateViewForFilter();
            }
        });
    }

    public boolean enabled() {
        return enable.isSelected();
    }

    public ParsedExpression getParsedExpression() {
        return fc.getParsedExpression();
    }
}
