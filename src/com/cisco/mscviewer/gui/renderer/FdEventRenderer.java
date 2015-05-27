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

public class FdEventRenderer extends EventRenderer {
    final static int MARGIN = 3;

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        final int H = maxDim.height;
        final int W = maxDim.height * 2 / 3;
        final int xs[] = new int[] { 0 - W / 2, 0 + W / 2 - W / 4, 0 + W / 2,
                0 + W / 2, 0 - W / 2, 0 - W / 2 };
        final int ys[] = new int[] { 0 - H / 2 + MARGIN, 0 - H / 2 + MARGIN,
                0 - H / 2 + MARGIN + H / 4, 0 + H / 2 - MARGIN,
                0 + H / 2 - MARGIN, 0 - H / 2 + MARGIN };
        g2d.setColor(Color.white);
        g2d.fillPolygon(xs, ys, xs.length);
        g2d.setColor(Color.gray);
        g2d.drawPolygon(xs, ys, xs.length);
        final int x0 = 0 - W / 2 + 2;
        final int x1 = 0 + W / 2 - 2;
        g2d.setColor(Color.lightGray);
        g2d.drawLine(x0, 0 - 3, x1, 0 - 3);
        g2d.drawLine(x0, 0, x1, 0);
        g2d.drawLine(x0, 0 + 3, x1, 0 + 3);
    }

    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        bb.x = x - maxDim.height / 2;
        bb.y = y - maxDim.height / 2 + MARGIN;
        bb.width = maxDim.height;
        bb.height = maxDim.height - 2 * MARGIN;
        return bb;
    }
}
