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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import com.cisco.mscviewer.Main;


public class ImageRenderer extends EventRenderer {
    private Image img;
    private Dimension dim = null;
    
    
    public ImageRenderer(Image img) {
        this.img = img;
    }

    private void computeDimension(int h) {
        double scalingFactor = ((double)h)/img.getHeight(null);
        dim = new Dimension((int)(img.getWidth(null)*scalingFactor),
            (int)(img.getHeight(null)*scalingFactor));
    }
    
    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        if (dim == null)
            computeDimension(maxDim.height);        
        g2d.drawImage(img, -dim.width/2, -dim.height/2, dim.width, dim.height, null);
    }


    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (dim == null)
            computeDimension(maxDim.height);
        if (bb == null)
            bb = new Rectangle();
        bb.x = x-dim.width/2;
        bb.y = y-dim.height/2;
        bb.width = dim.width;
        bb.height = dim.height;
        return bb;
    }


}
