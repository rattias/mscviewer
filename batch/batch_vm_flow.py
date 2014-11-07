from mscviewer import *
from vm import *


#fullm = msc_batch_load("/ws/rattias-sjc/logs/vm_flow/RP0-RP1.log")
f = vm_creation_flow()

for n in msc_entities(rootOnly=True):
    vars = {'node':n.getPath()}
    f.setvars(vars)
    idx = 0

    while idx >= 0:
        try:
            idx = f.match(start_event_idx=idx)
            m = f.get_model()
            print "found succesful match on node ", n.getPath()
        except FlowError, err:
            m = f.get_model()
            if len(m) == 0:
                # nothing matched, we are done
                break
            print f.pretty_str(0, "FLOW=", with_model=True)
        

