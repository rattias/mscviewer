/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Aug 2014
 */
package com.cisco.mscviewer.tree;

import java.util.ArrayList;

/**
 *
 * @author rattias
 */
public class IntervalTree extends AVLTree {
    public static boolean dbg = false;
    
    public IntervalTree(String name) {
        super(name);
    }
    
    public ArrayList<Interval> getIntervalsWithEndBound(int c) {
        ArrayList<Interval> els = new ArrayList<Interval>();
        getIntervalsWithEndBound(c, els);
        return els;
    }

    public ArrayList<Interval> getContainingIntervals(int c) {
        ArrayList<Interval> els = new ArrayList<Interval>();
        if (root != null)
            ((IntervalTreeNode)root).getContainingIntervals(c, els);
        return els;
    }
    
    public void getContainingIntervals(int c, ArrayList<Interval> ret) {
        if (root != null)
            ((IntervalTreeNode)root).getContainingIntervals(c, ret);        
    }

    public ArrayList<Interval> getIntersectingIntervals(int start, int end) {
        ArrayList<Interval> els = new ArrayList<Interval>();
        if (root != null)
            ((IntervalTreeNode)root).getIntersectingIntervals(start, end, els);
        return els;
        
    }

    public void getIntersectingIntervals(int start, int end, ArrayList<Interval> ret) {
        if (root != null)
            ((IntervalTreeNode)root).getIntersectingIntervals(start, end, ret);
//        System.out.println("PATH TO 116:"+pathToStart(116));
//        verify("???");
    }
    
    public void getIntervalsWithEndBound(int c, ArrayList<Interval> ret) {
        Interval o = (Interval)find(c);
        if (o != null) {
            ret.add(o);
        }
    }
    
    public void getIntervalsWithStartBound(int  c, ArrayList<Interval> ret) {
        if (root != null)
            ((IntervalTreeNode)root).getIntervalsWithStartBound(c, ret);
    }
    
    public void verifyIntegrity() throws TreeIntegrityException {
        if (root != null)
            ((IntervalTreeNode)root).verifyIntegrity("");
    }
    
    public void fixMinStart() {
        if (root != null)
            ((IntervalTreeNode)root).fixMinStartRecursive();
    }

    
    public String pathToStart(int v) {
        if (root == null)
            return null;
        ArrayList<String> al = new ArrayList<String>();
        ((IntervalTreeNode)root).pathToStart(v, al);
        StringBuilder sb = new StringBuilder();
        for(int i = al.size()-1; i>=0; i--)
            sb.append(al.get(i));
        return sb.toString();
    }
    
    @Override
    protected AVLTreeNode newNode(Value d) {
        return new IntervalTreeNode((Interval)d);
    }
    
}
