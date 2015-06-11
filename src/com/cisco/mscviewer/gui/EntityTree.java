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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.EntityHeaderModelListener;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.MSCDataModelListener;
import com.cisco.mscviewer.model.ViewModel;
import com.cisco.mscviewer.util.Resources;

class EntityTreeModel implements TreeModel, MSCDataModelListener {
    private final String root = "Entities";
    private final Vector<TreeModelListener> listeners;
    private final MSCDataModel dm;
    private HashMap<String, ArrayList<Entity>> sortedModel;

    public EntityTreeModel(MSCDataModel dm) {
        this.dm = dm;
        dm.addListener(this);
        listeners = new Vector<TreeModelListener>();
    }

    @Override
    public Object getRoot() {
        return root;
    }

    public ArrayList<Entity> getSortedModelChildren(String parentIdPath) {
        if (sortedModel == null)
            sortedModel = new HashMap<String, ArrayList<Entity>>();

        ArrayList<Entity> al = sortedModel.get(parentIdPath);
        if (al == null) {
            al = new ArrayList<Entity>();
            sortedModel.put(parentIdPath, al);
            if (parentIdPath == root) {
                for (int i = 0; i < dm.getRootEntityCount(); i++) {
                    al.add(dm.getRootEntityAt(i));
                }
            } else {
                final Entity en = dm.getEntity(parentIdPath);
                if (en == null) {
                    throw new Error("Entity " + parentIdPath + " not found");
                }
                for (int i = 0; i < en.getChildCount(); i++) {
                    al.add(en.getChildAt(i));
                }
            }
            Collections.sort(al, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    return ((Entity) o1).getName().compareTo(
                            ((Entity) o2).getName());
                }
            });
        }
        return al;
    }

    public Object getSortedModelElement(String parentIdPath, int index) {
        return getSortedModelChildren(parentIdPath).get(index);
    }

    @Override
    public Object getChild(Object parent, int index) {
        final String parentPath = parent instanceof Entity ? ((Entity) parent)
                .getId() : (String) parent;
        if (dm != null) {
            return getSortedModelElement(parentPath, index);
        } else
            return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (dm != null) {
            final String parentPath = parent instanceof Entity ? ((Entity) parent)
                    .getId() : (String) parent;
            return getSortedModelChildren(parentPath).size();
        } else
            return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        return (node == root) ? false : ((Entity) node).getChildCount() == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // do nothing!
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (dm != null) {
            final String parentPath = parent instanceof Entity ? ((Entity) parent)
                    .getId() : (String) parent;
            final ArrayList<Entity> al = getSortedModelChildren(parentPath);
            return al.indexOf(child);
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
        sortedModel = null;
        final Enumeration<TreeModelListener> listenersCount = listeners.elements();
        while (listenersCount.hasMoreElements()) {
            final TreeModelListener listener = listenersCount.nextElement();
            listener.treeNodesInserted(e);
        }
    }

    public void fireTreeNodesRemoved(TreeModelEvent e) {
        sortedModel = null;
        final Enumeration<TreeModelListener> listenersCount = listeners.elements();
        while (listenersCount.hasMoreElements()) {
            final TreeModelListener listener = listenersCount.nextElement();
            listener.treeNodesRemoved(e);
        }
    }

    public void fireTreeNodesChanged(TreeModelEvent e) {
        final Enumeration<TreeModelListener> listenersCount = listeners.elements();
        while (listenersCount.hasMoreElements()) {
            final TreeModelListener listener = listenersCount.nextElement();
            listener.treeNodesChanged(e);
        }
    }

    public void fireTreeStructureChanged(final TreeModelEvent e) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                sortedModel = null;
                final Enumeration<TreeModelListener> listenersCount = listeners
                        .elements();
                while (listenersCount.hasMoreElements()) {
                    final TreeModelListener listener = listenersCount.nextElement();
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
        if (o != en) {
            final int cnt = getChildCount(o);
            for (int i = 0; i < cnt; i++) {
                final Object child = getChild(o, i);
                buildPath(en, child, path);
            }
        }
    }

    public void updateTreeForEntityChange(Entity en) {
        final ArrayList<Object> arr = new ArrayList<Object>();
        final Object o = getRoot();
        buildPath(en, o, arr);
        final Object[] path = arr.toArray();
        final TreeModelEvent ev = new TreeModelEvent(this, path);
        sortedModel = null;
        fireTreeNodesChanged(ev);
    }

    public void updateTreeForEntityRemoved(Entity parentEn, Entity en) {
        final ArrayList<Object> arr = new ArrayList<Object>();
        final Object o = getRoot();
        buildPath(parentEn, o, arr);
        final Object[] path = arr.toArray();
        final TreeModelEvent ev = new TreeModelEvent(this, path);
        sortedModel = null;
        fireTreeNodesChanged(ev);
    }

    @Override
    public void entityAdded(MSCDataModel mscDataModel, Entity en) {
        final Object o[] = new Object[1];
        o[0] = getRoot();
        final TreeModelEvent ev = new TreeModelEvent(this, o);
        sortedModel = null;
        fireTreeStructureChanged(ev);
    }

    @Override
    public void eventAdded(MSCDataModel mscDataModel, Event ev) {
        // TODO Auto-generated method stub

    }

    @Override
    public void modelChanged(MSCDataModel mscDataModel) {
        final Object o[] = new Object[1];
        o[0] = getRoot();
        final TreeModelEvent ev = new TreeModelEvent(this, o);
        sortedModel = null;
        fireTreeStructureChanged(ev);
    }

    @Override
    public void eventsChanged(MSCDataModel mscDataModel) {
        // TODO Auto-generated method stub

    }

}

@SuppressWarnings("serial")
public class EntityTree extends JTree implements EntityHeaderModelListener {
    private final ViewModel eh;

    @Override
    public void entityMoved(ViewModel eh, Entity en, int toIdx) {
    }

    class EntityTreeRenderer extends DefaultTreeCellRenderer {
        private final ImageIcon entityIcon = Resources.getImageIcon(
                "entity.png", "entity");
        private final ImageIcon entityFadedIcon = Resources.getImageIcon(
                "entity_faded.png", "entity");
        private final ImageIcon entityOpenIcon = Resources.getImageIcon(
                "entity_open.png", "entity");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            Entity en = null;
            if (value instanceof Entity) {
                en = (Entity) value;
                if (en.getDescription() != null)
                    value = "<HTML>" + en.getName() + " <i>"
                            + en.getDescription() + "</i></HTML>";
            }

            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                    leaf, row, hasFocus);
            if (en != null) {
                if (en.hasEvents()) {
                    setIcon(entityIcon);
                    if (eh.indexOf(en) != -1) {
                        setIcon(entityOpenIcon);
                    } else
                        setIcon(entityIcon);
                } else {
                    setIcon(entityFadedIcon);
                    // setForeground(Color.lightGray);
                }
            }
            return this;
        }
    }

    private void flipNodeState(TreePath tp) {
        if (tp != null) {
            final Object o = tp.getLastPathComponent();
            if (o instanceof Entity) {
                final Entity en = (Entity) o;
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
        super(new EntityTreeModel(MSCDataModel.getInstance()));
        this.eh = eh;
        eh.addListener(this);

        setCellRenderer(new EntityTreeRenderer());
        setToggleClickCount(0);
        setRowHeight(24);
        getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "flipState");
        getActionMap().put("flipState", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final TreePath tp[] = EntityTree.this.getSelectionPaths();
                if (tp != null) {
                    for (final TreePath tp1 : tp) {
                        flipNodeState(tp1);
                    }
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    final TreePath tp = getPathForLocation(me.getX(), me.getY());
                    flipNodeState(tp);
                }
            }
        });
    }

    @Override
    public String convertValueToText(Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof Entity)
            return ((Entity) value).getName();
        else
            return super.convertValueToText(value, selected, expanded, leaf,
                    row, hasFocus);
    }

    public void setModels(MSCDataModel dm) {
        setModel(new EntityTreeModel(dm));
    }

    public void updateTreeForEntityChange(Entity en) {
        final EntityTreeModel m = (EntityTreeModel) getModel();
        m.updateTreeForEntityChange(en);
    }

    public void updateTreeForEntityRemoved(Entity parentEn, Entity en) {
        final EntityTreeModel m = (EntityTreeModel) getModel();
        m.updateTreeForEntityRemoved(parentEn, en);
    }

    @Override
    public void entityAdded(ViewModel eh, Entity en, int idx) {
        updateTreeForEntityChange(en);

    }

    @Override
    public void entityRemoved(ViewModel eh, Entity parentEn, Entity en, int idx) {
        updateTreeForEntityRemoved(parentEn, en);

    }

    @Override
    public void entitySelectionChanged(ViewModel eh, Entity en, int idx) {
    }

    @Override
    public int getEntityHeaderModelNotificationPriority() {
        return 0;
    }

    @Override
    public void boundsChanged(ViewModel entityHeaderModel, Entity en, int idx) {
        // TODO Auto-generated method stub

    }

    public void expandAll() {
        final int cnt = getRowCount();
        for (int i = 0; i < cnt; i++)
            expandRow(i);
    }

}
