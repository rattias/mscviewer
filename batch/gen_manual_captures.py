from msc.model import *
from msc.gui import *
import time

#Main.maximize();

### SIMPLE-FLOW.PNG ###
load("examples/simple-flow.msc");
open("User 1")
open("Backend")
open("Database")
capture_diagram("doc/manual/images/simple-flow.png");

#### CONCURRENT-FLOW.PNG ### 
load("examples/concurrent-flow.msc");
open("User 1")
open("Image Repo")
open("VM Manager")
open("Resource Manager")
capture_diagram("doc/manual/images/concurrent-flow.png");

### GUI-EX1.PNG ###
load("examples/lst1.msc");
capture_gui("MainFrame", "doc/manual/images/gui-ex1.png");


### GUI-EX2-OPEN.PNG ###
open("producer")
open("consumer")
capture_gui("MainFrame", "doc/manual/images/gui-ex2-open.png");

### GUI-EX3-CYCLE.PNG ###
set_property("model.sort-topologically", False)
load("examples/lst3.msc");
open("producer")
open("consumer")
capture_gui("MainFrame", "doc/manual/images/gui-ex3-cycle.png");
set_property("model.sort-topologically", True)

### GUI-EX4-TIME.PNG ###
load("examples/lst4.msc");
open("producer")
open("consumer")
capture_gui("MainFrame", "doc/manual/images/gui-ex4-time.png");

### GUI-EX5-DATA.PNG ###
load("examples/lst5.msc");
open("producer")
en = open("consumer")
for ev in events():
    if event_entity(ev) == en:
        event_select(ev);
        break;
    set_right_split_pane_divider_location(0.5);
data_show();
capture_gui("MainFrame", "doc/manual/images/gui-ex5-data.png");

### GEN GUI ENTITY TREE ###
load("examples/os.msc");
expand_entity_tree();
set_left_right_split_pane_divider_location(.35);
capture_gui("LeftPane", "doc/manual/images/left-pane.png");

#give time for last snapshot to complete
time.sleep(1)
Main.quit()