from mscviewer import *

def add_backward(selEv, res):
    pending = [selEv]
    for ev in pending:
        res.append(ev)
        ent = ev.getEntity()
        inters = ev.getIncomingInteractions()
        limit = ent.getFirstEventIndex()        
        for inter in inters:
            fromEv = inter.getFromEvent()
            if fromEv: 
                pending.append(fromEv)
        if len(inters) != 0:
            continue
        idx = ev.getIndex() - 1
        while idx >= limit:
            tmpEv = Main.getModel().getEventAt(idx)
            if tmpEv.getEntity() == ent:
                pending.append(tmpEv)
                break
            idx -= 1

def add_forward(selEv, res):
    pending = [selEv]
    for ev in pending:
        res.append(ev)
        ent = ev.getEntity()
        inters = ev.getOutgoingInteractions()
        limit = ent.getLastEventIndex()
        for inter in inters:
            toEv = inter.getToEvent()
            if toEv: 
                pending.append(toEv)

        idx = ev.getIndex() + 1
        while idx <= limit:
            tmpEv = Main.getModel().getEventAt(idx)
            if tmpEv.getEntity() == ent:
                inters = tmpEv.getIncomingInteractions()
                if len(inters) == 0:
                    pending.append(tmpEv)
                break
            idx += 1
	
@msc_fun
def mark_flow(before=True, after=True):
    """
    marks in green a flow starting from the selected event. 
    
    a flow here is a set S of event constructed as follows:
      1) TMP = {Es} (where Es is the selected event)
    2) for each event E in TMP:
        - add E to S
        - remove E from TMP
        - add to S all events which are sinks for interactions
        where E is the source
        - add to S the next event in the same Entity as E, unless
        such event is a sink
    3) if TMP is not empty, goto 2    
    
    Arguments:
    - before: if set to True all events before the selected one
              and belonging to the same flow are marked.
              default is True
    - after:  if set to True all events after the selected one
              and belonging to the same flow are marked.
              default is True
    """
    selEv = Main.getSelectedEvent()
    if not selEv:
        inter = Main.getSelectedInteraction()
        if not inter:
            raise Exception("No event or interaction was selected")
        selEv = inter.getToEvent()
    res = []
    if before:
        add_backward(selEv, res)
    if after:
        add_forward(selEv, res)
    msc_flow_mark(res, msc_color.GREEN)


class Node:
    def __init__(ev):
        self.ev = ev
        self.incoming = []
        self.outgoing = []
        
    def out():
        return [el.to for el in outgoing]
        
class Arc:
    def __init__(fr, to):
        self.fr = fr
        self.to = to
        fr.outgoing.add(self)
        to.incoming.add(self)
            

@msc_fun
def marked_to_flow():
    graph = set()
    m = Main.getModel()
    # find marked events and add them to set
    for i in range(0, m.getEventCount()):
        ev = m.getEventAt(i)        
        if ev.getMarker():
            graph.add(ev)
            
    
    # find roots
    roots = []
    for el in graph:
        if el.getPreviousEventForEntity() in graph:
            continue
        incs = el.getIncomingInteractions()
        isRoot = True
        for inc in incs:
            fs = inc.getFromEvent()
            if fs in graph:
                isRoot = False
                break
        if isRoot: 
            roots.append(el)
    print "roots=", roots
    f = [""]
    lr = len(roots)
    if lr > 1:
        f = ["fall("]
        for i in range(lr):
            get_flow(graph, roots[i], 1, f) 
            if i < lr-1:
                f[0] += ",\n"
            else:
                f[0] += ")"
        
    elif lr == 1:
        get_flow(graph, roots[0], 1, f)
    print f[0]
    
def get_nexts(graph, node):
  nexts = []
  n = node.getNextEventForEntity() 
  if n in graph:
      nexts = [n]
  else:
      nexts = []
  nexts.extend([outInter.getToEvent() for outInter in node.getOutgoingInteractions() if outInter.getType() != "Transition" and outInter.getToEvent() in graph])
  return nexts

def fevent(n):
    return 'fev("'+n.getEntity().getPath()+'", "'+n.getLabel()+'")'

def ind(v):
    return "  "*v

def get_flow(graph, node, indent, res):
    in_fseq = False
    while node:
        nexts = get_nexts(graph, node) 
        ln = len(nexts)
        if ln == 0:
            res[0] += "\n"+ind(indent)+fevent(node)
            if in_fseq:
                indent -= 1
                res[0] += "\n"+ind(indent)+") //fseq 1"
            return    
        elif ln == 1:
            if not in_fseq:
                res[0] += "\n"+ind(indent)+"fseq("
                indent += 1
                res[0] += "\n"+ind(indent)+fevent(node)
                in_fseq = True
            else:
                res[0] += ",\n"+ind(indent)+fevent(node)
            node = nexts[0]
        elif ln == 2:
            res[0] += ",\n"+ind(indent)+'fint("'+node.getEntity().getPath()+'", "'+nexts[1].getEntity().getPath()+', "'+node.getLabel()+'")'
            res[0] += ",\n"+ind(indent+1)+'src='
            get_flow(graph, nexts[0], indent+2, res)
            res[0] += ",\n"+ind(indent+1)+'dst='
            get_flow(graph, nexts[1], indent+2, res)
            res[0] += "\n"+ind(indent)+") //fint 2"
            if in_fseq:
                 res[0] += "\n"+ind(indent-1)+") //fseq"
            return              
        