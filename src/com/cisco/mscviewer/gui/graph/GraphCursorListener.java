package com.cisco.mscviewer.gui.graph;

import com.cisco.mscviewer.graph.GraphSeries;

public interface GraphCursorListener {
    public void cursorChanged(GraphSeries g, int idx);
}
