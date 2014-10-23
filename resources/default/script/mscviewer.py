from string import Template
from com.cisco.mscviewer import Main
from com.cisco.mscviewer.util import Report, ProgressReport, Utils
from com.cisco.mscviewer.model import Entity, Event, Interaction
from com.cisco.mscviewer.gui import Marker
from types import TupleType 
import sys 
import re
#import logging WATCH OUT! this import changes name of thread to MainThread for some reason
import inspect
import time

#MATCH_LOGGER =  logging.getLogger("flow_match")
#logging.basicConfig(filename='example.log',level=logging.DEBUG)
#logging.warning("logging enabled (warning)")
#logging.debug("logging enabled (debug)")

msc_debug = {} 
DBG_MATCH = " db"
class AbortSearchException(Exception):
    pass

class msc_color(object):
    GREEN = Marker.GREEN
    BLUE = Marker.BLUE
    RED = Marker.RED
    YELLOW = Marker.YELLOW
    

class FlowError(Exception):
    causes = None
    
    #causes is one or a list of FlowError 
    def __init__(self, message, causes=None):
        self.causes = causes        
        if self.causes != None:
            cstr = "\nCause:\n"        
            for c in self.causes:
                cstr += str(c)+"\n";
        else:
            cstr = ""
        Exception.__init__(self, message+cstr)
        
        

#decorator to declare entry point msc functions, which are listed in the GUI tree 
#class msc_fun(object):
#    def __init__(self, f):
#        self.msc_function = f
#        #print "GETARGSPEC: ", inspect.getargspec(self.f)
#        self.msc_cmd = True
#        self.__doc__ = f.__doc__
#    def __call__(self, *args, **kw):
#        self.msc_function(*args, **kw)

def msc_fun(f):
    f.is_msc_fun = True
    return f

        
class MSCTypeError(TypeError):
    def __init__(self, expectedtype, actualtype):
        TypeError.__init__(self, "Argument expected to be of type '"+str(expectedtype)+"', found '"+str(actualtype)+"'")

            
#def msc_list_functions(pkg):
#    ret = [] 
#    print "TYPE(PKG): ", type(pkg)
#    print "LISTING PKG: ", pkg
#    for name in dir(pkg):
#        print "FN: ", name 
#        for attr in dir(getattr(pkg, name)):
#            #print "ATTR ", attr
#            if attr == "msc_cmd":
#                print "IS MSC_FUN"
#                ret.append(name)
#    return ret

def msc_list_functions(pkg):
    ret = []  
    for fn_name in dir(pkg):
        fn = getattr(pkg, fn_name)
        if hasattr(fn, "is_msc_fun"):
            ret.append(fn_name) 
    return ret;

def msc_get_function_args():    
    for name in dir(pkg):
        f = getattr(pkg, "msc_entities")
        if f != None:
            return inspect.getargspec(f)
            
    
def msc_entities(model=None, rootOnly=False):
    """
    returns an iterable on all entities in the model
    """
    if model==None:
        model = Main.getModel();
    it = model.getEntityIterator(rootOnly)
    while it.hasNext():
        yield it.next()

def msc_entity_count(model=None, rootOnly=False):
    if model==None:
        model = Main.getModel();
    if rootOnly:
        return model.getRootEntityCount()
    else:
        return model.getEntityCount()

def msc_events(model=None):
    """
    returns an iterable on all events in the model
    """
    if model == None:
        model = Main.getModel()
    cnt = model.getEventCount()
    for i in range(cnt):
        yield model.getEventAt(i)

def msc_event_count(model=None):
    if model==None:
        model = Main.getModel()
    return model.getEventCount()


def msc_interactions(model=None):
    """
    returns an iterable on all interactions in the model
    """
    if model == None:
        model = Main.getModel()
    iter = model.getTransitionIterator()
    while iter.hasNext():
        yield iter.next()

def msc_event_interactions(ev, outgoing=True):
    """
    returns interactions incoming or outgoing from the
    """
    if outgoing:
        inter = ev.getOutgoingInteractions().iterator()
    else:
        inter = ev.getIncomingInteractions().iterator()
    while iter.hasNext():
        yield iter.next()

def msc_interaction_events(inter):
    return (inter.getFromEvent(), inter.getToEvent())
    
                                        	

def flow_add_event(res, mev):
    ints = mev.getIncomingInteractions()
    if ints != None:
        for t in ints:
            if not t in res:
                res.append(t)
                src = t.getFromEvent()              
                if src != None and not dst in res:
                    res.append(src)
    res.append(mev)
    ints = mev.getOutgoingInteractions()
    if ints != None:
        for t in ints:
            if not t in res:
                res.append(t)
                dst = t.getToEvent()
                if dst != None and not dst in res:
                    res.append(dst)
                            
          
class fen:
    def __init__(self, _path="???"):
        self.path = _path

class flow_base:
    traversed = False
    parent = None
    children = ()
    
    def __init__(self, *_arg):
        self.children = _arg
        for el in self.children:
            el.parent = self
        self.setvars({});

    def matched(self):
        return self.traversed and self._matched  
    
    def get_mismatching_fevs(self, arr=[]):
        if not self.traversed:
            return
        for el in self.get_children():
            el.get_mismatching_fevs(arr)
        return tuple(arr)

    def get_fevs_with_model(self, arr=None):
        if arr == None:
            arr = []
        if not self.traversed:
            return None
        if isinstance(self, fev) and self.model != None:
            arr.append(self)
        else:
            for el in self.children:
                el.get_fevs_with_model(arr)
        return tuple(arr)
        
    def get_model(self, arr=None, dbg=False):
        if arr == None:
            arr = []
        if dbg:
            print "--->DBG: traversed=", self.traversed, self.__class__.__name__
        if not self.traversed:
            return ()
        for el in self.children:
            if dbg:
                print "--->DBG: entering subel =", el.__class__.__name__
            el.get_model(arr, dbg=dbg)
        return tuple(arr)
    
    def get_root(self):
        el = self
        while el.parent != None:
            el = el.parent
        return el
    
    def get_children(self):
        return self.children
    
    def setvars(self, dict):
        self.vars = dict
        for el in self.children:
            el.setvars(dict)
            
    def getvars(self):
        return self.vars
    
    def addvars(self, dict):
        self.vars.update(dict);
       
    
    def countfev(self):
        total = 0
        for el in self.children:
            if isinstance(el, fev):
                total += 1
            else:
                total += el.countfev()
        return total
    
    def _indent(self, level, html=False):
        if html:
            return "&nbsp;"*(level*2)
        else:
            return " "*(level*2)
    

    def event_link(self, link, text):
        return '<a href=\"msc_event://'+link+'\">'+text+'</a>'
        
    def _pretty_str_fev_to_str(self, indent, html=False, human_friendly=False, with_model=False):
        if with_model and not self.traversed:
            return ""
        if self.description != None and self.model != None:
            fdesc = self.model.getEntity().getPath()+", "+self.description
        elif self.description != None:
            fdesc = self.description+"("+str(self)+")"
        elif self.model != None:
            fdesc = self.model.getEntity().getPath()+", "+self.str_label()
        else:   
            fdesc = self.str_resolved()
        if html:
            fdesc = Utils.stringToHTML(fdesc);
        if human_friendly and html:
            if self.model != None:
                return fdesc + ': '+self.event_link(str(self.model_idx), '<span style="color:#008000;">success</span>')
            else:
                return fdesc + ': <span style="color:#FF0000;">failure</span>'
        elif human_friendly:
            if self.model != None:
                return fdesc +': success'
            else:
                return fdesc + ': failure'
        elif html:
            if self.model != None:
                return '<span style="color:#008000;">'+Utils.stringToHTML(str(self))+'</span>: '+self.event_link(str(self.model_idx), Utils.stringToHTML(msc_model_2_str(self.model)))
            else:
                return '<span style="color:#FF0000;">'+Utils.stringToHTML(str(self))+'</span>'
        else:
            return str(self)
                                   
                                   
    def pretty_str(self, level=0, prefix="", html=False, with_model=False, human_friendly=False):        
        isfev = isinstance(self, fev)        
        if level == 0:      
            fevs = self.get_fevs_with_model()
            lnk = ""
            if fevs != None:
                for el in fevs:
                    lnk  += str(el.model_idx)+','
            prefix = self.event_link(lnk, prefix)
            
        if html:
            br = '<br>'
        else:
            br = '\n'
        if human_friendly:
            res = ''
        else:
            res = self._indent(level, html=html)
        
        if isfev:
            res += self._pretty_str_fev_to_str(level, html=html, human_friendly=human_friendly, with_model=with_model) 
        else:
            if not human_friendly:
                res += self.__class__.__name__+"("+br
            l = len(self.children)
            for idx in range(0,l-1):
                sub = self.children[idx].pretty_str(level+1, html=html, with_model=with_model, human_friendly=human_friendly)
                if len(sub.strip()) != 0: 
                    res += sub+','+br
            res += self.children[l-1].pretty_str(level+1, html=html, with_model=with_model, human_friendly=human_friendly);
            if not human_friendly:
                res += br+self._indent(level, html=html)+")"
        return prefix+res    
            

    def reset(self):
        self.traversed = False
        self.model = None
        #print "reset for "+self.pretty_str()
        for el in self.children:
            el.reset()

    def match(self, start_event_idx=0, model=None, progress=None):
        if model == None:
            model = Main.getModel()
        if model == None:
            raise Error("Null model")        
        ev_count = model.getEventCount()
        if ev_count == 0:
            raise Error("Empty model")        
        if ev_count<=start_event_idx:
            return -1
        self.reset()
        try:
            res = self.match_internal(model, start_event_idx, ev_count, progress=progress);
        except AbortSearchException:
            raise FlowError("Search aborted")
        return res
           
    def get_min_model_index(self):
        res = sys.maxint
        if self.traversed:
            if isinstance(self, fev) and self.model_idx >= 0:
                res = self.model_idx
            else:
                for el in self.children:
                    tmp = el.get_min_model_index()
                    if tmp < res: res = tmp
        return res
     
        
class fev(flow_base):
    def __init__(self, entity, label, 
                 predicate=None, predicate_arg=None, action=None, 
                 action_arg=None, descr=None, nomatch_action=None, nomatch_action_arg=None):
        self.model = None
        self.description = descr
        self.entity = Template(entity)          
        self.label = Template(label)        
        self.predicate = predicate
        self.predicate_arg = predicate_arg
        self.action = action
        self.action_arg = action_arg
        self.nomatch_action = nomatch_action
        self.nomatch_action_arg = nomatch_action_arg

    def get_model(self, arr=[], dbg=False):
        if dbg:
            print "--->DBG: traversed=", self.traversed, str(self), "model = ", msc_model_2_str(self.model)
        if (not self.traversed) or self.model == None:
            return None
        arr.append(self.model)
        return tuple(arr)
        
    def str_entity(self):
        return str(self.entity.template)

    def str_label(self):
        return(self.label.template)
    
    def __str__(self, repr=True, descr=False):
        res = ""
        if repr:
            res += 'fev('+self.str_entity()+', '+self.str_label()+')'     
        if descr and self.description != None:
            if res != "":
                res += ": "
            res += description
        return res

    def str_resolved(self):
        e = self.entity.substitute(self.vars)
        l = self.label.substitute(self.vars)
        return '('+str(e)+', '+str(l)+')'

    def process_params(self):
        en_re = re.compile("\\A"+self.entity.substitute(self.vars)+"\\Z")
        ev_re = re.compile("\\A"+self.label.substitute(self.vars)+"\\Z")
        return (en_re, ev_re)
        
    def match_params(self, args, mev, mev_idx):
        self.traversed = True
        en_re = args[0]
        ev_re = args[1]
        #msg  ="matching "+self.str_resolved()+" with ("+mev.getEntity().getPath()+", "+mev.getLabel()+")" 
        en_match = en_re.match(mev.getEntity().getPath())
        self.model = None 
        self._matched = False 
        self.model_idx = -1
        if en_match == None:
            #MATCH_LOGGER.debug(msg+"entity match failed")
            return False
        ev_match = ev_re.match(mev.getLabel())
        if ev_match == None:
            #MATCH_LOGGER.debug(msg+"label match failed")
            return False
            
        if self.predicate != None:
            if isinstance(self.predicate, tuple):
                for p in range(len(self.predicate)):
                    if self.predicate[p] != None and not self.predicate[p](self, mev, self.vars, self.predicate_arg[p]):
                        #MATCH_LOGGER.debug(msg+"predicate match failed (pred = "+str(self.predicate[p])+", arg = "+str(self.predicate_arg[p])+")")
                        return False 
            else:
                if not self.predicate(self, mev, self.vars, self.predicate_arg):
                    #MATCH_LOGGER.debug(msg+"predicate match failed (pred = "+str(self.predicate)+", arg = "+str(self.predicate_arg)+")")
                    return False
        gd = en_match.groupdict()
        if len(gd) > 0:
            self.vars.update(gd)
        gd = ev_match.groupdict()
        if len(gd) > 0:
            self.vars.update(gd)
        if self.action != None:
            if isinstance(self.action, tuple):
                for x in range(0, len(self.action)):
                    if self.action[x] != None:
                        self.action[x](self, mev, self.getvars(), self.action_arg[x])
            else:
                self.action(self, mev, self.getvars(), self.action_arg)
        self.model = mev
        self.model_idx = mev_idx
        self._matched = True
        #print "MATCH_PARAM: [",mev_idx,"] "+self.pretty_str()+" matched, "+msc_model_2_str(mev);
        return True                


    

def is_sink(fev, ev, vars, arg):
    return ev.getIncomingInteractions() != None
        
def fconn_to(client_entity,server_entity):
    return fev(client_entity, "<"+server_entity+"_init>", predicate=is_sink, descr="connecting to "+server_entity)
           
def fint(src, dst, label, 
          src_predicate=None, src_predicate_arg=None,
          dst_predicate=None, dst_predicate_arg=None,
          src_action=None, src_action_arg=None,
          dst_action=None, dst_action_arg=None,
          src_description=None, dst_description=None,
          src_flow=None, dst_flow=None):
          
         
    def is_source(fev, ev, vars, dstfev):
        ints = ev.getOutgoingInteractions()
        for inter in ints:            
            dst_ev = inter.getToEvent()
            (en_re, ev_re) = dstfev.process_params()
            if not en_re.match(dst_ev.getEntity().getPath()):
                return False;
            if not ev_re.match(dst_ev.getLabel()):
                return False;
            fev.dstev = dst_ev
            return True
        return False 

    def save_dst_event(fev, mev, vars, dst_event):
        dst_event[0] = fev.dstev
        del fev.dstev
#        to_evs = [inter.getToEvent() for inter in mev.getOutgoingInteractions()]
#        max_idx = max([ev.getIndex() for ev in to_evs])
#        events_and_max_idx[0].extend(to_evs)
#        events_and_max_idx[1][0] = max_idx

    def is_dest(fev, mev, vars, to_event):
        return mev is to_event[0]        
#        if mev.getIndex() > max_ev_idx[0]:
#            print "raising exc. at index ", mev.getIndex(), "max is ", max_ev_idx[0]
#            raise AbortSearchException()
#        res = mev in to_events
#        print "is_dest", mev,": ", res 
#        return res

    to_events  = [0]

    fd = fev(dst, label,
            predicate     = (is_dest,   dst_predicate),
            predicate_arg = (to_events, dst_predicate_arg),
            action        = dst_action, 
            action_arg    = dst_action_arg            
    )

    fs = fev(src, label,
            predicate     = (is_source,   src_predicate),
            predicate_arg = (fd,          src_predicate_arg),
            action        = (save_dst_event, src_action),
            action_arg    = (to_events,   src_action_arg)
    )

    

    if src_flow == None and dst_flow == None:
        return fseq(fs, fd)
    elif src_flow != None and dst_flow != None:
        return fseq(fs, fall(src_flow, fseq(fd, dst_flow)))
    elif src_flow != None:
        return fseq(fs, fall(src_flow, fd))
    elif dst_flow != None:
        return fseq(fs,fd,dst_flow)

def fmsg(src, dst, label, 
          src_predicate=None, src_predicate_arg=None,
          dst_predicate=None, dst_predicate_arg=None,
          src_action=None,    src_action_arg=None,
          dst_action=None,    dst_action_arg=None,
          src_flow=None,      dst_flow=None):
    return fint(src, dst, label, 
                src_predicate=src_predicate, src_predicate_arg=src_predicate_arg,
                dst_predicate=dst_predicate, dst_predicate_arg=dst_predicate_arg,
                src_action=src_action, src_action_arg=src_action_arg, 
                dst_action=dst_action, dst_action_arg=dst_action_arg,
                src_flow=src_flow, dst_flow=dst_flow,
                src_description=str(src)+' sending message "'+str(label)+'" to '+str(dst),
                dst_description=str(dst)+' receiving message "'+str(label)+'" from '+str(src))
        
def ftran(entity, from_state, to_state, predicate=None, predicate_arg=None, action=None, action_arg=None, body=None):
    if body != None:
        return seq(
                   fev(entity, from_state, descr="leaving state "+from_state),
                   body,
                   fev(entity, to_state, descr="entering state "+to_state)
                   )
    else:
        return seq(
                   fev(entity, from_state, descr="leaving state "+from_state),
                   fev(entity, to_state, descr="entering state "+to_state)
                   )
     
            
class fseq(flow_base):
    
    def match_internal(self, model, idx, count, progress=None):
        self.traversed = True
        self._matched = False
        men = None
        mcount = 0
        for el in self.get_children():
            if isinstance(el, fev):
                params = el.process_params()
                maxEvIdx = model.getEventCount()
                #for en in model.getEntityIterator(False):
                    #if params[0].match(en.getPath()) and en.getLastEventIndex() > maxEvIdx:
                    #    maxEvIdx = en.getLastEventIndex()
                if maxEvIdx < 0:
                    raise FlowError("No Entity found matching "+params[0].pattern)
                while idx <= maxEvIdx:
                    mev = model.getEventAt(idx)
                    v = el.match_params(params, mev, idx)
                    idx += 1 
                    if progress:
                        progress.progress(idx)

                    if v: break
                    if idx == count:
                        raise FlowError("element "+el.str_resolved()+" not found in model.")
            else:
                idx = el.match_internal(model, idx, count, progress=progress)
                mcount += 1
        self._matched = True;
        return idx


class fany(flow_base):

    def match_internal(self, model, idx, count, progress=None):
        self.traversed = True
        self._matched = False
        sz = model.getEventCount()
        exc = []
        children = self.get_children()
        for el in children:
            if isinstance(el, fev):
                params = el.process_params()
                while idx < count:
                    mev = model.getEventAt(idx)
                    v = el.match_params(params, mev, idx)
                    idx += 1
                    if progress:
                       progress.progress(idx)
                    if v:               
                        self._matched = True                        
                        return idx
            else:
                try:
                    idx1 = el.match_internal(model, idx, count, progress=progress)
                    self._matched = True
                    #clear other branches taken. We do this here
                    #because if no branch succeeds we want to keep
                    #the traversed and model info for the partial 
                    #successes.
                    for el1 in children:
                        if el1 != el:
                            el1.reset()
                    return idx1
                except FlowError, e:
                    #print "got exception ", e, "for index ", idx
                    exc.append(e)
                    continue
                    
        msg = ""
        raise FlowError("none of the flows for "+str(self)+" was found in model.", causes = exc)


class fall(flow_base):

    def match_internal(self, model, idx, count, progress=None):
        self.traversed = True
        self._matched = False
        sz = model.getEventCount()
        maxidx = -1
        for el in self.get_children():
            if isinstance(el, fev):         
                params = el.process_params()
                tidx = idx;
                while tidx < count:
                    mev = model.getEventAt(tidx)
                    v = el.match_params(params, mev, tidx) 
                    if v:               
                        if tidx>maxidx: maxidx = tidx               
                        break;
                    tidx += 1
                    if progress:
                        progress.progress(tidx)
                if tidx == count:
                    raise FlowError("element "+el.str_resolved()+" not found, in flow ")
            else:
                tmpidx = el.match_internal(model, idx, count, progress=progress)
                if tmpidx > maxidx:
                    maxidx = tmpidx
            self._matched = True 
        return maxidx


class frep(flow_base):
    min=1
    max=sys.maxint
    
    def __init__(self, children, min=1, max=sys.maxint):
        raise Exception("SHOULD NOT USE")
        flow_base.__init__(self, (children))
        self.min = min
        self.max = max
        
    def match_internal(self, model, idx, count, progress=None):
        self.traversed = True
        self._matched = False
        sz = model.getEventCount()
        rep = 0
        el = self.children[0]
        self.exception = None
        while True:
            if isinstance(el, fev):         
                params = el.process_params()
                while idx < count:
                    mev = model.getEventAt(idx)
                    v = el.match_params(params, mev, idx)
                    idx += 1 
                    if progress:
                        progress.progress(idx)
                    if v: break;
                if idx == count:
                    break
            else:
                try:
                    idx = el.match_internal(model, idx, count, progress=progress)
                except FlowError, e:
                    self.exception = e;
                    break           
            rep += 1
            if rep == self.max:
                break;
            
        if rep < self.min: 
            raise FlowError("element expected to be repeated  at least "+str(self.min)+" times, found only "+str(rep)+" times", 
                            causes = (self.exception,))
        self._matched = True
        return idx
    



        
def msc_flow_print(model=None):
    if model == None:
        model = Main.getModel()
    if model == None or model.getEventCount() == 0:
        raise Error("Invalid model")
    
    print "(",
    en = None
    first = True
    prt = ""
    for i in range(0, model.getEventCount()):
        ev = model.getEventAt(i)
        m = ev.getMarker()
        if m != None:
            if first:
                first = False
            else:
                prt += ","
            en1 = ev.getEntity()
            if en1 != en:
                en = en1
                prt += "\n(\"" + en.getPath() + "\",\""+ev.getLabel()+"\")"
            else:
                prt += "\"" + ev.getLabel() + "\""
    print prt + ")"
    
def msc_show(el):
    mf = Main.show(el)

def msc_error(modelel, msg):
    Report.error(modelel, msg)

def msc_event_index(ev, model=None):
    if model == None:
        model = Main.getModel()
    return model.getEventIndex(ev)
    
def msc_grab_remote_entity(fev, ev, fvars, varname):
    if ev.getIncomingInteractions() != None: 
        en = ev.getIncomingInteractions()[0].getToEvent().getEntity().getPath();
    else:    
        en = ev.getOutgoingInteractions()[0].getToEvent().getEntity().getPath();
    dict = {}
    dict[varname] = en
    fvars.update(dict)

def msc_grab_entity(fev, ev, fvars, varname):
    en = ev.getEntity().getPath();
    dict = {}
    dict[varname] = en
    fvars.update(dict)

def msc_grab_entities(fev, ev, fvars, varnames):
    if ev.getIncomingInteractions() != None: 
        dst_en = ev.getEntity().getPath();
        src_en = ev.getIncomingInteractions()[0].getFromEvent().getEntity().getPath();
    else:    
        src_en = ev.getEntity().getPath();
        dst_en = ev.getOutgoingInteractions()[0].getToEvent().getEntity().getPath();
    dict = {}
    dict[varnames[0]] = src_en
    dict[varnames[1]] = dst_en
    fvars.update(dict)
    
    
def msc_flow_show(evs, color=None):
    for el in evs:
        if color != None:
            el.setMarker(color)
        if isinstance(el, Event):
            en = el.getEntity();
            Main.show(en)
    #scroll to first entity/event
    Main.show(evs[0])

def msc_flow_mark(evs, color):
    for el in evs:
        if color != None:
            el.setMarker(color)
            inters = el.getIncomingInteractions()
            if inters != None:
                for inter in inters:
                    inev = inter.getFromEvent()
                    if inev != None and inev in evs:
                        inter.setMarker(color)
                
                    
                
def msc_model_2_str(m):
    res = ""
    if m == None:
        return "None";
    if isinstance(m, tuple) or isinstance(m, list):
        for el in m:
            res+=msc_model_2_str(el)+"\n"        
    else:
        res = "("+m.getEntity().getPath()+", "+m.getLabel()+")"
    return res

def msc_results_add_flow(f, valid=True, human_friendly=False, msg=None):
    if Main.batchMode():
      m = f.get_model()
      if valid:
        print "INFO: valid flow starting at line", m[0].getLineIndex()
      else:
        print "ERROR: invalid flow starting at line", m[0].getLineIndex()
      if msg:
        print msg
    else:
      if valid:
         prefix ='<span style="color:#008000;">VALID FLOW:</span><br>'
      else:
         prefix='<span style="color:#FF0000;">INVALID FLOW:</span><br>'
      str = f.pretty_str(level=0, prefix=prefix, html=True, with_model=True, human_friendly=human_friendly)
      Main.addResult(str)
    
def msc_results_add(msg):
    if Main.batchMode():
        print msg
    else:
        Main.addResult(msg)
    
def msc_batch_load(fname):
    return Main.batchLoad(fname);

#@msc_fun
#def gen_flow(output_file):
#    evs = []
#    roots = []
#    for ev in msc_events():
#      if ev.getMarker() != None:
#        evs.append(ev)
#      if len(ev.getIncomingInteractions()) == 0:
        
