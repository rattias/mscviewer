package com.cisco.mscviewer.gui.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.cisco.mscviewer.graph.GraphSeries;
import com.cisco.mscviewer.graph.Point;
import com.cisco.mscviewer.tree.Interval;

@SuppressWarnings("serial")
public class HeatGraphPanel extends GraphPanel {
    private final static int BAR_HEIGHT = 6;
    private final HashMap<GraphSeries, JLabel> labels;
    private static Color[] heatMap = new Color[256];
    private JPanel ylabels = null;

    static {
        final int l = heatMap.length;
        for (int i = 0; i < l; i++) {
            final float f = ((float) i) / l;
            heatMap[i] = new Color(red(f), green(f), blue(f));
        }
    }

    public HeatGraphPanel() {
        labels = new HashMap<GraphSeries, JLabel>();
    }

    @Override
    public void addGraph(GraphSeries gd) {
        super.addGraph(gd);
        ylabels = null;
        repaint();
    }

    @Override
    public JPanel getYLabelsPanel() {
        if (ylabels == null) {
            ylabels = new JPanel();
            ylabels.setBackground(Color.black);
            ylabels.setOpaque(true);
            ylabels.setLayout(new GridLayout(-1, 1));
            ylabels.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 4));
            for (final GraphSeries gd : getGraphData()) {
                if (isEnabled(gd)) {
                    final JLabel l = new JLabel(gd.getName(), SwingConstants.RIGHT);
                    l.setForeground(Color.lightGray);
                    labels.put(gd, l);
                    ylabels.add(l);
                }
            }
        }
        return ylabels;
    }

    @Override
    public void prepare(Graphics2D g) {
    }

    @Override
    public void paintGraph(Graphics2D g) {
        final GraphSeries[] graphs = getGraphData();
        final long minX = 0;
        final long maxX = getMaxModelX();
        for (final GraphSeries d : graphs) {
            if (isEnabled(d)) {
                final Interval in = d.getInterval(minX, maxX);
                g.setColor(getForeground(d));
                if (labels != null) {
                    final JLabel l = labels.get(d);
                    if (l == null)
                        continue;
                    final int y = l.getY() + l.getHeight() / 2;
                    double et = d.maxY() - d.minY();
                    if (et == 0)
                        et = 1;
                    int prevx = -1;
                    int prevcnt = 0;
                    for (int i = in.getStart(); i < in.getEnd(); i++) {
                        final Point p = d.point(i);
                        final int x = screenX(p.x());
                        if (x == prevx && prevcnt < heatMap.length - 1)
                            prevcnt++;
                        else {
                            prevx = x;
                            prevcnt = 0;
                        }
                        int idx = (int) ((p.y() / et) * heatMap.length);
                        if (idx >= heatMap.length)
                            idx = heatMap.length - 1;
                        // System.out.println(d.getName()+": idx = "+idx+", et= "+et+", p.y= "+p.y);
                        final Color c = heatMap[prevcnt];
                        g.setColor(c);
                        g.drawLine(x, y - BAR_HEIGHT / 2, x, y + BAR_HEIGHT / 2);
                    }
                }
            }
        }
    }

    @Override
    public void paintCursor(Graphics2D g2d) {
        super.paintCursor(g2d);
        final GraphSeries cursorGraph = getCursorGraph();
        if (cursorGraph == null)
            return;
        final int cursorScreenX = screenX(cursorGraph.point(cursorIdx).x());
        final JLabel l = labels.get(cursorGraph);
        if (l == null)
            return;
        final Integer gy = l.getY() + l.getHeight() / 2;
        g2d.drawOval(cursorScreenX - 3, gy - BAR_HEIGHT / 2, 6, BAR_HEIGHT);
    }

    private static float interpolate(float val, float y0, float x0, float y1,
            float x1) {
        return (val - x0) * (y1 - y0) / (x1 - x0) + y0;
    }

    private static float base(float val) {
        if (val <= -0.75)
            return 0;
        else if (val <= -0.25)
            return interpolate(val, 0.0f, -0.75f, 1.0f, -0.25f);
        else if (val <= 0.25)
            return 1.0f;
        else if (val <= 0.75)
            return interpolate(val, 1.0f, 0.25f, 0.0f, 0.75f);
        else
            return 0.0f;
    }

    private static float red(float gray) {
        return base(gray - 0.5f);
    }

    private static float green(float gray) {
        return base(gray);
    }

    private static float blue(float gray) {
        return base(gray + 0.5f);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getYLabelsPanel().getHeight());
    }

}
