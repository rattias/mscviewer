from com.cisco.mscviewer.model import MSCDataModel
from com.cisco.mscviewer.graph import GraphData
import mscviewer
import math
    
@mscviewer.msc_fun  
def event_rate_fn():
    """
    Creates a set of graph representing rate of events, one for each entity of the
    opened entities.
    """
    cnt = 0;
    info = {}
    evcount = mscviewer.event_count()
    for en in mscviewer.entities():
        info[en] = GraphData(en.getPath())
        info[en].setXType("time")
        
    pr = mscviewer.progress_start("processing events", 0, mscviewer.event_count());
    prev = {}
    twidth = mscviewer.event_timestamp(mscviewer.event_at(evcount-1)) - mscviewer.event_timestamp(mscviewer.event_at(0))
    for ev in mscviewer.events():
        cnt += 1
        mscviewer.progress_report(pr, cnt)
        en = mscviewer.event_entity(ev)
        enp = mscviewer.entity_path(en)
        t = mscviewer.event_timestamp(ev)
        v = 0
        if enp in prev:
            tprev = mscviewer.event_timestamp(prev[enp])
            v = t - tprev
            if v < 0:
                v = -v
            pv = v
            v = math.log(1+v)
        else:
            pv = 0
            v = 0
        info[en].add(t, v, ev)
        prev[enp] = ev
        
    arr = []
    for en in mscviewer.entities():
        inf = info[en]
        if not inf.isEmpty():
            arr.append(inf)
    mscviewer.graph(arr)
    pr.progressDone() 
