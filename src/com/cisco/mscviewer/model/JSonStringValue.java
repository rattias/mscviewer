package com.cisco.mscviewer.model;

public class JSonStringValue implements JSonValue {
    private final String value;

    public JSonStringValue(String s) {
        value = s;
    }

    @Override
    public String toString() {
        return value;
    }

}
