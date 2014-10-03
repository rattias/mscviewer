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
package com.cisco.mscviewer.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.plaf.ScrollBarUI;

import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.ViewModel;
import com.cisco.mscviewer.model.EntityHeaderModelListener;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.MSCDataModel;


@SuppressWarnings("serial")
class MarkerBar extends JPanel implements EntityHeaderModelListener, MouseListener {
    final static int markHeight = 4;
    final static int markWidth = 16;
    private final ViewModel viewModel;
    private final EntityHeader entityHeader;
    private final MSCDataModel model;
    private int topOffset=0, btmOffset=0; 
    private final MSCRenderer r;

    MarkerBar(EntityHeader eh, ViewModel m, MSCRenderer r) {
        this.entityHeader = eh;
        this.viewModel = m;        
        this.r = r;
        this.model = viewModel.getMSCDataModel();
        addMouseListener(this);
    }


    @Override
    public Dimension getPreferredSize() {
        return new Dimension(markWidth, -1);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        JScrollBar jsb = new JScrollBar();
        ScrollBarUI uui = jsb.getUI();
        Dimension d = uui.getPreferredSize(jsb);
        if (d != null) {
            topOffset = (int)d.getWidth()+entityHeader.getHeight();
            btmOffset = (int)d.getWidth();
        }

        if (model == null)
            return;
        g.setColor(Color.gray);
        g.drawLine(0, topOffset-1, getWidth(), topOffset-1);
        g.drawLine(0, getHeight()-btmOffset-1, getWidth(), getHeight()-btmOffset-1);

        int h = getHeight()-(topOffset+btmOffset);
        int evCount = viewModel.getEventCount();
        if (evCount == 0)
            return;
        int markerCellCount= h/markHeight+1;
        int prevCellIndex = -1;
        Marker[] um = null;
        for(int i=0; i<evCount; i++) {
            Event ev = viewModel.getEventAt(i);
            int cellIndex = i*markerCellCount/evCount;
            // flush pending marker
            if (cellIndex != prevCellIndex) {
                if (um != null) {
                    int cnt=0;
                    for (Marker um1 : um) {
                        if (um1 != null) {
                            cnt++;
                        }
                    }
                    if (cnt != 0) {
                        int y = topOffset+cellIndex*h/markerCellCount;                    
                        int mx=0;
                        int w = markWidth/cnt;
                        for (Marker um1 : um) {
                            if (um1 != null) {
                                g.setColor(um1.getColor());
                                g.fillRect(mx, y, w, markHeight);
                                mx += w;
                            }
                        }
                    }
                    prevCellIndex = cellIndex;
                }
                um = null;
            }
            if (viewModel.indexOf(ev.getEntity())< 0)
                continue;
            Marker m = ev.getMarker();
            if (m == null)
                continue;
            if (um == null)
                um = new Marker[Marker.values().length];
            um[m.ordinal()] = m;
        }
    }

    private int getEventIndexAt(int xx, int yy) {
        JScrollBar jsb = new JScrollBar();
        ScrollBarUI uui = jsb.getUI();
        Dimension d = uui.getPreferredSize(jsb);
        if (d != null) {
            topOffset = (int)d.getWidth()+entityHeader.getHeight();
            btmOffset = (int)d.getWidth();
        }

        if (model == null)
            return -1;

        int h = getHeight()-(topOffset+btmOffset);
        int evCount = viewModel.getEventCount();
        if (evCount == 0)
            return -1;
        int markerCellCount= h/markHeight+1;
        for(int i=0; i<evCount; i++) {
            Event ev = viewModel.getEventAt(i);
            if (viewModel.indexOf(ev.getEntity()) < 0)
                continue;
            Marker m = ev.getMarker();
            if (m == null)
                continue;
            int cellIndex = i*markerCellCount/evCount;
            int y = topOffset+cellIndex*h/markerCellCount;                    
            if (xx>=0 && xx <markWidth && yy>=y && yy<y+markHeight) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        //		int idx = getEventIndexAt(e.getX(), e.getY());
        //		if (idx >=0)
        //			//dataModel.
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int idx = getEventIndexAt(e.getX(), e.getY());
        if (idx >=0)
            r.setSelectedEventByViewIndex(idx);

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void entityAdded(ViewModel eh, Entity en, int idx) {
        repaint();

    }

    @Override
    public void entityRemoved(ViewModel eh, Entity en, int idx) {
        repaint();

    }

    @Override
    public void entitySelectionChanged(ViewModel eh, Entity en, int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getEntityHeaderModelNotificationPriority() {
        return 0;
    }


    @Override
    public void boundsChanged(ViewModel entityHeaderModel, Entity en,
            int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void entityMoved(ViewModel eh, Entity en, int toIdx) {
    }



}
