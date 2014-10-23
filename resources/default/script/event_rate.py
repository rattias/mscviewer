from com.cisco.mscviewer import Main
from com.cisco.mscviewer.model import MSCDataModel
from com.cisco.mscviewer.graph import GraphData
from mscviewer import *

    
    
@msc_fun
def event_rate_fn(window_ms=50):
    """
    Creates a set of graph representing rate of events, one for each entity of the
    opened entities.
    """
    model = Main.getModel()
    sz = model.getEventCount()
    cnt = 0;
    info = {}
    for en in msc_entities():
        info[en] = GraphData(en.getPath())
        info[en].setXType("time")
        
    pr = ProgressReport("processing events", "", 0, msc_event_count());
    for ev in msc_events():
        cnt += 1
        pr.progress(cnt)
        en = ev.getEntity()
        t = ev.getTimestamp()
        info[en].add(t, 1, ev)
    arr = []
    for en in msc_entities():
        inf = info[en]
        if not inf.isEmpty():
            arr.append(inf)
    model.addGraph(arr)
    pr.progressDone() 
