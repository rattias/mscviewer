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

public class CITCEventRenderer extends EventRenderer {

    /**
     *
     * @param props
     */
    @Override
    public void setup(JSonObject props) {
        super.setup(props);
    }


    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        int H = maxDim.height*2/3;
        int W = maxDim.height;
        g2d.setColor(Color.white);		
        g2d.fillRect(-W/2, -H/2, W, H);
        g2d.setColor(Color.gray);		
        g2d.drawRect(-W/2, -H/2, W, H);
        for(int x=-W/2+4; x<W/2; x+=4)
            g2d.drawLine(x, -H/2+1, x, H/2);
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
        bb.x = x-maxDim.height/2;
        bb.y = y-maxDim.height/2;
        bb.width = maxDim.height;
        bb.height = maxDim.height;
        return bb;
    }
}
