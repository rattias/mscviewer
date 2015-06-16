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
package com.cisco.mscviewer.model;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import com.cisco.mscviewer.gui.EntityHeader;
import com.cisco.mscviewer.util.Utils;

public class ViewModel implements MSCDataModelListener {

    class EntityInfo {
        Entity en;
        boolean selected;
        Component c;
        int birth, death;

        public EntityInfo(Entity en) {
            this.en = en;
            selected = false;
        }
    }

    // private MainPanel mainPanel;
    private final ArrayList<EntityHeaderModelListener> listeners;
    private final ArrayList<EntityInfo> ent;
    private final HashMap<Entity, EntityInfo> entSet;
    private final MSCDataModel dm;
    private int[] events;
    private int rightMargin;

    private MSCDataModelEventFilter filter;

    public ViewModel(MSCDataModel dm) {
        this.entSet = new HashMap<Entity, EntityInfo>();
        this.ent = new ArrayList<EntityInfo>();
        this.listeners = new ArrayList<EntityHeaderModelListener>();
        this.dm = dm;
        dm.addListener(this);
    }

    public void moveEntity(Entity en, int toIdx) {
        final int fromIdx = indexOf(en);
        if (fromIdx < 0)
            return;
        final EntityInfo ei = ent.get(fromIdx);
        ent.remove(fromIdx);
        ent.add(toIdx, ei);
        notifyEntityMoved(en, toIdx);
    }

    public Entity get(int i) {
        // sync on dm rather than this monitor to avoid deadlocks
        synchronized (dm) {
            return ent.get(i).en;
        }
    }

    public int indexOf(Entity en) {
        // sync on dm rather than this monitor to avoid deadlocks
        synchronized (dm) {
            final int sz = ent.size();
            for (int i = 0; i < sz; i++) {
                final EntityInfo ei = ent.get(i);
                if (ei.en == en)
                    return i;
            }
            return -1;
        }
    }

    public Dimension getEntityPreferredSize(int idx) {
        return ent.get(idx).c.getPreferredSize();
    }

    public void setEntityPreferredSize(int idx, Dimension d) {
        final EntityInfo ei = ent.get(idx);
        if (d.width == ei.c.getPreferredSize().width)
            return;
        final Dimension dmin = ei.c.getMinimumSize();
        final Dimension d1 = new Dimension(d);
        if (d.width < dmin.width)
            d1.width = dmin.width;
        ei.c.setPreferredSize(d1);
        notifyBoundsChanged(ei.en, idx);
    }

    public boolean contains(Entity en) {
        // sync on dm rather than this monitor to avoid deadlocks
        synchronized (dm) {
            return indexOf(en) != -1;
        }
    }

    public void add(Entity en) {
        // sync on dm rather than this monitor to avoid deadlocks.
        // sync is done by called add.
        add(ent.size(), en);
    }

    public void add(Entity[] en) {
        // sync on dm rather than this monitor to avoid deadlocks.
        // sync is done by called add.
        add(ent.size(), en);
    }

    public void add(int idx, Entity en) {
        // sync on dm rather than this monitor to avoid deadlocks
        if (!contains(en)) {
            synchronized (dm) {
                final EntityInfo ei = new EntityInfo(en);
                ent.add(idx, ei);
                entSet.put(en, ei);
                updateEvents();
            }
            notifyEntityAdded(en, idx);
        }
    }

    public void add(int idx, Entity en[]) {
        for (final Entity en1 : en) {
            synchronized (dm) {
                if (!contains(en1)) {
                    final EntityInfo ei = new EntityInfo(en1);
                    ent.add(idx, ei);
                    entSet.put(en1, ei);
                }
            }
        }
        // dm.updateFilteredEvents();
        updateEvents();
        for (int i = 0; i < en.length; i++) {
            notifyEntityAdded(en[i], idx + i);
        }
    }

    public void remove(Entity en) {
        // sync on dm rather than this monitor to avoid deadlocks
        // sync is done by called add.
        final int idx = indexOf(en);
        removeEntity(idx);
    }

    public void removeEntity(int idx) {
        // sync on dm rather than this monitor to avoid deadlocks
        EntityInfo ei;
        synchronized (dm) {
            ei = ent.remove(idx);
            // dm.updateFilteredEvents();
            updateEvents();
        }
        notifyEntityRemoved(ei.en.getParentEntity(), ei.en, idx);
    }

    public void reset() {
        for (int idx = ent.size() - 1; idx >= 0; idx--) {
            removeEntity(idx);
        }
        events = new int[0];
        // interInfo = new int[0];
    }

    public int entityCount() {
        // sync on dm rather than this monitor to avoid deadlocks
        synchronized (dm) {
            return ent.size();
        }
    }

    public void setSelected(Entity en, boolean v) {
        synchronized (dm) {
            Utils.trace(Utils.EVENTS, "called with (" + en.getName() + ", " + v);
            final EntityInfo ei = entityInfoForEntity(en);
            if (v == ei.selected) {
                Utils.trace(Utils.EVENTS, "was already " + (v ? "" : "de")
                        + "selected, no change.");
                return;
            }
            ei.selected = v;
        }
        Utils.trace(Utils.EVENTS, (v ? "" : "de")
                + "selecting and notifying...");
        notifySelectionChanged(en, indexOf(en));
    }

    public int indexOf(Event ev) {
        for (int i = 0; i < events.length; i++)
            if (dm.getEventAt(events[i]) == ev)
                return i;
        return -1;
    }

    public void addListener(EntityHeaderModelListener l) {
        if (!listeners.contains(l)) {
            final int pri = l.getEntityHeaderModelNotificationPriority();
            for (int i = 0; i < listeners.size(); i++)
                if (listeners.get(i).getEntityHeaderModelNotificationPriority() < pri) {
                    listeners.add(i, l);
                    return;
                }
            listeners.add(l);
        }

    }

    synchronized public void removeListener(EntityHeaderModelListener l) {
        listeners.remove(l);
    }

    private void notifyEntityAdded(final Entity en, final int idx) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                for (final EntityHeaderModelListener listener : listeners) {
                    listener.entityAdded(ViewModel.this, en, idx);
                }
            }
        });
    }

    public void notifyEntityMoved(final Entity en, final int toPos) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                for (final EntityHeaderModelListener listener : listeners) {
                    listener.entityMoved(ViewModel.this, en, toPos);
                }
            }
        });
    }

    public void notifyEntityRemoved(final Entity parentEn, final Entity en,
            final int idx) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                for (final EntityHeaderModelListener listener : listeners) {
                    listener.entityRemoved(ViewModel.this, parentEn, en, idx);
                }
            }
        });
    }

    private void notifySelectionChanged(final Entity en, final int idx) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                for (final EntityHeaderModelListener listener : listeners) {
                    listener.entitySelectionChanged(ViewModel.this, en, idx);
                }
            }
        });
    }

    private void notifyBoundsChanged(final Entity en, final int idx) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                for (final EntityHeaderModelListener listener : listeners) {
                    listener.boundsChanged(ViewModel.this, en, idx);
                }
            }
        });
    }

    private EntityInfo entityInfoForEntity(Entity en) {
        for (final EntityInfo ei : ent) {
            if (ei.en == en)
                return ei;
        }
        return null;
    }

    public Entity[] getSelectedEntities() {
        final ArrayList<Entity> ens = new ArrayList<Entity>();
        for (final EntityInfo ei : ent) {
            if (ei.selected)
                ens.add(ei.en);
        }
        return ens.toArray(new Entity[ens.size()]);
    }

    synchronized public void clearSelection() {
        final int sz = entityCount();
        for (int i = 0; i < sz; i++) {
            ent.get(i).selected = false;
            notifySelectionChanged(ent.get(i).en, i);
        }
    }

    synchronized public boolean isSelected(Entity entity) {
        return entityInfoForEntity(entity).selected;
    }

    public void setEntityComponent(int idx, Component c) {
        ent.get(idx).c = c;
    }

    public void setEntityBounds(int idx, Rectangle r) {
        EntityInfo ei;
        synchronized (dm) {
            ei = ent.get(idx);
            if (ei.c.getBounds().equals(r))
                return;
            ei.c.setLocation(r.x, r.y);
            ei.c.setBounds(r);
        }
        notifyBoundsChanged(ei.en, idx);
    }

    //
    // public void setPreferredEntitySize(int idx, Dimension d) {
    // System.out.println("ViewModel.setPreferredEntitySize(): "+d.width);
    // EntityInfo ei;
    // synchronized(dm) {
    // ei = ent.get(idx);
    // if (ei.c.getPreferredSize().equals(d))
    // return;
    // ei.c.setPreferredSize(d);
    // }
    // notifyBoundsChanged(ei.en, idx);
    // }

    // public void setEntityBounds(Entity en, Rectangle r) {
    // EntityInfo ei;
    // int idx;
    // synchronized(dm) {
    // idx = indexOf(en);
    // ei = ent.get(idx);
    // ei.r = r;
    // }
    // notifyBoundsChanged(ei.en, idx);
    // }

    public Rectangle getEntityBounds(int idx) {
        synchronized (dm) {
            return ent.get(idx).c.getBounds();
        }
    }

    public Dimension getEntitySize(int idx) {
        synchronized (dm) {
            return ent.get(idx).c.getSize();
        }
    }

    public int getEntityWidth(int entityIdx) {
        synchronized (dm) {
            if (entityIdx < 0 || entityIdx >= ent.size())
                return -1;
            // return ent.get(entityIdx).r.width;
            return ent.get(entityIdx).c.getWidth();
        }
    }

    public int getEntityWidth(Entity en) {
        if (en == null)
            return -1;
        synchronized (dm) {
            final EntityInfo ee = entSet.get(en);
            if (ee != null && ee.c != null)
                return ee.c.getWidth();
            else
                return -1;
        }
    }

    public int getEntityCenterX(int entityIdx) {
        synchronized (dm) {
            if (entityIdx >= ent.size())
                return 0;
            final Rectangle r = ent.get(entityIdx).c.getBounds();
            // System.out.println("centerX["+entityIdx+"] = "+(r.x+r.width/2));

            return r.x + r.width / 2;
        }
    }

    public int getTotalWidth() {
        synchronized (dm) {
            int w = 0;
            for (final EntityInfo ei : ent) {
                w += ei.c.getWidth();
            }
            return w+rightMargin;
        }
    }

    public void setEntityLocation(int idx, int x) {
        synchronized (dm) {
            final Rectangle r = getEntityBounds(idx);
            r.x = x;
            setEntityBounds(idx, r);
        }
    }

    public void setEntityLocation(Entity en, int x) {
        synchronized (dm) {
            final int cnt = ent.size();
            for (int i = 0; i < cnt; i++) {
                final EntityInfo ei = ent.get(i);
                if (ei.en == en) {
                    final Rectangle r = getEntityBounds(i);
                    r.x = x;
                    setEntityBounds(i, r);
                    break;
                }
            }
        }
    }

    @Override
    public void entityAdded(MSCDataModel mscDataModel, Entity en) {
    }

    @Override
    public void eventAdded(MSCDataModel mscDataModel, Event ev) {
    }

    @Override
    public void modelChanged(MSCDataModel mscDataModel) {
        while (!ent.isEmpty()) {
            final Entity en = ent.get(0).en;
            if (mscDataModel.getEntity(en.getId()) == null) {
                remove(en);
            }
        }
    }

    @Override
    public void eventsChanged(MSCDataModel mscDataModel) {
        // The entire set of events has changed, for example because a filter
        // has been applied.
        // we need to remove entities that are not in the filtered set of
        // events.
    }

    /**
     * called when entites are added/removed, or filter is changed/removed
     */
    private void updateEvents() {
        synchronized (dm) {
            for (final EntityInfo ent1 : ent) {
                ent1.birth = -1;
            }
            int sz = dm.getEventCount();
            final ArrayList<Integer> al = new ArrayList<Integer>();
            int viewIdx = 0;
            if (filter == null) {
                for (int i = 0; i < sz; i++) {
                    final Entity en = dm.getEventAt(i).getEntity();
                    final EntityInfo ei = entSet.get(en);
                    if (ei != null) {
                        al.add(i);
                        if (ei.birth < 0)
                            ei.birth = viewIdx;
                        ei.death = viewIdx;
                        viewIdx++;
                    }
                }
            } else {
                for (int i = 0; i < sz; i++) {
                    final Event ev = dm.getEventAt(i);
                    final Entity en = ev.getEntity();
                    final EntityInfo ei = entSet.get(en);
                    if (ei != null && filter.filter(ev)) {
                        al.add(i);
                        if (ei.birth < 0)
                            ei.birth = viewIdx;
                        ei.death = viewIdx;
                        viewIdx++;
                    }
                }
            }
            sz = al.size();
            events = new int[sz];
            for (int i = 0; i < sz; i++) {
                events[i] = al.get(i);
            }
        }
    }

    public void setFilter(MSCDataModelEventFilter filter) {
        this.filter = filter;
        updateEvents();
    }

    public MSCDataModelEventFilter getFilter() {
        return filter;
    }

    public int getEventCount() {
        return events.length;
    }

    public Event getEventAt(int idx) {
        return dm.getEventAt(events[idx]);
    }

    public int getModelIndexFromViewIndex(int fromIndex) {
        if (fromIndex == -1 || fromIndex >= events.length)
            return -1;
        return events[fromIndex];
    }

    public int getViewIndexFromModelIndex(int fromIndex) {
        if (fromIndex == -1)
            return -1;
        final int res = Arrays.binarySearch(events, fromIndex);
        return (res >= 0) ? res : -1;
    }

    public int getEntityBirthIndex(int idx) {
        final EntityInfo ei = ent.get(idx);
        return ei != null ? ei.birth : -1;
    }

    public int getEntityDeathIndex(int idx) {
        final EntityInfo ei = ent.get(idx);
        return ei != null ? ei.death : -1;
    }

    public int getFirstEventIndexForEntity(Entity en) {
        for (int i = 0; i < events.length; i++)
            if (dm.getEventAt(events[i]).getEntity() == en)
                return i;
        return -1;
    }

    public int getLastEventIndexForEntity(Entity en) {
        for (int i = events.length - 1; i >= 0; i--)
            if (dm.getEventAt(events[i]).getEntity() == en)
                return i;
        return -1;
    }

    public int getIndexForEvent(Event ev) {
        for (int i = 0; i < events.length; i++) {
            if (dm.getEventAt(events[i]) == ev)
                return i;
        }
        return -1;
    }
    
    public void setRighMarginWidth(int m) {
        rightMargin = m;
    }
    
    public int getRighMarginWidth() {
        return rightMargin;
    }
}
