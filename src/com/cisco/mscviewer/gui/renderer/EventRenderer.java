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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.cisco.mscviewer.model.JSonObject;

abstract public class EventRenderer {
    public final static Color SELECTION_COLOR = Color.red;
    private boolean scaleSource;

    /**
     *
     * @param ev
     * @param props
     */
    public void initialize(JSonObject props) {
        // this.ev = ev;
        setup(props);
    }

    /**
     *
     * @param b
     */
    protected void setScaleSource(boolean b) {
        scaleSource = b;
    }

    /**
     *
     * @return
     */
    public boolean scaleSource() {
        return scaleSource;
    }

    // public Event getEvent() {
    // return ev;
    // }

    /**
     *
     * @param props
     */
    public void setup(JSonObject obj) {
        // String style = props.getProperty("label_style");
        // if (style != null) {
        // if (style.equals("bold"))
        // labelStyle = Font.BOLD;
        // else if (style.equals("italic"))
        // labelStyle = Font.ITALIC;
        // else if (style.equals("plain"))
        // labelStyle = Font.PLAIN;
        // }
    }

    // public int getLabelStyle() {
    // return labelStyle;
    // }
    /**
     * renders the event. (x,y) is the center
     * 
     * @param g2d
     * @param maxDim
     * @param isSelected
     */
    abstract public void render(Graphics2D g2d, Dimension maxDim);

    /**
     *
     * @param maxDim
     * @param x
     * @param y
     * @param bb
     * @return
     */
    abstract public Rectangle getBoundingBox(Dimension maxDim, int x, int y,
            Rectangle bb);

    /**
     * return true if (px,py) lies within the figure, false otherwise
     * 
     * @param x
     * @param y
     * @param maxDim
     * @param px
     * @param py
     * @return
     */
    public boolean inSelectionArea(int x, int y, Dimension maxDim, int px,
            int py) {
        return getBoundingBox(maxDim, x, y, new Rectangle()).contains(px, py);
    }
}
