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

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.EntityHeaderModelListener;
import com.cisco.mscviewer.model.ViewModel;
import com.cisco.mscviewer.util.Utils;

@SuppressWarnings("serial")
class EntityListModel extends AbstractListModel<Entity> {
    private final String root = "Entities";
    private Vector<Entity> data;
    transient private final ViewModel eh;
    private boolean sorted;

    public EntityListModel(ViewModel eh) {
        this.eh = eh;
        data = new Vector<Entity>();
    }

    public ViewModel getEntityHeaderModel() {
        return eh;
    }

    public Object getRoot() {
        return root;
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public Entity getElementAt(int index) {
        return data.get(index);
    }

    public int getElementIndex(Object el) {
        return data.indexOf(el);
    }

    public void modelChanged() {
        final Vector<Entity> v = new Vector<Entity>();
        for (int i = 0; i < eh.entityCount(); i++) {
            v.add(eh.get(i));
        }
        if (sorted)
            Collections.sort(v, new Comparator<Entity>() {
                @Override
                public int compare(Entity o1, Entity o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
        data = v;
        fireContentsChanged(this, 0, getSize() - 1);
    }
}

@SuppressWarnings("serial")
class EntityList extends JList<Entity> implements EntityHeaderModelListener {

    public EntityList(ViewModel eh) {
        super(new EntityListModel(eh));
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "flipState");
        getActionMap().put("flipState", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final EntityListModel elm = (EntityListModel) getModel();
                final ViewModel ehm = elm.getEntityHeaderModel();
                final Entity sel[] = ehm.getSelectedEntities();
                for (final Entity sel1 : sel) {
                    ehm.remove(sel1);
                }
            }
        });

        getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        final EntityListModel elm = (EntityListModel) getModel();
                        final ViewModel ehm = elm.getEntityHeaderModel();
                        // sometimes we geta spurious event
                        for (int i = e.getFirstIndex(); i <= Math.min(
                                e.getLastIndex(), elm.getSize() - 1); i++) {
                            final boolean sel = isSelectedIndex(i);
                            final Entity en = elm.getElementAt(i);
                            final int idx = ehm.indexOf(en);
                            Utils.trace(Utils.EVENTS,
                                    "LIST idx " + en.getName()
                                            + ", model idx= " + idx
                                            + ",list idx=" + i + ",sel =" + sel);
                            if (idx >= 0)
                                ehm.setSelected(elm.getElementAt(i), sel);
                        }

                    }

                });
        // addMouseListener(new MouseAdapter() {
        // public void mouseClicked(MouseEvent me) {
        // if (me.getClickCount() ==2) {
        // TreePath tp = getPathForLocation(me.getX(), me.getY());
        // flipNodeState(tp);
        // }
        // }
        // });
    }

    @Override
    public void entityAdded(ViewModel eh, Entity en, int idx) {
        ((EntityListModel) getModel()).modelChanged();
    }

    @Override
    public void entityRemoved(ViewModel eh, Entity parentEn, Entity en, int idx) {
        ((EntityListModel) getModel()).modelChanged();
    }

    @Override
    public void entitySelectionChanged(ViewModel eh, Entity en, int idx) {
        Utils.trace(Utils.EVENTS, "entered");
        final EntityListModel elm = (EntityListModel) getModel();
        final int listIdx = elm.getElementIndex(en);
        if (listIdx < 0) {
            Utils.trace(Utils.EVENTS, "element " + en.getName()
                    + " is not in list!");
            return;
        }
        final boolean enSel = eh.isSelected(en);
        final boolean lsSel = (isSelectedIndex(listIdx));
        if (enSel != lsSel) {
            Utils.trace(Utils.EVENTS, "selecting " + en.getName() + ", "
                    + enSel);
            if (enSel) {
                setSelectedIndex(listIdx);
            } else {
                final int elidx = elm.getElementIndex(en);
                removeSelectionInterval(elidx, elidx);
            }
        }
    }

    @Override
    public int getEntityHeaderModelNotificationPriority() {
        return 0;
    }

    @Override
    public void boundsChanged(ViewModel entityHeaderModel, Entity en, int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void entityMoved(ViewModel eh, Entity en, int toIdx) {

    }

}
