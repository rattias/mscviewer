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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

import com.cisco.mscviewer.model.JSonObject;

public class StopRenderer extends EventRenderer {
    private static final BasicStroke basic2 = new BasicStroke(2.0f);
    // private static final BasicStroke basic3 = new BasicStroke(3.0f);
    private Color color;

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
        final String col = props.get("color").toString();
        if (col != null) {
            final int c = Integer.parseInt(col, 16);
            color = new Color(c);
        }
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        final Stroke s = g2d.getStroke();
        g2d.setColor(color);
        g2d.setStroke(basic2);
        g2d.drawLine(0 - maxDim.height / 2 + 3, 0 + maxDim.height / 2 - 1,
                0 + maxDim.height / 2 - 3, 0 + maxDim.height / 2 - 1);
        g2d.setStroke(s);
    }

    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        bb.x = x - maxDim.height / 2;
        bb.y = y + maxDim.height / 2 - 2;
        bb.width = maxDim.height;
        bb.height = 4;
        return bb;
    }

}
