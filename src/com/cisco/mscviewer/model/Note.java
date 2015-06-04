package com.cisco.mscviewer.model;

import java.awt.Point;


public class Note {
    private String text;
    private Point position;
    private boolean visible;

    public Note(String text) {
        this.text = text;
        position = new Point(-1, -1);
    }

    public String getText() {
        return text;
    }

    public void setPosition(Point p) {
        position.setLocation(p);
    }

    public Point getPosition() {
        return (Point)position.clone();
    }

    public void setVisible(boolean v) {
        visible = v;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setText(String n) {
        text = n;   
    }

}
