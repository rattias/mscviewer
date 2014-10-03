/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jan 2012
 */
package com.cisco.mscviewer.gui;
import com.cisco.mscviewer.model.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

class EntityTreeModel implements TreeModel, MSCDataModelListener {
    private final String root = "Entities";
    private final Vector<TreeModelListener> listeners;
    private final MSCDataModel dm;

    public EntityTreeModel(MSCDataModel dm) {
        this.dm = dm;
        dm.addListener(this);
        listeners = new Vector<TreeModelListener>();
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (dm != null) {
            if (parent == root)
                return dm.getRootEntityAt(index);
            else
                return ((Entity)parent).getChildAt(index);
        } else
            return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (dm != null) {
            if (parent == root)
                return dm.getRootEntityCount();
            else
                return ((Entity)parent).getChildCount();
        } else
            return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        return (node == root)? false : ((Entity)node).getChildCount() == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // do nothing!
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (dm != null) {
            if (parent == root) {
                for(int i=0; i<dm.getEntityCount(); i++)
                    if (dm.getRootEntityAt(i) == child)
                        return i;
            } else {
                for(int i=0; i<((Entity)parent).getChildCount(); i++)
                    if (((Entity)parent).getChildAt(i) == child)
                        return i;

            }
        }
        return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.addElement(l);			
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.removeElement(l);			
    }

    public void fireTreeNodesInserted(TreeModelEvent e) {
        Enumeration<TreeModelListener> listenersCount = listeners.elements();
        while(listenersCount.hasMoreElements()) {
            TreeModelListener listener = listenersCount.nextElement();
            listener.treeNodesInserted(e);
        }
    }

    public void fireTreeNodesRemoved(TreeModelEvent e) {
        Enumeration<TreeModelListener> listenersCount = listeners.elements();
        while(listenersCount.hasMoreElements()) {
            TreeModelListener listener = listenersCount.nextElement();
            listener.treeNodesRemoved(e);
        }
    }

    public void fireTreeNodesChanged(TreeModelEvent e) {
        Enumeration<TreeModelListener> listenersCount = listeners.elements();
        while(listenersCount.hasMoreElements()) {
            TreeModelListener listener = listenersCount.nextElement();
            listener.treeNodesChanged(e);
        }
    }

    public void fireTreeStructureChanged(final TreeModelEvent e) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Enumeration<TreeModelListener> listenersCount = listeners.elements();
                while(listenersCount.hasMoreElements()) {
                    TreeModelListener listener = listenersCount.nextElement();
                    listener.treeStructureChanged(e);
                }
            }

        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private void buildPath(Entity en, Object o, ArrayList<Object> path) {
        path.add(o);
        if(o != en) {
            int cnt = getChildCount(o);
            for(int i=0; i<cnt; i++) {
                Object child = getChild(o, i);
                buildPath(en, child, path);
            }
        }
    }

    public void updateTreeForEntityChange(Entity en) {
        ArrayList<Object> arr = new ArrayList<Object>();
        Object o = getRoot();
        buildPath(en, o, arr);
        Object[] path = arr.toArray();
        TreeModelEvent ev = new TreeModelEvent(this, path);
        fireTreeNodesChanged(ev);
    }

    @Override
    public void entityAdded(MSCDataModel mscDataModel, Entity en) {
        Object o[] = new Object[1];
        o[0]= getRoot();
        TreeModelEvent ev = new TreeModelEvent(this, o);
        fireTreeStructureChanged(ev);
    }

    @Override
    public void eventAdded(MSCDataModel mscDataModel, Event ev) {
        // TODO Auto-generated method stub

    }

    @Override
    public void modelChanged(MSCDataModel mscDataModel) {
        Object o[] = new Object[1];
        o[0]= getRoot();
        TreeModelEvent ev = new TreeModelEvent(this, o);
        fireTreeStructureChanged(ev);
    }

    @Override
    public void eventsChanged(MSCDataModel mscDataModel) {
        // TODO Auto-generated method stub

    }
}


@SuppressWarnings("serial")
class EntityTree extends JTree implements EntityHeaderModelListener {
    private final ViewModel eh;

    @Override
    public void entityMoved(ViewModel eh, Entity en, int toIdx) {       
    }

    class EntityTreeRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            Entity en = null;
            if (value instanceof Entity ) {
                en = (Entity)value;
                if (en.getDescription() != null)
                    value = "<HTML>"+en.getName()+" <i>"+en.getDescription()+"</i></HTML>";
            }

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);
            if (en != null) {
                if (en.hasEvents()) {
                    if (eh.indexOf(en) != -1) {
                        setForeground(Color.red);
                    } else
                        setForeground(Color.black);
                } else {
                    setForeground(Color.lightGray);
                }
            }
            return this;
        }
    }

    private void flipNodeState(TreePath tp) {
        if (tp != null) {
            Object o  = tp.getLastPathComponent();
            if (o instanceof Entity) {
                Entity en = (Entity)o;
                if (en.hasEvents()) {
                    if (eh.indexOf(en) == -1) {
                        eh.add(en);
                    } else {
                        eh.remove(en);
                    }
                    repaint();
                }
            }
        }
    }

    public EntityTree(ViewModel eh) {
        super(new EntityTreeModel(eh.getMSCDataModel()));
        this.eh = eh;
        eh.addListener(this);
        setCellRenderer(new EntityTreeRenderer());
        setToggleClickCount(0);
        getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "flipState");
        getActionMap().put("flipState", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath tp[] = EntityTree.this.getSelectionPaths();
                for (TreePath tp1 : tp) {
                    flipNodeState(tp1);
                }
            }			
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() ==2) {
                    TreePath tp = getPathForLocation(me.getX(), me.getY());
                    flipNodeState(tp);
                }
            }
        });
    }

    @Override
    public String convertValueToText(Object value, boolean selected,
            boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        if (value instanceof Entity)
            return ((Entity)value).getName();
        else
            return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
    }
    public void setModels(MSCDataModel dm) {
        setModel(new EntityTreeModel(dm));
    }

    public void updateTreeForEntityChange(Entity en) {
        EntityTreeModel m = (EntityTreeModel )getModel();
        m.updateTreeForEntityChange(en);
    }

    @Override
    public void entityAdded(ViewModel eh, Entity en, int idx) {
        updateTreeForEntityChange(en);

    }

    @Override
    public void entityRemoved(ViewModel eh, Entity en, int idx) {
        updateTreeForEntityChange(en);

    }

    @Override
    public void entitySelectionChanged(ViewModel eh, Entity en, int idx) {
    }

    @Override
    public int getEntityHeaderModelNotificationPriority() {
        return 0;
    }

    @Override
    public void boundsChanged(ViewModel entityHeaderModel, Entity en,
            int idx) {
        // TODO Auto-generated method stub

    }

}
