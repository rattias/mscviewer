import re

from mscviewer import *

def is_fsa_birth(fev, ev, vars, arg):
    """
    returns True if the event is of type "Birth", False otherwise
    """
    return ev.getType() == "Birth"

def act_capture_parent_name(fev, ev, fvars, parent_name):
    """
    this action should be called on the event of a birth of an FSA.
    It captures the name of the parent entity
    """
    src_en = ev.getIncomingInteractions()[0].getFromEvent().getEntity().getPath();
    fvars[parent_name] = src_en
    
def role_assignment_flow():
    assignment="$node/pm/[0-9]+/pm_role_assignment:.*"
    f = fseq(
        fev(assignment, "(?P<assignment>.*), 
            predicate=is_fsa_birth, 
            action=act_capture_parent_name, 
            action_arg="parent"),
        fany(
            fint("$parent","$assignment","pm_go_active_ev",
                dst_flow=fseq(
                    fev("$assignment","start"),
                    fint("$assignment", "(?P<client>.*)", "pm_lib_service_control\(\)"),
                    fint("$client", "(?P<pm>.*)", "pm_lib_service_accept_role\(\)"),
                    fint("$pm", "$assignment", "pm_go_active_ok_ev"),
                    fev("$assignment","wait_active"),
                    fint("$assignment", "$parent", "pm_role_assigned_ev"),
                    fev("$parent","wait_role")
                )
            ),
            fint("$parent","$assignment","pm_go_standby_ev",
                dst_flow= fseq(
                    fev("$assignment","start"),
                    fint("$assignment", "(?P<client>.*)", "pm_lib_service_control\(\)"),
                    fint("$client", "(?P<pm>.*)", "pm_lib_service_accept_role\(\)",
                        src_flow=
                            fint("$client", "(?P<pm>.*)", "pm_lib_service_ha_ready\(\)"),
                        dst_flow=fseq(
                            fint("$pm", "$assignment", "pm_go_standby_ok_ev"),
                            fev("$assignment","wait_standby"),
                            fint("$assignment", "$parent", "pm_role_assigned_ev"),
                            fev("$parent","wait_role")
                        )
                    )
                )
            )
        )
    )        
            
    return f

@msc_fun
def role_assignment_flow_verifier(human_friendly=True):
    """
    Function to verify PM role assignment flow
    """
    f = role_assignment_flow()
    
    for n in msc_entities(rootOnly=True):
        vars = {'node':n.getPath()}
        f.setvars(vars)
        idx = 0
        
        while idx >=0:
            try:
                idx = f.match(start_event_idx=idx)
                m = f.get_model()
                msc_flow_mark(m,msc_color.GREEN)
                msc_results_add_flow(f, valid=True, human_friendly=True)
            except FlowError, err:
                m = f.get_model()
                if len(m) == 0:
                    #nothing matched, we are done
                    break
                msc_flow_mark(m,msc_color.RED)
                msc_results_add_flow(f, valid=False, msg=str(err), human_friendly=True)

                # we had partial match, but idx was never returned.
                # need to try next match with first idx == smaller
                # index that matched + 1
                idx = f.get_min_model_index()+1
                 
        msc_results_add('<hr>')
            
            
            
            
