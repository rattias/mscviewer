echo "VM CREATION FLOW VERIFICATION"
bin/mscviewer --batch "test/calvados/vm.py,find_vm_flows()" /ws/rattias-sjc/logs/vm_flow/RP0-RP1.log
echo "PM ROLE ASSIGNMENT FLOW VERIFICATION"
bin/mscviewer --batch "test/calvados/pm_flow_tester.py,role_assignment_flow_verifier()" /ws/rattias-sjc/logs/vm_flow/RP0-RP1.log

