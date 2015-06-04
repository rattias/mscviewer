package com.cisco.mscviewer.graph;

public class Point implements Comparable<Point> {
    private long x;
    private double y;
    private Object o;

    public Point(long x, double y, Object o) {
        this.x = x;
        this.y = y;
        this.o = o;
    }

    public Object getObject() {
        return o;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public int compareTo(Point o) {
        if (x == o.x)
            return 0;
        return x < o.x ? -1 : 1;
    }

    @Override
    public int hashCode() {
        return (int)x;
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        Point p = (Point)other;
        return p.x == x && Double.doubleToLongBits(p.y) == Double.doubleToLongBits(y);
    }

    public long x() {
        return x;
    }
    
    public double y() {
        return y;
    }
    
    public Object object() {
        return o;
    }

    public void setX(long x1) {
        x = x1;
    }
    
    public void setY(double y1) {
        y = y1;
    }
    
    public void setObject(Object o1) {
        o = o1;
    }
}
