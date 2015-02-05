package com.cisco.mscviewer.model;

public class JSonStringValue implements JSonValue {
    private String value;
    
    public JSonStringValue(String s) {
        value = s;
    }
    
    public String toString() {
        return value;
    }

}
