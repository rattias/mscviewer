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

import java.awt.BasicStroke;
import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_MITER;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.HashMap;

/**
 *
 * @author rattias
 */
abstract public class Primitive {
    private static HashMap<String, Stroke> strokes = new HashMap<String, Stroke>();
    private Color strokeColor;
    private Color fillColor;
    private Shape s;
    private Stroke stroke;
    
    private static Stroke getStroke(float w, float[] dashes) {
        StringBuilder sb = new StringBuilder();
        sb.append(Float.toString(w));
        if (dashes != null) {
            sb.append('-');
            for(float v: dashes) {
                sb.append(Float.toString(v));
                sb.append('-');
            }
        }
        String key = sb.toString();
        Stroke s = strokes.get(key);
        if (s == null) {
            s = new BasicStroke(w, CAP_SQUARE, JOIN_MITER, 10.0f, dashes, 0.0f);
            strokes.put(key, s);
        }
        return s;
    }
    
    protected void setShape(Shape s) {
        this.s = s;        
    }
    public abstract void setContainerDimension(int width, int height);
            
    protected Shape getShape() {
        return s;
    }

    public void setStrokeProperties(float width, float[] dashes) {
        stroke = getStroke(width, dashes);
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStrokeColor(Color c) {
        strokeColor = c;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }
    
    public void setFillColor(Color c) {
        fillColor = c;
    }
    
    public Color getFillColor() {
        return fillColor;
    }

    
    public void render(Graphics2D g2d) {
        if (s == null)
            throw new Error(getClass().getName()+": it's null!");
        if (fillColor != null) {
            g2d.setColor(fillColor);
            g2d.fill(s);
        }
        if (strokeColor != null) 
            g2d.setColor(strokeColor);

        if (stroke != null) {
            g2d.setStroke(stroke);
        }
        g2d.draw(s);
    }
}
