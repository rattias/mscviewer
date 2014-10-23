package com.cisco.mscviewer.gui.graph;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.KeyStroke;

import com.cisco.mscviewer.graph.GraphData;
import com.cisco.mscviewer.graph.Point;



@SuppressWarnings("serial")
abstract public class GraphPanel extends JPanel  {
    private final static String LEFT = "Left";
    private final static String LEFT_LOCAL = "LeftLocal";
    private final static String RIGHT = "Right";
    private final static String RIGHT_LOCAL = "RightLocal";
    private static int AXIS_OFFSET = 10;
    private Vector<GraphCursorListener> listeners = new Vector<GraphCursorListener>();
    private ArrayList<GraphData> graph = new ArrayList<GraphData>();
    private HashMap<GraphData, Color> foregroundMap = new HashMap<GraphData, Color>();
    private HashSet<GraphData> enabled = new HashSet<GraphData>(); 
    private long minX = Long.MAX_VALUE;
    private long maxX = Long.MIN_VALUE;
    private float maxY = Float.MIN_VALUE;
    private long graphMinX = Long.MAX_VALUE;
    private long graphMaxX = Long.MIN_VALUE;
    Color axesColor = Color.cyan;
    Color cursorColor = Color.white;
    Color bkgColor = Color.black;
    protected int cursorIdx = -1;
    protected int cursorGraphIdx = -1;
    private float zoomFactor = 1.0f;
    private boolean axis = true;
    private boolean ticks = true;
    private boolean labels = true;
    private FontMetrics fm;
    
    private AbstractAction moveLeft = new AbstractAction(LEFT) {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveCursorLeft(false);
            repaint();
        }
    };

    private AbstractAction moveLeftLocal = new AbstractAction(LEFT_LOCAL) {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveCursorLeft(true);
            repaint();
        }
    };

//    @Override
//    public Dimension getPreferredSize() {
//        int w = (int)((getGraphMaxX()-getGraphMinX());
//        Dimension d = new Dimension(w, getHeight());
//        return d;
//    }

    
    private AbstractAction moveRight = new AbstractAction(RIGHT) {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveCursorRight(false);
            repaint();
        }
    };

    private AbstractAction moveRightLocal = new AbstractAction(RIGHT_LOCAL) {
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
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), LEFT);
        this.getActionMap().put(LEFT, moveLeft);
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK), LEFT_LOCAL);
        this.getActionMap().put(LEFT_LOCAL, moveLeftLocal);
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), RIGHT);
        this.getActionMap().put(RIGHT, moveRight);
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK), RIGHT_LOCAL);
        this.getActionMap().put(RIGHT_LOCAL, moveRightLocal);
    }
    

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        fm = g2d.getFontMetrics();
        //clear(g2d);
        prepare(g2d);
        if (axis)
            paintAxes(g2d);
        if (ticks)
            paintTicks(g2d);
        if (labels)
            paintLabels(g2d);
        paintGraph(g2d);
        paintCursor(g2d);
    }

        
    public FontMetrics getFontMetrics() {
        return fm;
    }

    
    
    public void setXInterval(long minX, long maxX) {
        if (minX <graphMinX)
            minX = graphMinX;
        if (maxX > graphMaxX)
            maxX = graphMaxX;
        repaint();
    }
        
    public String getXType() {
        return graph.isEmpty()? "" : graph.get(0).getXType();
    }

    public String getYType() {
        return graph.isEmpty()? "" : graph.get(0).getYType();
    }

    public void setupYLabels(JPanel p) {
        
    }
    
    public void addGraph(GraphData gd) throws IllegalArgumentException {
        if (! graph.isEmpty()) {
            GraphData g0 = graph.get(0);
            String g0xType = g0.getXType();
            String gdxType = gd.getXType();
            if (! gdxType.equals(g0xType))
                throw new IllegalArgumentException("Graph "+gd.getName()+" has X type "+gdxType+" and can't be rendered with graph "+g0.getName()+" of X type "+g0xType); 
            String g0yType = g0.getYType();
            String gdyType = gd.getYType();
            if (! gdyType.equals(g0yType))
                throw new IllegalArgumentException("Graph "+gd.getName()+" has Y type "+gdyType+" and can't be rendered with graph "+g0.getName()+" of X type "+g0yType); 
        }
        graph.add(gd);
        enabled.add(gd);
        minX = graphMinX = Math.min(minX, gd.minX());
        maxX = graphMaxX = Math.max(maxX, gd.maxX());
        maxY = Math.max(maxY,  gd.maxY());
        
    }
    
    public int getLeftOffset() { return AXIS_OFFSET; }
    public int getTopOffset() { return AXIS_OFFSET; }
    public int getRightOffset() { return AXIS_OFFSET; }
    public int getBottomOffset() { return AXIS_OFFSET; }
    
    public void setForeground(GraphData g, Color b) {
        foregroundMap.put(g, b);
    }
    
    public Color getForeground(GraphData g) {
        Color c = foregroundMap.get(g);
        if (c == null)
            c = getForeground();
        return c;
    }
    

    void clear(Graphics2D g) {
        g.setColor(bkgColor);
        //g.fillRect(0,  0, getWidth(), getHeight());
    }
    
    void prepare(Graphics2D g) {        
    }
    
    void paintAxes(Graphics2D g) {
        g.setColor(axesColor);
        g.drawLine(getLeftOffset(), getHeight()-getBottomOffset(), getLeftOffset(), getTopOffset());
        g.drawLine(getLeftOffset(), getHeight()-getBottomOffset(), getWidth()-getRightOffset(), getHeight()-getBottomOffset());
    }

    void paintTicks(Graphics2D g) {
    }

    void paintLabels(Graphics2D g) {
    }

    
    protected int screenX(long x) {
        int res = getLeftOffset() + (int)(((x-minX))*(getWidth()-getLeftOffset()-getRightOffset())/(maxX-minX));
        return res;
    }
    
    protected long modelX(int screenX) {
        return (((long)screenX) - getLeftOffset())*(maxX-minX)/(getWidth()-getLeftOffset()-getRightOffset())+minX;
    }
    

    protected int screenY(float y) {
        float percent = y/maxY;
        return (int)(getHeight() -  percent * getHeight()) - AXIS_OFFSET;
    }
    
    protected int oX() {
        return AXIS_OFFSET;
    }
    
    protected int oY() {
        return getHeight()-AXIS_OFFSET;
    }

    
    public GraphData[] getGraphData() {
        return graph.toArray(new GraphData[graph.size()]);
    }

    public long getMinX() {
        return minX;
    }
    
    public long getMaxX() {
        return maxX;
    }
    
    public long getGraphMinX() {
        return graphMinX;        
    }

    public long getGraphMaxX() {
        return graphMaxX;        
    }
    

      
    public void paintCursor(Graphics2D g) {
        if (cursorIdx >= 0) {
            int cursorScreenX = screenX(graph.get(cursorGraphIdx).point(cursorIdx).x);
            g.setColor(cursorColor);
            g.drawLine(cursorScreenX, AXIS_OFFSET, cursorScreenX, AXIS_OFFSET+getHeight());
        }
    }

    
    final public void setCursorColor(Color c) {
        cursorColor = c;
    }
    
 
    public void setCursorScreenX(int x) {
        if (x <= AXIS_OFFSET)
            x = AXIS_OFFSET+1;
        if (x > AXIS_OFFSET + getWidth())
            x = AXIS_OFFSET + getWidth();
        long cursorX = modelX(x);
        long closestDist = Long.MAX_VALUE;
        for(int i=0; i<graph.size(); i++) {
            GraphData g = graph.get(i);
            int idx = g.closestIndex(cursorX);
            Point p = g.point(idx);
            long dist = Math.abs(p.x-cursorX); 
            if (dist < closestDist) {
                cursorIdx = idx;
                cursorGraphIdx = i;
                closestDist = dist;
            }
        }
        for(GraphCursorListener l: listeners)
            l.cursorChanged(graph.get(cursorGraphIdx), cursorIdx);
        repaint();
    }
    
    public int getCursorIndex() {
        return cursorIdx;
    }
    
    public GraphData getCursorGraph() {
        return cursorGraphIdx >= 0 ? graph.get(cursorGraphIdx) : null;
    }
    
    public void addListener(GraphCursorListener l) {
        listeners.add(l);
    }
    
    abstract public void paintGraph(Graphics2D g);

    public int xToEventIdx(int x) {
        for(GraphData g: graph) {
            int v = g.xToIndex(x);
            if (v >=0)
                return v;
        }
        return -1;
    }

    public void setEnabled(GraphData gd, boolean value) {
        if (value)
            enabled.add(gd);
        else {
            int idx = graph.indexOf(gd);
            if (idx > cursorGraphIdx) {
                cursorGraphIdx--;
            } else if (idx == cursorGraphIdx){
                int tmpGraphIdx = cursorGraphIdx;
                int tmpIdx = cursorIdx; 
                boolean moving = true;
                while(cursorGraphIdx == tmpGraphIdx && moving) {
                    moving = moveCursorLeft(false);
                }
                if (! moving) {
                    cursorGraphIdx = tmpIdx;
                    cursorIdx = tmpIdx;
                    while(cursorGraphIdx == tmpGraphIdx && moving) {
                        moving = moveCursorRight(false);
                    }
                }
            }
            enabled.remove(gd);
        }
    }
    
    public boolean isEnabled(GraphData gd) {
        return enabled.contains(gd);
    }
    /**
     * move the cursor to the next point across multiple graphs. The next point
     * is defined by applying in order the following rules:
     * - if the next point in the current graph has the same x as the current cursor,
     *   that's the next cursor
     * - if another graph has one or more points on the same x as the current cursor,
     *   the first point for the first graph AFTER the current cursor graph is the
     *   next point
     * - the point with the minimum x > current cursor is the next point.
     */
    public boolean moveCursorRight(boolean local) {
        int minIdx = Integer.MAX_VALUE;
        int minGraphIdx = Integer.MAX_VALUE;
        if (cursorIdx < 0)
            return false;
        GraphData cursorGraph = graph.get(cursorGraphIdx);
        if (local) {
            if (cursorIdx == cursorGraph.size()-1)
                return false;
            minIdx = cursorIdx + 1;
            minGraphIdx  = cursorGraphIdx;
        } else {
            long minDist = Long.MAX_VALUE;
            long currX = cursorGraph.point(cursorIdx).x;
            for(int gidx = 0; gidx < graph.size(); gidx++) {
                GraphData g = graph.get(gidx);
                int idx;
                idx = g.insertionIndex(currX);
                if (idx > g.size()-1) {
                    continue;
                }
                long xx = g.point(idx).x;
                if (xx == currX) {
                    if (gidx < cursorGraphIdx) {
                        // rule 2
                        // this is not the graph of the current cursor position, but it has
                        // the same x. if it is one before, ignore it
                        continue;                    
                    }
                    if (gidx == cursorGraphIdx) {
                        // if we are on the same graph currently selected and with same x,
                        // move past the currently selected point
                        if (cursorIdx + 1 >= graph.get(gidx).size())
                            continue;
                        idx = cursorIdx + 1;
                        if (graph.get(gidx).point(idx).x == currX) {
                            minIdx = idx;
                            minGraphIdx = gidx;
                            break;
                        }
                    } else {
                        // if there are multiple points with currX, we have the rightmost. back
                        // to the leftmost.
                        for(idx--; idx>=0 && g.point(idx).x == xx; idx--)
                            ;
                        minIdx = idx + 1;
                        minGraphIdx = gidx;
                        break;
                    }
                }
                // rule 3          
                long dist = g.point(idx).x - currX;
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
            for(GraphCursorListener l: listeners)
                l.cursorChanged(graph.get(cursorGraphIdx), cursorIdx);
            return true;
        } else
            return false;
    }
    
    public boolean moveCursorLeft(boolean local) {
        int minIdx = Integer.MAX_VALUE;
        int minGraphIdx = Integer.MAX_VALUE;
        if (cursorIdx < 0 )
            return false;
        GraphData cursorGraph = graph.get(cursorGraphIdx);
        if (local) {
            if (cursorIdx == 0)
                return false;
            minIdx = cursorIdx-1;
            minGraphIdx = cursorGraphIdx;
        } else {
            long minDist = Long.MAX_VALUE;
            long currX = cursorGraph.point(cursorIdx).x;
            for(int gidx = graph.size()-1; gidx >= 0 ; gidx--) {
                GraphData g = graph.get(gidx);
                int idx;
                idx = g.insertionIndex(currX);
                long xx = (idx > g.size()-1) ? g.point(idx-1).x : g.point(idx).x;
                if (xx == currX) {
                    if (gidx > cursorGraphIdx) {
                        // rule 2
                        // this is not the graph of the current cursor position, but it has
                        // the same x. if it is one after, ignore it
                        continue;                    
                    }
                    if (gidx == cursorGraphIdx) {
                        // if we are on the same graph currently selected and with same x,
                        // move before the currently selected point
                        if (cursorIdx - 1 < 0)
                            continue;
                        idx = cursorIdx - 1;
                        if (graph.get(gidx).point(idx).x == currX) {
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
                long dist = currX - g.point(idx).x;
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
            for(GraphCursorListener l: listeners)
                l.cursorChanged(graph.get(cursorGraphIdx), cursorIdx);
            return true;
        } else
            return false;
    }

    public JPanel getYLabelsPanel() {
        return null;
    }

}
