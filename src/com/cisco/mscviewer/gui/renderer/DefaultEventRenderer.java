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

public class DefaultEventRenderer extends EventRenderer {
    private final int width = 8;

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        g2d.setColor(Color.gray);
        g2d.fillOval(-2, -2, 5, 5);
    }

    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (bb == null)
            bb = new Rectangle();
        bb.x = x - width / 2;
        bb.y = y - maxDim.height / 2;
        bb.width = width;
        bb.height = maxDim.height;
        return bb;
    }

}
