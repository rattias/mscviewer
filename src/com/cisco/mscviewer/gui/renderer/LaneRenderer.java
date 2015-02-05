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
import java.util.HashMap;

public class LaneRenderer extends InteractionRenderer {
    final static int MAX_LANES = 1280;
    private static final HashMap<Entity, String[]> hm = new HashMap<Entity, String[]>();
    private int lane;
    private final static int LANE_WIDTH = 10;
    BasicStroke basic, basic2, thickStroke;
    private final static BasicStroke dashedStroke =  new BasicStroke(1.0f, 
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL, 1.0f,
            new float[] { 3.0f, 3.0f, 3.0f, 3.0f },
            0.0f);
    int[] xp = new int[4];
    int[] yp = new int[4];

    @Override
    public void setColor(Color c) {
    }

    @Override
    public void setup(JSonObject props, Event ev) {
        float strokeWidth = 1.0f;
        if (props != null) {
            JSonNumberValue stroke = ((JSonNumberValue)props.get("stroke_width"));
            if (stroke != null)
                strokeWidth = stroke.floatValue();
        }
        basic = new BasicStroke(strokeWidth);
        basic2 = new BasicStroke(strokeWidth+2);
        thickStroke = getBasicStroke((int)strokeWidth+6, false);        

        if (getInteraction().getFromEvent() == ev) {
            Entity en = ev.getEntity();
            String lanes[] = hm.get(en);
            if (lanes == null) {
                lanes = new String[MAX_LANES];
                hm.put(en,lanes);
            }
            if (props != null) {
                JSonValue jpairingId = props.get("pairing_id");
                if (jpairingId != null) {
                    String pairingId = jpairingId.toString();
                    for(int i=0; i<MAX_LANES; i++) {
                        if (lanes[i] == null || lanes[i].equals(pairingId)) {
                            lanes[i] = pairingId;
                            lane = i;
                            if (lane > MAX_LANES/10)
                                System.err.println("WARNING: Entity "+en.getPath()+"  using more than "+lane+" lanes.");
                            return;
                        }
                    }
                    throw new Error(MAX_LANES+" lanes already taken!");
                }
            }
        } else {
            Entity en = ev.getEntity();
            String lanes[] = hm.get(en);
            if (lanes != null)
                lanes[lane] = null;
        }
    }


    private int computePoints(int[] x, int y[], Rectangle b1, Rectangle b2) {
        if (b1.x < 0) {
            // source is not visible
            x[0] = b2.x+b2.width;
            y[0] = b2.y+b2.height/2-2; 
            x[1] = x[0]+(lane+1)*LANE_WIDTH;
            y[1] = y[0]; 
            x[2] = x[1];
            y[2] = y[1]-32; 
            return 3;
        } else if (b2.x < 0) {
            // sink is not visible
            x[0] = b1.x+b1.width;
            y[0] = b1.y+b1.height/2+2; 
            x[1] = x[0]+(lane+1)*LANE_WIDTH;
            y[1] = y[0]; 
            x[2] = x[1];
            y[2] = y[1]+32;
            return 3;
        } else {
            int v1x = b1.x+b1.width/2;
            int v2x = b2.x+b2.width/2;
            y[0] = b1.y+b2.height/2+2;
            y[1] = y[0];
            y[2] = b2.y+b2.height/2-2;
            y[3] = y[2];
            if (v1x < v2x) {
                x[0] = b1.x+b1.width;
                x[1] = x[0]+(lane+1)*LANE_WIDTH;
                x[2] = x[1];
                x[3] = b2.x;
            } else if (v1x > v2x) {
                x[0] = b1.x;
                x[1] = x[0]-(lane+1)*LANE_WIDTH;
                x[2] = x[1];
                x[3] = b2.x+b2.width;
            } else {
                x[0] = b1.x+b1.width;
                x[1] = x[0]+(lane+1)*LANE_WIDTH;
                x[2] = x[1];
                x[3] = b2.x+b2.width;
            }
            return 4;
        }			
    }

    @Override
    public void render(Rectangle b1,Rectangle b2, Graphics2D g2d, boolean isSelected, Marker m) {
        int count = computePoints(xp, yp, b1, b2);
        Stroke st = g2d.getStroke();
        if (isSelected) {
            g2d.setStroke(basic2);
            g2d.setColor(EventRenderer.SELECTION_COLOR);
            for(int i=0; i<count-1; i++) {
                g2d.drawLine(xp[i], yp[i], xp[i+1], yp[i+1]);
            }
        }
        if (b1.x<0 || b2.x< 0)
            g2d.setStroke(dashedStroke);
        else
            g2d.setStroke(basic);
        g2d.setColor(Color.BLUE);
        for(int i=0; i<count-1; i++) {
            g2d.drawLine(xp[i], yp[i], xp[i+1], yp[i+1]);
        }
        if (m!=null) {
            Color c = m.getTransparentColor();
            g2d.setColor(c);
            g2d.setStroke(thickStroke);
            for(int i=0; i<count-1; i++) {
                g2d.drawLine(xp[i], yp[i], xp[i+1], yp[i+1]);
            }
        }
        g2d.setStroke(st);
    }



    @Override
    public boolean inSelectionArea(Rectangle b1, Rectangle b2, int px, int py, boolean self) {
        int count = computePoints(xp, yp, b1, b2);
        for(int i=0; i<count-1; i++) {
            if (xp[i] == xp[i+1] &&
                    Math.abs(px-xp[i])<4 && 
                    ((py >= yp[i] && py <= yp[i+1]) ||
                            (py <= yp[i] && py >= yp[i+1]))) 
                return true;
            if (yp[i] == yp[i+1] &&
                    Math.abs(py-yp[i])<4 && 
                    ((px >= xp[i] && px <= xp[i+1]) ||
                            (px <= xp[i] && px >= xp[i+1]))) 
                return true;
        }
        return false;
    }
}
