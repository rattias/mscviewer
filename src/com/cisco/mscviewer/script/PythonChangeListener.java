package com.cisco.mscviewer.script;

public interface PythonChangeListener {
    public void moduleAdded(String module);
    public void moduleRemoved(String module);
    public void moduleChanged(String module);    
}
