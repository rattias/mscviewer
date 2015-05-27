package com.cisco.mscviewer.model;

public enum JSonBooleanValue implements JSonValue {
    FALSE(false), TRUE(true);

    private boolean v;

    private JSonBooleanValue(boolean v) {
        this.v = v;
    }

    public boolean value() {
        return v;
    }

}
