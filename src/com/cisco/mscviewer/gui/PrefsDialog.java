/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jan 2011
 */
package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener; //property change stuff

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import com.cisco.mscviewer.model.InputUnit;
import com.cisco.mscviewer.model.OutputUnit;

@SuppressWarnings("serial")
class PrefsDialog extends JDialog implements ActionListener,
        PropertyChangeListener {
    private final JOptionPane optionPane;
    private final JComboBox<InputUnit> tsDeltaUnitCB;
    private final String btnEnterStr = "Enter";
    private final String btnCancelStr;
    private int timeFactor;
    private boolean approved;
    JRadioButton[] inputChoice, outputChoice;
    private final JCheckBox showZeros, showUnits, compactView, showDate;

    public PrefsDialog(JFrame aFrame, long initTimeFactor, OutputUnit abs,
            InputUnit delta) {
        super(aFrame, true);
        this.btnCancelStr = "Cancel";
        setTitle("Options");
        setLayout(new GridLayout(3, 1));
        // Create an array of the text and components to be displayed.
        final JPanel inputP = new JPanel();
        int cnt = InputUnit.values().length;
        inputP.setLayout(new GridLayout(cnt / 4 + 1, 0));
        inputP.setBorder(new TitledBorder("Input Timestamp"));
        inputChoice = new JRadioButton[InputUnit.values().length];
        final ButtonGroup g1 = new ButtonGroup();
        int i = 0;
        for (final InputUnit unit : InputUnit.values()) {
            inputChoice[i] = new JRadioButton(unit.toString());
            if (unit == InputUnit.NS)
                inputChoice[i].setSelected(true);
            inputChoice[i].addActionListener(this);
            g1.add(inputChoice[i]);
            inputP.add(inputChoice[i]);
            i++;
        }

        final JPanel outputP = new JPanel();
        cnt = OutputUnit.values().length;
        outputP.setLayout(new GridLayout(cnt / 4 + 1, 0));
        outputP.setBorder(new TitledBorder("View Timestamp"));
        outputChoice = new JRadioButton[OutputUnit.values().length];
        final ButtonGroup g2 = new ButtonGroup();
        i = 0;
        for (final OutputUnit unit : OutputUnit.values()) {
            outputChoice[i] = new JRadioButton(unit.toString());
            if (unit == abs)
                outputChoice[i].setSelected(true);
            outputChoice[i].addActionListener(this);
            g2.add(outputChoice[i]);
            outputP.add(outputChoice[i]);
            i++;
        }
        showZeros = new JCheckBox("Show leading zeros");
        outputP.add(showZeros);
        showUnits = new JCheckBox("Show Units");
        outputP.add(showUnits);
        showDate = new JCheckBox("Show Date");
        outputP.add(showDate);
        tsDeltaUnitCB = new JComboBox<InputUnit>(InputUnit.values());
        tsDeltaUnitCB.setSelectedItem(delta);

        final JPanel eventP = new JPanel();
        eventP.setLayout(new BorderLayout());
        eventP.setBorder(new TitledBorder("Event Options"));
        compactView = new JCheckBox("CompactView");
        eventP.add(compactView);
        final Object[] array = { inputP, outputP, eventP };

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

        // Register an event handler that reacts to option pane state changes.
        optionPane.addPropertyChangeListener(this);
        pack();
    }

    /** This method handles events for the input unit combo. */
    @Override
    public void actionPerformed(ActionEvent e) {
        final JRadioButton btn = (JRadioButton) e.getSource();
        if (btn.isSelected())
            System.out.println("selected " + btn.getText());

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
                // timeFactor = Integer.parseInt(txt);
                clearAndHide();
                approved = true;
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

    public InputUnit getInputUnit() {
        for (int i = 0; i < inputChoice.length; i++)
            if (inputChoice[i].isSelected())
                return InputUnit.values()[i];
        return null;
    }

    public void setInputUnit(InputUnit timestampUnit) {
        for (final JRadioButton inputChoice1 : inputChoice) {
            if (inputChoice1.getText().equals(timestampUnit.toString())) {
                inputChoice1.setSelected(true);
            }
        }
    }

    public OutputUnit getAbsoluteOutputUnit() {
        for (int i = 0; i < outputChoice.length; i++)
            if (outputChoice[i].isSelected())
                return OutputUnit.values()[i];
        return null;
    }

    public void setAbsoluteOutputUnit(OutputUnit absTimeUnit) {
        for (final JRadioButton outputChoice1 : outputChoice) {
            if (outputChoice1.getText().equals(absTimeUnit.toString())) {
                outputChoice1.setSelected(true);
            }
        }
    }

    public InputUnit getDeltaOutputUnit() {
        return (InputUnit) tsDeltaUnitCB.getSelectedItem();
    }

    public void setDeltaOutputUnit(InputUnit deltaTimeUnit) {
        tsDeltaUnitCB.setSelectedItem(deltaTimeUnit);
    }

    public boolean approved() {
        return approved;
    }

    public void setShowUnits(boolean v) {
        showUnits.setSelected(v);
    }

    public boolean getShowUnits() {
        return showUnits.isSelected();
    }

    public void setShowDate(boolean v) {
        showDate.setSelected(v);
    }

    public boolean getShowDate() {
        return showDate.isSelected();
    }

    public void setShowLeadingZeroes(boolean v) {
        showZeros.setSelected(v);
    }

    public boolean getShowLeadingZeroes() {
        return showZeros.isSelected();
    }

    public boolean getCompactView() {
        return compactView.isSelected();
    }

    public void setCompactView(boolean b) {
        compactView.setSelected(b);
    }

}
