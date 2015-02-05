package com.cisco.mscviewer.model;

public class JSonNumberValue implements JSonValue {
    private Number value;
    
    public JSonNumberValue(Number n) {
        value = n;
    }
    
    public int intValue() {
        return value.intValue();
    }
    
    public long longValue() {
        return value.longValue();
    }

    public float floatValue() {
        return value.floatValue();
    }

    public double doubleValue() {
        return value.doubleValue();
    }

    public String toString() {
        return value.toString();
    }

}
