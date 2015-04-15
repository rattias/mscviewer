"""This module contains the Pthon API to interact with MSCViewer GUI. 

  Even though Python scripts may have access to the full Java API directly, 
  this module defines a stable interface that will guarantee
  compatibility in the future. Internal Java APIs may, and most likely will, 
  be subject to change, so the user is strongly advised against its direct usage.
"""

from string import Template
from com.cisco.mscviewer import Main
from com.cisco.mscviewer.util import Report, ProgressReport
from com.cisco.mscviewer.gui import MainFrame
from com.cisco.mscviewer.gui.graph import HeatGraphWindow
from msc.graph import graph_type

import sys 
import re
#import logging WATCH OUT! this import changes name of thread to MainThread for some reason
import inspect
import time
            
def msc_fun(f):
    f.is_msc_fun = True
    return f

                         
def load(fname):
    """loads a new file in mscviewer.
    """
    return Main.load(fname);

def open(entity_id):
    """opens in the sequence diagram the entity by the given name, returning the Entity object"""
    return Main.open(entity_id)

def capture_diagram(file_path):
    """captures a PNG of the current content of the sequence diagram in the specified file"""
    Main.captureDiagram(file_path)    
    
def capture_gui(gui_element_name, file_path):
    """captures a screenshot of the specified GUI element in the specified file"""
    Main.captureGUI(gui_element_name, file_path)
    
def progress_start(msg, min, max):
    """starts a reporting of progress. 
    
    In GUI mode this will cause
    a progress window to show up. min and max are the range the
    progress is expected to cover. 
    """
    return ProgressReport(msg, "", min, max);
 
def progress_report(pr, cnt):
    """Reports a progress. 
    cnt should be in the range min,max, where min,max
    are the parameters passed to progress_start()
    """
    pr.progress(cnt)

def progress_done(pr):
    pr.progressDone()

def event_select(ev):
    """Selects the specified event in the GUI"""
    Main.select(ev)    
        
def event_selected():
    """returns the event currently selected in the GUI, or None.
    """
    return MainFrame.getInstance().getMainPanel().getMSCRenderer().getSelectedEvent()

def interaction_selected():
    """returns the interaction currently selected in the GUI, or None.
    """
    return MainFrame.getInstance().getMainPanel().getMSCRenderer().getSelectedInteraction()


def show(el):
    Main.show(el)

    
def error(model_el, msg):
    """reports an error.
    
    In GUI mode the result
    is reported in the a popup window. In batch mode the 
    error is printed on the console
    """
    if Main.batchMode():
        print msg
    else:
        Main.addResult(msg)
    Report.error(modelEl, msg)
    

    
def graph_show(graph, type):
    if type == graph_type.HEAT:
        HeatGraphWindow(graph);
    
  
def set_right_split_pane_divider_location(percent):
    """re-arranges the split between the sequence diagram area and the result/data area"""
    Main.setRightSplitPaneDividerLocation(percent)

def set_left_right_split_pane_divider_location(percent):
    """re-arranges the split between the entity tree area and the diagram/result area"""
    Main.setLeftRightSplitPaneDividerLocation(percent);

def data_show():
    """makes the data tab is visible"""
    Main.showDataTab()

def results_show():
    """makes the results tab is visible"""
    Main.showResultsTab()
    
def expand_entity_tree():
    """fully expands the entity tree so all leafs are visible"""
    Main.expandEntityTree()
