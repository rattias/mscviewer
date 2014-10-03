from mscviewer import *
import calvados
from calvados.vm import *

fullm = msc_batch_load("/ws/rattias-sjc/logs/vm_flow/RP0-RP1.log")
f = vm_creation_flow()

for n in msc_top_level_entities(model=fullm):
    vars = {'node':n.getPath()}
    f.setvars(vars)
    idx = 0

    while idx >= 0:
        try:
            idx = f.match(start_event_idx=idx, model=fullm)
            m = f.get_model()
            print f.pretty_str(0, "FLOW=", with_model=True)
        except FlowError, err:
            m = f.get_model()
            if len(m) == 0:
                # nothing matched, we are done
                break
            print f.pretty_str(0, "FLOW=", with_model=True)
            # we had partial match, but idx was never returned.
            # need to try next match with first idx == smaller
            # index that matched + 1
            idx = f.get_min_model_index() + 1
        

