package com.cisco.mscviewer.gui.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cisco.mscviewer.graph.GraphData;
import com.cisco.mscviewer.graph.Point;
import com.cisco.mscviewer.tree.Interval;

@SuppressWarnings("serial")
public class GraphCloudPanel extends GraphPanel {
    private JPanel labelsP;
    private JPanel graphP;
    private final static int BAR_HEIGHT = 6;
    private HashMap<GraphData, JLabel> labels; 
    
    public JPanel getYLabelsPanel() {
        JPanel p = new JPanel();
        Color b = getBackground();
        System.out.println(b);
        p.setBackground(b);
        p.setOpaque(true);
        p.setLayout(new GridLayout(-1, 1));
        p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 4));
        labels = new HashMap<GraphData, JLabel>();
        for(GraphData gd: getGraphData()) {
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
        GraphData[] graphs = getGraphData();
        long minX = getMinX();
        long maxX = getMaxX();
        for (GraphData d : graphs) {
            if (isEnabled(d)) {
                Interval in = d.getInterval(minX, maxX);
                g.setColor(getForeground(d)); 
                JLabel l = labels.get(d); 
                int y = l.getY() + l.getHeight()/2; 
                for(int i = in.getStart(); i<in.getEnd(); i++) {
                    Point p = d.point(i);
                    int x = screenX(p.x);
                    g.drawLine(x, y-BAR_HEIGHT/2, x, y+BAR_HEIGHT/2);
                }
            }
        }
    }
    
    public void paintCursor(Graphics2D g2d) {
        super.paintCursor(g2d);
        GraphData cursorGraph = getCursorGraph();
        if (cursorGraph != null) {
            int cursorScreenX = screenX(cursorGraph.point(cursorIdx).x);
            JLabel l = labels.get(cursorGraph);
            Integer gy = l.getY()+l.getHeight()/2;
            if (gy != null) {
                g2d.drawOval(cursorScreenX-3, gy-BAR_HEIGHT/2, 6, BAR_HEIGHT);
            }
        }
    }
    
}
