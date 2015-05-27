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
    """$descr{Functions that should be visible in the GUI under the $e{Script} tree should be
    decorated with msc_fun.}
    $header{Example}
    $code{@msc_fun
    def myfunct():
        pass
    }
    """
    f.is_msc_fun = True
    return f

                         
def load(file_path):
    """$descr{loads a new model file in mscviewer.}
    $header{Parameters}
    $param{file_path}{String}{path of the model file to be loaded}
    """
    return Main.load(file_path);

def open(entity_id):
    """$descr{opens in the sequence diagram the entity by the given name, returning the Entity object}
    $header{Parameters}
    $param{entity_id}{String}{The fully-qualified id of the entity to be opened}
    """
    return Main.open(entity_id)

def capture_diagram(file_path):
    """$descr{captures a PNG of the current content of the sequence diagram in the specified file}
    $header{Parameters}
    $param{file_path}{String}{path of the fiel to save the PNG image to}
    """
    Main.captureDiagram(file_path)    
    
def capture_gui(gui_element_name, file_path):
    """$descr{captures a screenshot of the specified GUI element in the specified file. This function
    is intended mostly for internal use to generate screen captures for the documentation}
    $header{Parameters}
    $param{gui_element_name}{String}{name of the GUI element}
    $param{file_path}{String}{path of the fiel to save the PNG image to}
    """
    Main.captureGUI(gui_element_name, file_path)
    
def progress_start(msg, min, max):
    """$descr{starts a reporting of progress. In GUI mode this will cause
    a progress window to show up. min and max are the range the
    progress is expected to cover. Returns a handle to be used with progress_report()}
    $header{Parameters}
    $param{msg}{String}{message to be shown int the progress window}
    $param{min}{int}{initial value for progress}
    $param{max}{int}{final value for progress}    
    """
    return ProgressReport(msg, "", min, max);
 
def progress_report(pr, cnt):
    """$descr{Reports a progress.} 
    $header{Parameters}
    $param{pr}{handle}{a handle returned by progress_start()}    
    $param{cnt}{int}{a value between $em{min} and $em{max}, the values that were passed to progress_start()}    
    """
    pr.progress(cnt)

def progress_done(pr):
    """$descr{closes the window reporting progress}
    """
    pr.progressDone()

def event_select(ev):
    """$descr{Selects the specified event in the GUI}
    $header{Parameters}
    $param{ev}{Event}{the event to be selected}    
    """
    Main.select(ev)    
        
def event_selected():
    """$descr{returns the event currently selected in the GUI, or None.}
    """
    return MainFrame.getInstance().getMainPanel().getMSCRenderer().getSelectedEvent()

def interaction_selected():
    """$descr{returns the interaction currently selected in the GUI, or None.}
    """
    return MainFrame.getInstance().getMainPanel().getMSCRenderer().getSelectedInteraction()


def show(el):
    Main.show(el)

    
def error(model_el, msg):
    """$descr{reports an error. In GUI mode the result
    is reported in the a popup window. In batch mode the 
    error is printed on the console}
    """
    if Main.batchMode():
        print msg
    else:
        Main.addResult(msg)
    Report.error(modelEl, msg)
    

    
def graph_show(graph, type):
    """$descr{(EXPERIMENTAL) Shows a graph in the GUI}
    """ 
    if type == graph_type.HEAT:
        HeatGraphWindow(graph);
    
  
def set_right_split_pane_divider_location(percent):
    """$descr{re-arranges the split between the sequence diagram area and the result/data area}
    $header{Parameters}
    $param{percent}{int}{the percentage (vertically) of the window that should be occupied by the sequence diagram}
    """    
    Main.setRightSplitPaneDividerLocation(percent)

def set_left_right_split_pane_divider_location(percent):
    """$descr{re-arranges the split between the entity tree area and the diagram/result area}
    $header{Parameters}
    $param{percent}{int}{the percentage (horizontally) of the window that should be occupied by the Entity tree}    
    """
    Main.setLeftRightSplitPaneDividerLocation(percent);

def data_show():
    """$descr{makes the data tab is visible}"""
    Main.showDataTab()

def results_show():
    """$descr{makes the results tab is visible}"""
    Main.showResultsTab()
    
def expand_entity_tree():
    """$descr{fully expands the entity tree so all leafs are visible}"""
    Main.expandEntityTree()
