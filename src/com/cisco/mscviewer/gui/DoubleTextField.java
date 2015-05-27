/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

@SuppressWarnings("serial")
class DoubleTextField extends JTextField {
    private final DecimalFormat zfFormat;
    private final DecimalFormat zfFormatE;
    private final double minNonExp;

    public DoubleTextField(double initVal, int decimalDigits) {
        super("" + initVal);
        String fmt = "####.";
        for (int i = 0; i < decimalDigits; i++)
            fmt += "#";
        zfFormat = new DecimalFormat(fmt);
        minNonExp = 1.0 / Math.pow(10, decimalDigits);
        fmt += "E0";
        zfFormatE = new DecimalFormat(fmt);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final double v = Double.parseDouble(getText());
                setValue(v);
            }
        });

    }

    @Override
    protected Document createDefaultModel() {
        return new DoubleTextDocument();
    }

    @Override
    public boolean isValid() {
        try {
            Double.parseDouble(getText());
            return true;
        } catch (final NumberFormatException e) {
            final String s = getText();
            if (!s.endsWith("."))
                return false;
            for (int i = 0; i < s.length() - 1; i++)
                if (s.charAt(i) < '0' || s.charAt(i) > '9')
                    return false;
            return true;
        } catch (final NullPointerException ex) {
            return true;
        }
    }

    public double getValue() {
        try {
            return Double.parseDouble(getText());
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    public void setValue(double zoomFactor) {
        if (zoomFactor < minNonExp)
            setText(zfFormatE.format(zoomFactor));
        else
            setText(zfFormat.format(zoomFactor));
    }

    class DoubleTextDocument extends PlainDocument {
        @Override
        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str == null)
                return;
            final String oldString = getText(0, getLength());
            final String newString = oldString.substring(0, offs) + str
                    + oldString.substring(offs);
            try {
                Double.parseDouble(newString + "0");
                super.insertString(offs, str, a);
            } catch (final NumberFormatException e) {
            }
        }
    }

}
