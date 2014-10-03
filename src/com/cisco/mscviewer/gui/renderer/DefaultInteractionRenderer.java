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
import com.cisco.mscviewer.model.JSonObject;
import com.cisco.mscviewer.model.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Arc2D;

class Segment {
    int x1, y1;
    int x2, y2;
}

public class DefaultInteractionRenderer extends InteractionRenderer {
    private final static int ARROW_TIP_SIDE = 10;
    private final static float ARROW_TIP_WIDTH = 2.8f;
    private final static int STUB_LEN=40; 
    private final static int ARC_BB_WIDTH = 40;
    private boolean straight;
    private boolean drawTip = true;
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
            straight = "true".equals(props.get("straight")); 
            String stroke = (String)props.get("stroke_width");
            if (stroke != null)
                strokeWidth = Float.parseFloat(stroke);
            drawTip = ! "false".equals(props.get("draw_tip"));
            dashed  = "true".equals(props.get("dashed"));
            String col = (String)props.get("color");
            if (col != null) {
                int c = Integer.parseInt(col, 16);
                color = new Color(c);
            }
        }
        int sw = (int)strokeWidth;
        if (! dashed) {
            basic = getBasicStroke(sw, false);
            basic2 = getBasicStroke(sw+4, false);
        } else {
            basic = getBasicStroke(sw, true);
            basic2 = getBasicStroke(sw+4, true);
        }
        dashedStroke =  getBasicStroke(sw, true);
        thickStroke= getBasicStroke(sw+6, false);        
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2, boolean self) {
        if (x1==0 && x2==0)
            System.out.println("!!!");
        double theta;
        if ((!self) || (straight)) {
            g2d.drawLine(x1, y1, x2, y2);
            if (drawTip) {
                float dx = x2-x1;
                float dy = y2-y1;
                theta = Math.atan2(dy, dx);
                int x3 = (int)(x2+ARROW_TIP_SIDE*Math.cos(theta-ARROW_TIP_WIDTH));
                int y3 = (int)(y2+ARROW_TIP_SIDE*Math.sin(theta-ARROW_TIP_WIDTH));
                int x4 = (int)(x2+ARROW_TIP_SIDE*Math.cos(theta+ARROW_TIP_WIDTH));
                int y4 = (int)(y2+ARROW_TIP_SIDE*Math.sin(theta+ARROW_TIP_WIDTH));
                g2d.fillPolygon(new int[]{x2, x3, x4}, new int[]{y2, y3, y4}, 3);
                g2d.drawLine(x2, y2, x3, y3);
                g2d.drawLine(x3, y3, x4, y4);
                g2d.drawLine(x4, y4, x2, y2);
            }
        } else {
            g2d.draw(new Arc2D.Float(x1-ARC_BB_WIDTH/2, y1, ARC_BB_WIDTH, y2-y1+1, -90, 180, Arc2D.OPEN));
            if (drawTip) {
                theta = Math.atan2((y2-y1)/8, -40);
                int x3 = (int)(x2+ARROW_TIP_SIDE*Math.cos(theta-ARROW_TIP_WIDTH));
                int y3 = (int)(y2+ARROW_TIP_SIDE*Math.sin(theta-ARROW_TIP_WIDTH));
                int x4 = (int)(x2+ARROW_TIP_SIDE*Math.cos(theta+ARROW_TIP_WIDTH));
                int y4 = (int)(y2+ARROW_TIP_SIDE*Math.sin(theta+ARROW_TIP_WIDTH));
                g2d.fillPolygon(new int[]{x2, x3, x4}, new int[]{y2, y3, y4}, 3);
                g2d.drawLine(x2, y2, x3, y3);
                g2d.drawLine(x3, y3, x4, y4);
                g2d.drawLine(x4, y4, x2, y2);
            }
        }
    }

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
        // a bounding box where x = -1 indicates the event is either not there, or the entity for 
        // the event is not being displayed
        // a null event indicates the event matching the other one is not in the data set
        Event fev = getInteraction().getFromEvent();
        Event tev = getInteraction().getToEvent();
        boolean self = (fev != null && tev != null && fev.getEntity() == tev.getEntity());
        Segment s= new Segment();
        getSegment(fev, b1, tev, b2, s);
        Stroke st = g2d.getStroke();
        if (isSelected) {
            g2d.setStroke(basic2);
            g2d.setColor(EventRenderer.SELECTION_COLOR);
            drawArrow(g2d, s.x1, s.y1, s.x2, s.y2, self);
        }
        g2d.setColor(color);
        if (fev == null || tev == null) {
            g2d.setStroke(dashedStroke);
            drawArrow(g2d, s.x1, s.y1, s.x2, s.y2, self);
        } else {  
            g2d.setStroke(basic);
            drawArrow(g2d, s.x1, s.y1, s.x2, s.y2, self);
        }
        if (m!=null) {
            Color c = m.getTransparentColor();
            g2d.setColor(c);
            g2d.setStroke(thickStroke);
            drawArrow(g2d, s.x1, s.y1, s.x2, s.y2, self);
        }
        g2d.setStroke(st);
    }


    private double pointSegmentDistance(int cx, int cy, Segment s) {
        double r_numerator = (cx-s.x1)*(s.x2-s.x1) + (cy-s.y1)*(s.y2-s.y1);
        double r_denomenator = (s.x2-s.x1)*(s.x2-s.x1) + (s.y2-s.y1)*(s.y2-s.y1);
        double r = r_numerator / r_denomenator;
        double ss =  ((s.y1-cy)*(s.x2-s.x1)-(s.x1-cx)*(s.y2-s.y1) ) / r_denomenator;

        double distanceLine = Math.abs(ss)*Math.sqrt(r_denomenator);
        double distanceSegment;
        if ( (r >= 0) && (r <= 1) ) {
            distanceSegment = distanceLine;
        } else {

            double dist1 = (cx-s.x1)*(cx-s.x1) + (cy-s.y1)*(cy-s.y1);
            double dist2 = (cx-s.x2)*(cx-s.x2) + (cy-s.y2)*(cy-s.y2);
            if (dist1 < dist2) {
                distanceSegment = Math.sqrt(dist1);
            } else {
                distanceSegment = Math.sqrt(dist2);
            }
        }
        return distanceSegment;
    }

    @Override
    public boolean inSelectionArea(Rectangle b1, Rectangle b2, int px, int py, boolean self) {
        Segment s = new Segment();
        Event fev = getInteraction().getFromEvent();
        Event tev = getInteraction().getToEvent();
        getSegment(fev, b1, tev, b2, s);
        double d;
        if ((!self) || straight) {
            d = pointSegmentDistance(px,py, s);
            return (d < 3);
        } else {
            Arc2D a2din = new Arc2D.Float(s.x1-ARC_BB_WIDTH/2+3, s.y1+3, ARC_BB_WIDTH-6, s.y2-s.y1+1-6, -90, 180, Arc2D.OPEN);		
            Arc2D a2dout = new Arc2D.Float(s.x1-ARC_BB_WIDTH/2-3, s.y1-3, ARC_BB_WIDTH+6, s.y2-s.y1+1+6, -90, 180, Arc2D.OPEN);
            Rectangle r1 = new Rectangle(px, py, 5, 5);
            return ((!a2din.intersects(r1)) && a2dout.intersects(r1));
        }
    }
}
