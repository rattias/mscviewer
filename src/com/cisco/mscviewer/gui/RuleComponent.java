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

import java.awt.AWTKeyStroke;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.MutableComboBoxModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import com.cisco.mscviewer.expression.ExpressionParser;
import com.cisco.mscviewer.expression.ParsedExpression;
import com.cisco.mscviewer.expression.ParserState;

@SuppressWarnings("serial")
class RuleComponent extends JComboBox<String> {
    final static int HISTORY_SZ = 16;
    JTextComponent ed;
    Document doc;

    public RuleComponent() {
        final MutableComboBoxModel<String> cbm = new DefaultComboBoxModel<String>();
        setModel(cbm);
        setEditable(true);
        ed = (JTextComponent) getEditor().getEditorComponent();
        ed.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Collections.<AWTKeyStroke> emptySet());

        ed.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_TAB) {
                    complete();
                }
            }
        });

        doc = ed.getDocument();

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!e.getActionCommand().equals("comboBoxEdited")) {
                    return;
                }
                final String el = (String) getSelectedItem();
                // parse expression
                final ParserState ps = new ParserState(el);
                final ParsedExpression expr = new ExpressionParser().parse(ps);
                if (expr == null) {
                    // invalid parsing
                    JOptionPane.showMessageDialog(RuleComponent.this,
                            "Invalid expression", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // update combo history
                int i;
                for (i = 0; i < cbm.getSize(); i++) {
                    if (cbm.getElementAt(i).equals(el)) {
                        if (i != 0)
                            cbm.removeElementAt(i);
                        break;
                    }
                }
                if (cbm.getSize() == 0 || i != 0)
                    cbm.insertElementAt(el, 0);
                if (cbm.getSize() > HISTORY_SZ)
                    cbm.removeElement(HISTORY_SZ - 1);
            }
        });
    }

    void complete() {
        final int pos = ed.getCaretPosition();
        if (pos >= 0) {
            try {
                final String txt = doc.getText(0, pos);
                final ParserState ps = new ParserState(txt, ParserState.COMPLETION);
                new ExpressionParser().parse(ps);
                final ArrayList<String> compls = ps.getCompletions();
                switch (compls.size()) {
                case 0:
                    Toolkit.getDefaultToolkit().beep();
                    break;
                case 1: {
                    final String comp = compls.get(0);
                    int i;
                    final int l = comp.length();
                    for (i = 0; i < l; i++) {
                        if (txt.endsWith(comp.substring(0, l - i)))
                            break;
                    }
                    final String s = (i == l && txt.charAt(txt.length() - 1) != ' ') ? " "
                            : "";
                    doc.insertString(txt.length(),
                            s + comp.substring(comp.length() - i) + " ", null);
                    break;
                }
                default: {
                    final JPopupMenu jpm = new JPopupMenu();
                    for (final String compl : compls) {
                        jpm.add(compl).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                final String comp = ((JMenuItem) e.getSource())
                                        .getText();
                                final int l = comp.length();
                                for (int j = 0; j <= l; j++) {
                                    if (txt.endsWith(comp.substring(0, l - j))) {
                                        try {
                                            final String s = (j == 0 && txt
                                                    .charAt(txt.length() - 1) != ' ') ? " "
                                                    : "";
                                            doc.insertString(
                                                    txt.length(),
                                                    s
                                                            + comp.substring(comp
                                                                    .length()
                                                                    - j) + " ",
                                                    null);
                                        } catch (final BadLocationException e1) {
                                            // TODO Auto-generated catch block
                                            e1.printStackTrace();
                                        }
                                        break;
                                    }
                                }
                            }
                        });
                    }
                    Point p = ed.getCaret().getMagicCaretPosition();
                    if (p == null)
                        p = new Point(0, 0);
                    jpm.show(RuleComponent.this, p.x, p.y);
                    break;
                }
                }
            } catch (final BadLocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    public ParsedExpression getParsedExpression() {
        // TODO Auto-generated method stub
        ParsedExpression expr = null;
        try {
            if (doc.getLength() == 0)
                return null;
            final ParserState ps = new ParserState(doc.getText(0, doc.getLength()));
            expr = new ExpressionParser().parse(ps);
        } catch (final BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return expr;
    }

    public String getText() {
        try {
            return doc.getText(0, doc.getLength());
        } catch (final BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
