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

public class ImageRenderer extends EventRenderer {
    private final Image img;
    private final String name;
    private Dimension dim = null;

    public ImageRenderer(String name, Image img) {
        this.name = name;
        this.img = img;
    }

    public String getName() {
        return name;
    }

    private void computeDimension(int h) {
        final double scalingFactor = ((double) h) / img.getHeight(null);
        dim = new Dimension((int) (img.getWidth(null) * scalingFactor),
                (int) (img.getHeight(null) * scalingFactor));
    }

    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        if (dim == null)
            computeDimension(maxDim.height);
        g2d.drawImage(img, -dim.width / 2, -dim.height / 2, dim.width,
                dim.height, null);
    }

    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (dim == null)
            computeDimension(maxDim.height);
        if (bb == null)
            bb = new Rectangle();
        bb.x = x - dim.width / 2;
        bb.y = y - dim.height / 2;
        bb.width = dim.width;
        bb.height = dim.height;
        return bb;
    }

}
