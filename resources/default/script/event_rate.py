import msc.model as model
import msc.graph as graph
import msc.gui as gui
import math
    
@gui.msc_fun  
def event_rate_fn():
    """
    Creates a set of graph representing rate of events, one for each entity of the
    opened entities.
    """
    cnt = 0;
    info = {}
    evcount = model.event_count()
    for en in model.entities():
        info[en] = graph.series(en.getPath())
        info[en].setXType("time")
        
    pr = gui.progress_start("processing events", 0, model.event_count());
    prev = {}
    twidth = model.event_timestamp(model.event_at(evcount-1)) - model.event_timestamp(model.event_at(0))
    for ev in model.events():
        cnt += 1
        gui.progress_report(pr, cnt)
        en = model.event_entity(ev)
        enp = model.entity_path(en)
        t = model.event_timestamp(ev)
        v = 0
        if enp in prev:
            tprev = model.event_timestamp(prev[enp])
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
        
    g = graph.graph("graph") 
    for en in model.entities():
        inf = info[en]
        if not inf.isEmpty():
            graph.graph_add_series(g, inf)
            print model.entity_path(en)
            for p in graph.series_points(inf):
                print p[0], p[1]
                    
    gui.graph_show(g, graph.graph_type.HEAT)
    gui.progress_done(pr)