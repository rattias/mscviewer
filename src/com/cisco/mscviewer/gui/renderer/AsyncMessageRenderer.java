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

public class AsyncMessageRenderer extends EventRenderer {

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
        final int SIDE = maxDim.height;
        g2d.setColor(Color.yellow);
        final int xs[] = new int[] { 0 - SIDE * 8 / 16, 0 - SIDE * 1 / 16,
                0 + SIDE * 2 / 16, 0 + SIDE * 8 / 16, 0 + SIDE * 1 / 16,
                0 - SIDE * 2 / 16 };
        final int ys[] = new int[] { 0 - SIDE * 8 / 16, 0 - SIDE * 1 / 16,
                0 - SIDE * 4 / 16, 0 + SIDE * 8 / 16, 0 + SIDE * 1 / 16,
                0 + SIDE * 4 / 16 };
        g2d.fillPolygon(xs, ys, xs.length);
        g2d.setColor(Color.yellow.darker());
        g2d.drawPolygon(xs, ys, xs.length);
    }

    @Override
    public boolean inSelectionArea(int x, int y, Dimension maxDim, int px,
            int py) {
        final Rectangle r = new Rectangle();
        getBoundingBox(maxDim, x, y, r);
        return r.contains(px, py);
    }

    /**
     *
     * @param maxDim
     * @param x
     * @param y
     * @param bb
     * @return
     */
    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        bb.x = x - maxDim.height / 2;
        bb.y = y - maxDim.height / 2;
        bb.width = maxDim.height;
        bb.height = maxDim.height;
        return bb;
    }
}
