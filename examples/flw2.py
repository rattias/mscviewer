from msc.flowdef import *
from msc.model import *
from msc.gui import msc_fun
import sys
	            
def get_flow():
    return fseq(
        fev("foo", "event_a"),
        fev("foo", "event_b")
        )

@msc_fun
def find_flow():
    f = get_flow()
    idx = 0
    found = False
    while idx >= 0:
        try:
            idx = f.match(start_event_idx=idx)
            if idx >= 0:
                print "found match!"
                found = True
                m = f.get_model_map()
                for fl in m:
                    ev = m[fl]
                    print str(fl), ':', event_line(ev)
        except FlowError, err:
            if not found:
                print "No match found (", str(err), ")"
            break
    return 
            
       
if __name__ == "__main__":
  find_flow()        
