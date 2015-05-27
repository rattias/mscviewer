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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cisco.mscviewer.expression.ExpressionParser;
import com.cisco.mscviewer.expression.ParsedExpression;
import com.cisco.mscviewer.model.ViewModel;

@SuppressWarnings("serial")
class FindPanel extends JPanel {
    private final MSCRenderer r;

    public FindPanel(final MSCRenderer r) {
        this.r = r;
        setLayout(new BorderLayout());
        final JLabel jl = new JLabel("Find:");
        add(jl, BorderLayout.WEST);

        final JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        add(p, BorderLayout.CENTER);
        final JButton prev = new JButton("<");
        final JButton next = new JButton(">");
        final RuleComponent fc = new RuleComponent();

        p.add(prev, BorderLayout.WEST);
        p.add(fc, BorderLayout.CENTER);
        p.add(next, BorderLayout.EAST);

        fc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ParsedExpression expr = fc.getParsedExpression();
                findNext(expr);
            }
        });

        prev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ParsedExpression expr = fc.getParsedExpression();
                findPrev(expr);
            }
        });

        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ParsedExpression expr = fc.getParsedExpression();
                findNext(expr);
            }
        });
    }

    private void findNext(ParsedExpression expr) {
        if (expr != null) {
            // if (t.toString().equals("event")) {
            int idx = r.getViewModelSelectedEventIndex();
            idx++;
            final ViewModel vm = r.getViewModel();
            for (; idx < vm.getEventCount(); idx++) {
                final ExpressionParser exp = new ExpressionParser();
                final boolean v = exp.evaluateAsJavaScriptonEvent(vm.getEventAt(idx),
                        expr);
                if (v) {
                    r.setSelectedEventByViewIndex(idx);
                    break;
                }
            }
            // } else if (t.toString().equals("interaction")) {
            //
            // }
        }
    }

    private void findPrev(ParsedExpression expr) {
        if (expr != null) {
            int idx = r.getViewModelSelectedEventIndex();
            idx--;
            final ViewModel vm = r.getViewModel();
            if (idx < 0)
                idx = vm.getEventCount() - 1;
            for (; idx >= 0; idx--) {
                final ExpressionParser exp = new ExpressionParser();
                final boolean v = exp.evaluateAsJavaScriptonEvent(vm.getEventAt(idx),
                        expr);
                if (v) {
                    r.setSelectedEventByViewIndex(idx);
                    break;
                }
            }
        }

    }

}
