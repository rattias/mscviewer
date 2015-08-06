package com.cisco.mscviewer.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cisco.mscviewer.model.MSCDataModel;

public class Graph {
    private final ArrayList<GraphSeries> series;
    private String name;
    
    public Graph(String name) {
        this.series = new ArrayList<GraphSeries>();
        MSCDataModel.getInstance().addGraph(this);
        this.name = name;
    }

    public void add(GraphSeries g) {
        series.add(g);
    }

    public List<GraphSeries> getSeries() {
        return Collections.unmodifiableList(series);
    }
}
