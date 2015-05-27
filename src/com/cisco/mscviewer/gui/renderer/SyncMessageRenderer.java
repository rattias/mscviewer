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
import java.awt.geom.AffineTransform;

import com.cisco.mscviewer.model.JSonObject;

public class SyncMessageRenderer extends EventRenderer {
    @SuppressWarnings("unused")
    private static final Stroke basic2 = new BasicStroke(2.0f);
    @SuppressWarnings("unused")
    private static final Stroke basic3 = new BasicStroke(4.0f);

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
        setScaleSource(true);
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        final Stroke s = g2d.getStroke();
        final int SIDE = maxDim.height;
        final int x = -SIDE / 2;
        final int y = -SIDE / 2;
        final int xs[] = { x + SIDE / 3, x + SIDE / 3, x + SIDE * 2 / 3,
                x + SIDE * 2 / 3, x + SIDE / 3, x + SIDE / 3 };
        final int ys[] = { y + 2, y + SIDE / 3, y + SIDE / 3, y + SIDE * 2 / 3,
                y + SIDE * 2 / 3, y + SIDE - 2 };
        // g2d.setStroke(basic2);
        g2d.setColor(Color.yellow);
        g2d.drawPolyline(xs, ys, 6);
        final AffineTransform t = g2d.getTransform();
        g2d.translate(1, 1);
        g2d.setColor(Color.yellow.darker());
        g2d.drawPolyline(xs, ys, 6);
        g2d.setTransform(t);
        g2d.setStroke(s);
    }

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
