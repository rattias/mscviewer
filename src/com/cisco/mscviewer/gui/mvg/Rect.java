/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Sep 2014
 */
package com.cisco.mscviewer.gui.mvg;

import java.awt.geom.Rectangle2D;

/**
 *
 * @author rattias
 */
public class Rect extends Primitive {
    private final float x, y, w, h;

    public Rect(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    @Override
    public void setContainerDimension(int width, int height) {
        setShape(new Rectangle2D.Float(x * width, y * height, w * width, h
                * height));
    }
}
