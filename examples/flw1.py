from mscviewer import *
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
    while idx >= 0:
        try:
            idx = f.match(start_event_idx=idx)
            print "FOUND MATCH!"
        except FlowError, err:
            print "NO MATCH!"
            break
            
       
if __name__ == "__main__":
  find_flow()        
