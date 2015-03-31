from com.cisco.mscviewer import Main
from com.cisco.mscviewer.model import MSCDataModel
from com.cisco.mscviewer.graph import GraphData
from mscviewer import *
import math
    
@msc_fun  
def event_rate_fn():
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
    prev = {}
    """
    for ev in msc_events():
        en = ev.getEntity()
        if en in prev:
            x = prev[en]            
            info[en].add(x, x, ev)
            prev[en] = x+1
        else:
            info[en].add(0, 0, ev)
            prev[en] = 1
    """                
    twidth = model.getEventAt(model.getEventCount()-1).getTimestamp() - model.getEventAt(0).getTimestamp()
    for ev in msc_events():
        cnt += 1
        pr.progress(cnt)
        en = ev.getEntity()
        enp = en.getPath()
        t = ev.getTimestamp()
        v = 0
        if enp in prev:
            tprev = prev[enp].getTimestamp()
            v = t - tprev
            if v < 0:
                v = -v
            pv = v
            v = math.log(1+v)
        else:
            pv = 0
            v = 0
        info[en].add(t, v, ev)
#        if en.getPath() == "XRVR":
#            print "XRVR: ",v, pv
        prev[enp] = ev
        
    arr = []
    for en in msc_entities():
        inf = info[en]
        if not inf.isEmpty():
            arr.append(inf)
    model.addGraph(arr)
    pr.progressDone() 
