"""This module contains the Pthon API to access MSCViewer models (entities, events, interactions). 

  Even though Python scripts may have access to the full Java API directly, 
  this module defines a stable interface that will guarantee
  compatibility in the future. Internal Java APIs may, and most likely will, 
  be subject to change, so the user is strongly advised against its direct usage.
"""

from string import Template
from com.cisco.mscviewer import Main
from com.cisco.mscviewer.model import MSCDataModel
from com.cisco.mscviewer.util import Report, ProgressReport, Utils
from com.cisco.mscviewer.model import Entity, Event, Interaction
from com.cisco.mscviewer.gui import Marker
from com.cisco.mscviewer.gui import MainFrame
from com.cisco.mscviewer.gui.graph import HeatGraphWindow
from types import TupleType 
from com.cisco.mscviewer.graph import Graph, GraphSeries


import sys 
import re
#import logging WATCH OUT! this import changes name of thread to MainThread for some reason
import inspect
import time

msc_debug = {} 
DBG_MATCH = " db"

############## UTILITY CLASSES #######################

class marker(object):
    GREEN = Marker.GREEN
    BLUE = Marker.BLUE
    RED = Marker.RED
    YELLOW = Marker.YELLOW

################ MODEL BROWSING FUNCTIONS ###############

#------ ENTITY-SPECIFIC FUNCTIONS --------------
def entities(root_only=False):
    """returns an iterable on all entities in the model.
    
    This function can be used to iterate on all entities in the model.
    
    :param root_only: if True, only top-level entities are iterated upon
    :type root_only: boolean
    
    **Example:** ::
    
        for en in entities():
            print entity_name(en)    
    """
    
    model = MSCDataModel.getInstance();
    it = model.getEntityIterator(root_only)
    while it.hasNext():
        yield it.next()

def entity_id(entity):
    """returns the unique ID of this entity"""
    return entity.getId()

    
def entity_count(root_only=False):
    """returns a count of the entities in the model

    :param root_only: if True, only top-level entities are counted
    :type root_only: boolean
    
    **Example:** ::
    
       printf "total entities: ",entity_count(), ", top-level: ", entity_count(True)    
    """
    model = MSCDataModel.getInstance();
    if root_only:
        return model.getRootEntityCount()
    else:
        return model.getEntityCount()

def entity_path(en):
    """returns the pathname of the entity.
    :param en: an entity
    """
    return en.getPath()
    

def entity_first_event_index(en):
    return en.getFirstEventIndex()

def entity_last_event_index(en):
    return en.getLastEventIndex()


#------ EVENT-SPECIFIC FUNCTIONS --------------

def event_entity(ev):
    """returns the entity this event occurred on.
    :param ev: an event
    :type ev: event (opaque type)
    
    **Example:** ::

        for ev in events():
            print entity_path(event_entity(ev))
    """
    return ev.getEntity()

def events():
    """returns an iterable on all events in the model.
    
    **Example:** ::

        for ev in events():
            print event_label(ev)
    """
    model = MSCDataModel.getInstance();
    cnt = model.getEventCount()
    for i in range(cnt):
        yield model.getEventAt(i)


def event_count():
    """returns the count of all events in the model.

    **Example:** ::

        for i in range(event_count()):
            print event_label(event_at(i))
    """
    model = MSCDataModel.getInstance();
    return model.getEventCount()

def event_at(idx=0):
    """returns the idx-th event in the model.

    :param idx: index of the event, should be between 0 (included) and event_count() (excluded).

    **Example:** ::

        for i in range(event_count()):
            print event_label(event_at(i))
    """
    model = MSCDataModel.getInstance();
    return model.getEventAt(idx)
        	
def event_timestamp(ev):
    """returns the timestamp for the event.

    The timestamp is in the form of an integer number of nanoseconds.
    
    :param ev: an event
    :type ev: Event

    **Example:** ::

        for ev in events:
            print event_timestamp(ev)            
    """
    return ev.getTimestamp()

def event_label(ev):
    """returns the label for the event.

    :param ev: an event
    :type ev: Event 

    **Example:** ::

        for ev in events:
            print event_timestamp(ev)            
    """
    return ev.getLabel()

def event_interactions(ev, outgoing=True):
    """returns interactions incoming or outgoing from the event, depending on 
    the value of the outgoing parameter.

    :param ev: an event
    :type ev: event (opaque type)
    :param outgoing: if True, outgoing interactions are returned, if False incoming interactions are reported (default=True).
    :type outgoing: boolean

    **Example:** ::

        print "event has ", len(event_interactions(ev, False), "incoming and", len(event_interactions(ev), "outgoing transitions"
    """
    if outgoing:
        inter = ev.getOutgoingInteractions()
    else:
        inter = ev.getIncomingInteractions()
    return inter
    
    
def event_type(ev):
    """returns the type of the event.
    
    :param ev: an event
    :type ev: Event 
    """
    return ev.getType()
    
def event_index(ev):
    """returns the index of the event in the model.

    :param ev: an event
    :type ev: Event 
    """
    model = MSCDataModel.getInstance();
    return model.getEventIndex(ev)
    
def event_marker(ev):
    """
    returns the current marker associated to the event,
    or None
    
    :param ev: an event
    :type ev: Event  
    """
    return ev.getMarker() 

def event_predecessor(ev, same_entity=False):
    """returns the event preceding this event.
    
    If same_entity is False returns the event 
    preceding this event in the model, otherwise
    returns the event preceding this event within
    the same entity.

    :param ev: an event
    :type ev: Event 
    """
    if same_entity:
        return ev.getPreviousEventForEntity()
    else:
        return ev.getPreviousEvent()   
       
def event_successor(ev, same_entity=False):
    """returns the event following this event.
    
    If same_entity is False returns the event 
    following this event in the model, otherwise
    returns the event following this event within
    the same entity.

    :param ev: an event
    :type ev: Event 
    """
    if same_entity:
        return ev.getNextEventForEntity()
    else:
        return ev.getNextEvent()

def event_is_block_begin(ev):
    """returns True if this event begins a block,
    False otherwise.
    """
    return ev.isBlockBegin()
    
    
#------ INTERACTION-SPECIFIC FUNCTIONS --------------

def interactions():
    """returns an iterable on all interactions in the model
    """
    model = MSCDataModel.getInstance();
    iter = model.getTransitionIterator()
    while iter.hasNext():
        yield iter.next()

def interaction_events(inter):
    """returns a tuple containing the from- and to- 
    event for the interaction.
    """
    return (inter.getFromEvent(), inter.getToEvent())
    
def interaction_from_event(inter):
    """returns the from event for the interaction.
    """
    return inter.getFromEvent()
    
def interaction_to_event(inter):
    """returns the to event for the interaction.
    """
    return inter.getToEvent()
       
def interaction_type(inter):
    """returns the type of the interaction.
    """
    return inter.getType()
             
