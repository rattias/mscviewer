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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;


public class ErrorRenderer extends EventRenderer {

    @Override
    public void setup(JSonObject props) {
        super.setup(props);
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        g2d.setColor(Color.orange);
        int x1 = 0;
        int x2 = maxDim.height/2;
        int x3 = -maxDim.height/2;
        int y1 = -maxDim.height/2;
        int y2 = maxDim.height/2;
        int y3 = maxDim.height/2;
        g2d.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        FontMetrics fm =  g2d.getFontMetrics();
        int des = fm.getDescent();
        int w = fm.stringWidth("!");
        g2d.setColor(Color.red);
        for(int i=0; i<2; i++)
            g2d.drawString("!", x1-w/2+i, y3-des);
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
