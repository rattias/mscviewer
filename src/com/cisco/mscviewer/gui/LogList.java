/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
package com.cisco.mscviewer.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.MSCDataModelListener;
import com.cisco.mscviewer.model.ViewModel;

/**
 * @author Roberto Attias
 * @since  Jun 2011
 */

@SuppressWarnings("serial")
class LogListRenderer extends JPanel {
    private Font f;
    private FontMetrics fm;
    private int line;
    private String text;
    private MSCDataModel dm;
    private int numWidth;
    private boolean isSelected;
    private final Color numBackground = new Color(0xE0E0FF); 
    private final Color selBackground = new Color(0xD0D0FF);

    public LogListRenderer(MSCDataModel dm) {
        this.dm = dm;
    }

    public void setText(String txt) {
        this.text = txt;
    }

    public void setNum(int n) {
        line = n;
    }

    private void initFontInfo(Graphics g) {
        f = getFont();
        fm = g.getFontMetrics(f);
        Rectangle2D r = fm.getStringBounds(""+dm.getData().size(), g);
        numWidth = (int)r.getWidth();
    }

    public void invalidate() {
        f = null;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (f == null)
            initFontInfo(g);
        g.setColor(numBackground);
        g.fillRect(0, 0, numWidth+5, getHeight());
        g.setColor(Color.black);
        g.drawString(""+line, 0, fm.getAscent());
        if (isSelected) {
            g.setColor(selBackground);
        } else
            g.setColor(Color.white);
        g.fillRect(numWidth+5, 0, getWidth(), getHeight());
        g.setColor(getForeground());
        g.drawString(text, numWidth+5, fm.getAscent());
    }

    public void setSelected(boolean s) {
        isSelected = s;
    }

    @Override
    public Dimension getPreferredSize() {
        if (f == null)
            initFontInfo(getGraphics());
        return new Dimension(fm.stringWidth(text), fm.getHeight());
    }
}

class LogListCellRenderer implements ListCellRenderer<String> {
    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private LogListRenderer r;
    private MSCDataModel dm;
    
    public LogListCellRenderer(MSCDataModel dm) {
        this.dm = dm;
    }
    
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
            boolean isSelected, boolean cellHasFocus) {
        if (r == null) {
            int sz = list.getModel().getSize();
            if (sz > 0) {
                r = new LogListRenderer(dm);
            }
        }
        String str = value;
        Color c = (str.contains("@msc_event")) ? 
                Color.blue : Color.black;

        r.setNum(index);
        r.setForeground(c);
        r.setText(str);
        r.setSelected(isSelected);
        return r;
    }
    
    public void invalidate() {
        if (r != null)
            r.invalidate();
    }
}

@SuppressWarnings("serial")
class LogListModel extends  AbstractListModel<String> {
    private final ArrayList<String> data;

    public LogListModel(MSCDataModel dm) {
        data = dm.getData();
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public String getElementAt(int index) {
        return data.get(index);
    }		

    public void fireContentsChanged() {
        fireContentsChanged(this, 0, getSize()-1);
    }
}

@SuppressWarnings("serial")
public class LogList extends JList<String> implements SelectionListener, MSCDataModelListener {
    //private MainPanel mp;
    private MSCDataModel dm;
    private ViewModel ehm;
    private LogListCellRenderer cellRenderer;
    
    public LogList(MSCDataModel m, ViewModel _ehm) {
        super(new LogListModel(m));
        dm = m;
        ehm = _ehm;
        dm.addListener(this);
        cellRenderer = new LogListCellRenderer(dm);
        setCellRenderer(cellRenderer);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                @SuppressWarnings("unchecked")
                JList<String> list = (JList<String>)evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Event ev = dm.getEventByLineIndex(index+1);
                    if (ev != null)
                        ehm.add(ev.getEntity());
                } else if (evt.getClickCount() == 3) {   // Triple-click
                    //unused for now
                }
            }
        });
    }

    @Override
    public void eventSelected(MSCRenderer renderer, Event selectedEvent,
            int viewEventIndex, int modelEventIndex) {
        int idx = renderer.getViewModelSelectedEventIndex();
        if (idx >=0) {
            Event ev = renderer.getViewModel().getEventAt(idx);
            int l = ev.getLineIndex()-1;
            int oldIdx = getSelectedIndex();
            if (l != oldIdx) {
                setSelectedIndex(l);
                ensureIndexIsVisible(l);
            }
        }		
    }		

    @Override
    public void interactionSelected(MSCRenderer renderer,
            Interaction selectedInteraction) {
    }

    @Override
    public void entityAdded(MSCDataModel mscDataModel, Entity en) {
    }

    @Override
    public void eventAdded(MSCDataModel mscDataModel, Event ev) {
        //((LogListModel)getModel()).fireContentsChanged();
        repaint();
    }

    @Override
    public void modelChanged(MSCDataModel mscDataModel) {
        if (cellRenderer != null)
            cellRenderer.invalidate();
        ((LogListModel)getModel()).fireContentsChanged();
    }

    @Override
    public void eventsChanged(MSCDataModel mscDataModel) {
        //	((LogListModel)getModel()).fireContentsChanged();
    }
}
