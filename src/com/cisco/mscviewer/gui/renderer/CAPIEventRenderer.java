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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class CAPIEventRenderer extends EventRenderer {
    static Rectangle2D r;
    static String str = "CAPI";
    int ascent;

    /**
     *
     * @param props
     */
    @Override
    public void setup(JSonObject props) {
        super.setup(props);
    }

    public void drawText(Graphics2D g2d, int H) {
        if (r == null) {
            FontMetrics fm = g2d.getFontMetrics();
            r = fm.getStringBounds(str, g2d);
            ascent = fm.getAscent();
        }
        AffineTransform tf = g2d.getTransform();
        g2d.scale(.7, .7);
        g2d.setColor(Color.black);
        g2d.drawString(str, (int)-(r.getWidth()/2), (int)(0+H/2+r.getHeight()+r.getY()));
        g2d.setTransform(tf);
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        int H = maxDim.height*2/3;
        int W = maxDim.height;
        g2d.setColor(Color.white);		
        g2d.fillRect(0-W/2, 0-H/2, W, H);
        g2d.setColor(Color.gray);		
        g2d.drawRect(0-W/2, 0-H/2, W, H);
        g2d.drawLine(0-W/2, 0-H/2, 0-W/2+3, 0-H/2+3);
        g2d.drawLine(0+W/2, 0-H/2, 0+W/2-3, 0-H/2+3);
        drawText(g2d, H);
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
