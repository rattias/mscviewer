/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;

import com.cisco.mscviewer.gui.renderer.BlockInteractionRenderer;
import com.cisco.mscviewer.gui.renderer.ErrorRenderer;
import com.cisco.mscviewer.gui.renderer.EventRenderer;
import com.cisco.mscviewer.gui.renderer.InteractionRenderer;
import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.InputUnit;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.MSCDataModelEventFilter;
import com.cisco.mscviewer.model.OutputUnit;
import com.cisco.mscviewer.model.ViewModel;
import com.cisco.mscviewer.tree.Interval;
import com.cisco.mscviewer.tree.IntervalTree;
import com.cisco.mscviewer.util.Resources;
import com.cisco.mscviewer.util.StyledDocumentUtils;

public class MSCRenderer {
    public final static int EVENT_HEIGHT = 20;
    private int eventHeight = EVENT_HEIGHT;
    // private EntityHeaderModel headerModel;
    private Event selectedEvent = null;
    private Interaction selectedInteraction = null;
    private int viewModelSelectedEventIndex = -1;
    private boolean showTime = true;
    private boolean showBlocks = true;
    private boolean showLabels = true;
    private boolean showNotes = true;
    private OutputUnit absTimeUnit = OutputUnit.H_M_S_MS;
    private InputUnit deltaTimeUnit = InputUnit.NS;
    // private InputUnit timestampUnit = InputUnit.NS;
    private final boolean drawBands = true;
    private boolean showUnits = true;
    private boolean showDate = false;
    private boolean showLeadingZeroes;
    private boolean compactView = true;
    private final Vector<SelectionListener> selListeners = new Vector<SelectionListener>();
    private int maxBBwidth = 0;
    private MSCDataModelEventFilter filter;
    private final ViewModel viewModel;
    private final ImageIcon infoIcon;
    private int zoomFactor = 100;
    private Font font;
    private Font mainFont;
    private final boolean timeProportional = false;

    final static BasicStroke basicStroke = new BasicStroke();
    final static BasicStroke selStroke = new BasicStroke(2.0f);
    final static BasicStroke dashedStroke = new BasicStroke(1.0f, // line width
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
                    3.0f, 3.0f, 3.0f, 3.0f }, 0.0f);
    final static int STUB_LEN = 40;

    class Segment {
        int x1, y1;
        int x2, y2;
        Stroke stroke;
        boolean labelAtSource;

        @Override
        public String toString() {
            return ("[" + x1 + "," + y1 + "]-[" + x2 + "," + y2 + "]");
        }
    }

    public MSCRenderer(ViewModel eh) {
        viewModel = eh;
        infoIcon = Resources.getImageIcon("32x32/info.png", "Info icon");
    }

    // public MSCRenderer() {
    // }

    // public void setDataModel(MSCDataModel dm) {
    // dataModel = dm;
    // updateForTimeUnitChanges();
    // }

    public int getEventHeight() {
        return eventHeight;
    }

    private Rectangle getEventBoundingBox(Event ev, int eventIndex,
            Dimension maxDim) {
        final int entityIndex = viewModel.indexOf(ev.getEntity());
        if (entityIndex < 0)
            return new Rectangle(-1, -1, 0, 0);
        final EventRenderer rn = ev.getRenderer();
        final Rectangle r = new Rectangle();
        final int x = viewModel.getEntityCenterX(entityIndex);
        final int y = (timeProportional) ? (int) ev.getTimestamp() : eventIndex
                * eventHeight + eventHeight / 2;
        rn.getBoundingBox(maxDim, x, y, r);
        return r;
    }

    private void drawLifeLines(Graphics2D g2d, boolean export, int minIdx,
            int maxIdx) {
        final int hdEntityCount = viewModel.entityCount();
        g2d.setColor(Color.lightGray);
        for (int i = 0; i < hdEntityCount; i++) {
            final int x = viewModel.getEntityCenterX(i);
            int y0, y1;
            if (export) {
                y0 = 0;
                y1 = getHeight();
            } else {
                if (timeProportional) {
                    y0 = (int) viewModel.getEventAt(
                            viewModel.getEntityBirthIndex(i)).getTimestamp();
                    y1 = (int) viewModel.getEventAt(
                            viewModel.getEntityBirthIndex(i)).getTimestamp();
                } else {
                    y0 = viewModel.getEntityBirthIndex(i) * eventHeight
                            + eventHeight / 2;
                    y1 = viewModel.getEntityDeathIndex(i) * eventHeight
                            + eventHeight / 2;
                }
                if (y0 < 0)
                    y0 = 0;
                if (y1 < 0)
                    y1 = getHeight();
            }
            g2d.drawLine(x, y0, x, y1);
        }
    }

    public void renderTimeHeader(Graphics2D g2d, boolean renderHeader,
            int viewY, int viewHeight) {
        final int evCount = viewModel.getEventCount();
        final int minIdx = viewY / eventHeight;
        int maxIdx = (viewY + viewHeight - 1) / eventHeight + 1;
        if (maxIdx > evCount - 1)
            maxIdx = evCount - 1;

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, viewModel.getTotalWidth(), getHeight());
        if (drawBands) {
            g2d.setColor(new Color(248, 248, 248));
            for (int i = minIdx; i <= maxIdx; i++) {
                if (i % 2 == 1) {
                    final int h = i * eventHeight;
                    g2d.fillRect(0, h, viewModel.getTotalWidth(),
                            eventHeight - 1);
                }
            }
        }
    }

    public String getTimeRepr(long timestamp) {
        return absTimeUnit.format(timestamp);
    }

    public void updateForTimeUnitChanges() {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            final int cnt = dataModel.getEventCount();
            for (int i = 0; i < cnt; i++) {
                final Event ev = dataModel.getEventAt(i);
                ev.setTimestampRepr(getTimeRepr(ev.getTimestamp()));
            }
        }
    }

    private void render(Graphics2D g2d, int viewMinIdx, int viewMaxIdx) {
        if (mainFont == null) {
            mainFont = g2d.getFont();
        }
        final AffineTransform tf = new AffineTransform();
        tf.scale(1, zoomFactor / 100.0);
        font = mainFont.deriveFont(tf);
        g2d.setFont(font);
        final int ascent = g2d.getFontMetrics().getAscent();
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            // render blocks first
            final Dimension max = new Dimension(64, eventHeight);
            int modelMinIdx = viewModel.getModelIndexFromViewIndex(viewMinIdx);
            final int modelMaxIdx = viewModel.getModelIndexFromViewIndex(viewMaxIdx);
            if (showBlocks) {
                IntervalTree.dbg = true;
                final ArrayList<Interval> al = dataModel.getBlocksInInterval(
                        modelMinIdx, modelMaxIdx);
                IntervalTree.dbg = false;
                for (final Interval block : al) {
                    final int beginIdx = block.getStart();
                    final int endIdx = block.getEnd();
                    final int beginViewIdx = viewModel
                            .getViewIndexFromModelIndex(beginIdx);
                    final int endViewIdx = viewModel
                            .getViewIndexFromModelIndex(endIdx);
                    final Event beginEv = dataModel.getEventAt(beginIdx);
                    final Event endEv = dataModel.getEventAt(endIdx);
                    final Entity entity = beginEv.getEntity();
                    final int entityIndex = viewModel.indexOf(entity);
                    if (entityIndex < 0)
                        continue;
                    final int x = viewModel.getEntityCenterX(entityIndex);
                    final int y0 = beginViewIdx * eventHeight + eventHeight / 2;
                    final int y1 = endViewIdx * eventHeight + eventHeight / 2;
                    final Rectangle rb = beginEv.getRenderer().getBoundingBox(max, x,
                            y0, null);
                    final Rectangle re = endEv.getRenderer().getBoundingBox(max, x,
                            y1, null);
                    final Rectangle r = rb.union(re);
                    g2d.setColor(Color.lightGray);
                    g2d.fillRect(x - 3, r.y, 7, r.height - 4);
                    g2d.setColor(Color.gray);
                    g2d.drawRect(x - 3, r.y, 7, r.height - 4);
                }
            }

            // render interactions
            final ArrayList<Interaction> inter = dataModel.getInteractionsInInterval(
                    modelMinIdx, modelMaxIdx);
            Rectangle r1, r2;
            for (final Interaction in : inter) {
                final int sourceModelIdx = in.getFromIndex();
                final int sourceViewIdx = viewModel
                        .getViewIndexFromModelIndex(sourceModelIdx);
                final int sinkModelIdx = in.getToIndex();
                final int sinkViewIdx = viewModel
                        .getViewIndexFromModelIndex(sinkModelIdx);
                if (sourceViewIdx < 0 && sinkViewIdx < 0)
                    continue;
                if (sourceViewIdx >= 0) {
                    final Event source = dataModel.getEventAt(sourceModelIdx);
                    final Entity sourceEn = source.getEntity();
                    final int sourceEntityIndex = viewModel.indexOf(sourceEn);
                    final Dimension maxDim = new Dimension(
                            viewModel.getEntityWidth(sourceEntityIndex),
                            eventHeight);
                    r1 = getEventBoundingBox(source, sourceViewIdx, maxDim);
                } else
                    r1 = new Rectangle(-1, 0, 0, 0);
                if (sinkViewIdx >= 0) {
                    final Event sink = dataModel.getEventAt(sinkModelIdx);
                    final Entity sinkEn = sink.getEntity();
                    final int sinkEntityIndex = viewModel.indexOf(sinkEn);
                    final Dimension maxDim = new Dimension(
                            viewModel.getEntityWidth(sinkEntityIndex),
                            eventHeight);
                    r2 = getEventBoundingBox(sink, sinkViewIdx, maxDim);
                } else
                    r2 = new Rectangle(-1, 0, 0, 0);
                final InteractionRenderer ir = in.getIRenderer();
                final Marker m = in.getMarker();
                ir.render(r1, r2, g2d, in == selectedInteraction, m);
            }

            // render events 
            for (int i = viewMinIdx; i <= viewMaxIdx; i++) {
                final Event ev = viewModel.getEventAt(i);
                final Entity en = ev.getEntity();
                final int entityIndex = viewModel.indexOf(en);
                if (entityIndex == -1)
                    continue;
                final int entityWidth = viewModel.getEntityWidth(entityIndex);
                final Dimension maxDim = new Dimension(entityWidth, eventHeight);
                final EventRenderer r = ev.getRenderer();
                if (entityIndex >= 0) {
                    final int x = viewModel.getEntityCenterX(entityIndex);
                    final int y = i * eventHeight + eventHeight / 2;
                    // g2d.drawString("ev_"+i, 0, y);
                    // g2d.drawString(ev.getType(), 0, y);
                    final AffineTransform t = g2d.getTransform();
                    g2d.translate(x, y);
                    final boolean scaled = r.scaleSource();
                    AffineTransform t1 = null;
                    if (scaled) {
                        t1 = g2d.getTransform();
                        g2d.scale(.7, .7);
                    }
                    r.render(g2d, maxDim);
                    if (scaled)
                        g2d.setTransform(t1);
                    if (ev == selectedEvent) {
                        g2d.setColor(Color.red);
                        g2d.setStroke(selStroke);
                        g2d.drawRect(-maxBBwidth / 2, -eventHeight / 2,
                                maxBBwidth, eventHeight);
                        g2d.setStroke(basicStroke);
                    }
                    final Marker m = ev.getMarker();
                    if (m != null) {
                        final Color c = m.getTransparentColor();
                        g2d.setColor(c);
                        g2d.fillRect(-maxBBwidth / 2, -eventHeight / 2,
                                maxBBwidth, eventHeight);
                    }
                    g2d.setTransform(t);
                    final Rectangle bb = new Rectangle();
                    r.getBoundingBox(maxDim, x, y, bb);
                    if (showTime) {
                        final String time = ev.getTimestampRepr();
                        if (time != null) {
                            g2d.setColor(Color.pink);
                            final int w = g2d.getFontMetrics().stringWidth(time);
                            g2d.drawString(time,
                                    x - maxBBwidth / 2 - w - 4, i * eventHeight
                                    + ascent);
                        }
                    }
                    if (ev.getRenderer() instanceof ErrorRenderer)
                        g2d.setColor(Color.RED);
                    else
                        g2d.setColor(Color.BLACK);
                    if (showLabels)
                        g2d.drawString(ev.getLabel(), x + maxBBwidth, i
                                * eventHeight + ascent);
                }
            }
            // render notes
            if (viewMinIdx > 4)
                viewMinIdx -= 4;
            else
                viewMinIdx = 0;
            for (int i = viewMinIdx; i <= viewMaxIdx; i++) {
                final Event ev = viewModel.getEventAt(i);
                final Entity en = ev.getEntity();
                final int entityIndex = viewModel.indexOf(en);
                if (entityIndex == -1)
                    continue;
                final int entityWidth = viewModel.getEntityWidth(entityIndex);
                final Dimension maxDim = new Dimension(entityWidth, eventHeight);
                final EventRenderer r = ev.getRenderer();
                final Rectangle bb = new Rectangle();
                final int x = viewModel.getEntityCenterX(entityIndex);
                final int y = i * eventHeight + eventHeight / 2;
                r.getBoundingBox(maxDim, x, y, bb);            
                if (ev.getNote() != null) {
                    g2d.setColor(Color.yellow);
                    final int W = 10;
                    int miniStickX = bb.x;
                    int miniStickY = bb.y+(bb.height-W)/2;
                    g2d.fillRect(miniStickX, miniStickY, W, W);
                    g2d.setColor(Color.black);
                    g2d.drawRect(miniStickX, miniStickY, W, W);
                    //AffineTransform at = g2d.getTransform();
                    Point p = ev.getNoteOffset();
                    if (p.x == -1 && p.y == -1) {
                        p.x = bb.width+16;
                        p.y = 0;
                        ev.setNotePosition(p);
                    }
                    if (showNotes) {
                        String n = ev.getNote();
                        StyledDocument doc = new DefaultStyledDocument();
                        StyledDocumentUtils.importXML(n, doc);                        
                        JTextPane tp = new JTextPane(doc);
                        Dimension bubbleDim = tp.getPreferredSize();
                        tp.setSize(bubbleDim);
                        BufferedImage bi = new BufferedImage(
                                bubbleDim.width+4,
                                bubbleDim.height+4,
                                BufferedImage.TYPE_INT_ARGB);
                        Graphics g = bi.getGraphics();
                        g.setColor(Color.yellow);
                        g.fillRect(0,  0,  bi.getWidth(), bi.getHeight());
                        g.setColor(Color.black);
                        g.drawRect(0,  0,  bi.getWidth()-1, bi.getHeight()-1);
                        g.translate(2, 2);
                        tp.paint(g);
                        g2d.drawImage(bi, bb.x+p.x, bb.y+p.y, null);
                        int[] xpts = new int[]{miniStickX+W/2, bb.x+p.x, bb.x+p.x, bb.x+p.x-W};
                        int[] ypts = new int[]{miniStickY+W/2, bb.y+p.y, bb.y+p.y+bubbleDim.height, bb.y+p.y+W};
                        g2d.setColor(Color.yellow);
                        Polygon poly = new Polygon(xpts, ypts, 4);
                        g2d.fillPolygon(poly);
                        g2d.setColor(Color.black);
                        g2d.drawPolygon(poly);
                    }
                }
            }
        }
    }

    // private void renderTimeProportional(Graphics2D g2d, int viewMinIdx, int
    // viewMaxIdx) {
    // if (mainFont == null) {
    // mainFont = g2d.getFont();
    // }
    // AffineTransform tf = new AffineTransform();
    // tf.scale(1, zoomFactor/100.0);
    // font = mainFont.deriveFont(tf);
    // g2d.setFont(font);
    // int ascent = g2d.getFontMetrics().getAscent();
    // synchronized(dataModel) {
    // int evCount = viewModel.getEventCount();
    // // render interactions first
    // for(int i=0; i<evCount; i++) {
    // Event ev = viewModel.getEventAt(i);
    // int entityIndex = viewModel.indexOf(ev.getEntity());
    // if (entityIndex < 0 )
    // continue;
    // Dimension maxDim = new Dimension(viewModel.getEntityWidth(entityIndex),
    // eventHeight);
    // Interaction[] incoming = ev.getIncomingInteractions();
    // if (incoming != null) {
    // for(Interaction in: incoming) {
    // // If we traverse events on a filtered view, then the source might not
    // // be there. in that case we do need to render the sink.
    // boolean sourceExists = in.getFromEvent() != null;
    // int sourceViewIdx =
    // viewModel.getViewIndexFromModelIndex(in.getFromIndex());
    // if (sourceExists && (i < viewMinIdx || sourceViewIdx > viewMaxIdx)) {
    // continue;
    // }
    // if ((!sourceExists) && (i<viewMinIdx || i>viewMaxIdx)) {
    // continue;
    // }
    // Rectangle r1 = null, r2;
    // r2 = getEventBoundingBox(ev, i, maxDim);
    // if (in.getFromEvent() != null) {
    // r1 = getEventBoundingBox(in.getFromEvent(), sourceViewIdx, maxDim);
    // } else
    // r1 = new Rectangle(-1, 0, 0, 0);
    // InteractionRenderer ir = in.getIRenderer();
    // ir.render(r1, r2, g2d, in == selectedInteraction);
    // }
    // }
    // Interaction[] outgoing = ev.getOutgoingInteractions();
    // if (outgoing != null && i <= viewMaxIdx) {
    // for(int j=0; j<outgoing.length; j++) {
    // int sinkIdx =
    // viewModel.getViewIndexFromModelIndex(outgoing[j].getToIndex());
    // if (sinkIdx >=0 && sinkIdx < viewMinIdx)
    // continue;
    // Rectangle r1, r2 = null;
    // r1 = getEventBoundingBox(ev, i, maxDim);
    // Event tev = outgoing[j].getToEvent();
    // if (tev != null) {
    // r2 = getEventBoundingBox(tev, sinkIdx, maxDim);
    // } else {
    // r2 = new Rectangle(-1, 0, 0, 0);
    // }
    // InteractionRenderer ir = outgoing[j].getIRenderer();
    // ir.render(r1, r2, g2d, outgoing[j] == selectedInteraction);
    // }
    // }
    // }
    //
    // // render events after
    // for(int i=viewMinIdx; i<=viewMaxIdx; i++) {
    // Event ev = viewModel.getEventAt(i);
    // Entity en = ev.getEntity();
    // int entityIndex = viewModel.indexOf(en);
    // if (entityIndex == -1)
    // continue;
    // int entityWidth = viewModel.getEntityWidth(entityIndex);
    // Dimension maxDim = new Dimension(entityWidth, eventHeight);
    // EventRenderer r = ev.getRenderer();
    // if (entityIndex >=0) {
    // int x = viewModel.getEntityCenterX(entityIndex);
    // int y = i*eventHeight+eventHeight/2;
    // AffineTransform t = g2d.getTransform();
    // g2d.translate(x, y);
    // boolean scaled = (ev.getOutgoingInteractions() != null &&
    // r.scaleSource());
    // AffineTransform t1 = null;
    // if (scaled) {
    // t1 = g2d.getTransform();
    // g2d.scale(.7, .7);
    // }
    // r.render(g2d, maxDim, ev == selectedEvent);
    // if (ev.getNote() != null) {
    // Rectangle bb = r.getBoundingBox(maxDim, 0, 0, null);
    // g2d.drawImage(infoIcon.getImage(), -bb.width/2-8, 0, null);
    // }
    // if (scaled)
    // g2d.setTransform(t1);
    // if (ev == selectedEvent) {
    // g2d.setColor(Color.red);
    // g2d.setStroke(selStroke);
    // g2d.drawRect(-maxBBwidth/2, -eventHeight/2, maxBBwidth, eventHeight);
    // g2d.setStroke(basicStroke);
    // }
    // Marker m = ev.getMarker();
    // if (m != null) {
    // Color c = m.getTransparentColor();
    // g2d.setColor(c);
    // g2d.fillRect(-maxBBwidth/2, -eventHeight/2, maxBBwidth, eventHeight);
    // }
    // g2d.setTransform(t);
    // Rectangle bb = new Rectangle();
    // r.getBoundingBox(maxDim, x, y, bb);
    // if (showTime) {
    // String time = ev.getTimestampRepr();
    // if (time != null) {
    // g2d.setColor(Color.pink);
    // int w = g2d.getFontMetrics().stringWidth(time);
    // g2d.drawString(time, x-maxBBwidth/2-w-4, i*eventHeight+ascent);
    // }
    // }
    // if (ev.getRenderer() instanceof ErrorRenderer)
    // g2d.setColor(Color.RED);
    // else
    // g2d.setColor(Color.BLACK);
    // int labelStyle = ev.getRenderer().getLabelStyle();
    // if (labelStyle != Font.PLAIN) {
    // Font f = g2d.getFont();
    // g2d.setFont(f.deriveFont(labelStyle));
    // g2d.drawString(ev.getLabel(), x+maxBBwidth, i*eventHeight+ascent);
    // g2d.setFont(f);
    // } else {
    // g2d.drawString(ev.getLabel(), x+maxBBwidth, i*eventHeight+ascent);
    // }
    // }
    // }
    // }
    // }

    private void computeMaxBBWidth() {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            final int cnt = viewModel.getEventCount();
            final Rectangle r = new Rectangle();
            for (int i = 0; i < cnt; i++) {
                final Event ev = viewModel.getEventAt(i);
                final Entity en = ev.getEntity();
                int w = viewModel.getEntityWidth(viewModel.indexOf(en));
                if (w < 0)
                    w = 0;
                final Dimension maxDim = new Dimension(w, eventHeight);
                ev.getRenderer().getBoundingBox(maxDim, 0, 0, r);
                if (r.width > maxBBwidth)
                    maxBBwidth = r.width;
            }
        }
    }

    public void updateCache() {
        computeMaxBBWidth();
    }

    public void render(Graphics2D g2d, boolean export, int viewY, int viewHeight) {
        if (getWidth() == 0)
            return;
        final MSCDataModelEventFilter f = viewModel.getFilter();
        if (this.filter != f) {
            computeMaxBBWidth();
            this.filter = f;
        }
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int minIdx, maxIdx;
        minIdx = viewY / eventHeight;
        maxIdx = (viewY + viewHeight - 1) / eventHeight + 1;

        final int evCount = viewModel.getEventCount();

        if (maxIdx > evCount - 1)
            maxIdx = evCount - 1;

        int height = getHeight();
        final int fontHeight = g2d.getFontMetrics().getHeight();
        if (export)
            height += fontHeight + 4;
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, viewModel.getTotalWidth() + 10, height);
        if ((!export) && drawBands) {
            g2d.setColor(new Color(248, 248, 248));
            for (int i = minIdx; i <= maxIdx; i++) {
                if (i % 2 == 1) {
                    final int h = i * eventHeight;
                    g2d.fillRect(0, h, viewModel.getTotalWidth(),
                            eventHeight - 1);
                }
            }
        }
        g2d.setColor(Color.BLACK);
        final int enCnt = viewModel.entityCount();
        if (export) {
            for (int i = 0; i < enCnt; i++) {
                final Entity en = viewModel.get(i);
                final int enx = viewModel.getEntityCenterX(i);
                final int w = g2d.getFontMetrics().stringWidth(en.getPath());
                final int ascent = g2d.getFontMetrics().getAscent();
                g2d.drawString(en.getPath(), enx - w / 2, ascent + 2);
                g2d.drawRect(enx - w / 2 - 1, 0, w + 1, g2d.getFontMetrics()
                        .getHeight());
            }
            g2d.translate(0, fontHeight + 4);
        }
        drawLifeLines(g2d, export, minIdx, maxIdx);
        render(g2d, minIdx, maxIdx);
    }

    public int getHeight() {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        if (dataModel == null)
            return 0;
        if (timeProportional) {
            long h;
            if (viewModel.getEventCount() > 0)
                h = viewModel.getEventAt(viewModel.getEventCount() - 1)
                        .getTimestamp()
                        - viewModel.getEventAt(0).getTimestamp();
            else
                h = 0;
            if (h > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else
                return (int) h;
        } else {
            final int cnt = viewModel.getEventCount();
            if (cnt == 0)
                return 0;
            return eventHeight * (cnt + 1);
        }
    }

    public int getWidth() {
        return viewModel.getTotalWidth();
    }

    public Event getSelectedEvent() {
        return selectedEvent;
    }

    public int getViewModelSelectedEventIndex() {
        return viewModelSelectedEventIndex;
    }

    public int getViewModelClosestEventIndex(int x, int y, int viewY,
            int viewHeight) {
        final int minIdx = viewY / eventHeight;
        int maxIdx = (viewY + viewHeight - 1) / eventHeight + 1;
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            if (maxIdx > viewModel.getEventCount() - 1)
                maxIdx = viewModel.getEventCount() - 1;
            for (int i = minIdx; i <= maxIdx; i++) {
                final Event ev = viewModel.getEventAt(i);
                final Entity en = ev.getEntity();
                final int enIdx = viewModel.indexOf(en);
                if (enIdx < 0)
                    continue;
                final Dimension maxDim = new Dimension(
                        viewModel.getEntityWidth(enIdx), eventHeight);
                final Point p = getEventPoint(i);
                if (p == null)
                    continue;
                if (ev.getRenderer().inSelectionArea(p.x, p.y, maxDim, x, y)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private Interaction selectClosestInteraction(int x, int y, int viewMinIdx,
            int viewMaxIdx) {
        final Interaction inter = getClosestInteraction(x, y, viewMinIdx, viewMaxIdx);
        setSelectedInteraction(inter);
        return inter;
    }

    private Interaction getClosestInteraction(int x, int y, int viewMinIdx,
            int viewMaxIdx) {
        final int modelMinIdx = viewModel.getModelIndexFromViewIndex(viewMinIdx);
        final int modelMaxIdx = viewModel.getModelIndexFromViewIndex(viewMaxIdx);
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        final ArrayList<Interaction> al = dataModel.getInteractionsInInterval(
                modelMinIdx, modelMaxIdx);
        for (final Interaction in : al) {
            final InteractionRenderer ir = in.getIRenderer();

            final Event ev1 = in.getFromEvent();
            int w1, w2;
            Entity en1, en2;
            if (ev1 != null) {
                en1 = ev1.getEntity();
                w1 = viewModel.getEntityWidth(en1);
            } else {
                en1 = null;
                w1 = -1;
            }
            final Event ev2 = in.getToEvent();
            if (ev2 != null) {
                en2 = ev2.getEntity();
                w2 = viewModel.getEntityWidth(en2);
            } else {
                en2 = null;
                w2 = -1;
            }
            if (w1 < 0 && w2 < 0)
                continue;
            final Dimension maxDim1 = new Dimension(w1, eventHeight);
            final Rectangle r1 = ev1 != null ? getEventBoundingBox(ev1,
                    viewModel.getViewIndexFromModelIndex(ev1.getIndex()),
                    maxDim1) : new Rectangle(-1, -1, 0, 0);
            final Dimension maxDim2 = new Dimension(w2, eventHeight);
            final Rectangle r2 = ev2 != null ? getEventBoundingBox(ev2,
                    viewModel.getViewIndexFromModelIndex(ev2.getIndex()),
                    maxDim2) : new Rectangle(-1, -1, 0, 0);
            if (ir.inSelectionArea(r1, r2, x, y, en1 == en2)) {
                return in;
            }
        }
        return null;
    }

    public Object getClosest(int x, int y, int viewY, int viewHeight) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            final int evCount = viewModel.getEventCount();
            final int minIdx = viewY / eventHeight;
            int maxIdx = (viewY + viewHeight - 1) / eventHeight + 1;
            if (maxIdx > evCount - 1)
                maxIdx = evCount - 1;
            for (int i = minIdx; i <= maxIdx; i++) {
                final Event ev = viewModel.getEventAt(i);
                final Entity en = ev.getEntity();
                final int enIdx = viewModel.indexOf(en);
                if (enIdx < 0)
                    continue;
                final Dimension maxDim = new Dimension(
                        viewModel.getEntityWidth(enIdx), eventHeight);
                final Point p = getEventPoint(i);
                if (p == null)
                    continue;
                if (ev.getRenderer().inSelectionArea(p.x, p.y, maxDim, x, y)) {
                    return ev;
                }
            }
            final Interaction in = getClosestInteraction(x, y, minIdx, maxIdx);
            return in;
        }
    }

    public int getClosestEventViewIndex(int x, int y, int viewY, int viewHeight) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            final int evCount = viewModel.getEventCount();
            final int minIdx = viewY / eventHeight;
            int maxIdx = (viewY + viewHeight - 1) / eventHeight + 1;
            if (maxIdx > evCount - 1)
                maxIdx = evCount - 1;
            for (int i = minIdx; i <= maxIdx; i++) {
                final Event ev = viewModel.getEventAt(i);
                final Entity en = ev.getEntity();
                final int enIdx = viewModel.indexOf(en);
                if (enIdx < 0)
                    continue;
                final Dimension maxDim = new Dimension(
                        viewModel.getEntityWidth(enIdx), eventHeight);
                final Point p = getEventPoint(i);
                if (p == null)
                    continue;
                if (ev.getRenderer().inSelectionArea(p.x, p.y, maxDim, x, y)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public int getClosestEventModelIndex(int x, int y, int viewY, int viewHeight) {
        return viewModel.getModelIndexFromViewIndex(getClosestEventViewIndex(x,
                y, viewY, viewHeight));
    }

    public void selectClosest(int x, int y, int viewY, int viewHeight) {
        final int idx = getClosestEventViewIndex(x, y, viewY, viewHeight);
        if (idx >= 0) {
            setSelectedEventByViewIndex(idx);
            return;
        }
        final int evCount = viewModel.getEventCount();
        final int minIdx = viewY / eventHeight;
        int maxIdx = (viewY + viewHeight - 1) / eventHeight + 1;
        if (maxIdx > evCount - 1)
            maxIdx = evCount - 1;
        if (selectClosestInteraction(x, y, minIdx, maxIdx) == null)
            setSelectedInteraction(null);
    }

    @SuppressWarnings("unused")
    private int pointEventDistance(int x, int y, int evIdx) {
        final Event ev = viewModel.getEventAt(evIdx);
        if (ev == null)
            return Integer.MAX_VALUE;
        final Entity en = ev.getEntity();
        final int enIdx = viewModel.indexOf(en);
        if (enIdx < 0)
            return Integer.MAX_VALUE;
        final int entityWidth = viewModel.getEntityWidth(enIdx);
        final int x1 = viewModel.getEntityCenterX(enIdx);
        final int y1 = evIdx * eventHeight + eventHeight / 2;
        return (int) Math.sqrt((x1 - x) * (x1 - x) + (y1 - y) * (y1 - y));
    }

    public void setSelectedEvent(Event ev) {
        selectedInteraction = null;
        selectedEvent = ev;
        final int modelIdx = ev.getIndex();
        viewModelSelectedEventIndex = selectedEvent != null ? viewModel
                .getViewIndexFromModelIndex(modelIdx) : -1;
        for (final SelectionListener selListener : selListeners) {
            selListener.eventSelected(this, selectedEvent,
                    viewModelSelectedEventIndex, modelIdx);
        }
    }

    public void setSelectedEventByModelIndex(int modelIdx) {
        final int currModelIndex = viewModel
                .getModelIndexFromViewIndex(viewModelSelectedEventIndex);
        if (modelIdx == currModelIndex) {
            return;
        }
        if (modelIdx != -1) {
            selectedInteraction = null;
            final MSCDataModel dataModel = MSCDataModel.getInstance();
            selectedEvent = dataModel.getEventAt(modelIdx);
            viewModelSelectedEventIndex = selectedEvent != null ? viewModel
                    .getViewIndexFromModelIndex(modelIdx) : -1;
        } else {
            selectedEvent = null;
            viewModelSelectedEventIndex = -1;
        }
        for (final SelectionListener selListener : selListeners) {
            selListener.eventSelected(this, selectedEvent,
                    viewModelSelectedEventIndex, modelIdx);
        }
    }

    public void setSelectedEventByViewIndex(int idx) {
        if (idx == viewModelSelectedEventIndex) {
            return;
        }
        if (idx != -1) {
            selectedInteraction = null;
            selectedEvent = viewModel.getEventAt(idx);
            viewModelSelectedEventIndex = (selectedEvent != null) ? idx : -1;
        } else {
            selectedEvent = null;
            viewModelSelectedEventIndex = -1;
        }
        final int modelIndex = viewModel
                .getModelIndexFromViewIndex(viewModelSelectedEventIndex);
        for (final SelectionListener selListener : selListeners) {
            selListener.eventSelected(this, selectedEvent,
                    viewModelSelectedEventIndex, modelIndex);
        }

    }

    public void setSelectedInteraction(Interaction inter) {
        if (inter != null) {
            selectedEvent = null;
            viewModelSelectedEventIndex = -1;
            if (selectedInteraction != inter) {
                selectedInteraction = inter;
                for (final SelectionListener selListener : selListeners) {
                    selListener.interactionSelected(this, selectedInteraction);
                }
            }
        }
    }

    public void selectByLineNumber(int lineIndex) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            for (int i = 0; i < viewModel.getEventCount(); i++) {
                if (viewModel.getEventAt(i).getLineIndex() == lineIndex)
                    setSelectedEventByViewIndex(i);
            }
        }
    }

    public ViewModel getViewModel() {
        return viewModel;
    }

    public void setTimeScaleFactor(long factor) {
        // tsScaleFactor = factor;
    }

    public long getTimeScaleFactor() {
        return 1;
    }

    public void setAbsTimeUnit(OutputUnit u) {
        absTimeUnit = u;
    }

    public OutputUnit getAbsTimeUnit() {
        return absTimeUnit;
    }

    public void setDeltaTimeUnit(InputUnit u) {
        deltaTimeUnit = u;
    }

    public InputUnit getDeltaTimeUnit() {
        return deltaTimeUnit;
    }

    // public void setTimestampUnit(InputUnit inputUnit) {
    // timestampUnit = inputUnit;
    // }

    // public InputUnit getTimestampUnit() {
    // return timestampUnit;
    // }

    public void cursorBegin(boolean ctrl) {
        if (selectedInteraction != null) {
            setSelectedEventByViewIndex(selectedInteraction.getFromIndex());
        }
    }

    public void cursorEnd(boolean ctrl) {
        if (selectedInteraction != null) {
            setSelectedEventByViewIndex(selectedInteraction.getToIndex());
        }
    }

    /**
     * If on event, move to the next event in the same entity If on interaction,
     * move to the event associated to the interaction which lower on the screen
     * 
     * @param ctrl
     */
    public void cursorDown(boolean ctrl) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            if (selectedEvent != null) {
                final Entity en = selectedEvent.getEntity();
                for (int i = viewModelSelectedEventIndex + 1; i < viewModel
                        .getEventCount(); i++) {
                    final Event ev = viewModel.getEventAt(i);
                    if (ev.getEntity() == en) {
                        setSelectedEventByViewIndex(i);
                        break;
                    }
                }
            }
            if (selectedInteraction != null) {
                if (ctrl) {
                    final Event fromEv = selectedInteraction.getFromEvent();
                    final Interaction[] inters = dataModel
                            .getOutgoingInteractions(fromEv);
                    final int evIdx = selectedInteraction.getToIndex();
                    Interaction nextDown = selectedInteraction;
                    int nextDownIdx = Integer.MAX_VALUE;
                    for (final Interaction in : inters) {
                        if (in == selectedInteraction)
                            continue;
                        final int toIdx = in.getToIndex();
                        if (toIdx > evIdx && toIdx < nextDownIdx) {
                            nextDownIdx = toIdx;
                            nextDown = in;
                        }
                    }
                    setSelectedInteraction(nextDown);
                } else {
                    int fromEnIdx = -1, toEnIdx = -1;
                    int fromEvIdx = -1, toEvIdx = -1;
                    if (selectedInteraction.getFromIndex() != -1) {
                        fromEnIdx = viewModel.indexOf(selectedInteraction
                                .getFromEvent().getEntity());
                        fromEvIdx = viewModel
                                .getViewIndexFromModelIndex(selectedInteraction
                                        .getFromIndex());
                    }
                    if (selectedInteraction.getToIndex() != -1) {
                        toEnIdx = viewModel.indexOf(selectedInteraction
                                .getToEvent().getEntity());
                        toEvIdx = viewModel
                                .getViewIndexFromModelIndex(selectedInteraction
                                        .getToIndex());
                    }
                    if (fromEnIdx >= 0 && toEnIdx >= 0) {
                        if (fromEvIdx < toEvIdx)
                            setSelectedEventByViewIndex(toEvIdx);
                        else
                            setSelectedEventByViewIndex(fromEvIdx);
                    } else if (fromEnIdx >= 0) {
                        // outgoing stub, drawn below event center, open hidden
                        // sink entity if possible.
                        final Event toEv = selectedInteraction.getToEvent();
                        if (toEv != null) {
                            final Entity toEn = toEv.getEntity();
                            if (toEn != null) {
                                viewModel.add(toEn);
                            }
                        }
                    } else if (toEnIdx >= 0) {
                        // incoming stub, drawn above event center, select event
                        setSelectedEventByViewIndex(toEvIdx);
                    }
                }
            }
        }
    }

    /**
     * If on event, move to the previous event in the same entity If on
     * interaction, move to the event associated to the interaction which higher
     * on the screen
     * 
     * @param ctrl
     */
    public void cursorUp(boolean ctrl) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            if (selectedEvent != null) {
                final Entity en = selectedEvent.getEntity();
                for (int i = viewModelSelectedEventIndex - 1; i >= 0; i--) {
                    final Event ev = viewModel.getEventAt(i);
                    if (ev.getEntity() == en) {
                        setSelectedEventByViewIndex(i);
                        break;
                    }
                }
            }
            if (selectedInteraction != null) {
                if (ctrl) {
                    final Event fromEv = selectedInteraction.getFromEvent();
                    final Interaction[] inters = dataModel
                            .getOutgoingInteractions(fromEv);
                    final int evIdx = selectedInteraction.getToIndex();
                    Interaction nextUp = selectedInteraction;
                    int nextUpIdx = Integer.MIN_VALUE;
                    for (final Interaction in : inters) {
                        if (in == selectedInteraction)
                            continue;
                        final int toIdx = in.getToIndex();
                        if (toIdx < evIdx && toIdx > nextUpIdx) {
                            nextUpIdx = toIdx;
                            nextUp = in;
                        }
                    }
                    setSelectedInteraction(nextUp);
                } else {
                    int fromEnIdx = -1, toEnIdx = -1;
                    int fromEvIdx = -1, toEvIdx = -1;
                    if (selectedInteraction.getFromIndex() != -1) {
                        fromEnIdx = viewModel.indexOf(selectedInteraction
                                .getFromEvent().getEntity());
                        fromEvIdx = viewModel
                                .getViewIndexFromModelIndex(selectedInteraction
                                        .getFromIndex());
                    }
                    if (selectedInteraction.getToIndex() != -1) {
                        toEnIdx = viewModel.indexOf(selectedInteraction
                                .getToEvent().getEntity());
                        toEvIdx = viewModel
                                .getViewIndexFromModelIndex(selectedInteraction
                                        .getToIndex());
                    }
                    if (fromEnIdx >= 0 && toEnIdx >= 0) {
                        if (fromEvIdx > toEvIdx)
                            setSelectedEventByViewIndex(toEvIdx);
                        else
                            setSelectedEventByViewIndex(fromEvIdx);
                    } else if (fromEnIdx >= 0) {
                        // outgoing stub, going below event center, select event
                        setSelectedEventByViewIndex(fromEvIdx);
                    } else if (toEnIdx >= 0) {
                        // incoming stub, drawn above event center, open hidden
                        // from entity if possible.
                        final Event fromEv = selectedInteraction.getFromEvent();
                        if (fromEv != null) {
                            final Entity fromEn = fromEv.getEntity();
                            if (fromEn != null) {
                                viewModel.add(fromEn);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * If on event, move to the interaction outgoing or incoming which is drawn
     * at the left of the event. if more than one exists, the first one is
     * selected. If on interaction, move to the event at the left of the
     * interaction, if there is only one. If more than one event is at the left
     * (for example, for a lane interaction), does nothing, the user can use
     * cursorUp() cursorDown() instead.
     * 
     * @param ctrl
     */
    public void cursorLeft(boolean ctrl) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            if (selectedEvent != null) {
                final Entity selEn = selectedEvent.getEntity();
                final int selEnIdx = viewModel.indexOf(selEn);
                final Interaction[] incoming = dataModel
                        .getIncomingInteractions(selectedEvent);
                if (incoming != null) {
                    for (final Interaction in : incoming) {
                        if (in.getIRenderer() instanceof BlockInteractionRenderer)
                            continue;
                        final Event otherEv = in.getOtherEvent(selectedEvent);
                        final int otherEnIdx = (otherEv != null) ? viewModel
                                .indexOf(otherEv.getEntity()) : -1;
                        if (otherEnIdx < selEnIdx) {
                            setSelectedInteraction(in);
                            setSelectedEventByViewIndex(-1);
                            return;
                        }
                    }
                }
                final Interaction[] outgoings = dataModel
                        .getOutgoingInteractions(selectedEvent);
                if (outgoings != null) {
                    int otherEvIdx = Integer.MAX_VALUE;
                    int otherEnIdx = Integer.MAX_VALUE;
                    Interaction inter = null;
                    for (final Interaction in : outgoings) {
                        if (in.getIRenderer() instanceof BlockInteractionRenderer)
                            continue;
                        final int v = in.getOtherEventIndex(selectedEvent);
                        if (v >= 0 && v < otherEvIdx) {
                            inter = in;
                            otherEvIdx = v;
                            final Entity otherEn = in.getOtherEvent(selectedEvent)
                                    .getEntity();
                            otherEnIdx = viewModel.indexOf(otherEn);
                        }
                    }
                    if (otherEnIdx < selEnIdx) {
                        setSelectedEventByViewIndex(-1);
                        setSelectedInteraction(inter);
                    }
                }
            } else if (selectedInteraction != null) {
                int fromEnIdx = -1, toEnIdx = -1;
                int fromEvIdx = -1, toEvIdx = -1;
                if (selectedInteraction.getFromIndex() != -1) {
                    fromEnIdx = viewModel.indexOf(selectedInteraction
                            .getFromEvent().getEntity());
                    fromEvIdx = viewModel
                            .getViewIndexFromModelIndex(selectedInteraction
                                    .getFromIndex());
                }
                if (selectedInteraction.getToIndex() != -1) {
                    toEnIdx = viewModel.indexOf(selectedInteraction
                            .getToEvent().getEntity());
                    toEvIdx = viewModel
                            .getViewIndexFromModelIndex(selectedInteraction
                                    .getToIndex());
                }
                if (fromEnIdx >= 0 && toEnIdx >= 0) {
                    if (fromEnIdx < toEnIdx)
                        setSelectedEventByViewIndex(fromEvIdx);
                    else if (fromEnIdx > toEnIdx)
                        setSelectedEventByViewIndex(toEvIdx);
                    // do nothing if on same entity
                } else if (fromEnIdx >= 0) {
                    // outgoing stub, drawn to right of Event, select Event
                    setSelectedEventByViewIndex(fromEvIdx);
                } else if (toEnIdx >= 0) {
                    // incoming stub, open source
                    final Event fromEv = selectedInteraction.getFromEvent();
                    if (fromEv != null) {
                        final Entity en = fromEv.getEntity();
                        if (en != null) {
                            viewModel.add(toEnIdx, en);
                            setSelectedEventByViewIndex(fromEvIdx);
                        }
                    }
                }
            }
        }
    }

    /**
     * If on event, move to the interaction outgoing or incoming which is drawn
     * at the right of the event. if more than one exists, the first one is
     * selected. If on interaction, move to the event at the right of the
     * interaction, if there is only one. If more than one event is at the right
     * (for example, for a lane interaction), does nothing, the user can use
     * cursorUp() cursorDown() instead.
     * 
     * @param ctrl
     */
    public void cursorRight(boolean ctrl) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            if (selectedEvent != null) {
                final Entity selEn = selectedEvent.getEntity();
                final int selEnIdx = viewModel.indexOf(selEn);
                final Interaction[] incoming = dataModel
                        .getIncomingInteractions(selectedEvent);
                if (incoming != null) {
                    for (final Interaction in : incoming) {
                        if (in.getIRenderer() instanceof BlockInteractionRenderer)
                            continue;
                        final Event otherEv = in.getOtherEvent(selectedEvent);
                        final int otherEnIdx = (otherEv != null) ? viewModel
                                .indexOf(otherEv.getEntity()) : -1;
                        if (otherEnIdx >= selEnIdx) {
                            setSelectedInteraction(in);
                            return;
                        }
                    }
                }
                final Interaction[] outgoings = dataModel
                        .getOutgoingInteractions(selectedEvent);
                if (outgoings != null) {
                    for (final Interaction in : outgoings) {
                        if (!(in.getIRenderer() instanceof BlockInteractionRenderer)) {
                            setSelectedInteraction(in);
                            return;
                        }
                    }
                }
            } else if (selectedInteraction != null) {
                int fromEnIdx = -1, toEnIdx = -1;
                int fromEvIdx = -1, toEvIdx = -1;
                if (selectedInteraction.getFromIndex() != -1) {
                    fromEnIdx = viewModel.indexOf(selectedInteraction
                            .getFromEvent().getEntity());
                    fromEvIdx = viewModel
                            .getViewIndexFromModelIndex(selectedInteraction
                                    .getFromIndex());
                }
                if (selectedInteraction.getToIndex() != -1) {
                    toEnIdx = viewModel.indexOf(selectedInteraction
                            .getToEvent().getEntity());
                    toEvIdx = viewModel
                            .getViewIndexFromModelIndex(selectedInteraction
                                    .getToIndex());
                }
                if (fromEnIdx >= 0 && toEnIdx >= 0) {
                    if (fromEnIdx > toEnIdx)
                        setSelectedEventByViewIndex(fromEvIdx);
                    else if (fromEnIdx < toEnIdx)
                        setSelectedEventByViewIndex(toEvIdx);
                    // do nothing if on same entity
                } else if (fromEnIdx >= 0) {
                    // outgoing stub, open sink
                    final Event toEv = selectedInteraction.getToEvent();
                    if (toEv != null) {
                        final Entity en = toEv.getEntity();
                        if (en != null) {
                            viewModel.add(fromEnIdx + 1, en);
                            final int idx = viewModel
                                    .getViewIndexFromModelIndex(selectedInteraction
                                            .getToIndex());
                            setSelectedEventByViewIndex(idx);
                        }
                    }
                } else if (toEnIdx >= 0) {
                    // incoming stub, drawn to left of Event, select event
                    setSelectedEventByViewIndex(toEvIdx);
                }
            }
        }
    }

    public Rectangle getEventBoundingRect(int evIdx) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            if (evIdx >= viewModel.getEventCount())
                return null;
            final Event ev = viewModel.getEventAt(evIdx);
            final int enIdx = viewModel.indexOf(ev.getEntity());
            if (enIdx < 0)
                return null;
            final int entityWidth = viewModel.getEntityWidth(enIdx);
            final Dimension maxDim = new Dimension(entityWidth, eventHeight);
            final int x = viewModel.getEntityCenterX(enIdx);
            final int y = evIdx * eventHeight;
            final Rectangle bb = new Rectangle();
            ev.getRenderer().getBoundingBox(maxDim, x, y, bb);
            return bb;
        }
    }

    public Point getEventPoint(int evIdx) {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            final Event ev = viewModel.getEventAt(evIdx);
            final int enIdx = viewModel.indexOf(ev.getEntity());
            if (enIdx < 0)
                return null;
            final int x = viewModel.getEntityCenterX(enIdx);
            final int y = evIdx * eventHeight + eventHeight / 2;
            return new Point(x, y);
        }
    }

    public void setShowUnits(boolean showUnits) {
        this.showUnits = showUnits;
    }

    public boolean getShowUnits() {
        return showUnits;
    }

    public void setShowDate(boolean sd) {
        showDate = sd;
    }

    public boolean getShowDate() {
        return showDate;
    }

    public void setShowTime(boolean sd) {
        showTime = sd;
    }

    public boolean sShowTime() {
        return showTime;
    }

    public void setShowLeadingZeroes(boolean showLeadingZeroes) {
        this.showLeadingZeroes = showLeadingZeroes;
    }

    public boolean getShowLeadingZeroes() {
        return showLeadingZeroes;
    }

    public String getSelectedStatusString() {
        final MSCDataModel dataModel = MSCDataModel.getInstance();
        synchronized (dataModel) {
            if (selectedEvent != null) {
                return "Event: local, t="
                        + getTimeRepr(selectedEvent.getTimestamp());
            } else if (selectedInteraction != null) {
                String s = "Selected Event:";
                final Event fev = selectedInteraction.getFromEvent();
                final Event tev = selectedInteraction.getToEvent();
                if (fev != null)
                    s += "from: (" + getTimeRepr(fev.getTimestamp()) + ":"
                            + fev.getEntity().getName() + ")";
                if (tev != null) {
                    s += " to: (" + getTimeRepr(tev.getTimestamp()) + ":"
                            + tev.getEntity().getName() + ")";
                }
                if (fev != null && tev != null)
                    s += " latency="
                            + getTimeRepr(tev.getTimestamp()
                                    - fev.getTimestamp());
                return s;
            }
            return "";
        }
    }

    public boolean getCompactView() {
        return compactView;
    }

    public void setCompactView(boolean b) {
        compactView = b;
    }

    public Interaction getSelectedInteraction() {
        return selectedInteraction;
    }

    public void addSelectionListener(SelectionListener l) {
        selListeners.add(l);
    }

    public ViewModel getEntityHeaderModel() {
        return viewModel;
    }

    public void setZoomFactor(int v) {
        zoomFactor = v;
        eventHeight = EVENT_HEIGHT * v / 100;
    }

    public void setShowBlocks(boolean show) {
        showBlocks = show;
    }

    public void setShowLabels(boolean show) {
        showLabels = show;
    }

    public void setShowNotes(boolean show) {
        showNotes = show;
    }
}
