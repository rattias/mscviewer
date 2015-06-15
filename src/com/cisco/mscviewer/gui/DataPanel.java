/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import com.cisco.mscviewer.io.JSonException;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.JSonArrayValue;
import com.cisco.mscviewer.model.JSonObject;
import com.cisco.mscviewer.model.JSonValue;

class TreeTableNodeModel {
    String fieldName;
    Object value;

    public TreeTableNodeModel(String key, Object value) {
        fieldName = key;
        this.value = value;
    }
}

/**
 * @author Roberto Attias
 * @since Aug 2012
 */
class TreeTableModel extends AbstractTreeTableModel {
    private final static String[] COLUMN_NAMES = { "Key", "Value" };

    public TreeTableModel(JSonValue obj) {
        super(new TreeTableNodeModel("ROOT", obj));
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    @Override
    public boolean isLeaf(Object node) {
        final Object value = ((TreeTableNodeModel) node).value;
        if (value instanceof JSonObject
                && ((JSonObject) value).getFieldCount() > 0)
            return false;
        if (value instanceof JSonArrayValue
                && ((JSonArrayValue) value).size() > 0)
            return false;
        return true;
    }

    @Override
    public int getChildCount(Object node) {
        int c;
        final TreeTableNodeModel o = (TreeTableNodeModel) node;
        if (o.value instanceof JSonObject
                && ((JSonObject) o.value).getFieldCount() > 0)
            c = ((JSonObject) o.value).getFieldCount();
        else if (o.value instanceof JSonArrayValue
                && ((JSonArrayValue) o.value).size() > 0)
            c = ((JSonArrayValue) o.value).size();
        else
            c = 0;
        return c;
    }

    @Override
    public Object getChild(Object node, int index) {
        final TreeTableNodeModel o = (TreeTableNodeModel) node;
        Object res = null;
        if (o.value instanceof JSonObject) {
            final String k = ((JSonObject) o.value).getKeys()[index];
            final JSonValue v = ((JSonObject) o.value).get(k);
            res = new TreeTableNodeModel(k, v);
        } else if (o.value instanceof JSonArrayValue) {
            final ArrayList<JSonValue> arr = ((JSonArrayValue) o.value).value();
            final String k = "[" + index + "]";
            final JSonValue v = arr.get(index);
            res = new TreeTableNodeModel(k, v);
        }
        return res;
    }

    @Override
    public int getIndexOfChild(Object node, Object child) {
        final TreeTableNodeModel o = (TreeTableNodeModel) node;
        final TreeTableNodeModel co = (TreeTableNodeModel) child;
        int res = -1;
        if (o.value instanceof JSonObject) {
            final JSonObject jo = (JSonObject) o.value;
            for (final Object v : jo.getValues()) {
                res++;
                if (v == co.value)
                    break;
            }
        }
        return res;
    }

    @Override
    public Object getValueAt(Object node, int column) {
        final TreeTableNodeModel o = (TreeTableNodeModel) node;
        switch (column) {
        case 0:
            return o.fieldName;
        case 1:
            if (o.value instanceof JSonObject) {
                final JSonObject jo = ((JSonObject) o.value);
                final int count = jo.getFieldCount();
                if (count == 0)
                    return "{} (empty)";
                else
                    return "{...} (" + count + " elements)";
            } else if (o.value instanceof JSonArrayValue) {
                final JSonArrayValue v = (JSonArrayValue) o.value;
                if (v.size() == 0)
                    return "[] (empty)";
                else
                    return "[...] (" + v.size() + " elements)";
            } else if (o == null || o.value == null)
                return "null";
            else
                return o.value.toString();
        }
        return null;
    }
}

/**
 *
 * @author rattias
 */
@SuppressWarnings("serial")
public class DataPanel extends JPanel implements SelectionListener {
    public static final String NAME = "Event Data";
    private JXTreeTable tree;

    public DataPanel() {
            setLayout(new BorderLayout());
            tree = new JXTreeTable(new TreeTableModel(null));
            this.add(new JScrollPane(tree), BorderLayout.CENTER);
            tree.setShowGrid(true);
            tree.setColumnMargin(1);
            tree.setRowMargin(1);
            tree.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            tree.setRootVisible(false);
    }

    public void setModel(JSonValue obj) {
        final TreeTableModel ttm = new TreeTableModel(obj);
        tree.setTreeTableModel(ttm);

    }

    @Override
    public void eventSelected(MSCRenderer renderer, Event selectedEvent,
            int viewEventIndex, int modelEventIndex) {
        final JSonValue o = (selectedEvent != null) ? selectedEvent.getData() : null;
        tree.setTreeTableModel(new TreeTableModel(o));
    }

    @Override
    public void interactionSelected(MSCRenderer renderer,
            Interaction selectedInteraction) {
    }
}
