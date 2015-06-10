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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.LogListModel;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.MSCDataModelListener;
import com.cisco.mscviewer.model.ViewModel;

/**
 * @author Roberto Attias
 * @since Jun 2011
 */

@SuppressWarnings("serial")
class LogListRenderer extends JPanel {
    private Font f;
    private FontMetrics fm;
    private int line;
    private String text;
    private final MSCDataModel dm;
    private int numWidth;
    private boolean isSelected;
    private final Color numBackground = new Color(0xE0E0FF);
    private final Color selBackground = new Color(0xD0D0FF);
    private int copyBeginIndex;
    private int copyBeginOffset;
    private int copyEndIndex;
    private int copyEndOffset;
    private int currLineIdx;

    public LogListRenderer(MSCDataModel dm) {
        this.dm = dm;
    }

    public void setLine(int lineIdx, String txt) {
        this.currLineIdx = lineIdx;
        this.text = txt;
    }

    public void setNum(int n) {
        line = n;
    }

    private void initFontInfo(Graphics g) {
        f = getFont();
        fm = g.getFontMetrics(f);
        final Rectangle2D r = fm.getStringBounds("" + dm.getSourceLineCount(), g);
        numWidth = (int) r.getWidth() + 5;
    }

    public int getNumWidth() {
        return numWidth;
    }
    
    public FontMetrics getFontMetrics() {
        return fm;
    }
    
    @Override
    public void invalidate() {
        f = null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (f == null)
            initFontInfo(g);
        g.setColor(numBackground);
        g.fillRect(0, 0, numWidth, getHeight());
        g.setColor(Color.black);
        g.drawString("" + line, 0, fm.getAscent());
        int bi, bo, ei, eo;
        int[] v = getCopyInfo();
        bi = v[0];
        bo = v[1];
        ei = v[2];
        eo = v[3];
        if (currLineIdx<bi || currLineIdx>ei) {
            if (isSelected) {
                g.setColor(selBackground);
            } else
                g.setColor(Color.white);
            g.fillRect(numWidth, 0, getWidth(), getHeight());
            g.setColor(getForeground());
            g.drawString(text, numWidth, fm.getAscent());
        } else {
            String left, center, right;
            if (currLineIdx == bi && bi == ei) {
                left = text.substring(0, bo);
                center = text.substring(bo, eo);
                right = text.substring(eo);
            } else if (currLineIdx == bi) {
                left = text.substring(0, bo);
                center = text.substring(bo);
                right = "";
            } else if (currLineIdx == ei) {
                left = "";
                center = text.substring(0, eo);
                right = text.substring(eo);
            } else {
                left = "";
                center = text;
                right = "";
            }            
            // draw left part
            if (isSelected) {
                g.setColor(selBackground);
            } else
                g.setColor(Color.white);
            int x = numWidth;
            int w = fm.stringWidth(left);
            g.fillRect(x, 0, w, getHeight());
            g.setColor(getForeground());
            g.drawString(left, x, fm.getAscent());
            
            // draw center part
            x += w;
            w = fm.stringWidth(center);
            g.setColor(Color.green);
            g.fillRect(x, 0, w, getHeight());
            g.setColor(getForeground());
            g.drawString(center, x, fm.getAscent());                

            // draw right part
            x += w;
            w = fm.stringWidth(right);
            if (isSelected) {
                g.setColor(selBackground);
            } else
                g.setColor(Color.white);
            g.fillRect(x, 0, w, getHeight());
            g.setColor(getForeground());
            g.drawString(right, x, fm.getAscent());                
        }
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

    public void setCopyBegin(int index, int off) {
        copyBeginIndex = index;
        copyBeginOffset = off;
    }

    public void setCopyEnd(int index, int off) {
        copyEndIndex = index;
        copyEndOffset = off;
    }

    public int[] getCopyInfo() {
        int[] v;
        if (copyBeginIndex < copyEndIndex) {
            v = new int[]{copyBeginIndex, copyBeginOffset, copyEndIndex, copyEndOffset};
        } else if (copyBeginIndex > copyEndIndex) {
            v = new int[]{copyEndIndex, copyEndOffset, copyBeginIndex, copyBeginOffset};
        } else if (copyBeginOffset < copyEndOffset) {
            // index is same
            v = new int[]{copyBeginIndex, copyBeginOffset, copyEndIndex, copyEndOffset};
        } else {
            // index is same
            v = new int[]{copyEndIndex, copyEndOffset, copyBeginIndex, copyBeginOffset};
        }

        return v;
    }
    
    public void clearCopy() {
        copyBeginIndex = -1;
        copyBeginOffset = -1;
        copyEndIndex = -1;
        copyEndOffset = -1;
    }
}

class LogListCellRenderer implements ListCellRenderer<String> {
    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private LogListRenderer r;
    private final MSCDataModel dm;

    public LogListCellRenderer(MSCDataModel dm) {
        this.dm = dm;
    }

    public LogListRenderer getRenderer() {
        return r;
    }
    
    @Override
    public Component getListCellRendererComponent(JList<? extends String> list,
            String value, int index, boolean isSelected, boolean cellHasFocus) {
        if (r == null) {
            final int sz = list.getModel().getSize();
            if (sz > 0) {
                r = new LogListRenderer(dm);
            }
        }
        final String str = value;
        Color c = Color.black;
        if (str != null && (str.contains("@msc_event") || str.contains("@event"))) 
            c = Color.blue;

        r.setNum(index);
        r.setForeground(c);
        r.setLine(index, str);
        r.setSelected(isSelected);
        return r;
    }

    public void invalidate() {
        if (r != null)
            r.invalidate();
    }
}


@SuppressWarnings("serial")
public class LogList extends JList<String> implements SelectionListener,
        MSCDataModelListener {
    private final MSCDataModel dm;
    private final ViewModel ehm;
    private final LogListCellRenderer cellRenderer;
 
    public LogList(MSCDataModel m, ViewModel _ehm) {
        super(m.getLogListModel());
        dm = m;
        ehm = _ehm;
        dm.addListener(this);
        cellRenderer = new LogListCellRenderer(dm);
        setCellRenderer(cellRenderer);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                @SuppressWarnings("unchecked")
                final
                JList<String> list = (JList<String>) evt.getSource();
                if (evt.getClickCount() == 2) {
                    final int index = list.locationToIndex(evt.getPoint());
                    final Event ev = dm.getEventByLineIndex(index + 1);
                    if (ev != null)
                        ehm.add(ev.getEntity());
                } else if (evt.getClickCount() == 3) { // Triple-click
                    // unused for now
                }
            }
           
            @Override
            public void mousePressed(MouseEvent evt) {
                LogListRenderer r = cellRenderer.getRenderer();
                if (r == null)
                    return;
                @SuppressWarnings("unchecked")
                JList<String> list = (JList<String>) evt.getSource();
                final int index = list.locationToIndex(evt.getPoint());
                String line = dm.getLogListModel().getElementAt(index);
                int x = evt.getX();
                int off = getLineOffset(line, x);
                if (off != -1) {
                    r.setCopyBegin(index, off);
                    r.setCopyEnd(index, off);
                }
            }
            @Override
            public void mouseReleased(MouseEvent evt) {
                LogListRenderer r = cellRenderer.getRenderer();
                @SuppressWarnings("unchecked")
                JList<String> list = (JList<String>) evt.getSource();

                if (r == null)
                    return;
                StringBuilder sb = new StringBuilder();
                int[] ci = r.getCopyInfo();
                int bi = ci[0];
                int bo = ci[1];
                int ei = ci[2];
                int eo = ci[3];
                if (bi == ei)
                    sb.append(list.getModel().getElementAt(bi).substring(bo, eo));
                else {
                    sb.append(list.getModel().getElementAt(bi).substring(bo));
                    for (int i=bi; i<ei; i++) {
                        sb.append(list.getModel().getElementAt(i));
                    }
                    sb.append(list.getModel().getElementAt(ei).substring(0, eo));
                }
                Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(sb.toString());
                clpbrd.setContents(stringSelection, null);
                r.clearCopy();
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent evt) {
                LogListRenderer r = cellRenderer.getRenderer();
                if (r == null)
                    return;
                @SuppressWarnings("unchecked")
                JList<String> list = (JList<String>) evt.getSource();
                final int index = list.locationToIndex(evt.getPoint());
                String line = dm.getLogListModel().getElementAt(index);
                int x = evt.getX();
                int off = getLineOffset(line, x);
                if (off != -1)
                    r.setCopyEnd(index, off);
                repaint();
            }
        });
    }

    private int getLineOffset(String line, int x) {
        LogListRenderer r = cellRenderer.getRenderer();
        int xOff = r.getNumWidth();
        if (x < xOff)
            return -1;
        FontMetrics fm = r.getFontMetrics();                
        int idx;
        for(idx = 0; idx < line.length(); idx++) {
            int w = fm.charWidth(line.charAt(idx));
            if (x >= xOff && x < xOff+w)
                break;
            xOff += w;
        }
        return (idx < line.length()) ? idx : -1;                
    }
    
    @Override
    public void eventSelected(MSCRenderer renderer, Event selectedEvent,
            int viewEventIndex, int modelEventIndex) {
        final int idx = renderer.getViewModelSelectedEventIndex();
        if (idx >= 0) {
            final Event ev = renderer.getViewModel().getEventAt(idx);
            final int l = ev.getLineIndex() - 1;
            final int oldIdx = getSelectedIndex();
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
        // ((LogListModel)getModel()).fireContentsChanged();
        repaint();
    }

    @Override
    public void modelChanged(MSCDataModel mscDataModel) {
        if (cellRenderer != null)
            cellRenderer.invalidate();
        LogListModel lm = (LogListModel)getModel();
        lm.fireContentsChanged();
    }

    @Override
    public void eventsChanged(MSCDataModel mscDataModel) {
        // ((LogListModel)getModel()).fireContentsChanged();
    }
}
