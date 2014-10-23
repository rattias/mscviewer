package com.cisco.mscviewer.model;

import com.cisco.mscviewer.tree.Interval;

public class SimpleInterval implements Interval {
    private int begin, end;
    
    public SimpleInterval(int b, int e) {
        begin = b;
        end = e;
    }
    
    public void setStart(int v) {
        begin = v;
    }
    
    public void setEnd(int v) {
        end = v;
    }
    
    @Override
    public int getValue() {
        return end;
    }

    @Override
    public int getStart() {
        return begin;
    }

    @Override
    public int getEnd() {
        return end;
    }

}
