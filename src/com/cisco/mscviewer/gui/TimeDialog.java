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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener; //property change stuff

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.cisco.mscviewer.model.Unit;

@SuppressWarnings("serial")
class TimeDialog extends JDialog implements ActionListener,
        PropertyChangeListener {
    // private JTextField tsScaleTf;
    private final JComboBox<Unit> tsInputUnitCB;
    private final JComboBox<Unit> tsAbsUnitCB;
    private final JOptionPane optionPane;
    private final JComboBox<Unit> tsDeltaUnitCB;
    private final String btnEnterStr = "Enter";
    private final String btnCancelStr = "Cancel";
    private int timeFactor;
    private boolean approved;

    public TimeDialog(JFrame aFrame, long initTimeFactor, Unit abs, Unit delta) {
        super(aFrame, true);
        setTitle("Quiz");

        // Create an array of the text and components to be displayed.
        final String tsScaleStr = "Input timestamp unit";
        // tsScaleTf = new JTextField(""+initTimeFactor);
        tsInputUnitCB = new JComboBox<Unit>(Unit.values());
        final String tsAbsUnitStr = "Output absolute time unit";
        tsAbsUnitCB = new JComboBox<Unit>(Unit.values());
        tsAbsUnitCB.setSelectedItem(abs);
        final String tsDeltaUnitStr = "Output time-delta unit";
        tsDeltaUnitCB = new JComboBox<Unit>(Unit.values());
        tsDeltaUnitCB.setSelectedItem(delta);
        final Object[] array = { tsScaleStr, tsInputUnitCB, tsAbsUnitStr,
                tsAbsUnitCB, tsDeltaUnitStr, tsDeltaUnitCB };

        // Create an array specifying the number of dialog buttons
        // and their text.
        final Object[] options = { btnEnterStr, btnCancelStr };

        // Create the JOptionPane.
        optionPane = new JOptionPane(array, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, options, options[0]);

        // Make this dialog display it.
        setContentPane(optionPane);

        // Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                /*
                 * Instead of directly closing the window, we're going to change
                 * the JOptionPane's value property.
                 */
                optionPane.setValue(JOptionPane.CLOSED_OPTION);
            }
        });

        // Ensure the text field always gets the first focus.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent ce) {
                // tsScaleTf.requestFocusInWindow();
            }
        });

        // Register an event handler that puts the text into the option pane.
        // tsScaleTf.addActionListener(this);

        // Register an event handler that reacts to option pane state changes.
        optionPane.addPropertyChangeListener(this);
        pack();
    }

    /** This method handles events for the text field. */
    @Override
    public void actionPerformed(ActionEvent e) {
        optionPane.setValue(btnEnterStr);
    }

    /** This method reacts to state changes in the option pane. */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        final String prop = e.getPropertyName();

        if (isVisible()
                && (e.getSource() == optionPane)
                && (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY
                        .equals(prop))) {
            final Object value = optionPane.getValue();

            if (value == JOptionPane.UNINITIALIZED_VALUE) {
                // ignore reset
                return;
            }

            // Reset the JOptionPane's value.
            // If you don't do this, then if the user
            // presses the same button next time, no
            // property change event will be fired.
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

            if (btnEnterStr.equals(value)) {
                // String txt = tsScaleTf.getText();
                // try {
                // timeFactor = Integer.parseInt(txt);
                clearAndHide();
                approved = true;
                // }catch(NumberFormatException ex) {
                // JOptionPane.showMessageDialog(this,
                // "Invalid scale factor. Must be an integer",
                // "Error",
                // JOptionPane.ERROR_MESSAGE);
                // tsScaleTf.requestFocusInWindow();
                // }
            } else { // user closed dialog or clicked cancel
                clearAndHide();
                approved = false;
            }
        }
    }

    /** This method clears the dialog and hides it. */
    public void clearAndHide() {
        // tsScaleTf.setText(null);
        setVisible(false);
    }

    public int getScaleFactor() {
        return timeFactor;
    }

    public Unit getInputUnit() {
        return (Unit) tsInputUnitCB.getSelectedItem();
    }

    public Unit getAbsoluteOutputUnit() {
        return (Unit) tsAbsUnitCB.getSelectedItem();
    }

    public Unit getDeltaOutputUnit() {
        return (Unit) tsDeltaUnitCB.getSelectedItem();
    }

    public boolean approved() {
        return approved;
    }
}
