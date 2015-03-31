package com.cisco.mscviewer.gui.graph;


import javax.swing.JCheckBox;

import com.cisco.mscviewer.graph.GraphData;

@SuppressWarnings("serial")
public class EntityCheckBox extends JCheckBox {
    public GraphData data;
    
    public EntityCheckBox(GraphData gd) {
        super(gd.getName());
        data = gd;
    }
}
