"""This module contains the Pthon API to interact with MSCViewer graphing capabilities. 

  Even though Python scripts may have access to the full Java API directly, 
  this module defines a stable interface that will guarantee
  compatibility in the future. Internal Java APIs may, and most likely will, 
  be subject to change, so the user is strongly advised against its direct usage.
"""

from com.cisco.mscviewer.graph import Graph, GraphSeries

class graph_type(object):
    HEAT = 1

def graph(name=None):
    """THIS IS AN EXPERIMENTAL API"""
    return Graph(name)

def series(name):
    return GraphSeries(name)
    

def graph_add_series(graph, series):
    graph.add(series)
    
def graph_series():
    graph.getSeries()

def series_point_at(series, idx):
    p = series.getPointAt(idx)
    return (p.x, p.y, p.o)

def series_points(series):
    class ThisIter:
        def __init__(self, series):
            self.idx = 0
            self.series = series
            
        def __iter__(self):
            return self

        def next(self):
            if self.idx >= len(self.series.getPoints()):
                raise StopIteration
            self.idx += 1
            return series_point_at(self.series, self.idx-1)
    return ThisIter(series)              
