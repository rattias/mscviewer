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

import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.gui.renderer.InteractionRenderer;
import com.cisco.mscviewer.tree.Interval;

/**
 * an <code>Interaction</code> represents a cause-effect relationship between
 * two {@link Event}s. Messaging between two entities, initiation and completion
 * of an asynchronous operation in an entity, the instantiation of an entity by
 * another one are all examples of interactions. <br>
 *
 * An interaction has a source event (or cause) and a sink event (or effect).
 * sometimes a source file may contain information only about the source or sink
 * for an interaction, in which case the other event for the interaction is
 * <code>null</code>. Note that source and sink event may belong to the same or
 * different entities. <br>
 *
 * An interaction has a type and an associated renderer.
 *
 * Interactions are stored in the data model as Intervals in an interval tree
 * {@see http://en.wikipedia.org/wiki/Interval_tree}. Elements of the tree must
 * implement the {@link Interval} interface, which this class does implement.
 *
 * @author rattias
 */
public final class Interaction implements Interval {

    private final MSCDataModel model;
    int fromIndex, toIndex;
    private InteractionRenderer irenderer;
    private Marker marker;

    /**
     * Instantiates an <code>Interaction</code> with the specified model,
     * indices for source and sink even in the data model
     *
     * @param model
     *            {@link MSCDataModel} the interaction belongs to
     * @param fromIdx
     *            data model index of the source event
     * @param toIdx
     *            data model index of the sink event
     * @param irenderer
     *            renderer for the interaction
     */
    public Interaction(MSCDataModel model, int fromIdx, int toIdx,
            InteractionRenderer irenderer) {
        this.model = model;
        this.fromIndex = fromIdx;
        this.toIndex = toIdx;
        this.setIRenderer(irenderer);
    }

    /**
     * if the source/sink event is passed as argument, returns the sink/source
     * event.
     * 
     * @param ev
     * @return
     */
    public Event getOtherEvent(Event ev) {
        if (ev == getFromEvent()) {
            return getToEvent();
        } else if (ev == getToEvent()) {
            return getFromEvent();
        } else {
            return null;
        }
    }

    /**
     * returns the type of the Event. The type corresponds to the name of the
     * Renderer class, stripped of the "Renderer" suffix.
     * 
     * @return
     */
    public String getType() {
        final String clName = irenderer.getClass().getName();
        return clName.substring(clName.lastIndexOf('.') + 1,
                clName.indexOf("Renderer"));
    }

    /**
     * returns the data model index of the other event (@see getOtherEvent}
     * 
     * @param ev
     * @return
     */
    public int getOtherEventIndex(Event ev) {
        if (ev == getFromEvent()) {
            return toIndex;
        } else if (ev == getToEvent()) {
            return fromIndex;
        } else {
            return -1;
        }
    }

    /**
     * return the source event for this interaction, or <code>null</code> if the
     * interaction has no source event.
     * 
     * @return the source event
     */
    public Event getFromEvent() {
        return model.getEventAt(fromIndex);
    }

    /**
     * return the sink event for this interaction, or <code>null</code> if the
     * interaction has no sink event.
     * 
     * @return the sink event
     */
    public Event getToEvent() {
        return model.getEventAt(toIndex);
    }

    /**
     * return the data model index of the source event for this interaction, or
     * <code>null</code> if the interaction has no source event.
     * 
     * @return
     */
    public int getFromIndex() {
        return fromIndex;
    }

    /**
     * return the data model index of the sink event for this interaction, or
     * <code>null</code> if the interaction has no source event.
     * 
     * @return
     */
    public int getToIndex() {
        return toIndex;
    }

    /**
     * sets the data model index of the source event for this interaction.
     * 
     * @param from
     */
    public void setFromIndex(int from) {
        if (from == toIndex) {
            throw new Error("Invalid self-looping Interaction (" + fromIndex
                    + "," + toIndex + ")->(" + from + "," + toIndex + ")");
        }
        fromIndex = from;
    }

    /**
     * sets the data model index of the sink event for this interaction.
     * 
     * @param from
     */
    public void setToIndex(int to) {
        if (fromIndex == to) {
            throw new Error("Invalid self-looping Interaction (" + fromIndex
                    + "," + toIndex + ")->(" + fromIndex + "," + to + ")");
        }
        toIndex = to;
    }

    /**
     * sets the data model indices for source and sink event of this interaction
     * 
     * @param newFrom
     * @param newTo
     */
    public void setFromToIndices(int newFrom, int newTo) {
        fromIndex = newFrom;
        toIndex = newTo;
    }

    /**
     * sets the {@link InteractionRenderer} for this Interaction
     * 
     * @param irenderer
     */
    public void setIRenderer(InteractionRenderer irenderer) {
        this.irenderer = irenderer;
    }

    /**
     * returns the {@link InteractionRenderer} for this Interaction
     * 
     * @return
     */
    public InteractionRenderer getIRenderer() {
        return irenderer;
    }

    /**
     * sets a {@link Marker} for this interaction
     * 
     * @param currentMarker
     */
    public void setMarker(Marker currentMarker) {
        marker = currentMarker;
    }

    /**
     * retusn the marker associated to this interaction
     * 
     * @return
     */
    public Marker getMarker() {
        return marker;
    }

    @Override
    public String toString() {
        String s;
        final Event fromEvent = getFromEvent();
        final Event toEvent = getToEvent();
        if (fromEvent != null) {
            s = "(" + fromEvent.getEntity().getPath() + ", "
                    + fromEvent.getLabel() + ")";
        } else {
            s = "(???,???)";
        }
        if (toEvent != null) {
            s += "->(" + toEvent.getEntity().getPath() + ", "
                    + toEvent.getLabel() + ")";
        } else {
            s += "->(???,???)";
        }
        return s;
    }

    /**
     * Implements the corresponding method of the {@link Interval} interface
     * 
     * @return
     */
    @Override
    public int getStart() {
        if (fromIndex > 0 && toIndex > 0)
            return Math.min(fromIndex,  toIndex);
        else 
            return fromIndex >= 0 ? fromIndex : toIndex;
    }

    /**
     * Implements the corresponding method of the {@link Interval} interface
     * 
     * @return
     */
    @Override
    public int getEnd() {
        if (fromIndex > 0 && toIndex > 0)
            return Math.max(fromIndex,  toIndex);
        else 
            return toIndex >= 0 ? toIndex : fromIndex;
    }

    /**
     * Implements the corresponding method of the {@link Value} interface
     * 
     * @return
     */
    @Override
    public int getValue() {
        return getEnd();
    }
}
