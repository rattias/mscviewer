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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener; //property change stuff

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import com.cisco.mscviewer.gui.colorpicker.ColorPicker;
import com.cisco.mscviewer.gui.colorpicker.TextColorPicker;
import com.cisco.mscviewer.model.InputUnit;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.OutputUnit;
import com.cisco.mscviewer.util.PersistentPrefs;

@SuppressWarnings("serial")
class PrefsDialog extends JDialog implements ActionListener,
PropertyChangeListener {
    private final JOptionPane optionPane;
    private final String btnResetStr = "Reset";
    private final String btnEnterStr = "Ok";
    private final String btnCancelStr;
    private  JTextArea help;
    private boolean approved;
    JRadioButton[] inputChoice, dateOutputChoice, timeOutputChoice;
    private JCheckBox showUnits;
    private JCheckBox showWeekday;
    private JCheckBox showFullYear;
    private JCheckBox showParentPath;
    private JCheckBox showDescription;
    private JCheckBox showID;
    private JTabbedPane tabbedPane;

    private String helpText;
    private PersistentPrefs prefs;
    private TextColorPicker lpBackground;
    private TextColorPicker lpForeground;
    private TextColorPicker lpSelBackground;
    private TextColorPicker lpCopySelBackground;
    private TextColorPicker lpNumBackground;
    private TextColorPicker lpNumForeground;
    private TextColorPicker evenEventBackground;
    private TextColorPicker lifeLines;
    private TextColorPicker labelColor;
    private TextColorPicker oddEventBackground;
    private TextColorPicker timestampColor;
    private TextColorPicker interactionColor;

    private static String inputHelp = 
            "This group of controls defines how to interpret values for the \"time\" keyword associated to events" +
                    " when such values don't specify an explicit unit. The value is assumed to be the number of" +
                    " the specified time units elapsed since the epoch.\n\n";
    private static String outputTimeHelp = 
            "This group of controls defines how to format time associated to events in the sequence diagram.\n\n";

    private static String outputDateHelp = 
            "This group of controls defines how to format date associated to events in the sequence diagram.\n\n";

    private void setHelp(String s) {
        helpText = s;
        updateHelp();
    }

    private void updateHelp() {
        switch(tabbedPane.getSelectedIndex()) {
        case 0:
            MSCDataModel m = MSCDataModel.getInstance();
            long ns;
            if (m != null && m.getEventCount() > 0)
                ns = m.getEventAt(0).getTimestamp();
            else
                ns = System.currentTimeMillis()*1000000;
            help.setText(helpText+ "Current Format: "+getOutputUnit().format(ns));
            break;
        case 1:
            break;
        }

    }

    public PrefsDialog(JFrame aFrame, PersistentPrefs prefs) {
        super(aFrame, true);
        this.prefs = prefs;
        this.btnCancelStr = "Cancel";
        setTitle("Preferences");
        setLayout(new GridLayout(3, 1));
        // Create an array of the text and components to be displayed.
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(" Time ", createTimeTab());
        tabbedPane.addTab(" Entity ", createEntityTab());
        tabbedPane.addTab(" Colors ", createColorTab());
        updateGUI();
        // Create help area
        JPanel helpP = new JPanel();
        helpP.setLayout(new BorderLayout());
        help = new JTextArea();
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setRows(10);
        help.setEditable(false);
        helpP.add(help, BorderLayout.CENTER);
        helpP.setBorder(BorderFactory.createTitledBorder("Help"));    

        final Object[] elements = { tabbedPane, helpP};
        // Create an array specifying the number of dialog buttons
        // and their text.
        final Object[] options = { btnResetStr, btnEnterStr, btnCancelStr };


        // Create the JOptionPane.
        optionPane = new JOptionPane(elements, JOptionPane.QUESTION_MESSAGE,
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
        j         */
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
        updateHelp();        
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
                prefs.persist();
                clearAndHide();
                approved = true;
            } else if (btnResetStr.equals(value)) {
                prefs.restore();
                updateGUI();
            } else{ // user closed dialog or clicked cancel
                clearAndHide();
                prefs.restore();
                approved = false;
            }
        }
    }

    private void addMouseListenerRecursive(Component c, MouseListener m) {
        c.addMouseListener(m);
        if (c instanceof Container) {
            Container cont = (Container)c;
            for(Component subc : cont.getComponents())
                subc.addMouseListener(m);
        }
    }

    private void fillLine(JPanel p, int nCols) {
        int nEls = p.getComponentCount();
        int cnt = nEls % nCols;
        if (cnt != 0) 
            cnt = nCols - cnt;
        for(int i = 0; i<cnt; i++)
            p.add(new JPanel());
    }

    private JPanel createTimeTab() {
        final JPanel inputP = new JPanel();
        int cnt = InputUnit.values().length;
        inputP.setLayout(new GridLayout(cnt / 4 + 1, 0));
        inputP.setBorder(new TitledBorder("Input Timestamp"));
        inputChoice = new JRadioButton[InputUnit.values().length];
        final ButtonGroup g1 = new ButtonGroup();
        int i = 0;
        for (final InputUnit unit : InputUnit.values()) {
            inputChoice[i] = new JRadioButton(unit.toString());
            if (prefs.getTimeInputUnit() == unit)
                inputChoice[i].setSelected(true);
            inputChoice[i].addActionListener((e) -> { updateHelp(); prefs.setTimeInputUnit(unit);});
            g1.add(inputChoice[i]);
            inputP.add(inputChoice[i]);
            i++;
        }
        addMouseListenerRecursive(inputP, new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHelp(inputHelp);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                help.setText("");
            }            
        });

        final JPanel outputTimeP = new JPanel();
        cnt = OutputUnit.TimeMode.values().length;
        outputTimeP.setLayout(new GridLayout(0, 4));
        outputTimeP.setBorder(new TitledBorder("Time Format"));
        timeOutputChoice = new JRadioButton[OutputUnit.TimeMode.values().length];
        final ButtonGroup g2 = new ButtonGroup();
        i = 0;
        for (final OutputUnit.TimeMode unit : OutputUnit.TimeMode.values()) {
            timeOutputChoice[i] = new JRadioButton(unit.toString());
            if (prefs.getTimeOutputUnit().getTimeMode() == unit)
                timeOutputChoice[i].setSelected(true);
            timeOutputChoice[i].addActionListener((e) -> { 
                updateHelp();
                if (((JRadioButton)e.getSource()).isSelected()) {
                    OutputUnit ou = prefs.getTimeOutputUnit(); 
                    ou.setTimeMode(unit);
                    // cause notification to listeners
                    prefs.setTimeOutputUnit(ou);
                }
            });
            g2.add(timeOutputChoice[i]);
            outputTimeP.add(timeOutputChoice[i]);
            i++;
        }
        fillLine(outputTimeP, 4);
        showUnits = new JCheckBox("Show Units");
        showUnits.addActionListener((e) -> {
            updateHelp(); 
            OutputUnit ou = prefs.getTimeOutputUnit(); 
            if (showUnits.isSelected())
                ou.setFlags( ou.getFlags() | OutputUnit.TIME_UNIT);
            else
                ou.setFlags( ou.getFlags() & ~OutputUnit.TIME_UNIT);
            // cause notification to listeners
            prefs.setTimeOutputUnit(ou);
        });
        outputTimeP.add(showUnits);
        addMouseListenerRecursive(outputTimeP, new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHelp(outputTimeHelp);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                help.setText("");
            }            
        });

        final JPanel outputDateP = new JPanel();
        cnt = OutputUnit.DateMode.values().length;
        outputDateP.setLayout(new GridLayout(0, 4));
        outputDateP.setBorder(new TitledBorder("Date Format"));
        dateOutputChoice = new JRadioButton[OutputUnit.DateMode.values().length];
        final ButtonGroup g3 = new ButtonGroup();
        i = 0;
        for (final OutputUnit.DateMode unit : OutputUnit.DateMode.values()) {
            dateOutputChoice[i] = new JRadioButton(unit.toString());
            if (prefs.getTimeOutputUnit().getDateMode() == unit)
                dateOutputChoice[i].setSelected(true);
            dateOutputChoice[i].addActionListener((e) -> {
                OutputUnit ou = prefs.getTimeOutputUnit(); 
                ou.setDateMode(unit);
                // cause notification to listeners
                prefs.setTimeOutputUnit(ou);
            });
            g3.add(dateOutputChoice[i]);
            outputDateP.add(dateOutputChoice[i]);
            i++;
        }
        fillLine(outputDateP, 4);
        showWeekday = new JCheckBox("Show Weekday");
        showWeekday.addActionListener((e) -> {
            updateHelp(); 
            OutputUnit ou = prefs.getTimeOutputUnit(); 
            if (showWeekday.isSelected())
                ou.setFlags(ou.getFlags() | OutputUnit.WEEK_DAY);
            else
                ou.setFlags(ou.getFlags() & ~OutputUnit.WEEK_DAY);
            // cause notification to listeners
            prefs.setTimeOutputUnit(ou);
        });
        outputDateP.add(showWeekday);

        showFullYear= new JCheckBox("Show Full Year");
        showFullYear.addActionListener((e) -> {
            updateHelp(); 
            OutputUnit ou = prefs.getTimeOutputUnit(); 
            if (showFullYear.isSelected())
                ou.setFlags(ou.getFlags() | OutputUnit.LONG_YEAR);
            else
                ou.setFlags(ou.getFlags() & ~OutputUnit.LONG_YEAR);
            // cause notification to listeners
            prefs.setTimeOutputUnit(ou);
        });
        outputDateP.add(showFullYear);
        //        fillLine(outputDateP, 4);        
        //        ColorPicker cp = new TextColorPicker("Date Color", TextColorPicker.TYPE_FOREGROUND);
        //        outputDateP.add(cp);
        addMouseListenerRecursive(outputDateP, new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHelp(outputDateHelp);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                help.setText("");
            }            
        });
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(inputP);
        container.add(outputTimeP);
        container.add(outputDateP);
        container.add(Box.createVerticalGlue());
        return container;
    }

    private JPanel createEntityTab() {
        final JPanel headerP = new JPanel();
        headerP.setLayout(new GridLayout(0, 1));
        headerP.setBorder(new TitledBorder("Entity in Sequence Diagram"));
        showParentPath = new JCheckBox("Show Full Path");
        showParentPath.addActionListener((e) -> {
            updateHelp();
            prefs.setSHowEntityFullPath(showParentPath.isSelected());
        });
        headerP.add(showParentPath);
        showDescription = new JCheckBox("Show Description");
        showDescription.addActionListener((e) -> {
            updateHelp();
            prefs.setShowEntityDescription(showDescription.isSelected());            
        });
        headerP.add(showDescription);
        showID = new JCheckBox("Show ID rather than name");
        showID.addActionListener((e) -> {
            updateHelp();
            prefs.setShowEntityAsID(showID.isSelected());                        
        });
        headerP.add(showID);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(headerP);
        container.add(Box.createVerticalGlue());
        return container;
    }

    private JPanel createColorTab() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        final JPanel seqDiaP = new JPanel();
        seqDiaP.setLayout(new GridLayout(0, 2));
        seqDiaP.setBorder(new TitledBorder("Sequence Diagram"));
        oddEventBackground = new TextColorPicker("Background (odd events)", TextColorPicker.TYPE_BACKGROUND, "");
        oddEventBackground.addColorSelectionListener((c) -> prefs.setOddEventBackgroundColor(c)); 
        seqDiaP.add(oddEventBackground);

        evenEventBackground = new TextColorPicker("Background (even events)", TextColorPicker.TYPE_BACKGROUND, "");
        evenEventBackground.addColorSelectionListener((c) -> prefs.setEvenEventBackgroundColor(c)); 
        seqDiaP.add(evenEventBackground);

        lifeLines = new TextColorPicker("Life Lines", TextColorPicker.TYPE_BACKGROUND, "");
        lifeLines.addColorSelectionListener((c) -> prefs.setLifelineColor(c)); 
        seqDiaP.add(lifeLines);

        labelColor = new TextColorPicker("Event Labels", TextColorPicker.TYPE_BACKGROUND, "");
        labelColor.addColorSelectionListener((c) -> prefs.setEventLabelColor(c)); 
        seqDiaP.add(labelColor);

        timestampColor = new TextColorPicker("Timestamps", TextColorPicker.TYPE_BACKGROUND, "");
        timestampColor.addColorSelectionListener((c) -> prefs.setEventTimestampColor(c)); 
        seqDiaP.add(timestampColor);

        interactionColor = new TextColorPicker("Interaction (default)", TextColorPicker.TYPE_BACKGROUND, "");
        interactionColor.addColorSelectionListener((c) -> prefs.setDefaultInteractionColor(c)); 
        seqDiaP.add(interactionColor );

        container.add(seqDiaP);

        final JPanel logP = new JPanel();
        logP.setLayout(new GridLayout(0, 2));
        logP.setBorder(new TitledBorder("Log File Tab"));
        container.add(logP);

        lpBackground = new TextColorPicker("Default Background", TextColorPicker.TYPE_BACKGROUND, "");
        lpBackground .addColorSelectionListener((c) -> prefs.setLogFileBackgroundColor(c)); 
        logP.add(lpBackground );

        lpForeground = new TextColorPicker("Foreground", TextColorPicker.TYPE_BACKGROUND, "");
        lpForeground.setSelectedColor(prefs.getLogFileForegroundColor());
        lpForeground.addColorSelectionListener((c) -> prefs.setLogFileForegroundColor(c)); 
        logP.add(lpForeground );

        lpSelBackground = new TextColorPicker("Selected Event Background", TextColorPicker.TYPE_BACKGROUND, "");
        lpSelBackground .addColorSelectionListener((c) -> prefs.setLogFileSelectedBackgroundColor(c)); 
        logP.add(lpSelBackground );

        lpCopySelBackground = new TextColorPicker("Copy Background", TextColorPicker.TYPE_BACKGROUND, "");
        lpCopySelBackground .addColorSelectionListener((c) -> prefs.setLogFileCopyBackgroundColor(c)); 
        logP.add(lpCopySelBackground );

        lpNumBackground = new TextColorPicker("Line Number Background", TextColorPicker.TYPE_BACKGROUND, "");
        lpNumBackground .addColorSelectionListener((c) -> prefs.setLogFileLineNumberBackgroundColor(c)); 
        logP.add(lpNumBackground);

        lpNumForeground = new TextColorPicker("Line Number Foreground", TextColorPicker.TYPE_BACKGROUND, "");
        lpNumForeground.addColorSelectionListener((c) -> prefs.setLogFileLineNumberForegroundColor(c)); 
        logP.add(lpNumForeground);


        container.add(logP);

        container.add(Box.createVerticalGlue());
        return container;
    }

    private void updateGUI() {
        int i=0;
        for (final InputUnit unit : InputUnit.values()) {
            if (prefs.getTimeInputUnit() == unit)
                inputChoice[i].setSelected(true);
            i++;
        }
        i = 0;
        for (final OutputUnit.TimeMode unit : OutputUnit.TimeMode.values()) {
            if (prefs.getTimeOutputUnit().getTimeMode() == unit)
                timeOutputChoice[i].setSelected(true);
            i++;
        }
        i = 0;
        for (final OutputUnit.DateMode unit : OutputUnit.DateMode.values()) {
            if (prefs.getTimeOutputUnit().getDateMode() == unit)
                dateOutputChoice[i].setSelected(true);
            i++;
        }
        showUnits.setSelected((prefs.getTimeOutputUnit().getFlags() & OutputUnit.TIME_UNIT) != 0);
        showWeekday.setSelected((prefs.getTimeOutputUnit().getFlags() & OutputUnit.WEEK_DAY) != 0);
        showFullYear.setSelected((prefs.getTimeOutputUnit().getFlags() & OutputUnit.LONG_YEAR) != 0);
        showParentPath.setSelected(prefs.getShowEntityFullPath());
        showDescription.setSelected(prefs.getShowEntityDescription());
        showID.setSelected(prefs.getShowEntityAsID());
        oddEventBackground.setSelectedColor(prefs.getOddEventBackgroundColor());
        evenEventBackground.setSelectedColor(prefs.getEvenEventBackgroundColor());
        lifeLines.setSelectedColor(prefs.getLifelineColor());
        labelColor.setSelectedColor(prefs.getEventLabelColor());
        timestampColor.setSelectedColor(prefs.getEventTimestampColor());
        interactionColor.setSelectedColor(prefs.getDefaultInteractionColor());
        lpBackground.setSelectedColor(prefs.getLogFileBackgroundColor());
        lpSelBackground.setSelectedColor(prefs.getLogFileSelectedBackgroundColor());
        lpCopySelBackground.setSelectedColor(prefs.getLogFileCopyBackgroundColor());
        lpNumBackground .setSelectedColor(prefs.getLogFileLineNumberBackgroundColor());
        lpNumForeground.setSelectedColor(prefs.getLogFileLineNumberForegroundColor());
        MainFrame.getInstance().repaint();
        repaint();
    }

    /** This method clears the dialog and hides it. */
    public void clearAndHide() {
        // tsScaleTf.setText(null);
        setVisible(false);
    }

    public OutputUnit getOutputUnit() {
        OutputUnit.TimeMode tm = null;
        OutputUnit.DateMode dm = null;
        for (int i = 0; i < timeOutputChoice.length; i++)
            if (timeOutputChoice[i].isSelected()) {
                tm = OutputUnit.TimeMode.values()[i];
                break;
            }
        if (tm == null)
            throw new Error("No time was selected?");
        for (int i = 0; i < dateOutputChoice.length; i++)
            if (dateOutputChoice[i].isSelected()) {
                dm = OutputUnit.DateMode.values()[i];
                break;
            }
        if (dm == null)
            throw new Error("No date was selected?");
        int timeUnit = showUnits.isSelected() ? OutputUnit.TIME_UNIT : 0;
        int weekDay = showWeekday.isSelected() ? OutputUnit.WEEK_DAY : 0;
        int fullYear = showFullYear.isSelected() ? OutputUnit.LONG_YEAR: 0;
        return new OutputUnit(dm, tm, timeUnit | weekDay | fullYear);
    }



    public boolean approved() {
        return approved;
    }

}
