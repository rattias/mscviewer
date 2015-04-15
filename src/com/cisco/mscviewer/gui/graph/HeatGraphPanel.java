package com.cisco.mscviewer.gui.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cisco.mscviewer.graph.GraphSeries;
import com.cisco.mscviewer.graph.Point;
import com.cisco.mscviewer.tree.Interval;

@SuppressWarnings("serial")
public class HeatGraphPanel extends GraphPanel {
    private final static int BAR_HEIGHT = 6;
    private HashMap<GraphSeries, JLabel> labels; 
    private static Color[] heatMap = new Color[256];
    
    static {
        int l = heatMap.length;
        for(int i=0; i<l; i++) {
            float f = ((float)i)/l;
            heatMap[i] = new Color(red(f), green(f), blue(f)); 
        }
    }
    
    public HeatGraphPanel() {
        labels = new HashMap<GraphSeries, JLabel>();
    }
    
    public JPanel getYLabelsPanel() {
        JPanel p = new JPanel();
        p.setBackground(Color.black);
        p.setOpaque(true);
        p.setLayout(new GridLayout(-1, 1));
        p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 4));
        for(GraphSeries gd: getGraphData()) {
            if (isEnabled(gd)) {
                JLabel l = new JLabel(gd.getName(), JLabel.RIGHT);
                l.setForeground(Color.lightGray);
                labels.put(gd, l);
                p.add(l);
            }
        }
        return p;
    }

    public void prepare (Graphics2D g) {        
    }
    
    
    @Override
    public void paintGraph(Graphics2D g) {
        GraphSeries[] graphs = getGraphData();
        long minX = 0;
        long maxX = getMaxModelX();
        for (GraphSeries d : graphs) {
            if (isEnabled(d)) {
                Interval in = d.getInterval(minX, maxX);
                g.setColor(getForeground(d)); 
                if (labels != null) {
                    JLabel l = labels.get(d); 
                    int y = l.getY() + l.getHeight()/2;
                    double et = d.maxY()-d.minY();
                    if (et == 0)
                        et = 1;
                    int prevx = -1;
                    int prevcnt = 0;
                    for(int i = in.getStart(); i<in.getEnd(); i++) {
                        Point p = d.point(i);
                        int x = screenX(p.x);
                        if (x == prevx && prevcnt < heatMap.length-1)
                            prevcnt++;
                        else {
                            prevx = x;
                            prevcnt=0;
                        }
                        int idx = (int)((p.y/et)*heatMap.length);
                        if (idx >= heatMap.length)
                            idx = heatMap.length-1;
                        //System.out.println(d.getName()+": idx = "+idx+", et= "+et+", p.y= "+p.y);
                        Color c = heatMap[prevcnt];
                        g.setColor(c);
                        g.drawLine(x, y-BAR_HEIGHT/2, x, y+BAR_HEIGHT/2);
                    }
                }
            }
        }
    }
    
    public void paintCursor(Graphics2D g2d) {
        super.paintCursor(g2d);
        GraphSeries cursorGraph = getCursorGraph();
        if (cursorGraph == null)
            return;
        int cursorScreenX = screenX(cursorGraph.point(cursorIdx).x);
        JLabel l = labels.get(cursorGraph);
        if (l == null)
            return;
        Integer gy = l.getY()+l.getHeight()/2;
        if (gy == null)
            return;
        g2d.drawOval(cursorScreenX-3, gy-BAR_HEIGHT/2, 6, BAR_HEIGHT);
    }

    private static float interpolate(float val, float y0, float x0, float y1, float x1 ) {
        return (val-x0)*(y1-y0)/(x1-x0) + y0;
    }

    
    private static float base(float val) {
        if ( val <= -0.75 ) return 0;
        else if ( val <= -0.25 ) return interpolate( val, 0.0f, -0.75f, 1.0f, -0.25f);
        else if ( val <= 0.25 ) return 1.0f;
        else if ( val <= 0.75 ) return interpolate( val, 1.0f, 0.25f, 0.0f, 0.75f);
        else return 0.0f;
    }

    private static float red(float gray) {
        return base(gray - 0.5f);
    }
    private static float green(float gray) {
        return base(gray);
    }
    private static float  blue(float gray) {
        return base(gray + 0.5f);
    }
}
