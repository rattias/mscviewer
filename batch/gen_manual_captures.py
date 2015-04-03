from mscviewer import *
import time

#Main.maximize();

### SIMPLE-FLOW.PNG ###
Main.batchLoad("examples/simple-flow.msc");
Main.open("User 1")
Main.open("Backend")
Main.open("Database")
Main.captureDiagram("doc/manual/images/simple-flow.png");

#### CONCURRENT-FLOW.PNG ### 
Main.batchLoad("examples/concurrent-flow.msc");
Main.open("User 1")
Main.open("Image Repo")
Main.open("VM Manager")
Main.open("Resource Manager")
Main.captureDiagram("doc/manual/images/concurrent-flow.png");

### GUI-EX1.PNG ###
Main.batchLoad("examples/lst1.msc");
Main.captureGUI("MainFrame", "doc/manual/images/gui-ex1.png");

### GUI-EX2-OPEN.PNG ###
Main.open("producer")
Main.open("consumer")
Main.captureGUI("MainFrame", "doc/manual/images/gui-ex2-open.png");

### GUI-EX3-CYCLE.PNG ###
Main.batchLoad("examples/lst3.msc");
Main.open("producer")
Main.open("consumer")
Main.captureGUI("MainFrame", "doc/manual/images/gui-ex3-cycle.png");

### GUI-EX4-TIME.PNG ###
Main.batchLoad("examples/lst4.msc");
Main.open("producer")
Main.open("consumer")
Main.captureGUI("MainFrame", "doc/manual/images/gui-ex4-time.png");

### GUI-EX5-DATA.PNG ###
Main.batchLoad("examples/lst5.msc");
Main.open("producer")
en = Main.open("consumer")
m = Main.getModel();
for i in range(m.getEventCount()):
    ev = m.getEventAt(i)
    if ev.getEntity() == en:
        Main.select(ev);
        break;
    Main.setRightSplitPaneDividerLocation(0.5);
Main.showDataTab();
Main.captureGUI("MainFrame", "doc/manual/images/gui-ex5-data.png");

### GEN GUI ENTITY TREE ###
Main.batchLoad("examples/os.msc");
Main.expandEntityTree();
Main.setLeftRightSplitPaneDividerLocation(.35);
Main.captureGUI("LeftPane", "doc/manual/images/left-pane.png");

#give time for last snapshot to complete
time.sleep(1)
Main.quit()