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
import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.model.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

public class RectangleInteractionRenderer extends InteractionRenderer {
//    private final static int ARROW_TIP_SIDE = 10;
//    private final static float ARROW_TIP_WIDTH = 2.8f;
    private final static int STUB_LEN=40; 
//    private final static int ARC_BB_WIDTH = 40;
    private boolean dashed = false;
    Color color = Color.blue;
    BasicStroke basic, basic2, dashedStroke, thickStroke;

    @Override
    public void setColor(Color c) {
        color = c;
    }

    @Override
    public void setup(JSonObject props, Event ev) {
        float strokeWidth = 1.0f;
        if (props != null) {
            Integer stroke = (Integer)props.get("stroke_width");
            if (stroke != null)
                strokeWidth = stroke;
            Boolean ds = (Boolean)props.get("dashed");
            if (ds)
                dashed = true;
            String col = (String)props.get("color");
            if (col != null) {
                int c = Integer.parseInt(col, 16);
                color = new Color(c);
            }
        }
        if (! dashed) {
            basic = new BasicStroke(strokeWidth);
            basic2 = new BasicStroke(strokeWidth+4);
        } else {
            basic = new BasicStroke(strokeWidth,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 1.0f,
                    new float[] { 3.0f, 3.0f, 3.0f, 3.0f },
                    0.0f);
            basic2 = new BasicStroke(strokeWidth+4,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 1.0f,
                    new float[] { 3.0f, 3.0f, 3.0f, 3.0f },
                    0.0f);
        }
        dashedStroke =  new BasicStroke(strokeWidth, 
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 1.0f,
                new float[] { 3.0f, 3.0f, 3.0f, 3.0f },
                0.0f);
        thickStroke= new BasicStroke(strokeWidth+6);
    }


    @SuppressWarnings("unused")
    private void getSegment(Event fev, Rectangle b1, Event tev, Rectangle b2, Segment s) {
        boolean self = (fev != null && tev != null && fev.getEntity() == tev.getEntity());
        if (self) {
            s.x1 = b1.x+b1.width/2;
            s.y1 = b1.y+b1.height;
            s.x2 = b2.x+b2.width/2;
            s.y2 = b2.y;
        } else if (b1.x >= 0 && b2.x >= 0) {
            s.x1 = b1.x+b1.width;
            s.y1 = b1.y+b1.height/2;
            s.x2 = b2.x+b2.width;
            if (s.x1<s.x2) {
                s.x2 = b2.x;
                s.y2 = b2.y+b2.height/2;
            } else if (s.x1>s.x2){
                s.x1 = b1.x;
                s.y2 = b2.y+b2.height/2;
            } else {
                s.y2 = b2.y+b2.height/2;
            }
        } else if (b1.x >=0) {
            s.x1 = b1.x+b1.width;
            s.y1 = b1.y+b1.height/2;
            s.x2 = s.x1 + STUB_LEN;
            s.y2 = b1.y + b1.height;			
        } else {
            s.x2 = b2.x;
            s.y2 = b2.y+b2.height/2;
            s.x1 = s.x2 - STUB_LEN;
            s.y1 = b2.y;
        }		
    }

    @Override
    public void render(Rectangle b1,Rectangle b2, Graphics2D g2d, boolean isSelected, Marker m) {
        Rectangle r;
        if (b1.x >= 0) {
            r = new Rectangle(b1);
            if (b2.x >= 0) 
                r.add(b2);
        } else { 
            if (b2.x >= 0)
                r = new Rectangle(b2);
            else {
                return;
            }
        }
        r.x--;
        r.y--;
        r.width++;
        g2d.setColor(Color.white);
        g2d.fill(r);
        Stroke st = g2d.getStroke();
        if (isSelected) {
            g2d.setStroke(basic2);
            g2d.setColor(EventRenderer.SELECTION_COLOR);
            g2d.draw(r);
        }
        g2d.setColor(color);
        g2d.setStroke(basic);
        g2d.draw(r);
        if (m!=null) {
            Color c = m.getTransparentColor();
            g2d.setColor(c);
            g2d.setStroke(thickStroke);
            g2d.draw(r);
        }
        g2d.setStroke(st);
    }


    @Override
    public boolean inSelectionArea(Rectangle b1, Rectangle b2, int px, int py, boolean self) {
        return false;
    }

}
