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


public class BirthRenderer extends  EventRenderer {
    private final static int TRIANGLE_BASE = 13;

    /**
     *
     * @param props
     */
    @Override
    public void setup(JSonObject props) {
        super.setup(props);
        setScaleSource(true);
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        g2d.setColor(Color.GREEN);
        int x1 = 0-TRIANGLE_BASE/2;
        int x2 = 0+TRIANGLE_BASE/2;
        int x3 = 0;
        int y1 = 0-TRIANGLE_BASE/2;
        int y2 = 0-TRIANGLE_BASE/2;
        int y3 = 0+TRIANGLE_BASE/2;
        g2d.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }

    @Override
    public boolean inSelectionArea(int x, int y, Dimension maxDim, int px, int py) {
        return px >= x-TRIANGLE_BASE/2 && px < x+TRIANGLE_BASE/2 &&
        py >= y-TRIANGLE_BASE/2 && py < y+TRIANGLE_BASE/2;		
    }

    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle rr) {
        if (rr == null)
            rr = new Rectangle();
        rr.x = x-TRIANGLE_BASE/2;
        rr.y = y-TRIANGLE_BASE/2;
        rr.width = TRIANGLE_BASE; 
        rr.height = TRIANGLE_BASE;
        return rr;
    }

}
