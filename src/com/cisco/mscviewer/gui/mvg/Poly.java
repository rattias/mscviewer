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

import java.awt.geom.Path2D;
import java.util.ArrayList;

/**
 *
 * @author rattias
 */
public class Poly extends Primitive {
    class Point {
        float x, y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private final ArrayList<Point> points;

    public Poly(float x0, float y0) {
        points = new ArrayList<Point>();
        points.add(new Point(x0, y0));
    }

    public void addPoint(float x, float y) {
        points.add(new Point(x, y));
    }

    @Override
    public void setContainerDimension(int w, int h) {
        final Path2D s = new Path2D.Float();
        final boolean first = true;
        for (final Point p : points) {
            if (first)
                s.moveTo(p.x * w, p.y * h);
            else
                s.lineTo(p.x * w, p.y * h);
        }
        setShape(s);
    }

}
