package com.cisco.mscviewer.gui.graph;

import com.cisco.mscviewer.graph.GraphData;
import com.cisco.mscviewer.graph.Point;

public interface GraphCursorListener {
    public void cursorChanged(GraphData g, int idx);
}
