from mscviewer import *
import time

Main.maximize();
Main.show("RP0/1633/1633/pm_service_starter:0")
Main.show("RP0/1633/1633/pm_role_negotiation:25")
Main.show("RP0/1633/1633/pm_role_assignment:50")
time.sleep(3)

Utils.getPNGSnapshot("MainPanelJSP", "/tmp/MainPanel.png");

