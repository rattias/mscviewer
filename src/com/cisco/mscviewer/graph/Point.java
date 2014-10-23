package com.cisco.mscviewer.graph;
public class Point implements Comparable<Point> {
    public long x;
    public float y;
    public Object o;
    
    public Point(long x, float y, Object o) {
        this.x = x;
        this.y = y;
        this.o = o;
    }
    
    public Object getObject() {
        return o;
    }
    
    public String toString() {
        return "("+x+", "+y+")";
    }
    
    @Override
    public int compareTo(Point o) {   
        if (x == o.x)
            return 0;        
        return x < o.x ? -1 : 1;
    }
}
