from msc.model import *
from msc.gui import *
from msc.flowdef import *

from sets import Set

def add_backward(selEv, res):
    pending = [selEv]
    for ev in pending:
        res.append(ev)
        ent = event_entity(ev)
        inters = event_interactions(ev)
        limit = entity_first_event_index(ent)        
        for inter in inters:
            fromEv = interaction_from_event(inter)
            if fromEv: 
                pending.append(fromEv)
        if len(inters) != 0:
            continue
        idx = event_index(ev) - 1
        while idx >= limit:
            tmpEv = event_at(idx)
            if event_entity(tmpEv) == ent:
                pending.append(tmpEv)
                break
            idx -= 1

def add_forward(selEv, res):
    pending = [selEv]
    for ev in pending:
        res.append(ev)
        ent = event_entity(ev)
        inters = event_interactions(ev, outgoing=True)
        limit = entity_last_event_index(ent)
        for inter in inters:
            toEv = interaction_to_event(inter)
            if toEv: 
                pending.append(toEv)

        idx = ev.getIndex() + 1
        while idx <= limit:
            tmpEv = event_at(idx)
            if event_entity(tmpEv) == ent:
                inters = event_interactions(tmpEv)
                if len(inters) == 0 and not event_is_block_begin(tmpEv):
                    pending.append(tmpEv)
                break
            idx += 1
	
@msc_fun
def mark_flow(before=False, after=True):
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
        such event begins a block
    3) if TMP is not empty, goto 2    
    
    Arguments:
    - before: if set to True all events before the selected one
              and belonging to the same flow are marked.
              default is False
    - after:  if set to True all events after the selected one
              and belonging to the same flow are marked.
              default is True
    """
    selEv = event_selected()
    if not selEv:
        inter = interaction_selected()
        if not inter:
            raise Exception("No event or interaction was selected")
        selEv = interaction_to_event(inter)
    res = []
    if before:
        add_backward(selEv, res)
    if after:
        add_forward(selEv, res)
    flow_mark(res, marker.GREEN)


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
    """
    prints on standard output a flow definition which captures the flow
    currently marked.
    """
    
    graph = set()
    # find marked events and add them to set
    for ev in events():
        if event_marker(ev):
            graph.add(ev)
            
    
    # find roots
    roots = []
    for el in graph:
        if event_predecessor(el, same_entity=True) in graph:
            continue
        incs = event_interactions(el, outgoing=False)
        isRoot = True
        for inc in incs:
            fs = interaction_from_event(inc)
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
        print "calling get_flow"
        get_flow(graph, roots[0], 1, f)
    print f[0]
    
def get_nexts(graph, node):
  nexts = []
  n = event_successor(node, same_entity=True) 
  if n in graph and not event_is_block_begin(n):
      nexts = [n]
  else:
      nexts = []
  nexts.extend([interaction_to_event(outInter) for outInter in event_interactions(node, outgoing=True) if interaction_type(outInter) != "Transition" and interaction_to_event(outInter) in graph])
  print "NEXTS(",event_index(node),") = ", [event_index(ev) for ev in nexts]
  return nexts

def fevent(n):
    return 'fev("'+n.getEntity().getPath()+'", "'+n.getLabel()+'")'

def ind(v):
    return "  "*v

def get_flow(graph, node, indent, res, s=Set()):
    in_fseq = False
    while node:
        if node in s:
            # raise Exception("node "+str(event_index(node))+" already in set")
            return
        s.add(node)
    	print ind(indent), "NODE ", event_index(node), node.isBlockBegin()
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
            get_flow(graph, nexts[0], indent+2, res, s)
            res[0] += ",\n"+ind(indent+1)+'dst='
            get_flow(graph, nexts[1], indent+2, res, s)
            res[0] += "\n"+ind(indent)+") //fint 2"
            if in_fseq:
                 res[0] += "\n"+ind(indent-1)+") //fseq"
            return              
"""        