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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.util.PNGSnapshotTarget;
import com.cisco.mscviewer.util.Utils;
import com.cisco.mscviewer.script.PythonFunction;

@SuppressWarnings("serial")
class FunctionParametersDialog extends JDialog implements PNGSnapshotTarget {
    public final static int OK = 0;
    public final static int CANCEL = 1;
    private int value;

    class ArgPanel extends JPanel {
        private boolean validInput = true;

        public ArgPanel(
                String title, final String[] args, String defs[], String[] values, 
                final int offset, int count, JTextField[] tfarr, ActionListener al) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(title));
            Box vbox = Box.createVerticalBox();
            JScrollPane jsp = new JScrollPane(vbox) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    return d;
                }
            };
            add(jsp, BorderLayout.CENTER);
            boolean isOptional = offset >= args.length-defs.length;
            JLabel[] lab = new JLabel[count];
            Dimension maxLabelSize = new Dimension(0, 0);
            Dimension maxTFSize = new Dimension(Integer.MAX_VALUE, 0);
            vbox.add(Box.createVerticalStrut(5));
            vbox.add(Box.createVerticalGlue());
            for (int i=0; i<count; i++) {
                Box hbox = Box.createHorizontalBox();
                final String argName = args[offset+i];
                lab[i] = new JLabel(argName, JLabel.TRAILING);
                Dimension d = lab[i].getPreferredSize();
                dimmax(maxLabelSize, d); 
                final JTextField f = tfarr[offset+i] = new JTextField(20);
                d = f.getPreferredSize();
                dimmax(maxTFSize, d);
                hbox.add(Box.createHorizontalStrut(5));
                hbox.add(lab[i]);


                hbox.add(Box.createHorizontalStrut(5));
                hbox.add(f);
                final String currValue;
                final String def;
                if (isOptional) {
                    def = defs[i];
                    currValue = values[offset+i] == null ? def : values[offset+i];
                } else {
                    def = null;
                    currValue = values[offset+i];
                }
                if (isOptional) {
                    final JCheckBox cb = new JCheckBox();
                    boolean isDefault = currValue.equals(def);
                    cb.setSelected(! isDefault);
                    f.setEnabled(! isDefault);
                    cb.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (cb.isSelected()) {
                                f.setText(currValue);
                                f.setEnabled(true);                                
                            } else {
                                f.setText(def);
                                f.setEnabled(false);
                            }
                        }
                    });
                    hbox.add(cb);
                    hbox.add(Box.createHorizontalStrut(5));
                } else              
                    hbox.add(Box.createHorizontalStrut(5));
                vbox.add(hbox);
                vbox.add(Box.createVerticalGlue());
                vbox.add(Box.createVerticalStrut(5));
                f.setDocument(new SpecialDoc(al));
                f.setText(currValue);
            }
            for (int i=0; i<count; i++) {
                lab[i].setPreferredSize(maxLabelSize);
                tfarr[offset+i].setMaximumSize(maxTFSize);
            }
            jsp.setPreferredSize(new Dimension(jsp.getPreferredSize().width, 3*(maxTFSize.height+5)));
            //jsp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3*(maxTFSize.height+5)));
        }

        private void dimmax(Dimension d1, Dimension d2) {
            if (d2.width > d1.width)
                d1.width = d2.width;
            if (d2.height > d1.height)
                d1.height = d2.height;
        }

        public boolean hasValidInput() {
            return validInput;
        }

    }

    class SpecialDoc extends PlainDocument {
        private final ActionListener l;

        public SpecialDoc(ActionListener l) {
            this.l = l;
        }

        @Override
        public void remove(int off, int len) throws BadLocationException {
            super.remove(off, len);
            l.actionPerformed(null);
        }

        @Override
        public void insertString (int off, String str, AttributeSet attr) throws BadLocationException {
            super.insertString (off, str, attr);
            l.actionPerformed(null);
        }    
    }

    private final PythonFunction fun;

    public FunctionParametersDialog(PythonFunction fun) {
        super(MainFrame.getInstance(), true);
        this.fun = fun; 
    }


    public int open() {
        final ArgPanel mandatory, optional;
        String[] args = fun.getArgNames();
        String[] def = fun.getArgDefaults();
        String[] vals = fun.getArgValues();
        final JButton ok = new JButton("Ok");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FunctionParametersDialog.this.setValue(OK);
                setVisible(false);
            }           
        });
        final JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Main.extra) {
                    Utils.getPNGSnapshot(FunctionParametersDialog.this);
                }
                FunctionParametersDialog.this.setValue(CANCEL);
                setVisible(false);
            }
        });
        JPanel okCancPanel = new JPanel();
        okCancPanel.add(ok);
        okCancPanel.add(cancel);

        int regArgCount = args.length-def.length;
        final JTextField[] tfarr = new JTextField[args.length];
        if (regArgCount>0 || def.length>0) {
            JPanel docAndParms= new JPanel();                            
            docAndParms.setLayout(new BorderLayout());
            ActionListener al = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (JTextField tfarr1 : tfarr) {
                        if (tfarr1 != null) {
                            String txt = tfarr1.getText(); 
                            if (txt == null || txt.isEmpty()) {
                                ok.setEnabled(false);
                                return;
                            }
                        }
                    }
                    ok.setEnabled(true);

                }
            };
            JTextArea doc = new JTextArea();
            doc.setEditable(false);
            doc.setText(fun.getDoc());
            JPanel docP = new JPanel();
            docP.setLayout(new BorderLayout());
            docP.add(new JScrollPane(doc), BorderLayout.CENTER);
            docP.setBorder(BorderFactory.createTitledBorder("Description"));
            Dimension d = docP.getPreferredSize();
            docP.setPreferredSize(new Dimension(d.width, 100));
            int cnt = 1;
            if (regArgCount >0) {
                mandatory = new ArgPanel("Mandatory Arguments", args, def, vals, 0, regArgCount, tfarr, al);
                cnt++;
            } else 
                mandatory = null;
            if (def.length>0) {
                optional = new ArgPanel("Optional Arguments", args, def, vals, regArgCount, def.length, tfarr, al);
                cnt++;
            } else 
                optional = null;
            docAndParms.setLayout(new GridLayout(cnt, 1));
            docAndParms.add(docP);
            if (mandatory != null)
                docAndParms.add(mandatory);
            if (optional != null) 
                docAndParms.add(optional);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    setValue(CANCEL);
                }
            });
            setTitle("Specify arguments for function "+fun.getName()+"()");
            JPanel cp = (JPanel)getContentPane();
            cp.setLayout(new BorderLayout());
            cp.add(docAndParms, BorderLayout.CENTER);
            cp.add(okCancPanel, BorderLayout.SOUTH);
            pack();
            Window w = getOwner();
            Rectangle r = w.getBounds();
            int x = r.x + (r.width-getWidth())/2;
            int y = r.y + (r.height-getHeight())/2;
            setLocation(x, y);
            setVisible(true);
            if (value == CANCEL)
                return value;
            for(int i=0; i<args.length; i++) {
                String v = tfarr[i].getText();
                fun.setArgValue(args[i], v);
            }
            return OK;
        } else {
            JOptionPane.showMessageDialog(MainFrame.getInstance(), "No parameters to configure");
            return CANCEL;
        }
    }

    private void setValue(int v) {
        value = v;
    }

    @Override
    public Component getPNGSnapshotClient() {
        return null;
    }
}
