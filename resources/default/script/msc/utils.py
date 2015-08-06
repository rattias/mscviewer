from string import Template
import re
from com.cisco.mscviewer import Main
from com.cisco.mscviewer.util import Utils

def is_batch_mode():
    """$descr{Returns True if the tool was started in batch mode, False otherwise}"""
    return Main.batchMode()

def msc_print(msg):
    """$descr{produces output. In GUI mode the output
    is reported in the Result View. In batch mode the 
    result is printed on the console}
    """
    if Main.batchMode():
        print msg
    else:
        Main.addResult(msg)
        
def string_to_html(str):
    return Utils.stringToHTML(str)
	