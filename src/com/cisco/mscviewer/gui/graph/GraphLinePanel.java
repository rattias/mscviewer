package com.cisco.mscviewer.gui.graph;

import java.awt.Graphics2D;

import com.cisco.mscviewer.graph.GraphData;
import com.cisco.mscviewer.graph.Point;
import com.cisco.mscviewer.tree.Interval;

@SuppressWarnings("serial")
public class GraphLinePanel extends GraphPanel {    
    @Override
    public void paintGraph(Graphics2D g) {
        GraphData[] graphs = getGraphData();
        long minX = getMinX();
        long maxX = getMaxX();
        for (GraphData d : graphs) {
            if (isEnabled(d)) {
                Interval in = d.getInterval(minX, maxX);
                int start = in.getStart();
                Point p = d.point(start);
                int x0 = screenX(p.x);
                int y0 = screenY(p.y);
                g.setColor(getForeground(d));
                for(int i = start+1; i<in.getEnd(); i++) {
                    p = d.point(i);
                    int x1 = screenX(p.x);
                    int y1 = screenY(p.y);
                    g.drawLine(x0, y0, x1, y1);
                    x0 = x1;
                    y0 = y1;
                }
            }
        }
    }    
}
