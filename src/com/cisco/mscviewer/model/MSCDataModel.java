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
package com.cisco.mscviewer.model;

import com.cisco.mscviewer.util.Report;
import com.cisco.mscviewer.util.Utils;
import com.cisco.mscviewer.graph.GraphData;
import com.cisco.mscviewer.gui.graph.GraphPanel;
import com.cisco.mscviewer.gui.graph.GraphWindow;
import com.cisco.mscviewer.model.graph.TopologyGraph;
import com.cisco.mscviewer.model.graph.TopologyError;
import com.cisco.mscviewer.tree.AVLTreeNode;
import com.cisco.mscviewer.tree.InOrderAVLTreeNodeIterator;
import com.cisco.mscviewer.tree.Interval;
import com.cisco.mscviewer.tree.IntervalTree;
import com.cisco.mscviewer.tree.TreeIntegrityException;
import com.cisco.mscviewer.tree.Visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

class EventTimestampComparator implements Comparator<Event> {
    @Override
    public int compare(Event ev0, Event ev1) {
        // TODO Auto-generated metod stub
        return (int)(ev1.getTimestamp()-ev0.getTimestamp());
    }
}

class KeyEvent extends Event {
    public KeyEvent(long timestamp) {
        super(null, timestamp, null, null, 0, null, null);
    }
}
       
/**
 * An <code>MSCDataModel</code> contains {@link Entity}s, {@link Event}s and 
 * {@link Interaction}s for a data model. 
 * @author rattias
 */
public final class MSCDataModel {
    private final LinkedHashMap<String, Entity> entities = new LinkedHashMap<String, Entity>();
    private final ArrayList<Entity> rootEntities;
    private ArrayList<Event> events;
    private IntervalTree interactions;
    private IntervalTree blocks;
    private EventTimestampComparator eventTimestampComparator;
    private final ArrayList<String> data;
    private final Vector<MSCDataModelListener> listeners;
    //    private MSCDataModelEventFilter filter;
    private String path;
    private boolean notificationEnabled;
    private String openPath;
    private ArrayList<GraphData[]> graphs = new ArrayList<GraphData[]>(); 
    
    /**
     * Instantiate a data model
     */
    public MSCDataModel() {
        this.listeners = new Vector<MSCDataModelListener>();
        this.events = new ArrayList<Event>();
        this.rootEntities = new ArrayList<Entity>();
        this.data = new ArrayList<String>();
        this.interactions = new IntervalTree("interactions");
        this.blocks = new IntervalTree("blocks");

        reset();
    }

    /**
     * resets data model to initial state
     */
    public void reset() {
        entities.clear();
        rootEntities.clear();
        events.clear();
        interactions = new IntervalTree("interactions");
        blocks = new IntervalTree("blocks");
        notifyModelChanged();
        data.clear();
    }

    /**
     * sets the path of the input file this data model is generated from.
     * @param fname 
     */
    public void setFilePath(String fname) {
        this.path = fname;
    }

    public String getFilePath() {
        return path;
    }

    /**
     * Adds an {@link Entity} to the model. 
     * 
     * If an entity with the specified entityPath does not exists, then it
     * adds one. If it does, it just sets the displayName
     * 
     * @param entityPath the fully-qualified path of the entity
     * @param displayName the display name
     * @return 
     */
    public Entity addEntity(String entityPath, String displayName) {
        Entity en;
        synchronized(this) {
            en = entities.get(entityPath);
            if (en != null) {
                if (displayName != null)
                    en.setName(displayName);
                return en;
            }        
            String parentPath = Entity.getParentId(entityPath);
            if (parentPath != null) {
                Entity parent = entities.get(parentPath);
                if (parent == null) {
                    parent = addEntity(parentPath, null);
                }
                en = new Entity(entityPath, parent, displayName);
            } else {
                en = new Entity(entityPath, null, displayName);
                rootEntities.add(en);
            }
            entities.put(entityPath, en);
        }
        //notifyEntityAdded(en);
        return en;
    }
    
    /**
     * returns the number of entities in this model
     * @return 
     */
    public int getEntityCount() {
        return entities.size();
    }

    /**
     * returns the entity with the specified ID.
     * @param id
     * @return 
     */
    public Entity getEntity(String id) {
        return entities.get(id);
    }

    /**
     * returns the number of root entities.
     * @return 
     */
    public int getRootEntityCount() {
        return rootEntities.size();
    }

    /**
     * returns the <code>idx</code>-th top-level entity.
     * 
     * A top-level entity is an entity with null parent.
     * @param idx
     * @return 
     */
    public Entity getRootEntityAt(int idx) {
        return rootEntities.get(idx);
    }

    /**
     * returns an iterator on all entities in this model.
     * @return 
     */
    public Iterator<Entity> getEntityIterator(boolean rootOnly) {
        return rootOnly ? rootEntities.iterator() : entities.values().iterator();
    }

    /**
     * adds an event to the model. 
     * @param ev
     * @return the index of the event in the data model
     */
    public int addEvent(Event ev) {
        synchronized(this) {
            int idx = events.size();
            events.add(ev);
            Entity en = ev.getEntity();
            if (en.getFirstEventIndex() == -1)
                en.setFirstEventIndex(idx);
            en.setLastEventIndex(idx);
            ev.setIndex(idx);
            return idx;
        }
        //notifyEventAdded(ev);		
    }

        /**
     * returns the <code>idx</code>-th event in the data model.
     * @param idx
     * @return 
     */
    public Event getEventAt(int idx) {
        if (idx == -1)
            return null;
        return events.get(idx);
    }

//    public int getEventIndex(Event ev) {
//        return events.indexOf(ev);
//    }
    
    /**
     * returns the number of events in the data model.
     * @return 
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * returns the event with the specified timestamp
     * @param timestamp
     * @return 
     */
    public Event getEventWithTimestamp(long timestamp) {
        Event kev = new KeyEvent(timestamp);
        int idx = Collections.binarySearch(events, kev, eventTimestampComparator);
        if (idx >= 0)
            return events.get(idx);
        else
            return null;
    }

    /**
     * returns an iterator on all events contained in the 
     * specified range of timestamps
     * @param ts0
     * @param ts1
     * @return
     */
    public EventRange getEventRangeInTimeWindow(long ts0, long ts1) {
        Event k0 = new KeyEvent(ts0);
        Event k1 = new KeyEvent(ts1);
        int idx0 = Collections.binarySearch(events, k0, eventTimestampComparator);
        int idx1 = Collections.binarySearch(events, k1, eventTimestampComparator);
        return new EventRange(this, 
                idx0>=0 ? idx0 : -(idx0+1),
                        idx1>=0 ? idx1 : (-(idx1+1))-1);
    }




    
//    public void insertEvent(int newIdx, Event ev) {
//        synchronized(this) {
//            events.add(newIdx, ev);
//            ev.setIndex(newIdx);
//            if (ev.getRenderer() instanceof BirthRenderer)
//                ev.getEntity().setFirstEventIndex(newIdx);
//            if (ev.getRenderer() instanceof DeathRenderer)
//                ev.getEntity().setLastEventIndex(newIdx);
//            for(int i=newIdx; i<events.size(); i++)
//                events.get(i).setIndex(i);
//        }
//        //notifyEventAdded(ev);              
//    }

    /**
     * Adds an interaction to the data model
     * @param inter 
     */
    public void addInteraction(Interaction inter) {
        interactions.add(inter);
    }
    
    public void addBlock(Interval block) {
        blocks.add(block);        
    }

    
    /**
     * returns the number of interactions in this data model.
     * @return 
     */
    public int getInteractionCount() {
        return interactions.count();
    }

    
    public Iterator<Interaction> getInteractionIterator() {
        class InteractionIterator implements Iterator<Interaction> {
            private InOrderAVLTreeNodeIterator it;
            public InteractionIterator(InOrderAVLTreeNodeIterator it) {
                this.it = it;
            }
            
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Interaction next() {
                return (Interaction)it.next().getData();
            }

            @Override
            public void remove() {
                it.remove();
            }            
        }
        
        return new InteractionIterator(new InOrderAVLTreeNodeIterator(interactions));
    }
    
//    public void setEvent(int index, Event ev) {
//        events.set(index, ev);
//        ev.setIndex(index);
//    }
    /**
     * returns an array of interactions having <code>ev</code> as sink event.
     * @param ev
     * @return 
     */
    public Interaction[] getIncomingInteractions(Event ev) {
        return getIncomingInteractions(ev.getIndex());
    }

    /**
     * returns an array of interactions having <code>ev</code> as source event.
     * @param ev
     * @return 
     */
    public Interaction[] getOutgoingInteractions(Event ev) {
        return getOutgoingInteractions(ev.getIndex());
    }

    /**
     * returns an array of interactions having the event at the specified
     * index as source event.
     * @param fromEventIdx
     * @return 
     */
    public Interaction[] getOutgoingInteractions(int fromEventIdx) {
        ArrayList<Interval> al = new ArrayList<Interval>();
        interactions.getIntervalsWithStartBound(fromEventIdx, al);
        for(Iterator<Interval> it = al.iterator(); it.hasNext();) {
            Interval in = it.next();
            if (((Interaction)in).getFromIndex() == -1)
                it.remove();
        }
        return al.toArray(new Interaction[al.size()]);
    }

        
    /**
     * returns an array of interactions having the event at the specified
     * index as sink event.
     * @param toEventIdx
     * @return 
     */
    public Interaction[] getIncomingInteractions(int toEventIdx) {
        ArrayList<Interval> al = new ArrayList<Interval>();
        interactions.getIntervalsWithEndBound(toEventIdx, al);
        for(Iterator<Interval> it = al.iterator(); it.hasNext();) {
            Interval in = it.next();
            if (((Interaction)in).getToIndex() == -1)
                it.remove();
        }
        return al.toArray(new Interaction[al.size()]);
    }
    
    /**
     * returns an array of interactions whose source event has
     * index <= <code>modelIdx</code> and sink event has index
     * >= <code>modelIdx</code>.
     * For interactions whose source/sink is <code>null<code>, 
     * this method assumes the interaction source/sink index has the
     * same value as the corresponding sink/source. In other words, 
     * the interval of indices span by the Interaction is just one point.
     * @param modelIdx
     * @return 
     */
    public Interaction[] getInteractionsSurrounding(int modelIdx) {
        ArrayList<Interval> al = new ArrayList<Interval>();
        interactions.getContainingIntervals(modelIdx, al);
        return al.toArray(new Interaction[al.size()]);
    }

    /**
     * returns an array of interactions whose source event has
     * index <= <code>modelMinIdx</code> and sink event has index
     * >= <code>modelMaxIdx</code>.
     * For interactions whose source/sink is <code>null<code>, 
     * this method assumes the interaction source/sink index has the
     * same value as the corresponding sink/source. In other words, 
     * the interval of indices span by the Interaction is just one point.
     * @param modelMinIdx
     * @param modelMaxIdx
     * @return 
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Interaction> getInteractionsInInterval(int modelMinIdx, int modelMaxIdx) {
        ArrayList<Interval> al = new ArrayList<Interval>();
        interactions.getIntersectingIntervals(modelMinIdx, modelMaxIdx, al);
        // need to clone just to make generics happy!
        return (ArrayList<Interaction>)al.clone();
    }

    public ArrayList<Interval> getBlocksInInterval(int modelMinIdx, int modelMaxIdx) {
        boolean debug = true;
        ArrayList<Interval> al = new ArrayList<Interval>();
        blocks.getIntersectingIntervals(modelMinIdx, modelMaxIdx, al);
        if (debug) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for(Interval in : al) {
                if (in.getStart()<min)
                    min = in.getStart();
                if (in.getEnd()>max)
                    max = in.getEnd();
            }
        }
        return al;
    }

    
    /**
     * adds a line of the input source file to the model.
     * @param line 
     */
    public void addDataLine(String line) {
        data.add(line);		
    }

    /**
     * returns the list of lines of the input file.
     * @return 
     */
    public ArrayList<String> getData() {
        return data;
    }


    //    public MSCDataModelEventFilter getFilter() {
    //        return filter;
    //    }

    //    public void updateFilteredEvents() {
    //    	synchronized(this) {
    //	        if (filter != null) {
    //	            int sz = events.size();
    //	            filteredEvents = new ArrayList<Event>();
    //	            // reset all interaction indexes to -1
    //	            for(int i=0; i<sz; i++) {
    //	                Event ev = events.get(i);
    //	                if (filter.filter(ev)) {
    //	                    Interaction[] outgoing = ev.getOutgoingInteractions();
    //	                    Interaction incoming = ev.getIncomingInteraction();
    //	                    if (outgoing != null) {
    //	                        for(int j=0; j<outgoing.length; j++) {
    //	                            outgoing[j].fromIndex = -1;
    //	                            outgoing[j].toIndex = -1;
    //	                        }
    //	                    }
    //	                    if (incoming != null) {
    //	                        incoming.fromIndex = -1;
    //	                        incoming.toIndex = -1;
    //	                    }
    //	                }
    //	            }
    //	            // add filtered events and compute all indexes
    //	            int curIdx = 0;
    //	            for(int i=0; i<sz; i++) {
    //	                Event ev = events.get(i);
    //	                if (filter.filter(ev)) {
    //	                    filteredEvents.add(ev);
    //	                    Entity en = ev.getEntity();
    //	                    if (ev.getRenderer() instanceof BirthRenderer) {
    //	                        en.setFilteredBirthIndex(curIdx);
    //	                    }
    //	                    if (ev.getRenderer() instanceof DeathRenderer)
    //	                        en.setFilteredDeathIndex(curIdx);
    //	                    Interaction[] outgoing = ev.getOutgoingInteractions();
    //	                    Interaction incoming = ev.getIncomingInteraction();
    //	                    if (outgoing != null) {
    //	                        for(int j=0; j<outgoing.length; j++) {
    //	                            outgoing[j].fromIndex = curIdx;
    //	                        }
    //	                    }
    //	                    if (incoming != null) {
    //	                        incoming.toIndex = curIdx;
    //	                    }
    //	                    curIdx++;
    //	
    //	                }
    //	            }
    //	
    //	        } else {
    //	            // restore indexes
    //	            int sz = events.size();
    //	            filteredEvents = events;
    //	            for(int i=0; i<sz; i++) {
    //	                Event ev = events.get(i);
    //	                Interaction[] outgoing = ev.getOutgoingInteractions();
    //	                Interaction incoming = ev.getIncomingInteraction();
    //	                if (outgoing != null) {
    //	                    for(int j=0; j<outgoing.length; j++) {
    //	                        outgoing[j].toIndex = -1;
    //	                        outgoing[j].fromIndex = i;
    //	                    }
    //	                }
    //	                if (incoming != null) {
    //	                    incoming.toIndex = i;
    //	                    if (incoming.getFromEvent() != null) {
    //	                        for(int j=filteredEvents.size()-1; j>=0; j--) {
    //	                            Event ev1 = filteredEvents.get(j);
    //	                            if (ev1 == incoming.getFromEvent() ) {
    //	                                incoming.fromIndex = j;
    //	                                break;
    //	                            }
    //	                        }							
    //	                    }	
    //	                }
    //	            }
    //	        }
    //    	}
    //        notifyEventsChanged();    	
    //    }

    //    public void setFilter(MSCDataModelEventFilter dataModelFilter) {
    //        this.filter = dataModelFilter;
    //        updateFilteredEvents();
    //    }
    //    


    //    public int getIndexForEvent(Event ev) {
    //        for(int i=0, sz = filteredEvents.size(); i<sz; i++) {
    //            if (filteredEvents.get(i) == ev)
    //                return i;
    //        }
    //        return -1;
    //    }

    /**
     * adds a listener to the model
     * @param l 
     */
    public void addListener(MSCDataModelListener l) {
        listeners.add(l);
    }


    /**
     * removes a listener from the model
     * @param l 
     */
    public void removeListener(MSCDataModelListener l) {
        listeners.remove(l);
    }


//    private void notifyEntityAdded(final Entity en) {
//        if (notificationEnabled)
//            Utils.dispatchOnAWTThreadLater(new Runnable() {
//                @Override
//                public void run() {
//                    for (MSCDataModelListener listener : listeners) {
//                        listener.entityAdded(MSCDataModel.this, en);
//                    }
//                }
//            });
//    }
//
//
//
//    private void notifyEventAdded(final Event ev) {
//        if (notificationEnabled) {
//            Utils.dispatchOnAWTThreadLater(new Runnable() {
//                @Override
//                public void run() {
//                    for (MSCDataModelListener listener : listeners) {
//                        listener.eventAdded(MSCDataModel.this, ev);
//                    }
//                }
//            });
//        }
//    }

    /**
     * Invoked to notify listeners about a change to the model.
     */
    public void notifyModelChanged() {
        if (notificationEnabled) {
            Utils.dispatchOnAWTThreadLater(new Runnable() {
                @Override
                public void run() {
                    for (MSCDataModelListener listener : listeners) {
                        listener.modelChanged(MSCDataModel.this);
                    }
                }
            });
        }
    }

//    public void notifyEventsChanged() {
//        if (notificationEnabled) {
//            Utils.dispatchOnAWTThreadLater(new Runnable() {
//                @Override
//                public void run() {
//                    for (MSCDataModelListener listener : listeners) {
//                        listener.eventsChanged(MSCDataModel.this);
//                    }
//                }
//            });
//        }
//    }


    
    //    public int getFirstFilteredEventIndexForEntity(Entity en) {
    //        for(int i=0; i<filteredEvents.size(); i++) {
    //            Event ev = filteredEvents.get(i); 
    //            if (ev.getEntity() == en)
    //                return i;
    //        }
    //        return -1;
    //    }

//    public Event getFirstEventForEntity(Entity en) {
//        for (Event ev : events) { 
//            if (ev.getEntity() == en)
//                return ev;
//        }
//        return null;
//    }

    //    public int getLastFilteredEventIndexForEntity(Entity en) {
    //        for(int i=filteredEvents.size()-1; i>=0; i--) {
    //            Event ev = filteredEvents.get(i); 
    //            if (ev.getEntity() == en)
    //                return i;
    //        }
    //        return -1;
    //    }

//    public Event getLastEventForEntity(Entity en) {
//        for(int i=events.size(); i>=0; i--) {
//            Event ev = events.get(i); 
//            if (ev.getEntity() == en)
//                return ev;
//        }
//        return null;
//    }

    //    @SuppressWarnings("unchecked")
    //    public void sortEventsWithClockSkew() {
    //        // step 2: move events which are not in correct ts order
    //        Collections.sort(events, new Comparator() {
    //            @Override
    //            public int compare(Object o1, Object o2) {
    //                Event ev1 = ((Event)o1);
    //                Event ev2 = ((Event)o2);
    //                long res = ev1.getTimestampCorrectedWithSkew() - ev2.getTimestampCorrectedWithSkew();
    //                return (res<0) ? -1 : ((res > 0) ? +1 : 0);
    //            }			
    //        });
    //        // fix indexes
    //        for(int i=0; i<getEventCount(); i++) {
    //            Event ev = getEventAt(i);
    //            Interaction[] outgoing = ev.getOutgoingInteractions();
    //            Interaction incoming = ev.getIncomingInteraction();
    //            if (outgoing!= null) {
    //                for(int j=0; j<outgoing.length; j++) {
    //                    outgoing[j].fromIndex = i;
    //                }
    //            }
    //            if (incoming != null) {
    //                incoming.toIndex = i;
    //            }
    //        }
    //        // recompute filtered events
    //        setFilter(filter);
    //    }

    /**
     * Performs topological sorting of the model. 
     * 
     * Once an input file is loaded and the model created, due to skews
     * in clocks of entities with non-synchronized clocks it is possible that
     * an interaction may appear with the sink event preceding the source event.
     * This is undesirable, as it can be confusing for the observer. This function
     * performs a topological sorting of the graph represented by events and 
     * interaction to take care of this situation. Events may be "pushed up"
     * or "pushed down" as long as this doesn't break any reasonable constraints.<br>
     * 
     * The topological sorting may fail if the model has unexpected loops (for
     * example events e1, e2, belonging to entity E1, e3, e4 to entity E2, and 
     * interactions e1->e4, e4->e2, e2->e3, e3->e1 (which would imply e1 happened
     * before itself).
     * @param obs 
     */
    public void topoSort() {
        if (true) {
        TopologyGraph graph = new TopologyGraph(this);
        IntervalTree.dbg = true;
        try {
            int sz = events.size();
            // topoSort() returns a map from new index to old, i.e.
            // a node that was at index evs[i] should go to index i
            final int[] evs = graph.topoSort();
            
            ArrayList<Event> newevs = new ArrayList<Event>(sz);
            // remap events
            for (Entity en: rootEntities) {
                en.setFirstEventIndex(-1);
            }
            for(int i=0; i<sz; i++) {
                Event ev = events.get(evs[i]);
                newevs.add(ev);
                ev.setIndex(i);
                Entity en = ev.getEntity();
                if (en.getFirstEventIndex() == -1)
                    en.setFirstEventIndex(i);
                en.setLastEventIndex(i);
            }

            // remap interactions
            class NodeVisitor implements Visitor {
                private IntervalTree newTree;
                public NodeVisitor(IntervalTree newTree) {
                    this.newTree = newTree;
                }
                        
                @Override
                public boolean visit(AVLTreeNode tn) {
                    Interaction inter = (Interaction)tn.getData();
                    int oldFrom = inter.getFromIndex();
                    int oldTo = inter.getToIndex();
                    // we use old event array here to map from old index to new
                    int newFrom = (oldFrom != -1) ? events.get(oldFrom).getIndex() : -1;
                    int newTo = (oldTo != -1) ? events.get(oldTo).getIndex() : -1;
                    inter.setFromToIndices(newFrom, newTo);
                    // although we don't traverse the children any longer,
                    // we remove them to allow memory recycle for tree nodes
                    tn.detachChildren();
                    newTree.add(inter);
                    return false;
                }
            }
            IntervalTree newTree = new IntervalTree("tmptree");
            interactions.postorder(new NodeVisitor(newTree));
            interactions = newTree;
            
            //assing event array last
            events = newevs;

            try {
                interactions.verifyIntegrity();
            } catch (TreeIntegrityException ex) {
                ex.printStackTrace();
            }
            notifyModelChanged();
        } catch (TopologyError e) {
            Report.exception(e);
        }
        try {
            interactions.verifyIntegrity();
        }catch(TreeIntegrityException er) {
            System.err.println(er.getMessage());
            System.err.println(er.getTreePath());
        }
        }
    }

    /**
     * returns the event that was generated from the line at the specified 
     * index in the source file.
     * @param lnum
     * @return 
     */
    public Event getEventByLineIndex(int lnum) {
        int sz = events.size();
        for(int i=0; i<sz; i++) {
            Event ev = events.get(i);
            if (ev.getLineIndex() == lnum)
                return ev;
        }
        return null;
    }

    /**
     * enables/disables notification of events.
     * @param v 
     */
    public void enableNotification(boolean v) {
        notificationEnabled = v;
    }

    /**
     * clears all markers for events and interactions
     */
    public void clearMarkers() {
        for (Event ev : events) {
            ev.setMarker(null);            
        }
        for(InOrderAVLTreeNodeIterator it = new InOrderAVLTreeNodeIterator(interactions); it.hasNext();) {
            AVLTreeNode tn = it.next();
            Interaction in = (Interaction)tn.getData();
            in.setMarker(null);
        }
    }
    
    public String getOpenPath() {
        return openPath;
    }
    
    public void setOpenPath(String path) {
        openPath = path;
    }

    public GraphWindow addGraph(GraphData d[]) {
        try {
            graphs.add(d);
            GraphWindow w = new GraphWindow(d);
            return w;
        }catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}
