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
package com.cisco.mscviewer.gui.renderer;


import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.model.JSonObject;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;


abstract public class  InteractionRenderer {
    private final static HashMap<String, BasicStroke> bs = new HashMap<String, BasicStroke>();
    private Interaction inter;

    
    protected final static BasicStroke getBasicStroke(int width, boolean dashed) {
        String k = width+"-"+dashed;
        BasicStroke b = bs.get(k);
        if (b == null) {
            if (dashed)
                b = new BasicStroke(width,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 1.0f,
                    new float[] { 3.0f, 3.0f, 3.0f, 3.0f },
                    0.0f);
            else
                b = new BasicStroke(width);
            bs.put(k, b);
        }
        return b;
    }

    public void initialize(Interaction inter, JSonObject props, Event ev) {
        if (inter == null)
            throw new NullPointerException("Null interaction!");
        this.inter = inter;
        setup(props, ev);
    }

    abstract public void setup(JSonObject props, Event ev);


    public Interaction getInteraction() {
        return inter;
    }


    public abstract void render(
            Rectangle fb,
            Rectangle tb,
            Graphics2D g2d, boolean isSelected, Marker m);

    public abstract boolean inSelectionArea(
            Rectangle b1,
            Rectangle b2,
            int px, int py, boolean self);

    public abstract void setColor(Color color);
}
