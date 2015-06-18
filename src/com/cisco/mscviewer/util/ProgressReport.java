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
package com.cisco.mscviewer.util;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.gui.MainFrame;

@SuppressWarnings("serial")
class GPane extends JComponent {
    public GPane() {
        addMouseListener(new MouseAdapter() {
        });
        addMouseMotionListener(new MouseMotionAdapter() {
        });
        addFocusListener(new FocusAdapter() {
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        // gets the current clipping area
        final Rectangle clip = g.getClipBounds();

        // sets a 65% translucent composite
        final AlphaComposite alpha = AlphaComposite.SrcOver.derive(0.65f);
        final Composite composite = g2.getComposite();
        g2.setComposite(alpha);

        // fills the background
        g2.setColor(getBackground());
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);
        g2.setComposite(composite);

    }

}

/**
 *
 * @author rattias
 */
public class ProgressReport {

    private static final JDialog singletonDialog = new JDialog(MainFrame.getInstance(),
            "Progress Status", false);
    private static final GPane gpane = new GPane();
    private JPanel innerPane;
    private static ArrayList<Object> elements = new ArrayList<Object>();
    private JProgressBar pb;
    private JLabel msgLabel;
    private int minValue, maxValue;
    private boolean shouldShow;
    private ProgressReport parent;
    private int percent;
    private int childrenBaseValue;
    private ArrayList<ProgressReport> children;
    @SuppressWarnings("unused")
    private String activity;
    @SuppressWarnings("unused")
    private String msg;
    private float scaleFactor;
    
    public ProgressReport(final String activity, final String msg) {
        this(activity, msg, -1, -1);
    }

    public ProgressReport(final String activity, final String msg,
            final int minValue, final int maxValue) {
        this(activity, msg, null, -1, minValue, maxValue, true);
    }

    private ProgressReport(final String activity, final String msg,
            ProgressReport parent, int percent, final int minValue,
            final int maxValue, boolean shouldShow) {
        this.activity = activity;
        this.parent = parent;
        this.percent = percent;
        this.msg = msg;
        this.scaleFactor = 100.0f/(maxValue-minValue);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.shouldShow = shouldShow;
        if (Main.batchMode())
            return;
        closeDialog();
        createProgressGUI(activity, msg, minValue, maxValue);
    }

    public ProgressReport subReport(String activity, String msg, int percent,
            int minValue, int maxValue, boolean show) {
        final ProgressReport child = new ProgressReport(activity, msg, this, percent,
                minValue, maxValue, show);
        if (children == null) {
            children = new ArrayList<ProgressReport>();
            childrenBaseValue = Main.batchMode() ? 0 : getProgress();
        }
        closeDialog();
        children.add(child);
        return child;
    }

    private void createProgressGUI(String activity, String msg, int minValue,
            int maxValue) {
        synchronized (singletonDialog) {
            msgLabel = new JLabel("<html>" + Utils.stringToHTML(msg)
                    + "</html>");
            pb = new JProgressBar(minValue, maxValue);
            if (shouldShow) {
                if (maxValue < 0) {
                    pb.setIndeterminate(true);
                }

                innerPane = new JPanel();
                innerPane.setBorder(new TitledBorder(activity));
                innerPane.setLayout(new BorderLayout());
                innerPane.add(pb, BorderLayout.SOUTH);
                innerPane.add(msgLabel, BorderLayout.CENTER);
                elements.add(innerPane);
                if (maxValue < 0)
                    updateDialogForProgress();
            }
        }
    }

    private void closeDialog() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (singletonDialog.isVisible()) {
                    singletonDialog.setVisible(false);
                    final MainFrame mf = MainFrame.getInstance();
                    if (mf != null) {
                        mf.setGlassPane(gpane);
                        gpane.setVisible(false);
                    }
                    return;
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);
    }
    
    private void updateDialogForProgress() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!singletonDialog.isVisible()) {
                    singletonDialog.setContentPane(new JOptionPane(elements
                            .toArray(new Object[elements.size()]),
                            JOptionPane.PLAIN_MESSAGE,
                            JOptionPane.DEFAULT_OPTION, null,
                            new String[] { "Cancel" }));
                    singletonDialog.pack();
                    final JFrame f = MainFrame.getInstance();
                    if (f != null) {
                        final Rectangle r = f.getBounds();
                        r.x = r.x + (r.width - singletonDialog.getWidth()) / 2;
                        r.y = r.y + (r.height - singletonDialog.getHeight()) / 2;
                        singletonDialog.setLocation(r.x, r.y);
                    }
                    final MainFrame mf = MainFrame.getInstance();
                    if (mf != null) {
                        mf.setGlassPane(gpane);
                        gpane.setVisible(true);
                    }
                    singletonDialog.setVisible(true);
                } else {
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);
    }

    public void progress(String msg, int v) {
        if (Main.batchMode())
            return;
        if (children != null && children.size() > 0)
            throw new UnsupportedOperationException(
                    "updating progress for a ProgressReport when children are present is not allowed.");
        msgLabel.setText("<html>" + Utils.stringToHTML(msg) + "</html>");
        progressInternal(v);
    }

    public void progress(int v) {
        if (Main.batchMode())
            return;
        // System.out.println("progress("+activity+"): "+v);
        if (children != null && children.size() > 0)
            throw new UnsupportedOperationException(
                    "updating progress for a ProgressReport when children are present is not allowed.");
        progressInternal(v);
    }

    private void progressInternal(int v) {
        if (Main.batchMode())
            return;
        synchronized(singletonDialog) {
            if (pb == null)
                return;
            int perc = (int)((v-minValue)*scaleFactor);
            int oldPerc = (int)((pb.getValue()-minValue)*scaleFactor);
            if (perc == oldPerc)
                return;
            if (!pb.isIndeterminate())
                pb.setValue(v);
            if (parent != null)
                parent.updateForChildren();
            updateDialogForProgress();
        }
    }

    public int getProgress() {
        synchronized(singletonDialog) {
            if (pb != null)
                return pb.getValue();
            else
                return 0;
        }
    }

    private void updateForChildren() {
        int totValue = childrenBaseValue;
        for (final ProgressReport p : children) {
            final int childPercent = (p.getProgress() - p.minValue) * 100
                    / (p.maxValue - p.minValue);
            final int totalChildContribute = (maxValue - minValue) * p.percent / 100;
            totValue += totalChildContribute * childPercent / 100;
        }
        progressInternal(totValue);
    }

    synchronized public void progressDone() {
        if (children != null) {
            for (final ProgressReport pr : children) {
                pr.progressDone();
            }
        }
        elements.remove(innerPane);
        if (parent != null) {
            parent.children.remove(this);
            final int totalChildContribute = (parent.maxValue - parent.minValue)
                    * percent / 100;
            parent.childrenBaseValue += totalChildContribute;
            parent.updateForChildren();
        }
        closeDialog();
    }

    public void setMinMaxValue(int min, int max) {
        minValue = min;
        maxValue = max;
        pb.setMinimum(min);
        pb.setMaximum(max);
    }

    @Override
    public String toString() {
        return "value=" + pb.getValue() + ", minValue=" + minValue
                + ", maxValue=" + maxValue + ", percent=" + percent
                + "parent percent="
                + ((parent != null) ? parent.percent : "???");
    }

    public static void main(String args[]) throws InterruptedException {
        // TEST 1 -------------------------
        System.out.println("ProgressReport main");
        ProgressReport pr = new ProgressReport(
                "Test1",
                "a main bar with a child for the\n"
                        + "first 50%, then anohter for the remaining 50%, both children hidden",
                0, 1500);
        ProgressReport pr1 = pr.subReport("act1", "msg1", 50, 0, 500, false);
        for (int i = 0; i < 500; i++) {
            pr1.progress(i);
            Thread.sleep(5);
        }
        pr1.progressDone();
        ProgressReport pr2 = pr.subReport("act2", "msg2", 50, 0, 350, false);
        for (int i = 0; i < 350; i++) {
            pr2.progress(i);
            Thread.sleep(5);
        }
        pr2.progressDone();
        pr.progressDone();
        // TEST 2 -------------------------
        pr = new ProgressReport("Test2",
                "same as before, but with the two bars visible", 0, 1500);
        pr1 = pr.subReport("act1", "msg1", 50, 0, 500, true);
        for (int i = 0; i < 500; i++) {
            pr1.progress(i);
            Thread.sleep(5);
        }
        pr1.progressDone();
        pr2 = pr.subReport("act2", "msg2", 50, 0, 750, true);
        for (int i = 0; i < 750; i++) {
            pr2.progress(i);
            Thread.sleep(5);
        }
        pr2.progressDone();
        pr.progressDone();
        // TEST 3 -------------------------
        pr = new ProgressReport("Test2",
                "two visible children, progressing at same time", 0, 1500);
        pr1 = pr.subReport("act1", "msg1", 30, 0, 500, true);
        pr2 = pr.subReport("act2", "msg2", 70, 0, 350, true);
        for (int i = 0; i < 1200; i++) {
            pr1.progress(i);
            pr2.progress(i);
            Thread.sleep(5);
        }
        pr1.progressDone();
        pr2.progressDone();
        pr.progressDone();
        System.out.println("done");
    }

    public static void cleanup() {
        singletonDialog.setVisible(false);
        final MainFrame mf = MainFrame.getInstance();
        if (mf != null) {
            mf.setGlassPane(gpane);
            gpane.setVisible(false);
        }

    }
}

