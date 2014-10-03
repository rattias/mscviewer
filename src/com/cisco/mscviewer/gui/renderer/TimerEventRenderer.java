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

public class TimerEventRenderer extends EventRenderer {

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
    }


    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        int SIDE = maxDim.height-4;
        g2d.setColor(Color.black);		
        g2d.drawOval(0-SIDE/2, 0-SIDE/2, SIDE, SIDE);
        g2d.setColor(Color.gray);		
        g2d.drawOval(0-SIDE/2-1, 0-SIDE/2-1, SIDE, SIDE);

        g2d.drawLine(0-SIDE/2, 0, 0-SIDE/2+3, 0);
        g2d.drawLine(0+SIDE/2, 0, 0+SIDE/2-3, 0);
        g2d.drawLine(0, 0-SIDE/2, 0, 0-SIDE/2+3);
        g2d.drawLine(0, 0+SIDE/2, 0, 0+SIDE/2+3);
        g2d.drawLine(0, 0, 0+SIDE/3, 0+SIDE/3);
        g2d.drawLine(0, 0, 0, 0-SIDE-2-2);
    }


    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        bb.x = x-maxDim.height/2;
        bb.y = y-maxDim.height/2;
        bb.width = maxDim.height;
        bb.height = maxDim.height;
        return bb;
    }
}
