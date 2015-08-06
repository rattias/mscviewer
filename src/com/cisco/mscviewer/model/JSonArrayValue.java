package com.cisco.mscviewer.model;

import java.util.ArrayList;
import java.util.List;

public class JSonArrayValue implements JSonValue {
    private final List<JSonValue> value;

    @SuppressWarnings("unchecked")
    public JSonArrayValue(ArrayList<JSonValue> al) {
        value = (List<JSonValue>) al.clone();
    }

    public JSonArrayValue() {
        value = new ArrayList<JSonValue>();
    }

    public List<JSonValue> value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public int size() {
        return value.size();
    }

}
