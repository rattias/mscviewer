package com.cisco.mscviewer.gui.graph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import com.cisco.mscviewer.graph.GraphSeries;
import com.cisco.mscviewer.graph.Point;

@SuppressWarnings("serial")
abstract public class GraphPanel extends JPanel {
    private final static String LEFT = "Left";
    private final static String LEFT_LOCAL = "LeftLocal";
    private final static String RIGHT = "Right";
    private final static String RIGHT_LOCAL = "RightLocal";
    protected static final long ZOOM_THRESHOLD = 10;
    private static int AXIS_OFFSET = 10;
    private final Vector<GraphCursorListener> listeners = new Vector<GraphCursorListener>();
    private final ArrayList<GraphSeries> graph = new ArrayList<GraphSeries>();
    private final HashMap<GraphSeries, Color> foregroundMap = new HashMap<GraphSeries, Color>();
    private final HashSet<GraphSeries> enabled = new HashSet<GraphSeries>();
    private int startX = -1, endX = -1;
    /* graph model X min and max values for viewport */
    private long minViewportModelX = 0;
    private long maxViewportModelX = 0;
    /* graph model X min and max values */
    private long minViewModelX = Long.MAX_VALUE;
    private long maxViewModelX = Long.MIN_VALUE;
    private double zoomFactor = 1.0;
    Color axesColor = Color.cyan;
    Color cursorColor = Color.white;
    Color bkgColor = Color.black;
    protected int cursorIdx = -1;
    protected int cursorGraphIdx = -1;
    private final boolean axis = true;
    private final boolean ticks = true;
    private final boolean labels = true;
    private FontMetrics fm;
    private int prevPixelVisibleWidth = -1;
    private long prevVisibleWidth = -1;

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        final long visibleWidth = maxViewportModelX - minViewportModelX;
        final long modelWidth = maxViewModelX - minViewModelX;
        final JScrollPane jsp = getScrollPane();
        if (jsp != null) {
            final Dimension ext = jsp.getViewport().getExtentSize();
            final int pixelVisibleWidth = ext.width;
            if (pixelVisibleWidth != prevPixelVisibleWidth
                    || visibleWidth != prevVisibleWidth) {
                prevVisibleWidth = visibleWidth;
                prevPixelVisibleWidth = pixelVisibleWidth;
                zoomFactor = ((double) pixelVisibleWidth) / visibleWidth;
                // System.out.println("zoomFactor = "+zoomFactor);
                final int pixelWidth = screenWidth(modelWidth);
                if (pixelWidth != getWidth()) {
                    setPreferredSize(new Dimension(pixelWidth, getHeight()));
                    revalidate();
                }
            }
            final java.awt.Point p = getScrollPane().getViewport().getViewPosition();
            final int x = screenX(minViewportModelX);
            if (p.x != x) {
                p.x = x;
                getScrollPane().getViewport().setViewPosition(p);
            }

        }

        final GraphPanel gp = GraphPanel.this;
        final Graphics2D g2d = (Graphics2D) g;
        fm = g2d.getFontMetrics();
        // clear(g2d);
        GraphPanel.this.prepare(g2d);
        if (axis)
            gp.paintAxes(g2d);
        if (ticks)
            gp.paintTicks(g2d);
        if (labels)
            gp.paintLabels(g2d);
        gp.paintGraph(g2d);
        gp.paintCursor(g2d);
        if (startX >= 0) {
            g2d.setColor(Color.white);
            g2d.setXORMode(Color.black);
            g2d.fillRect(startX, 0, endX - startX, getHeight());
            g2d.setPaintMode();
        }
    }

    private final AbstractAction moveLeft = new AbstractAction(LEFT) {
        @Override
        public void actionPerformed(ActionEvent e) {
            long l0 = System.currentTimeMillis();
            moveCursorLeft(false);
            repaint();
            long l1 = System.currentTimeMillis();
            System.out.println("ELAPSED: "+(l1-l0));
        }
    };

    private final AbstractAction moveLeftLocal = new AbstractAction(LEFT_LOCAL) {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveCursorLeft(true);
            repaint();
        }
    };

    // @Override
    // public Dimension getPreferredSize() {
    // int w = (int)((getGraphMaxX()-getGraphMinX());
    // Dimension d = new Dimension(w, getHeight());
    // return d;
    // }

    private final AbstractAction moveRight = new AbstractAction(RIGHT) {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveCursorRight(false);
            repaint();
        }
    };

    private final AbstractAction moveRightLocal = new AbstractAction(RIGHT_LOCAL) {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveCursorRight(true);
            repaint();
        }
    };

    public GraphPanel() {
        setFocusable(true);
        setOpaque(true);
        setBackground(bkgColor);
        final InputMap im = getInputMap();
        final ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), LEFT);
        am.put(LEFT, moveLeft);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK),
                LEFT_LOCAL);
        am.put(LEFT_LOCAL, moveLeftLocal);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), RIGHT);
        am.put(RIGHT, moveRight);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK),
                RIGHT_LOCAL);
        am.put(RIGHT_LOCAL, moveRightLocal);
        final MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                if (ev.isShiftDown())
                    startX = endX = ev.getX();
            }

            @Override
            public void mouseReleased(MouseEvent ev) {
                if (startX >= 0) {
                    int l, r;
                    if (startX > endX) {
                        l = endX;
                        r = startX;
                    } else {
                        l = startX;
                        r = endX;
                    }
                    if (r - l > ZOOM_THRESHOLD) {
                        final long minX = modelX(l);
                        final long maxX = modelX(r);
                        System.out.println("ZOOM TO " + minX + "," + maxX);
                        setGraphVisibleInterval(minX, maxX);
                    } else {
                        System.out.println("l = " + l + ", r = " + r);
                        if (ev.isShiftDown())
                            zoom(ev.getX(), 2f / 3f);
                        else if (ev.isControlDown())
                            zoom(ev.getX(), 3f / 2f);
                    }
                }
                startX = endX = -1;
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent ev) {
                endX = ev.getX();
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent ev) {
                grabFocus();
                if (ev.getClickCount() == 2) {

                } else
                    setCursorScreenX(ev.getX());
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                // possibly we were just added to scrollpane.
                // update various things
                resetYLabelPanel();
            }
        });
    }

    public void zoom(int center, double ratio) {
        final long wCenter = modelX(center);
        // System.out.println("center = "+center);
        final long w = (maxViewModelX - minViewModelX);
        final long newW = (long) (w * ratio);
        // System.out.println("prev w = "+w+", new w = "+newW);
        final long newMinX = wCenter - newW / 2;
        final long newMaxX = wCenter + newW / 2;
        setGraphVisibleInterval(newMinX, newMaxX);
    }

    public FontMetrics getFontMetrics() {
        return fm;
    }

    public String getXType() {
        return graph.isEmpty() ? "" : graph.get(0).getXType();
    }

    public String getYType() {
        return graph.isEmpty() ? "" : graph.get(0).getYType();
    }

    public void addGraph(GraphSeries gd) {
        if (!graph.isEmpty()) {
            final GraphSeries g0 = graph.get(0);
            final String g0xType = g0.getXType();
            final String gdxType = gd.getXType();
            if (!gdxType.equals(g0xType))
                throw new IllegalArgumentException("Graph " + gd.getName()
                        + " has X type " + gdxType
                        + " and can't be rendered with graph " + g0.getName()
                        + " of X type " + g0xType);
            final String g0yType = g0.getYType();
            final String gdyType = gd.getYType();
            if (!gdyType.equals(g0yType))
                throw new IllegalArgumentException("Graph " + gd.getName()
                        + " has Y type " + gdyType
                        + " and can't be rendered with graph " + g0.getName()
                        + " of X type " + g0yType);
        }
        graph.add(gd);
        enabled.add(gd);
        resetYLabelPanel();
        if (gd.maxX() > maxViewModelX) {
            maxViewModelX = gd.maxX();
        }
        if (gd.minX() < minViewModelX) {
            minViewModelX = gd.minX();
        }
        setGraphVisibleInterval(minViewModelX, maxViewModelX);
    }

    private JScrollPane getScrollPane() {
        Component p;
        for (p = getParent(); p != null; p = p.getParent())
            if (p instanceof JScrollPane)
                return (JScrollPane) p;
        return null;
    }

    public void resetYLabelPanel() {
        final JPanel y = getYLabelsPanel();
        final JScrollPane jsp = getScrollPane();
        if (jsp != null)
            jsp.setRowHeaderView(y);
    }

    private void setGraphVisibleInterval(long min, long max) {
        minViewportModelX = min;
        maxViewportModelX = max;
        final JScrollPane jsp = getScrollPane();
        if (jsp != null) {
            final Rectangle r = new Rectangle(screenX(minViewportModelX), 0,
                    screenWidth(maxViewportModelX - minViewportModelX),
                    getHeight());
            scrollRectToVisible(r);
            final int y = jsp.getViewport().getViewPosition().y;
            final int x = screenX(minViewportModelX);
            jsp.getViewport().setViewPosition(new java.awt.Point(x, y));
        }
        repaint();
    }

    public int getLeftOffset() {
        return AXIS_OFFSET;
    }

    public int getTopOffset() {
        return AXIS_OFFSET;
    }

    public int getRightOffset() {
        return AXIS_OFFSET;
    }

    public int getBottomOffset() {
        return AXIS_OFFSET;
    }

    public long getMaxModelX() {
        return maxViewModelX;
    }

    public long getModelWidth() {
        return maxViewModelX - minViewModelX;
    }

    public void setForeground(GraphSeries g, Color b) {
        foregroundMap.put(g, b);
    }

    public Color getForeground(GraphSeries g) {
        Color c = foregroundMap.get(g);
        if (c == null)
            c = getForeground();
        return c;
    }

    void clear(Graphics2D g) {
        g.setColor(bkgColor);
        // g.fillRect(0, 0, getWidth(), getHeight());
    }

    void prepare(Graphics2D g) {
    }

    void paintAxes(Graphics2D g) {
        g.setColor(axesColor);
        g.drawLine(getLeftOffset(), getHeight() - getBottomOffset(),
                getLeftOffset(), getTopOffset());
        g.drawLine(getLeftOffset(), getHeight() - getBottomOffset(), getWidth()
                - getRightOffset(), getHeight() - getBottomOffset());
    }

    void paintTicks(Graphics2D g) {
    }

    void paintLabels(Graphics2D g) {
    }

    /** converts from model X to screen X */
    protected int screenX(long x) {
        final int res = screenWidth(x - minViewModelX) + getLeftOffset();
        return res;
    }

    /** converts from screen X to model X */
    protected long modelX(int screenX) {
        return modelWidth(screenX - getLeftOffset()) + minViewModelX;
    }

    protected int screenY(double y) {
        final double percent = y / getHeight();
        return (int) (getHeight() - percent * getHeight()) - AXIS_OFFSET;
    }

    /** converts from model width to screen width */
    protected int screenWidth(long w) {
        return (int) (w * zoomFactor);
    }

    /** converts from screen width to model width */
    protected long modelWidth(int w) {
        return (long) (w / zoomFactor);
    }

    protected int oX() {
        return AXIS_OFFSET;
    }

    protected int oY() {
        return getHeight() - AXIS_OFFSET;
    }

    public GraphSeries[] getGraphData() {
        return graph.toArray(new GraphSeries[graph.size()]);
    }

    public long getMinViewportModelX() {
        return minViewportModelX;
    }

    public long getMaxViewportModelX() {
        return maxViewportModelX;
    }

    public void paintCursor(Graphics2D g) {
        if (cursorIdx >= 0 && cursorGraphIdx >= 0) {
            final int cursorScreenX = screenX(graph.get(cursorGraphIdx).point(
                    cursorIdx).x());
            g.setColor(cursorColor);
            g.drawLine(cursorScreenX, AXIS_OFFSET, cursorScreenX, AXIS_OFFSET
                    + getHeight());
        }
    }

    final public void setCursorColor(Color c) {
        cursorColor = c;
    }

    public void setCursorScreenX(int x) {
        if (x <= AXIS_OFFSET)
            x = AXIS_OFFSET + 1;
        if (x > AXIS_OFFSET + getWidth())
            x = AXIS_OFFSET + getWidth();
        final long cursorX = modelX(x);
        long closestDist = Long.MAX_VALUE;
        for (int i = 0; i < graph.size(); i++) {
            final GraphSeries g = graph.get(i);
            final int idx = g.closestIndex(cursorX);
            final Point p = g.point(idx);
            final long dist = Math.abs(p.x() - cursorX);
            if (dist < closestDist) {
                cursorIdx = idx;
                cursorGraphIdx = i;
                closestDist = dist;
            }
        }
        for (final GraphCursorListener l : listeners)
            l.cursorChanged(graph.get(cursorGraphIdx), cursorIdx);
        repaint();
    }

    public int getCursorIndex() {
        return cursorIdx;
    }

    public GraphSeries getCursorGraph() {
        return cursorGraphIdx >= 0 ? graph.get(cursorGraphIdx) : null;
    }

    public void addListener(GraphCursorListener l) {
        listeners.add(l);
    }

    abstract public void paintGraph(Graphics2D g);

    public int xToEventIdx(int x) {
        for (final GraphSeries g : graph) {
            final int v = g.xToIndex(x);
            if (v >= 0)
                return v;
        }
        return -1;
    }

    public void setEnabled(GraphSeries gd, boolean value) {
        if (value)
            enabled.add(gd);
        else {
            final int idx = graph.indexOf(gd);
            if (idx > cursorGraphIdx) {
                cursorGraphIdx--;
            } else if (idx == cursorGraphIdx) {
                final int tmpGraphIdx = cursorGraphIdx;
                final int tmpIdx = cursorIdx;
                boolean moving = true;
                while (cursorGraphIdx == tmpGraphIdx && moving) {
                    moving = moveCursorLeft(false);
                }
                if (!moving) {
                    cursorGraphIdx = tmpIdx;
                    cursorIdx = tmpIdx;
                    while (cursorGraphIdx == tmpGraphIdx && moving) {
                        moving = moveCursorRight(false);
                    }
                }
            }
            enabled.remove(gd);
        }
    }

    public boolean isEnabled(GraphSeries gd) {
        return enabled.contains(gd);
    }

    /**
     * move the cursor to the next point across multiple graphs. The next point
     * is defined by applying in order the following rules: - if the next point
     * in the current graph has the same x as the current cursor, that's the
     * next cursor - if another graph has one or more points on the same x as
     * the current cursor, the first point for the first graph AFTER the current
     * cursor graph is the next point - the point with the minimum x > current
     * cursor is the next point.
     */
    public boolean moveCursorRight(boolean local) {
        int minIdx = Integer.MAX_VALUE;
        int minGraphIdx = Integer.MAX_VALUE;
        if (cursorIdx < 0)
            return false;
        final GraphSeries cursorGraph = graph.get(cursorGraphIdx);
        if (local) {
            if (cursorIdx == cursorGraph.size() - 1)
                return false;
            minIdx = cursorIdx + 1;
            minGraphIdx = cursorGraphIdx;
        } else {
            long minDist = Long.MAX_VALUE;
            final long currX = cursorGraph.point(cursorIdx).x();
            for (int gidx = 0; gidx < graph.size(); gidx++) {
                final GraphSeries g = graph.get(gidx);
                int idx;
                idx = g.insertionIndex(currX);
                if (idx > g.size() - 1) {
                    continue;
                }
                final long xx = g.point(idx).x();
                if (xx == currX) {
                    if (gidx < cursorGraphIdx) {
                        // rule 2
                        // this is not the graph of the current cursor position,
                        // but it has
                        // the same x. if it is one before, ignore it
                        continue;
                    }
                    if (gidx == cursorGraphIdx) {
                        // if we are on the same graph currently selected and
                        // with same x,
                        // move past the currently selected point
                        if (cursorIdx + 1 >= graph.get(gidx).size())
                            continue;
                        idx = cursorIdx + 1;
                        if (graph.get(gidx).point(idx).x() == currX) {
                            minIdx = idx;
                            minGraphIdx = gidx;
                            break;
                        }
                    } else {
                        // if there are multiple points with currX, we have the
                        // rightmost. back
                        // to the leftmost.
                        for (idx--; idx >= 0 && g.point(idx).x() == xx; idx--)
                            ;
                        minIdx = idx + 1;
                        minGraphIdx = gidx;
                        break;
                    }
                }
                // rule 3
                final long dist = g.point(idx).x() - currX;
                if (dist < minDist) {
                    minGraphIdx = gidx;
                    minIdx = idx;
                    minDist = dist;
                }
            }
            if (minIdx == Integer.MAX_VALUE) {
                minIdx = cursorIdx;
                minGraphIdx = cursorGraphIdx;
            }
        }
        if (minIdx != cursorIdx || minGraphIdx != cursorGraphIdx) {
            cursorIdx = minIdx;
            cursorGraphIdx = minGraphIdx;
            for (final GraphCursorListener l : listeners)
                l.cursorChanged(graph.get(cursorGraphIdx), cursorIdx);
            return true;
        } else
            return false;
    }

    public boolean moveCursorLeft(boolean local) {
        int minIdx = Integer.MAX_VALUE;
        int minGraphIdx = Integer.MAX_VALUE;
        if (cursorIdx < 0)
            return false;
        final GraphSeries cursorGraph = graph.get(cursorGraphIdx);
        if (local) {
            if (cursorIdx == 0)
                return false;
            minIdx = cursorIdx - 1;
            minGraphIdx = cursorGraphIdx;
        } else {
            long minDist = Long.MAX_VALUE;
            final long currX = cursorGraph.point(cursorIdx).x();
            for (int gidx = graph.size() - 1; gidx >= 0; gidx--) {
                final GraphSeries g = graph.get(gidx);
                int idx;
                idx = g.insertionIndex(currX);
                final long xx = (idx > g.size() - 1) ? g.point(idx - 1).x() : g
                        .point(idx).x();
                if (xx == currX) {
                    if (gidx > cursorGraphIdx) {
                        // rule 2
                        // this is not the graph of the current cursor position,
                        // but it has
                        // the same x. if it is one after, ignore it
                        continue;
                    }
                    if (gidx == cursorGraphIdx) {
                        // if we are on the same graph currently selected and
                        // with same x,
                        // move before the currently selected point
                        if (cursorIdx - 1 < 0)
                            continue;
                        idx = cursorIdx - 1;
                        if (graph.get(gidx).point(idx).x() == currX) {
                            minIdx = idx;
                            minGraphIdx = gidx;
                            break;
                        }
                    } else {
                        minIdx = idx;
                        minGraphIdx = gidx;
                        break;
                    }
                } else {
                    idx--;
                    if (idx < 0)
                        continue;
                }
                // rule 3
                final long dist = currX - g.point(idx).x();
                if (dist < minDist) {
                    minGraphIdx = gidx;
                    minIdx = idx;
                    minDist = dist;
                }
            }
            if (minIdx == Integer.MAX_VALUE)
                return false;
        }
        if (minIdx != cursorIdx || minGraphIdx != cursorGraphIdx) {
            cursorIdx = minIdx;
            cursorGraphIdx = minGraphIdx;
            for (final GraphCursorListener l : listeners)
                l.cursorChanged(graph.get(cursorGraphIdx), cursorIdx);
            return true;
        } else
            return false;
    }

    public JPanel getYLabelsPanel() {
        return null;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(0, 0);
    }
}
