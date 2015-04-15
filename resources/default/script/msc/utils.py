from string import Template
import re
from com.cisco.mscviewer import Main
from com.cisco.mscviewer.util import Utils

def is_batch_mode():
    """Returns True if the tool was started in batch mode, False otherwise"""
    return Main.batchMode()

def results_report(msg):
    """reports a result. 
    
    In GUI mode the result
    is reported in the Result View. In batvch mode the 
    result is printed on the console
    """
    if Main.batchMode():
        print msg
    else:
        Main.addResult(msg)
        
def string_to_html(str):
    return Utils.stringToHTML(str)
	