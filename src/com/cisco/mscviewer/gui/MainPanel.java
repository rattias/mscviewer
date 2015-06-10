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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.EntityHeaderModelListener;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.MSCDataModelListener;
import com.cisco.mscviewer.model.ViewModel;

@SuppressWarnings("serial")
class TimeHeader extends JPanel {
    private final MainPanel mp;

    public TimeHeader(MainPanel mp) {
        this.mp = mp;
    }

    @Override
    protected void paintComponent(Graphics g) {
        final MSCRenderer r = mp.getMSCRenderer();
        final Dimension d = getSize();
        if (d.height == 0)
            return;

        if (r != null) {
            final JViewport jvp = (JViewport) getParent();
            final Rectangle rec = jvp.getViewRect();
            r.renderTimeHeader((Graphics2D) g, false, rec.y, rec.height);
        }
    }
}

/**
 *
 * @author rattias
 */
@SuppressWarnings("serial")
public class MainPanel extends JPanel implements Scrollable, KeyListener,
        SelectionListener, MSCDataModelListener, EntityHeaderModelListener {
    // private float zoomFactor = 1.0f;
    private final MainFrame mf;
    private final MSCRenderer r;
    // private Rectangle selectionRectangle = null;
    private final EntityHeader entityHeader;
    private final ViewModel viewModel;

    public MainPanel(final MainFrame mf, EntityHeader entityHeader,
            ViewModel viewModel) {
        this.mf = mf;
        this.entityHeader = entityHeader;
        this.viewModel = viewModel;
        MSCDataModel.getInstance().addListener(this);
        viewModel.addListener(this);
        setFocusable(true);
        addKeyListener(this);
        r = new MSCRenderer(viewModel);
        r.addSelectionListener(this);
        setBackground(Color.WHITE);
        ToolTipManager.sharedInstance().registerComponent(this);
        revalidate();
    }

    public MainFrame getMainFrame() {
        return mf;
    }

    public MSCRenderer getMSCRenderer() {
        return r;
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2d = (Graphics2D) g;
        final JViewport jvp = getAncestorViewport();
        final Rectangle rec = jvp.getViewRect();
        r.render(g2d, false, rec.y, rec.height);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        final Dimension d = getPreferredSize();
        return d;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle arg0, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL)
            return (int) arg0.getHeight() / 2;
        else
            return entityHeader.getMinEntityWidth();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle arg0, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL)
            return 20;
        else
            return 30;
    }

    // public void setSelectionRectangle(Rectangle r) {
    // Rectangle r1 = selectionRectangle;
    // selectionRectangle = null;
    // if (r1 != null)
    // paintImmediately(r1.x-2, r1.y-2, r1.width+4, r1.height+4);
    // selectionRectangle = r;
    // if (r != null)
    // paintImmediately(r.x-2, r.y-2, r.width+4, r.height+4);
    // }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isShiftDown()) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                r.cursorUp(e.isControlDown());
                break;
            case KeyEvent.VK_DOWN:
                r.cursorDown(e.isControlDown());
                break;
            case KeyEvent.VK_LEFT:
                r.cursorLeft(e.isControlDown());
                break;
            case KeyEvent.VK_RIGHT:
                r.cursorRight(e.isControlDown());
                break;
            case 'B':
                r.cursorBegin(e.isControlDown());
                break;
            case 'E':
                r.cursorEnd(e.isControlDown());
                break;
            }
        }
        repaint();
    }

    public void makeEventWithIndexVisible(int evIdx) {
        if (evIdx < 0)
            return;
        final Rectangle rect = r.getEventBoundingRect(evIdx);
        Container c;
        for (c = getParent(); !(c instanceof JScrollPane); c = c.getParent())
            ;
        final JScrollPane jsp = (JScrollPane) c;
        final JViewport jvp = jsp.getViewport();
        final Component view = jvp.getView();
        final Rectangle visRect = jvp.getViewRect();
        if (rect == null) {
            view.repaint();
            return;
        }
        if (visRect.contains(rect)) {
            view.repaint();
            return;
        }
        int x, y;
        if (rect.x > visRect.x
                && rect.x + rect.width < visRect.x + visRect.width)
            x = visRect.x;
        else {
            if (rect.width < visRect.width) {
                x = rect.x - (visRect.width - rect.width) / 2;
                if (x + visRect.width > entityHeader.getWidth())
                    x = entityHeader.getWidth() - visRect.width;
                else if (x < 0)
                    x = 0;
            } else {
                x = rect.x;
                if (x + visRect.width > entityHeader.getWidth())
                    x = entityHeader.getWidth() - visRect.width;
            }
        }
        if (rect.y > visRect.y
                && rect.y + rect.height < visRect.y + visRect.height) {
            y = visRect.y;
        } else {
            if (rect.height < visRect.height) {
                y = rect.y - (visRect.height - rect.height) / 2;
                if (y + visRect.height > r.getHeight())
                    y = r.getHeight() - visRect.height;
            } else {
                y = rect.y;
                if (y + visRect.height > r.getHeight())
                    y = r.getHeight() - visRect.height;
            }
            if (y < 0)
                y = 0;
        }
        setViewPosition(x, y);
    }

    public void setViewPosition(int x, int y) {
        final JViewport jvp = getAncestorViewport();
        final Point p = new Point(x, y);
        jvp.setViewPosition(p);
        final Component view = jvp.getView();
        view.repaint();        
    }
    
    public Point getViewPosition() {
        final JViewport jvp = getAncestorViewport();
        final Rectangle rec = jvp.getViewRect();
        return new Point(rec.x, rec.y);
    }


    
    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void eventSelected(MSCRenderer renderer, Event selectedEvent,
            int viewEventIndex, int modelEventIndex) {
        if (viewEventIndex >= 0) {
            makeEventWithIndexVisible(viewEventIndex);
        }
        repaint();
    }

    @Override
    public void interactionSelected(MSCRenderer renderer,
            Interaction selectedInteraction) {
        // TODO Auto-generated method stub

    }

    @Override
    public void modelChanged(MSCDataModel m) {
        // int idx = -1;
        // Event ev = r.getSelectedEvent();
        // Interaction in = r.getSelectedInteraction();
        // if (ev != null) {
        // idx = viewModel.getIndexForEvent(ev);
        // } else if (in != null) {
        // int fromIdx = in.getFromIndex();
        // int toIdx = in.getToIndex();
        // if (fromIdx > 0)
        // idx = fromIdx;
        // else
        // idx = toIdx;
        // }
        r.updateForTimeUnitChanges();
        revalidate();
        // makeEventWithIndexVisible(idx);
    }

    public void scrollToSelected() {
        int idx = -1;
        final Event ev = r.getSelectedEvent();
        final Interaction in = r.getSelectedInteraction();
        if (ev != null) {
            idx = viewModel.getIndexForEvent(ev);
        } else if (in != null) {
            final int fromIdx = in.getFromIndex();
            final int toIdx = in.getToIndex();
            if (fromIdx > 0)
                idx = fromIdx;
            else
                idx = toIdx;
        }
        revalidate();
        makeEventWithIndexVisible(idx);
    }

    @Override
    public void eventsChanged(MSCDataModel m) {
        modelChanged(m);
    }

    private JViewport getAncestorViewport() {
        Component c;
        for (c = getParent(); !(c instanceof JViewport); c = c.getParent())
            ;
        return (JViewport) c;
    }

    public void updateViewForFilter() {
        // String exp = mf.getCurrentFilterRegExp();
        if (mf.filteringEnabled())
            viewModel.setFilter(new JSViewFilter(MSCDataModel.getInstance(),
                    viewModel, mf.getFilterExpression()));
        else
            viewModel.setFilter(new CompactViewFilter(viewModel, ".*"));
        updateView();
    }

    public void updateView() {
        getMSCRenderer().updateCache();
        revalidate();
        int idx = getMSCRenderer().getViewModelSelectedEventIndex();
        if (idx == -1) {
            final Interaction inter = getMSCRenderer().getSelectedInteraction();
            if (inter != null) {
                idx = inter.getFromIndex();
                if (idx == -1)
                    idx = inter.getToIndex();
                idx = viewModel.getViewIndexFromModelIndex(idx);
            }
        }
        makeEventWithIndexVisible(idx);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int w = entityHeader.getEntitiesTotalWidth();
        final JViewport vp = getAncestorViewport();
        w = Math.max(w, vp.getWidth());
        final Dimension d = new Dimension(w, r.getHeight());
        return d;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        final JViewport jvp = getAncestorViewport();
        final Rectangle rec = jvp.getViewRect();
        final int idx = getMSCRenderer().getViewModelClosestEventIndex(e.getX(),
                e.getY(), rec.y, rec.height);
        if (idx >= 0) {
            final Event ev = viewModel.getEventAt(idx);
            if (ev != null) {
                return ev.getNote();
            }
        }
        return null;
    }

    @Override
    public void entityAdded(MSCDataModel mscDataModel, Entity en) {
        updateView();
    }

    @Override
    public void eventAdded(MSCDataModel mscDataModel, Event ev) {
        updateView();
    }

    @Override
    public void entityAdded(ViewModel eh, Entity en, int idx) {
        updateView();
    }

    @Override
    public void entityRemoved(ViewModel eh, Entity parentEn, Entity en, int idx) {
        updateView();
    }

    @Override
    public void entitySelectionChanged(ViewModel eh, Entity en, int idx) {
        updateView();
    }

    @Override
    public int getEntityHeaderModelNotificationPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void boundsChanged(ViewModel entityHeaderModel, Entity en, int idx) {
        revalidate();
        repaint();
    }

    public MSCDataModel getModelView() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setZoomFactor(int v) {
        r.setZoomFactor(v);
        revalidate();
    }

    @Override
    public void entityMoved(ViewModel eh, Entity en, int toIdx) {
        updateView();
    }
    
    public int getFirstVisibleEventIndex() {
        final JViewport jvp = getAncestorViewport();
        final Rectangle rec = jvp.getViewRect();
        return rec.y / r.getEventHeight();
    }

}
