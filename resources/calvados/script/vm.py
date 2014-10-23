from string import Template
import re
from mscviewer import *
	
            
def vm_creation_flow(human_friendly=True):
    # print "*** vm_creation_flow called with arg=", arg, "with_model=", with_model, "human=", human 
    # the following comment will show up as tooltip in GUI. you can use HTML too!
    """
    matches a service VM creation flow for Calvados. 
    
    For each node it checks the VM definition flow correctness. It reports in the Result panel all matches preceded
    by "VALID", and all the mistmatches preceded by "INVALID". 
    The user can click on the "FLOW" link to open the entities involved in the entire flow. 
    
    Keyword arguments:
    
    human_friendly -- if  set to True the output has human-readable descriptions for the flow
    elements and an indication of match/mismatch for each. If set to False the output shows
    the fev specification for the flow elements and the event representation of the matching event, if any
    
    In case of match for a fev the "match" keyword or the event representation can be clicked on in the report
    to open the specific entity and/or select the event    
    
    """
     
    SDR_MGR = "$node/sdr_mgr/[0-9]+"
    SDR_MGR_OP = "$node/sdr_mgr/[0-9]+/sdr_operations:.*"
    VM_MGR = "$node/vm_manager/[0-9]+"
    INSTAGT = "$node/instagt/[0-9]+"
    SHELF_MGR = "$node/shelf_mgr/[0-9]+"

    f = fseq(
        # first of all, we want sdr_mgr to connect to a whole bunch of processes. make sure that happened.
        # conection can happen in any order, so use fall() to check in parallel. subsequent parsing will
        # continue from max(index of fall elements)
        fall(
            fconn_to(SDR_MGR, "instagt"),
            fconn_to(SDR_MGR, "vm_manager"),
            fconn_to(SDR_MGR, "nm"),
            fconn_to(SDR_MGR, "cm"),
            fconn_to(SDR_MGR, "hushd"),
            fconn_to(SDR_MGR, "ccc_driver"),
            fconn_to(SDR_MGR, "sdr_rm"),
        ),
        fint(SDR_MGR, SDR_MGR_OP, "ev_create_sdr"),
        fev(SDR_MGR_OP, "sdr_start"),
        fint(SDR_MGR_OP, INSTAGT, "instagt_prepare_sdr_part\(\)",
                src_flow=fseq(
                    fmsg(SDR_MGR_OP, SHELF_MGR, "nm_allocate_service_vm_ip_mac_address\(\)"),
                    fmsg(SHELF_MGR, SDR_MGR, "nm_service_vm_ip_mac_alloc_resp\(\)"),
                    fmsg(SDR_MGR, SHELF_MGR, "nm_allocate_service_vm_ip_mac_address\(\)"),
                    fmsg(SHELF_MGR, SDR_MGR, "nm_service_vm_ip_mac_alloc_resp\(\)"),
                    fmsg(SDR_MGR, SDR_MGR_OP, "ev_vm_ip_mac_rcvd"),
                    fev(SDR_MGR_OP, "sdr_wait_define_vm"),
                    fev(SDR_MGR_OP, "sdr_vm_ip_mac_rcvd", descr="receiving IP and MAC"),
                    fev(SDR_MGR_OP, "sdr_wait_define_vm")
                ),
                dst_flow=fseq(
                    fmsg(INSTAGT, "(?P<instmgr>.*)", "calv_instmgr_get_sdrdb_entry\(\)"),
                    fmsg("$instmgr", INSTAGT, "calv_instmgr_get_sdrdb_entry_reply\(\)"),
                    fmsg(INSTAGT, SDR_MGR, "instagt_prepare_sdr_part_reply\(\)"),
                    fmsg(SDR_MGR, SDR_MGR_OP, "ev_sdr_part_created"), 				   
                    fev(SDR_MGR_OP, "sdr_wait_define_vm"),
                    fev(SDR_MGR_OP, "sdr_part_created", descr="creating SDR partition"),
                    fmsg(SDR_MGR_OP, VM_MGR, "vm_manager_define_vm\(\)"),
                    fmsg(VM_MGR, SDR_MGR, "vm_manager_vm_defined\(\)"),
                    fmsg(SDR_MGR, SDR_MGR_OP, "ev_sdr_vm_defined"),
                    fev(SDR_MGR_OP, "sdr_vm_define"),
                    fev(SDR_MGR_OP, "ev_sdr_start_vm"),
                    fev(SDR_MGR_OP, "sdr_vm_define"),
                    fev(SDR_MGR_OP, "ev_sdr_start_vm"),
                    fev(SDR_MGR_OP, "sdr_vm_define", descr="starting VM"),
                    fmsg(SDR_MGR_OP, VM_MGR, "vm_manager_start_vm\(\)",
                        src_flow=fev(SDR_MGR_OP, "sdr_vm_start"),
                        dst_flow=fseq(
                            fmsg(VM_MGR, SDR_MGR, "vm_manager_vm_started\(\)"),
                            fev(SDR_MGR, "ev_sdr_vm_started", descr="issuing scheduling"),
                        )
                    ),
                    fev(SDR_MGR_OP, "ev_sdr_vm_started"),
                    fev(SDR_MGR_OP, "sdr_vm_start"),
                    fev(SDR_MGR_OP, "ev_sdr_create_done"),
                    fev(SDR_MGR_OP, "sdr_vm_start"),
                    fev(SDR_MGR_OP, "ev_sdr_create_done"),
                    fev(SDR_MGR_OP, "sdr_vm_start"),
                    fev(SDR_MGR_OP, "sdr_start")
                )
        )
    )

    return f



@msc_fun
def find_vm_flows(human_friendly=True):
    """
    matches a service VM creation flow for Calvados. 
    
    For each node it checks the VM definition flow correctness. It reports in the Result panel all matches preceded
    by "VALID", and all the mistmatches preceded by "INVALID". 
    The user can click on the "FLOW" link to open the entities involved in the entire flow. 
    
    Keyword arguments:
    
    human_friendly -- if  set to True the output has human-readable descriptions for the flow
    elements and an indication of match/mismatch for each. If set to False the output shows
    the fev specification for the flow elements and the event representation of the matching event, if any
    
    In case of match for a fev the "match" keyword or the event representation can be clicked on in the report
    to open the specific entity and/or select the event    
    
    """
    f = vm_creation_flow()
    for n in msc_entities(rootOnly=True):
        vars = {'node':n.getPath()}
        f.setvars(vars)
        idx = 0

        while idx >= 0:
            try:
                idx = f.match(start_event_idx=idx)
                m = f.get_model()
                msc_flow_mark(m, msc_color.GREEN)
                msc_results_add_flow(f, human_friendly=human_friendly, valid=True)
            except FlowError, err:
                m = f.get_model()
                if len(m) == 0:
                    # nothing matched, we are done
                    break
                msc_results_add_flow(f, valid=False, human_friendly=human_friendly)
                # we had partial match, but idx was never returned.
                # need to try next match with first idx == smaller
                # index that matched + 1
                idx = f.get_min_model_index() + 1
        
        msc_results_add('<hr>')
    
    
   
if __name__ == "__main__":
    find_vm_flows()

      
