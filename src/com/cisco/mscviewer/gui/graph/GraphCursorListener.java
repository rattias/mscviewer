package com.cisco.mscviewer.gui.graph;

import com.cisco.mscviewer.graph.GraphData;

public interface GraphCursorListener {
    public void cursorChanged(GraphData g, int idx);
}
