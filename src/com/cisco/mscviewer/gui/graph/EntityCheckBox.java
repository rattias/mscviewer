package com.cisco.mscviewer.gui.graph;


import javax.swing.JCheckBox;

import com.cisco.mscviewer.graph.GraphSeries;

@SuppressWarnings("serial")
public class EntityCheckBox extends JCheckBox {
    public GraphSeries data;
    
    public EntityCheckBox(GraphSeries gd) {
        super(gd.getName());
        data = gd;
    }
}
