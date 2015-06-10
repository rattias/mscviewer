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

import java.awt.Point;

import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.gui.renderer.DefaultEventRenderer;
import com.cisco.mscviewer.gui.renderer.EventRenderer;
import com.cisco.mscviewer.gui.renderer.ImageRenderer;
import com.cisco.mscviewer.io.Session;

/**
 * An <code>Event</code> represents an occurence of relevance for an
 * {@link Entity} within a data model (see {@link MSCDataModel}.
 * 
 * An Event is characterized by a timestamp, a label, a type.
 * 
 * @author rattias
 */
public class Event {

    private final MSCDataModel model;
    /** model the event belongs to */
    private final long timestamp;
    /** timestamp when the event occurred */
    private final Entity en;
    /** Entity the event belongs to */
    private final int lineIndex;
    /** index of the line in the source file this event was generated from */
    private int index;
    /** index of this event within the model */
    private String ts;
    /** cached timestamp representation */
    private Marker marker;
    /** marker this Event is marked with (see {@link Marker}) */
    private EventRenderer renderer;
    /** renderer object associated to this event */
    private final String label;
    /** label for the Event */
    private Note note;
    /** note associated to the Event */
    private JSonValue data;
    /** some data */
    private boolean blockBegin;

    /**
     * Instantiates an Event with the specified parameters.
     * 
     * @param dm
     *            the {@link MSCDataModel} this Event belongs to
     * @param timestamp
     *            the timestamp the event occurred at
     * @param en
     *            the {@link Entity} the event belong to
     * @param label
     *            the label associated to the event
     * @param lineIndex
     *            the index of the source line this event was generated from
     * @param renderer
     *            the {@link EventRenderer} associated to the event
     * @param props
     *            properties for the renderer
     */
    public Event(MSCDataModel dm, long timestamp, Entity en, String label,
            int lineIndex, EventRenderer renderer, JSonObject props) {
        if (en == null)
            throw new NullPointerException("Null entity");
        model = dm;
        this.timestamp = timestamp;
        this.en = en;
        this.label = label;
        this.lineIndex = lineIndex;
        if (renderer == null)
            this.renderer = new DefaultEventRenderer();
        else
            this.renderer = renderer;
        this.renderer.initialize(props);
        this.blockBegin = false;
    }

    /**
     * returns the {@link Entity} this event belongs to.
     * 
     * @return the {@link Entity} this event belongs to
     */
    public Entity getEntity() {
        return en;
    }

    /**
     * returns the index of the line in the source file this event was generated
     * from.
     * 
     * @return returns the index of the line in the source file this event was
     *         generated from
     */
    public int getLineIndex() {
        return lineIndex;
    }

    /**
     * returns the {@link EventRenderer} associated to this event
     */
    public final EventRenderer getRenderer() {
        return renderer;
    }

    /**
     * caches the representation of the timestamp according to the configuration
     * of the time parameters for the view
     * 
     * @param timestamp
     */
    public void setTimestampRepr(String timestamp) {
        ts = timestamp;
    }

    /**
     * returns the cached timestamp representation
     * 
     * @return
     */
    public String getTimestampRepr() {
        return ts;
    }

    /**
     * returns the timestamp
     * 
     * @return
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * returns the label
     * 
     * @return
     */
    public String getLabel() {
        return label;
    }

    /**
     * returns the node
     * 
     * @param n
     *            the note
     */
    public void setNote(String n) {
        if (note == null)
            note = new Note(n);
        else
            note.setText(n);
        Session.setUpToDate(false);
    }
    

    public boolean noteIsVisible() {
        return note != null && note.isVisible();
    }

    /**
     * sets the node
     * 
     * @return the note
     */
    public String getNote() {
        return note != null ? note.getText() : null;
    }

    public void setNoteVisible(boolean v) {
        if (note == null) 
            note = new Note("");
        note.setVisible(v);
        Session.setUpToDate(false);
    }
    
    public Point getNoteOffset() {
        return note == null ? new Point(-1, -1) : note.getPosition();
    }

    public void setNotePosition(Point  p) {
        if (note == null) 
            note = new Note("");
        note.setPosition(p);
        Session.setUpToDate(false);
    }
    
    /**
     * returns the type of the Event. The type corresponds to the name of the
     * Renderer class, stripped of the "Renderer" suffix.
     * 
     * @return
     */
    public String getType() {
        final EventRenderer r = getRenderer();
        if (r instanceof ImageRenderer) {
            return ((ImageRenderer) r).getName();
        } else {
            final String clName = getRenderer().getClass().getName();
            return clName.substring(clName.lastIndexOf('.') + 1,
                    clName.indexOf("Renderer"));
        }
    }

    @Override
    public String toString() {
        return "Event [timestamp=" + timestamp + ", label=" + label + ", note="
                + note + ", entity=" + en.getPath() + ", lineIndex="
                + lineIndex + "]";
    }

    /**
     * sets the marker, or clears it if <code>null</code> is passed.
     * 
     * @param m
     *            a value from {@link Marker}
     */
    public void setMarker(Marker m) {
        marker = m;
        Session.setUpToDate(false);
    }

    /**
     * returns the marker that marked this event, or <code>null</code> if there
     * is no marking
     * 
     * @return
     */
    public Marker getMarker() {
        return marker;
    }

    /**
     * sets a generic Map to store multiple values with this object. Could be
     * used to store, for example, a JSON object.
     * 
     * @param data
     */
    public void setData(JSonValue data) {
        this.data = data;
    }

    /**
     * returns the data associated to this event
     * 
     * @return
     */
    public JSonValue getData() {
        // try {
        // JSonObject o = new
        // JSonObject("{\"key\":\"value\", \"foo\":[1, 2], \"bar\":{\"x\":\"vx\", \"y\":true}}");
        // return o;
        // } catch (JSonException ex) {
        // throw new Error(ex);
        // }
        return data;
    }

    /**
     * sets the model index of this event.
     * 
     * @param index
     */
    void setIndex(int index) {
        this.index = index;
    }

    /**
     * returns the model index of this event
     * 
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * returns the {@see Interaction}s incoming into this Event.
     * 
     * @return the {@see Interaction}s incoming into this Event.
     */
    public Interaction[] getIncomingInteractions() {
        return model.getIncomingInteractions(this);
    }

    /**
     * returns the {@see Interaction}s outgoing from this Event.
     * 
     * @return the {@see Interaction}s outgoing from this Event.
     */
    public Interaction[] getOutgoingInteractions() {
        return model.getOutgoingInteractions(this);
    }

    public Event getPreviousEventForEntity() {
        for (int i = getIndex() - 1; i >= 0; i--) {
            final Event ev = model.getEventAt(i);
            if (ev.getEntity() == getEntity())
                return ev;
        }
        return null;
    }

    public Event getPreviousEvent() {
        final int idx = getIndex();
        if (idx == 0)
            return null;
        return model.getEventAt(idx - 1);
    }

    public Event getNextEventForEntity() {
        final int cnt = model.getEventCount();

        for (int i = getIndex() + 1; i < cnt; i++) {
            final Event ev = model.getEventAt(i);
            if (ev.getEntity() == getEntity())
                return ev;
        }
        return null;
    }

    public Event getNextEvent() {
        final int idx = getIndex();
        if (idx == model.getEventCount() - 1)
            return null;
        return model.getEventAt(idx + 1);
    }

    public void setBlockBegin() {
        blockBegin = true;
    }

    // public void setBlockEnd() {
    // blockBegin = false;
    // }

    public boolean isBlockBegin() {
        return blockBegin;
    }

}
