package com.cisco.mscviewer.gui.graph;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class CheckBoxList extends JList<EntityCheckBox> {
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public CheckBoxList() {
        setCellRenderer(new CellRenderer());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                final int index = locationToIndex(e.getPoint());

                if (index != -1) {
                    final EntityCheckBox checkbox = getModel().getElementAt(index);
                    checkbox.setSelected(!checkbox.isSelected());
                    repaint();
                }
            }
        });

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    protected class CellRenderer implements ListCellRenderer<JCheckBox> {
        @Override
        public Component getListCellRendererComponent(
                JList<? extends JCheckBox> list, JCheckBox value, int index,
                boolean isSelected, boolean cellHasFocus) {
            final JCheckBox checkbox = value;

            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(true);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager
                    .getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            return checkbox;
        }

    }
}