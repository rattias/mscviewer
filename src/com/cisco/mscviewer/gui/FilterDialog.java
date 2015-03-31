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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener; //property change stuff
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

@SuppressWarnings("serial")
class FilterDialog extends JDialog
implements ActionListener,
PropertyChangeListener {
    private JTable table;
    private Vector<Vector<String>> filters;
    private Vector<Vector<String>> tmpFilters;
    private String btnEnterStr= "Ok";
    private String btnCancelStr = "Cancel";
    private boolean approved;
    JButton add, remove;
    private Vector<String> columns; 
    private JOptionPane optionPane;

    public  FilterDialog(JFrame aFrame, Vector<Vector<String>> filters) {
        super(aFrame, true);
        setTitle("Filters");
        this.filters = filters;
        tmpFilters = new Vector<Vector<String>>();
        for(int i=0; i<filters.size(); i++) {
            Vector<String> f = filters.get(i);
            Vector<String> newF = new Vector<String>();
            for(int j=0; j<f.size(); j++) {
                newF.add(f.elementAt(j));
            }
            tmpFilters.add(f);
        }

        JPanel c = new JPanel();
        c.setLayout(new BorderLayout());

        JTextArea jta = new JTextArea(
                "Define filters to limit the number of shown events." +
                "Once one of the filter is selected through the combo box above the diagram," +
        "Only events whose label satisfy the filter regular expression are shown.");
        jta.setEditable(false);
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);
        jta.setBackground(getBackground());
        jta.setBorder(new EtchedBorder());

        columns = new Vector<String>();
        columns.add("Name");
        columns.add("Regular Expression");
        table  = new JTable(tmpFilters, columns);
        //table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Set the first visible column to 100 pixels wide
        TableColumnModel cm  = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(1).setPreferredWidth(400);
        JScrollPane jsp = new JScrollPane(table);

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 5));
        add = new JButton("Add");
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultTableModel tm = (DefaultTableModel)table.getModel();
                int cnt = tm.getRowCount();
                tm.addRow(new String[]{"filter"+cnt, ".*"});
            }
        });
        remove = new JButton("Remove");
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int x = table.getSelectedRow();
                if (x >=0)
                    ((DefaultTableModel)table.getModel()).removeRow(x);
            }
        });
        buttons.add(new JPanel());
        buttons.add(add);
        buttons.add(new JPanel());
        buttons.add(remove);
        buttons.add(new JPanel());

        c.add(jta, BorderLayout.NORTH);
        c.add(jsp, BorderLayout.CENTER);
        c.add(buttons, BorderLayout.SOUTH);
        Object[] array = {c};

        //Create an array specifying the number of dialog buttons
        //and their text.
        Object[] options = {btnEnterStr, btnCancelStr};

        //Create the JOptionPane.
        optionPane = new JOptionPane(array,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                options,
                options[0]);

        //Make this dialog display it.
        setContentPane(optionPane);

        //Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                /*
                 * Instead of directly closing the window,
                 * we're going to change the JOptionPane's
                 * value property.
                 */
                optionPane.setValue(new Integer(
                        JOptionPane.CLOSED_OPTION));
            }
        });



        //Register an event handler that reacts to option pane state changes.
        optionPane.addPropertyChangeListener(this);
        pack();
    }

    /** This method handles events for the input unit combo. */
    public void actionPerformed(ActionEvent e) {
        //JRadioButton btn = (JRadioButton )e.getSource();
    }

    /** This method reacts to state changes in the option pane. */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (isVisible()
                && (e.getSource() == optionPane)
                && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
                        JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
            Object value = optionPane.getValue();

            if (value == JOptionPane.UNINITIALIZED_VALUE) {
                //ignore reset
                return;
            }

            //Reset the JOptionPane's value.
            //If you don't do this, then if the user
            //presses the same button next time, no
            //property change event will be fired.
            optionPane.setValue(
                    JOptionPane.UNINITIALIZED_VALUE);

            if (btnEnterStr.equals(value)) {
                clearAndHide();
                filters = tmpFilters;
                approved = true;
            } else { //user closed dialog or clicked cancel
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

    public Vector<Vector<String>> getFilters() {
        return filters;
    }

}
