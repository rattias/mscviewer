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
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.ScrollPane;
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
    private static ArrayList<JDialog> dialogs = new ArrayList<JDialog>();
    private static final GPane gpane = new GPane();    private JPanel innerPane;
    private JLabel msgLabel;
    private boolean shouldShow;
    private ProgressReport parent;
    private int percent;
    private long childrenBaseValue;
    private ArrayList<ProgressReport> children;
    @SuppressWarnings("unused")
    private String activity;
    @SuppressWarnings("unused")
    private String msg;
    private float scaleFactor;
    private long minValue;
    private long maxValue;
    private JProgressBar progressBar;
    private boolean done = false;
    private int oldPerc = 0;
    
    public ProgressReport(final String activity, final String msg) {
        this(activity, msg, -1, -1);
    }

    public ProgressReport(final String activity, final String msg,
            final long minValue, final long maxValue) {
        this(activity, msg, null, -1, minValue, maxValue, true);
    }

    private ProgressReport(final String activity, final String msg,
            ProgressReport parent, int percent, final long minValue,
            final long maxValue, boolean shouldShow) {
        this.activity = activity;
        this.parent = parent;
        this.percent = percent;
        this.msg = msg;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.scaleFactor = 100.0f/(maxValue-minValue);
        this.shouldShow = shouldShow;
        if (Main.batchMode())
            return;
        updateDialogForNewProgressReport();
    }

    public ProgressReport subReport(String activity, String msg, int percent,
            int minValue, int maxValue, boolean show) {
        final ProgressReport child = new ProgressReport(activity, msg, this, percent,
                minValue, maxValue, show);
        if (children == null) {
            children = new ArrayList<ProgressReport>();
            childrenBaseValue = Main.batchMode() ? 0 : getProgress();
        }
        children.add(child);
        return child;
    }

    
    private JPanel createPanel (String msg) {
        msgLabel = new JLabel("<html>" + Utils.stringToHTML(msg)
                + "</html>");
        progressBar = new JProgressBar(0, 100);
        if (maxValue < 0) {
            progressBar.setIndeterminate(true);
        }

        innerPane = new JPanel();
        innerPane.setBorder(new TitledBorder(activity));
        innerPane.setLayout(new BorderLayout());
        innerPane.add(progressBar, BorderLayout.SOUTH);
        innerPane.add(msgLabel, BorderLayout.CENTER);
        return innerPane;
    }
    
    /**
     * create, close or updates the dialog when a new ProgressReport is created
     * or one is marked as completed.
     */
    private void updateDialogForDone() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (parent == null) {
                    // we're destroying the dialog here
                    JDialog dialog = (JDialog)SwingUtilities.getWindowAncestor(progressBar);
                    synchronized(dialogs) {
                        dialogs.remove(dialog);
                        dialog.setVisible(false);
                        final MainFrame mf = MainFrame.getInstance();
                        if (mf != null && dialogs.isEmpty()) {
                            mf.setGlassPane(gpane);
                            gpane.setVisible(false);
                        }
                        dialog = null;
                        return;
                    }
                } else {
                    JDialog dialog = (JDialog)SwingUtilities.getWindowAncestor(parent.progressBar);
                    //we're removing the subprogress here
                    JOptionPane p = (JOptionPane)dialog.getContentPane();
                    if (progressBar == null) 
                        throw new Error("WHAT?");
                    p.remove(progressBar.getParent());
                    dialog.pack();
                }
            }
        };
        Utils.dispatchOnAWTThreadNow(r);
    }
    
    private void updateDialogForNewProgressReport() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (parent == null) {
                    // this is the main Progress. 
                    JPanel panel = createPanel(msg);
                    // we're creating the dialog here
                    JDialog dialog = new JDialog(MainFrame.getInstance(), "Progress Status", false);
                    synchronized(dialogs) {
                        dialogs.add(dialog);
                    }
                    dialog.setContentPane(
                            new JOptionPane(
                                    new Object[]{panel},
                                    JOptionPane.PLAIN_MESSAGE,
                                    JOptionPane.DEFAULT_OPTION, null,
                                    new String[] { "Cancel" }
                                    )
                            );
                    dialog.pack();
                    final JFrame f = MainFrame.getInstance();
                    if (f != null) {
                        final Rectangle r = f.getBounds();
                        r.x = r.x + (r.width - dialog.getWidth()) / 2;
                        r.y = r.y + (r.height - dialog.getHeight()) / 2;
                        dialog.setLocation(r.x, r.y);
                    }
                    final MainFrame mf = MainFrame.getInstance();
                    if (mf != null) {
                        mf.setGlassPane(gpane);
                        gpane.setVisible(true);
                    }
                    dialog.setVisible(true);
                } else {
                    // this is a sub-progress 
                    JDialog dialog = (JDialog)SwingUtilities.getWindowAncestor(parent.progressBar);

                    // we're adding the subprogress here
                    JPanel panel = createPanel(msg);
                    JOptionPane opt = (JOptionPane)dialog.getContentPane();
                    opt.add(panel, opt.getComponentCount()-1);
                    dialog.pack();
                }
            }
        };
        Utils.dispatchOnAWTThreadNow(r);      
    }

    public void progress(String msg, int v) {
        if (Main.batchMode())
            return;
        progress(v);
        SwingUtilities.invokeLater( () -> msgLabel.setText("<html>" + Utils.stringToHTML(msg) + "</html>"));        
    }

    public void progress(long v) {
        if (Main.batchMode())
            return;
        if (children != null && children.size() > 0)
            throw new UnsupportedOperationException(
                    "updating progress for a ProgressReport when children are present is not allowed.");
        progressInternal(v);
    }


    private void progressInternal(long v) {
        if (Main.batchMode())
            return;
        if (progressBar == null)
            return;
        int perc = (int)((v-minValue)*scaleFactor);
        if (perc == oldPerc)
            return;
        // we use a field variable for oldPerc rather than
        // getting the progressBar value because the latter is 
        // updated by EDT through invokeLater(), hence it could
        // stay old for quite a bit if the EDT is not scheduled.
        oldPerc = perc;
        if (parent != null)
            parent.updateForChildren();
        SwingUtilities.invokeLater(() -> progressBar.setValue(perc));
    }

    public int getProgress() {
        if (progressBar != null)
            return progressBar.getValue();
        else
            return 0;
    }

    private void updateForChildren() {
        long totValue = childrenBaseValue;
        for (final ProgressReport p : children) {
            final int childPercent = p.getProgress();
            final long totalChildContribute = (maxValue - minValue) * p.percent / 100;
            totValue += totalChildContribute * childPercent / 100;
        }
        progressInternal(totValue);
    }

    public void progressDone() {
        if (done == true)
            return;
        if (children != null) {
            for (final ProgressReport pr : children) {
                pr.progressDone();
            }
        }
        if (parent != null) {
            parent.children.remove(this);
            final long totalChildContribute = (parent.maxValue - parent.minValue) * percent / 100;
            parent.childrenBaseValue += totalChildContribute;
            parent.updateForChildren();
        }
        updateDialogForDone();
    }

//    public void setMinMaxValue(int min, int max) {
//        minValue = min;
//        maxValue = max;
//        pb.setMinimum(min);
//        pb.setMaximum(max);
//    }

//    @Override
//    public String toString() {
//        return "value=" + pb.getValue() + ", minValue=" + minValue
//                + ", maxValue=" + maxValue + ", percent=" + percent
//                + "parent percent="
//                + ((parent != null) ? parent.percent : "???");
//    }

    public static void main1(String args[]) throws InterruptedException {
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
        synchronized(dialogs) {
            for(JDialog dialog: dialogs) {         
                dialog.setVisible(false);
            }
        }
        final MainFrame mf = MainFrame.getInstance();
        if (mf != null) {
            mf.setGlassPane(gpane);
            gpane.setVisible(false);
        }
    }
}

