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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;


public class DeathRenderer extends EventRenderer {
    private static BasicStroke basic2 = new BasicStroke(2.0f);

    private final static int INTRA_EV_WIDTH = 12;

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        g2d.setColor(Color.RED);
        Stroke s = g2d.getStroke();
        g2d.setStroke(basic2);
        g2d.drawLine(-INTRA_EV_WIDTH/2, -INTRA_EV_WIDTH/2, INTRA_EV_WIDTH/2, INTRA_EV_WIDTH/2);
        g2d.drawLine(INTRA_EV_WIDTH/2, -INTRA_EV_WIDTH/2, -INTRA_EV_WIDTH/2, INTRA_EV_WIDTH/2);
        g2d.setStroke(s);
    }

    @Override
    public boolean inSelectionArea(int x, int y, Dimension maxDim, int px, int py) {
        return px >= x-INTRA_EV_WIDTH/2 && px < x+INTRA_EV_WIDTH/2 &&
        py >= y-INTRA_EV_WIDTH/2 && py < x+INTRA_EV_WIDTH/2;		
    }

    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        bb.x = x-INTRA_EV_WIDTH/2;
        bb.y = y-INTRA_EV_WIDTH/2;
        bb.width = INTRA_EV_WIDTH; 
        bb.height = INTRA_EV_WIDTH;
        return bb;
    }

}
