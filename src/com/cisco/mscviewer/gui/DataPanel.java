/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
package com.cisco.mscviewer.gui;
import com.cisco.mscviewer.io.JSonException;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.JSonObject;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

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
 * @since  Aug 2012
 */
class TreeTableModel extends AbstractTreeTableModel {
    private final static String[] COLUMN_NAMES = {"Key", "Value"};
    
    public TreeTableModel(JSonObject obj) {
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
        boolean b = ! (((TreeTableNodeModel)node).value instanceof JSonObject);
        return b;
    }
    
    @Override
    public int getChildCount(Object node) {
        int c;
        TreeTableNodeModel o = (TreeTableNodeModel)node;
        if (o.value instanceof JSonObject)
            c = ((JSonObject)o.value).getFieldCount();
        else
            c = 0;
        return c;
    }
    
    @Override
    public Object getChild(Object node, int index) {
        TreeTableNodeModel o = (TreeTableNodeModel)node;
        Object res = null;
        if (o.value instanceof JSonObject) {
            String k = ((JSonObject)o.value).getKeys()[index];
            Object v = ((JSonObject)o.value).get(k);
            res = new TreeTableNodeModel(k, v);
        }
        return res;
    }
    
    @Override
    public int getIndexOfChild(Object node, Object child) {
        TreeTableNodeModel o = (TreeTableNodeModel)node;
        TreeTableNodeModel co = (TreeTableNodeModel)child;
        int res = -1;
        if (o.value instanceof JSonObject) {
            JSonObject jo = (JSonObject)o.value;
            for(Object v: jo.getValues()) {
                res++;
                if (v == co.value)
                    break;
            }
        }
        return res;
    }
    
    @Override
    public Object getValueAt(Object node, int column) {
        TreeTableNodeModel o = (TreeTableNodeModel)node;
        switch (column) {
            case 0:
                return o.fieldName;
            case 1:
                if (o.value instanceof JSonObject)
                    return "";
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
    private JXTreeTable tree;
    
    public DataPanel() {
        try {
            JSonObject j = new JSonObject("{\"key\":\"value\", \"foo\":[1, 2], \"bar\":{\"x\":\"vx\", \"y\":true}}");
            setLayout(new BorderLayout());
            tree = new JXTreeTable(new TreeTableModel(j));
            this.add(new JScrollPane(tree), BorderLayout.CENTER);
            tree.setShowGrid(true);
            tree.setColumnMargin(1);
            tree.setRowMargin(1);
            tree.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);        
            tree.setRootVisible(false); 
        } catch (JSonException ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setModel(JSonObject obj) {
        TreeTableModel ttm = new TreeTableModel(obj);
        tree.setTreeTableModel(ttm);
        
    }

    @Override
    public void eventSelected(MSCRenderer renderer, Event selectedEvent, int viewEventIndex, int modelEventIndex) {
        JSonObject o = (selectedEvent != null)? selectedEvent.getData() : null;
        tree.setTreeTableModel(new TreeTableModel(o));
    }

    @Override
    public void interactionSelected(MSCRenderer renderer, Interaction selectedInteraction) {
    }
}
