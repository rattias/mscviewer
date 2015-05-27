package com.cisco.mscviewer.model;

public class JSonNumberValue implements JSonValue {
    private final Number value;

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

    @Override
    public String toString() {
        return value.toString();
    }

}
