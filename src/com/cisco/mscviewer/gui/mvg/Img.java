/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Sept 2014
 */
package com.cisco.mscviewer.gui.mvg;

import java.awt.Graphics2D;
import java.awt.Image;

/**
 *
 * @author rattias
 */
public class Img extends Primitive {
    private final Image img;
    private final float xf, yf, wf, hf;
    private int x, y, w, h;
    
    public Img(float x0, float y0, float w, float h, Image img) {
        xf = x0;
        yf = y0;
        wf = w;
        hf = h;                
        this.img = img;
    }
    
    @Override
    public void setContainerDimension(int cw, int ch) {
        x = (int)(xf*cw);
        y = (int)(yf*ch);
        w = (int)(wf*cw);
        h = (int)(hf*ch);        
    }
    
    @Override
    public final void render(Graphics2D g2d) {
        g2d.drawImage(img, x, y, w, h, null); 
    }
}
