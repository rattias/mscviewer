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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

@SuppressWarnings("serial")
class FindDialog extends JDialog implements PropertyChangeListener {
    private final JOptionPane optionPane;
    private final String btnEnterStr = "Ok";
    private final String btnCancelStr = "Cancel";
    private boolean approved;
    private final JScriptPanel jsp;

    public FindDialog(JFrame aFrame) {
        super(aFrame, true);
        setTitle("Find");
        jsp = new JScriptPanel();
        jsp.setScriptContext("function filter(label, type) {$$}");
        final Object[] array = { jsp };
        final Object[] options = { btnEnterStr, btnCancelStr };

        // Create the JOptionPane.
        optionPane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, options, options[0]);

        // Make this dialog display it.
        setContentPane(optionPane);
        // setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
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
        // Register an event handler that reacts to option pane state changes.
        optionPane.addPropertyChangeListener(this);
        pack();
    }

    /**
     * This method reacts to state changes in the option pane.
     * 
     * @param e
     */
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
                clearAndHide();
                // script = jsp.getScript();
                approved = true;
            } else { // user closed dialog or clicked cancel
                clearAndHide();
                approved = false;
            }
        }
    }

    /** This method clears the dialog and hides it. */
    public void clearAndHide() {
        setVisible(false);
    }

    public boolean approved() {
        return approved;
    }

}
