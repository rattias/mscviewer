package com.cisco.mscviewer.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cisco.mscviewer.model.MSCDataModel;

public class Graph {
    private String name;
    private ArrayList<GraphSeries> series;
    
    public Graph(String name) {
        this.name = name;
        this.series = new ArrayList<GraphSeries>();
        MSCDataModel.getInstance().addGraph(this);
    }
    
    public void add(GraphSeries g) {
        series.add(g);
    }
    
    public List<GraphSeries> getSeries() {
        return Collections.unmodifiableList(series);
    }
}
