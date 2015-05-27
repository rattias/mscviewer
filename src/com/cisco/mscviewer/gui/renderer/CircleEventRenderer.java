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

public class CircleEventRenderer extends EventRenderer {
    Color color = Color.YELLOW;
    Color color1;
    float factor = 1.0f;

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
        final String rad = props.get("radius").toString();
        if (rad != null)
            factor = Float.parseFloat(rad);
        final String col = props.get("color").toString();
        if (col != null) {
            final int c = Integer.parseInt(col, 16);
            color = new Color(c);
        }
        color1 = color.darker();
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        final int radius = (int) (maxDim.height / 2 * factor);
        g2d.setColor(color);
        g2d.fillOval(-radius, -radius, radius * 2, radius * 2);
        g2d.setColor(color1);
        g2d.drawOval(-radius, -radius, radius * 2, radius * 2);
    }

    @Override
    public boolean inSelectionArea(int x, int y, Dimension maxDim, int px,
            int py) {
        final int radius = (int) (maxDim.height / 2 * factor);
        final int dx = x - px;
        final int dy = y - py;
        final boolean b = (dx * dx + dy * dy < radius * radius);
        return b;
    }

    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        final int diameter = (int) (maxDim.height * factor);
        bb.x = x - diameter / 2;
        bb.y = y - diameter / 2;
        bb.width = diameter;
        bb.height = diameter;
        return bb;
    }

}
