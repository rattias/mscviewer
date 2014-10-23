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

import com.cisco.mscviewer.util.Resources;
import com.cisco.mscviewer.util.Utils;
import com.cisco.mscviewer.model.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class EntityHeader extends JPanel implements EntityHeaderModelListener {

    //private MainPanel mainPanel;
    private ViewModel ehm;
//    private final Vector<EntityHeaderListener> listeners;
    private Dimension preferredSize;
    private Component dragging = null;
    private int draggingProspectiveIndex = -1;

    CustomButton getCBFromIndex(int idx) {
        return (CustomButton) ((Container) getComponent(idx)).getComponent(1);
    }

    @Override
    public void entityMoved(ViewModel eh, Entity en, int toIdx) {
        JPanel pp = getPanel(en);
        setComponentZOrder(pp, toIdx);
        doLayout();
        repaint();
    }

    class ButtonDragger implements MouseListener, MouseMotionListener {

        private int dx;

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        @SuppressWarnings("unused")
        private int getPanelIndex(JPanel pp) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            int idx;
            for (idx = 0; idx < getComponentCount(); idx++) {
                if (getComponent(idx) == pp) {
                    return idx;
                }
            }
            return -1;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            CustomButton b = (CustomButton) e.getSource();
            JPanel pp = (JPanel) b.getParent();
            //dragging = pp;
            Point p = SwingUtilities.convertPoint(pp, e.getPoint(), EntityHeader.this);
            dx = b.getParent().getX() - p.x;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            if (dragging != null) {
                stopDragging(e);
            }
        }

        private void startDragging(MouseEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            CustomButton b = (CustomButton) e.getSource();
            JPanel pp = (JPanel) b.getParent();
            dragging = pp;
            preferredSize = EntityHeader.super.getPreferredSize();
            // following two lines are to move panel to front in Z order. also, panel is now first 
            // in component list
            setComponentZOrder(pp, 0);
            draggingProspectiveIndex = 0;
        }

        private void stopDragging(MouseEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            dragging = null;
            ehm.moveEntity(((CustomButton) e.getSource()).getEntity(), draggingProspectiveIndex);
            draggingProspectiveIndex = -1;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            if (dragging == null) {
                startDragging(e);
            }
            CustomButton b = (CustomButton) e.getSource();
            JPanel pp = (JPanel) b.getParent();
            Point p = SwingUtilities.convertPoint(pp, e.getPoint(), EntityHeader.this);
            int lx = p.x + dx;
            // set Panel location. There is a ComponentListener to transfer bounds change to model,
            // but that fires only at mouseReleased, so we also set model coords.
            ehm.setEntityLocation(getEntity(pp), lx);
            int count = getComponentCount();
            int cx = lx + pp.getWidth() / 2;
            int x = (cx < 0) ? pp.getWidth() : 0;
            // first component is the one we're dragging, loop on the others	
            draggingProspectiveIndex = -1;
            for (int idx = 1; idx < count; idx++) {
                JPanel curr = (JPanel) getComponent(idx);
                if (cx >= x && cx < x + curr.getWidth()) {
                    x += pp.getWidth();
                    draggingProspectiveIndex = idx - 1;
                }
                Point pos = curr.getLocation();
                pos.x = x;
                //curr.setLocation(pos);
                ehm.setEntityLocation(getEntity(curr), x);
                x += curr.getWidth();
            }
            if (draggingProspectiveIndex == -1) {
                draggingProspectiveIndex = (cx < 0) ? 0 : count - 1;
            }
            //doLayout();
            repaint();
        }
    }
    ButtonDragger bd = new ButtonDragger();

    ActionListener selectListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            CustomButton cb = (CustomButton) actionEvent.getSource();
            Entity en = cb.getEntity();
            boolean selected = cb.getModel().isSelected();
            Utils.trace(Utils.EVENTS, "Button for " + en.getName() + " " + selected);
            ehm.setSelected(en, selected);
            scrollToVisible(cb.getEntity());
            // do following only if CTRL not pressed (need to add that check)
            for (int i = 0; i < ehm.entityCount(); i++) {
                Entity en1 = ehm.get(i);
                if (en1 != en) {
                    Utils.trace(Utils.EVENTS, "setting model for " + en.getName() + " to false");
                    ehm.setSelected(en1, false);
                }
            }
//            notifyEntitySelectionChanged(cb.getEntity(), ehm.indexOf(cb.getEntity()));
        }
    };

    AbstractAction leftKeyPressed = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            CustomButton b = (CustomButton) e.getSource();
            int idx = getButtonIndex(b);
            if (idx > 0) {
                int newx = b.getX() - b.getWidth();
                int butw = b.getWidth();
                ehm.moveEntity(b.getEntity(), idx - 1);
                b.requestFocus();
                b.setSelected(true);

                Container c;
                for (c = EntityHeader.this.getParent(); !(c instanceof JScrollPane); c = c.getParent())
                    ;
                JScrollPane pn = (JScrollPane) c;
                int jspx = pn.getViewport().getViewPosition().x;
                if (newx < jspx) {
                    Rectangle r = pn.getViewport().getViewRect();
                    r.x -= butw;
                    ((JComponent) pn.getViewport().getView()).scrollRectToVisible(r);
                }
                repaint();
            }
        }
    };

    @Override
    public Dimension getPreferredSize() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        // when dragging a button somehow the preferred height becomes one. we fix it.
        if (preferredSize != null) {
            return preferredSize;
        }
        Dimension r = super.getPreferredSize();
        if (r.height == 1 && getComponentCount() > 0) {
            r.height = getComponent(0).getHeight();
        }
        return r;
    }

    AbstractAction rightKeyPressed = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            CustomButton b = (CustomButton) e.getSource();
            int idx = getButtonIndex(b);
            if (idx < getComponentCount() - 1) {
                int newx = b.getX() + b.getWidth();
                int butw = b.getWidth();
                ehm.moveEntity(b.getEntity(), idx + 1);
                b.requestFocus();
                b.setSelected(true);
                Container c;
                for (c = EntityHeader.this.getParent(); !(c instanceof JScrollPane); c = c.getParent())
                    ;
                JScrollPane pn = (JScrollPane) c;
                Rectangle jspRect = pn.getViewport().getViewRect();
                if (newx + butw > jspRect.x + jspRect.width) {
                    Rectangle r = pn.getViewport().getViewRect();
                    r.x += butw;
                    ((JComponent) pn.getViewport().getView()).scrollRectToVisible(r);
                }
                repaint();
            }
        }
    };

    AbstractAction plusKeyPressed = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            CustomButton b = (CustomButton) e.getSource();
            int idx = ehm.indexOf(b.getEntity());
            Dimension d = ehm.getEntityPreferredSize(idx);
            d.width += 6;
            ehm.setEntityPreferredSize(idx, d);            
        }
    };

    AbstractAction minusKeyPressed = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                throw new Error("should be in event dispatch thread!");
            }
            int count = ehm.entityCount();
            if (count == 1)
                // in case only one entity is shown we can't shrink
                return;
            CustomButton b = (CustomButton) e.getSource();
            int idx = ehm.indexOf(b.getEntity());
            Dimension d = ehm.getEntityPreferredSize(idx);
            d.width -= 6;
            ehm.setEntityPreferredSize(idx, d);
            if (ehm.getEntityPreferredSize(idx).width == d.width) {
                int nidx = idx < count-1 ? idx+1: idx-1;
                d = ehm.getEntityPreferredSize(nidx);
                d.width += 6;
                ehm.setEntityPreferredSize(nidx, d);
            }
        }
    };

    public EntityHeader(ViewModel ehm) {
//        this.listeners = new Vector<EntityHeaderListener>();
        this.ehm = ehm;
        ehm.addListener(this);
        setLayout(null);
        //setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    }

    @Override
    public Dimension getSize() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        Dimension d = super.getSize();
        return d;
    }

    private int getButtonIndex(CustomButton b) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) == b.getParent()) {
                return i;
            }
        }
        return -1;
    }

    public int getEntityCenterX(int entityIdx) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        if (entityIdx >= getComponentCount()) {
            return 0;
        }
        Component c = getComponent(entityIdx);
        return c.getX() + c.getWidth() / 2;
    }

    //	public int getEntityWidth() {
    //		return (getComponentCount()>0) ? getComponent(0).getWidth() : 0;
    //	}
    public int getEntitiesTotalWidth() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        int w = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            w += getComponent(i).getWidth();
        }
        return w;
    }

    //	public void setMainPanel(MainPanel mp) {
    //		mainPanel = mp;
    //	}
    public Entity getEntity(int i) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        return getCBFromIndex(i).getEntity();
    }

    public Entity getEntity(JPanel p) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        return ((CustomButton) p.getComponent(1)).getEntity();
    }

    public JPanel getPanel(Entity en) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        int cnt = getComponentCount();
        for (int i = 0; i < cnt; i++) {
            if (getEntity(i) == en) {
                return (JPanel) getComponent(i);
            }
        }
        return null;
    }

    public void add(Entity en) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        for (int i = 0; i < getComponentCount(); i++) {
            if (getCBFromIndex(i).getEntity() == en) {
                return;
            }
        }
        add(en, getComponentCount());
    }

    public Component add(final Entity en, final int idx) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        CustomButton b = new CustomButton(en);
        b.setBorderPainted(false);
        b.setToolTipText("SHIFT+cursor keys to move");
        b.addActionListener(selectListener);
        b.addMouseListener(bd);
        b.addMouseMotionListener(bd);
        b.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK), "VK_LEFT");
        b.getActionMap().put("VK_LEFT", leftKeyPressed);
        b.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK), "VK_RIGHT");
        b.getActionMap().put("VK_RIGHT", rightKeyPressed);

        b.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_MASK), "VK_PLUS");
        b.getActionMap().put("VK_PLUS", plusKeyPressed);
        b.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "VK_MINUS");
        b.getActionMap().put("VK_MINUS", minusKeyPressed);
        final JPanel p = new JPanel();
        p.setMinimumSize(new Dimension(64, 20));
        p.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK), "VK_LEFT");
        p.getActionMap().put("VK_LEFT", leftKeyPressed);
        p.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK), "VK_RIGHT");
        p.getActionMap().put("VK_RIGHT", rightKeyPressed);

        p.setBorder(BorderFactory.createLineBorder(Color.black));
//        p.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                ehm.setEntityBounds(ehm.indexOf(en), p.getBounds());
//            }
//        });
        JButton x = new JButton(Resources.getImageIcon("close1.png", "close"));
        x.setBorderPainted(false);
        x.setMargin(new Insets(2, 0, 2, 0));
        class XActionListener implements ActionListener {

            final private Entity en;

            XActionListener(Entity en) {
                this.en = en;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                ehm.remove(en);
                repaint();
            }
        }
        x.addActionListener(new XActionListener(en));
        JToggleButton y = new JToggleButton(Resources.getImageIcon("iconize.png", "iconize"));
        y.setFocusable(false);
        y.setName("iconize");
        y.setSelectedIcon(Resources.getImageIcon("iconize1.png", "iconize"));
        y.setBorderPainted(false);

        y.setMargin(new Insets(2, 0, 2, 0));
        class YActionListener implements ActionListener {

            final private JPanel p, p1;
            private Dimension oldDim;

            YActionListener(JPanel p, JPanel p1) {
                this.p = p;
                this.p1 = p1;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((JToggleButton) e.getSource()).isSelected()) {
                    oldDim = p.getPreferredSize();
                    p.setPreferredSize(p1.getSize());
                    p.setMaximumSize(p1.getSize());
                    // model adapts because there is a componentlistener that transfers boudn changes to 
                    // model
                    //ehm.setEntityBounds(idx, p.getBounds());
                    doLayout();
                } else {
                    if (oldDim != null) {
                        p.setPreferredSize(oldDim);
                        p.setMaximumSize(null);
                        //ehm.setEntityBounds(idx, p.getBounds());
                        doLayout();
                    }
                }
            }
        }
        JPanel p1 = new JPanel();
        y.addActionListener(new YActionListener(p, p1));
        //		p1.setLayout(new BorderLayout());
        p1.setLayout(new BoxLayout(p1, BoxLayout.LINE_AXIS));
        p1.add(y, BorderLayout.WEST);
        p1.add(x, BorderLayout.EAST);

        p.setLayout(new BorderLayout());
        p.add(p1, BorderLayout.EAST);
        p.add(b, BorderLayout.CENTER);
        add(p, idx);
        ehm.setEntityComponent(idx, p);
        //updateIndexes();
        //mainPanel.getMainFrame().updateTree(en);
        //notifyListenersEntityAdded(en);
        doLayout();
        return p;
    }

    private JViewport getViewPort() {
        Component c;
        for (c = EntityHeader.this.getParent(); !(c instanceof JScrollPane); c = c.getParent())
            ;
        JScrollPane pn = (JScrollPane) c;
        return pn.getViewport();
    }

    @Override
    public void doLayout() {
        if (dragging != null) {
            return;
        }
        int count = getComponentCount();
        if (count == 0) {
            return;
        }
        int x = 0;
        int preferredTotalW = 0;
        int preferredTotalH = 0;
        for (int i = 0; i < count; i++) {
            Component c = getComponent(i);
            Dimension pd = c.getPreferredSize();
            preferredTotalW += pd.width;
            preferredTotalH = Math.max(preferredTotalH, pd.height);
        }
        Dimension vpd = getViewPort().getExtentSize();
        int diff = vpd.width - preferredTotalW;
        if (diff <= 0) 
            diff = 0;
        int part = diff / count;
        int last = diff - part*(count-1);
        for (int i = 0; i < count; i++) {
            Component c = getComponent(i);
            Dimension d = c.getPreferredSize();
            int neww = d.width + ((i < count - 1) ? part  : last);
            c.setBounds(x, 0, neww, preferredTotalH);
            x += neww;
        }
        Dimension newDim = new Dimension(x, preferredTotalH);
        Dimension oldDim = getSize();
        if (!newDim.equals(oldDim)) {
            setSize(newDim);
            setMinimumSize(newDim);
            setPreferredSize(newDim);
        }
        revalidate();
        repaint();
    }

    public void flipIconizedState(Entity en) {

    }

    public void remove(Entity en) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        int cnt = getComponentCount();
        for (int i = 0; i < cnt; i++) {
            CustomButton b = getCBFromIndex(i);
            if (b.getEntity() == en) {
                remove(b.getParent());
                break;
            }
        }
        doLayout();
    }

//    public void moveEntity(int oldIdx, int newIdx) {
//        if (!SwingUtilities.isEventDispatchThread()) {
//            throw new Error("should be in event dispatch thread!");
//        }
//        CustomButton b = getCBFromIndex(oldIdx);
//        JPanel pp = b.getParent()
////        Dimension d = b.getParent().getPreferredSize();
////        remove(b.getEntity());
////        Component c = add(b.getEntity(), newIdx);
////        c.setPreferredSize(d);
//        setComponentZOrder(pp, 0);
//        doLayout();
//    }
//    public void addListener(EntityHeaderListener l) {
//        listeners.add(l);
//    }
//
//    public void removeListener(EntityHeaderListener l) {
//        listeners.remove(l);
//    }
//    public void notifyLayoutChanged() {
//        for (EntityHeaderListener listener : listeners) {
//            System.out.println("NOTIFYING");
//            listener.notifyLayoutChanged(this);
//        }
//    }
//
//    public void notifyEntityAdded(Entity en, int idx) {
//        for (EntityHeaderListener listener : listeners) {
//            listener.entityAdded(this, en, idx);
//        }
//    }
//
//    public void notifyEntityRemoved(Entity en, int idx) {
//        for (EntityHeaderListener listener : listeners) {
//            listener.entityRemoved(this, en, idx);
//        }
//    }
//
//    public void notifyEntitySelectionChanged(Entity en, int idx) {
//        for (EntityHeaderListener listener : listeners) {
//            listener.entitySelectionChanged(this, en, idx);
//        }
//    }
    public Entity getEntityAt(Point p) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        Component c = getComponentAt(p);
        if (c == null) {
            return null;
        }
        CustomButton cb = (CustomButton) ((JPanel) c).getComponent(1);
        return cb.getEntity();
    }

    public int[] getSelectedIndices() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        int cnt = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            if (getCBFromIndex(i).isSelected()) {
                cnt++;
            }
        }
        int[] idx = new int[cnt];
        for (int i = 0, j = 0; i < getComponentCount(); i++) {
            if (getCBFromIndex(i).isSelected()) {
                idx[j] = i;
                j++;
            }
        }
        return idx;
    }

    @Override
    public void entityAdded(ViewModel eh, Entity en, int idx) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        add(en, idx);
        doLayout();
//        notifyEntityAdded(en, idx);
    }

    @Override
    public void entityRemoved(ViewModel eh, Entity en, int idx) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        remove(en);
        doLayout();
//        notifyEntityRemoved(en, idx);
    }

    @Override
    public void entitySelectionChanged(ViewModel eh, Entity en, int idx) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        Utils.trace(Utils.EVENTS, "CB.entitySelectionChanged()");
        int cnt = getComponentCount();
        for (int i = 0; i < cnt; i++) {
            CustomButton cb = getCBFromIndex(i);
            Entity en1 = cb.getEntity();
            boolean isButtonSelected = cb.isSelected();
            boolean isModelSelected = eh.isSelected(en1);
            Utils.trace(Utils.EVENTS, en1.getName() + ": but=" + isButtonSelected + ", model=" + isModelSelected);
            if (isButtonSelected != isModelSelected) {
                cb.setSelected(isModelSelected);
                if (isModelSelected) {
                    scrollToVisible(en1);
                }
            }
        }
//        if (somethingChanged) {
//            notifyEntitySelectionChanged(en, idx);
//        }
    }

    //	public int getComponentCount() {
    //        if (! SwingUtilities.isEventDispatchThread())
    //            throw new Error("should be in event dispatch thread!");
    //		return super.getComponentCount();
    //	}
    public int getMinEntityWidth() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < getComponentCount(); i++) {
            int w = getComponent(i).getWidth();
            if (w < min) {
                min = w;
            }
        }
        return min;
    }

    public int getEntityWidth(int idx) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        return (idx < getComponentCount()) ? getComponent(idx).getWidth() : 0;
    }

    public int getEntityWidth(Entity en) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        for (int i = 0; i < getComponentCount(); i++) {
            CustomButton b = getCBFromIndex(i);
            if (b.getEntity() == en) {
                return getEntityWidth(i);
            }
        }
        return -1;
    }

    public void scrollToVisible(Entity en) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("should be in event dispatch thread!");
        }
        int idx = ehm.indexOf(en);
        int cx = getEntityCenterX(idx);
        int w = getEntityWidth(idx);
        int x0 = cx - w / 2;
        int x1 = cx + w / 2;

        Container c;
        for (c = EntityHeader.this.getParent(); !(c instanceof JScrollPane); c = c.getParent())
            ;
        JScrollPane pn = (JScrollPane) c;
        Rectangle jspRect = pn.getViewport().getViewRect();
        Rectangle r = pn.getViewport().getViewRect();
        if (x0 < jspRect.x) {
            r.x = x0;
            ((JComponent) pn.getViewport().getView()).scrollRectToVisible(r);
        } else if (x1 > jspRect.x + jspRect.width) {
            r.x = x0 - jspRect.width + w;
            ((JComponent) pn.getViewport().getView()).scrollRectToVisible(r);
        }
    }

    @Override
    public int getEntityHeaderModelNotificationPriority() {
        // this compoenent has to be notified before anybody else so that
        // number of buttons and entities remain in sync.
        return 10;
    }

    @Override
    public void boundsChanged(ViewModel entityHeaderModel, Entity en,
            int idx) {
        doLayout();
    }
}
