package com.cisco.mscviewer.gui;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;


//UI which allows to span a rubberband on top of the component
public class RolloverUI<V extends JComponent> extends LayerUI<V> {
    private JLayer<?> l;
    private boolean selecting;

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        l = (JLayer<?>) c;
        // this LayerUI will receive mouse/motion events
        l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK);
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        // JLayer must be returned to its initial state
        l.setLayerEventMask(0);
        l = null;
    }

    // intercept events as appropriate 

    @Override
    protected void processMouseEvent(MouseEvent e, JLayer<? extends V> l) {
        super.processMouseMotionEvent(e, l);
        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            V view = l.getView();
            if (view.isEnabled())
                view.setBorder(BorderFactory.createLineBorder(Color.black));
        } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
            V view = l.getView();
            view.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        }
    }


}