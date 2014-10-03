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

import com.cisco.mscviewer.model.JSonObject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

public class ConnectionRenderer extends EventRenderer {
    private final static int wire_len = 3;
    private int gap;

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
        if ("true".equals(props.get("completed")))
            gap = 0;
        else
            gap = 7;
        setScaleSource(false);
    }


    public void render(Graphics2D g2d, Dimension maxDim) {
        int H = maxDim.height*2/3;
        int W = maxDim.height;
        Stroke s = g2d.getStroke();
        g2d.setColor(new Color(0x8080FF));
        g2d.fillArc(0-(W+gap)/2, 0-H/3, W, H*2/3, 90, 180);
        g2d.setColor(new Color(0x80B080));
        g2d.fillArc(0-(W+gap)/2+gap, 0-H/3, W, H*2/3, 90, -180);
        g2d.setColor(Color.black);
        g2d.drawArc(0-(W+gap)/2, 0-H/3, W, H*2/3, 90, 180);
        g2d.drawArc(0-(W+gap)/2+gap, 0-H/3, W, H*2/3, 90, -180);

        g2d.drawLine(0-gap/2, 0-H/3, 0-gap/2, 0+H/3);
        g2d.drawLine(0+gap/2, 0-H/3, 0+gap/2, 0+H/3);

        g2d.drawLine(0-(W+gap)/2-wire_len, 0, 0-(W+gap)/2, 0);
        g2d.drawLine(0-(W+gap)/2+gap+W, 0, 0-(W+gap)/2+gap+W+wire_len, 0);
        g2d.setStroke(s);
    }


    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        bb.x = x-maxDim.height/2-wire_len;
        bb.y = y-maxDim.height/3;
        bb.width = maxDim.height+2*wire_len;
        bb.height = maxDim.height*2/3;

        return bb;
    }
}
