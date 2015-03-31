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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author rattias
 */
public class IntervalTreeNode extends AVLTreeNode {
    private int  minStart;
    
    public IntervalTreeNode(Interval el) {
        super(el);
        minStart = el.getStart();
        
    }
    
    
    @Override
    public void add(AVLTreeNode tn) { 
        super.add(tn);
        fixMinStart();
    }
    

    private void fixMinStart() {
        Interval in = (Interval)data;
        minStart = Integer.MAX_VALUE;
        if (left != null && minStart > ((IntervalTreeNode)left).minStart)
            minStart = ((IntervalTreeNode)left).minStart;
        if (right != null && minStart > ((IntervalTreeNode)right).minStart)
            minStart = ((IntervalTreeNode)right).minStart;
        int  st = in.getStart();
        if (st >= 0 && minStart > st)
            minStart = st;
    }

    protected void fixMinStartRecursive() {
        if (left != null)
            ((IntervalTreeNode)left).fixMinStartRecursive();
        if (right != null)
            ((IntervalTreeNode)right).fixMinStartRecursive();
        fixMinStart();
    }
    
    @Override
    protected void balance() {
        super.balance();
        fixMinStart();
    }
    

    @Override
    protected void rotateLL() {        
        super.rotateLL();
        fixMinStartRecursive();
    }
    @Override
    protected void rotateLR() {
        super.rotateLR();
        fixMinStartRecursive();        
    }
    
    @Override
    protected void rotateRR() {
        super.rotateRR();
        fixMinStartRecursive();    
    }
    @Override
    protected void rotateRL() {
        super.rotateRL();
        fixMinStartRecursive();
    } 
    

    
    protected void getContainingIntervals(int c, ArrayList<Interval> ret) {
        ArrayList<Interval> al = ret;
        Interval ite = (Interval)data;
        int s = ite.getStart();
        int e = ite.getEnd();
        if (c >= s && c <= e)
            al.add(ite);        
        if (left != null && c >= minStart && c <= e) 
            ((IntervalTreeNode)left).getContainingIntervals(c, al);
        if (right != null && c <= ((Interval)data).getEnd()) 
            ((IntervalTreeNode)right).getContainingIntervals(c, al);
    }
    
    public ArrayList<Interval> getIntersectingIntervals(int start, int end) {
        ArrayList<Interval> els = new ArrayList<Interval>();
        getIntersectingIntervals(start, end, els);
        return els;
        
    }

    public void getIntersectingIntervals(int start, int end, ArrayList<Interval> ret) {
        Interval ite = (Interval)data;
        if (end < minStart)
            return;
        if (start <= ite.getEnd() && end >= ite.getStart()) {
            ret.add(ite);
        } else {
        }
        if (left!= null && start <= ite.getEnd()) { 
            ((IntervalTreeNode)left).getIntersectingIntervals(start, end, ret);
        }
        if (right != null) { 
            ((IntervalTreeNode)right).getIntersectingIntervals(start, end, ret);
        }
    }
    

    protected void verifyIntegrity(String path) throws TreeIntegrityException {
        final boolean debug = false;
        path += toString();
        if (left != null)
            ((IntervalTreeNode)left).verifyIntegrity(path + "-L->");
        if (right != null)
            ((IntervalTreeNode)right).verifyIntegrity(path + "-R->");
        Interval in = (Interval)data;
        int start = in.getStart();
        int end = in.getEnd();
        if (start < 0 && end < 0)
            throw new TreeIntegrityException("Invalid start-end", path, data.getValue());
        if (left != null) { 
            Interval lin = (Interval)left.data;
            String lx = "-L->" + left.toString();
            if (debug) {
                // disabled temporarily 
                if (lin.getStart() < minStart) {
                    throw new TreeIntegrityException("left minstart error left.start="+lin.getStart()+", minStart="+minStart, path + lx, left.getData().getValue());
                }
                int lmin = ((IntervalTreeNode)left).minStart;
                if (lmin < minStart) {
                    throw new TreeIntegrityException("left minstart error left.minstart="+lmin+", minStart="+minStart, path + lx, left.getData().getValue());
                }
            }
            if (lin.getEnd() > in.getEnd()) {
                throw new TreeIntegrityException("left end error: left.end="+lin.getEnd()+", this.end="+in.getEnd(), path + lx, left.getData().getValue());            
            }
        }
        if (right != null) {
            String rx = "-R->" + right.toString();
            Interval rin = (Interval)right.data;
            if (debug) {
                // disabled temporarily 
                if (rin.getStart() < minStart)
                    throw new TreeIntegrityException("right minstart error this="+nodeRep()+", right="+right.nodeRep(), path + rx, right.getData().getValue());
            }
            if (rin.getEnd() < in.getEnd())
                throw new TreeIntegrityException("right end error", path + rx, right.getData().getValue());   
        }
    }
    
    
    
    protected final void getIntervalsWithStartBound(int  c, ArrayList<Interval> ret) {
        ArrayList<Interval> al = ret;
        Interval ite = (Interval)data;
        int ite_start = ite.getStart();
        int ite_end = ite.getEnd();
        if (c == ite_start)
            al.add(ite);
        if (c < minStart)
            return;
        if (left!= null && c <= ite_end) 
            ((IntervalTreeNode)left).getIntervalsWithStartBound(c, al);        
        if (right != null) 
            ((IntervalTreeNode)right).getIntervalsWithStartBound(c, al);        
    }

    @Override
    public String nodeRep() {
        Interval d = (Interval)data;
        return "["+d.getStart()+","+d.getEnd()+"]:"+minStart;
    }
    
    public static void main(String args[])  {
        class Info implements Interval {
            private int start, end;
            
            @Override
            public int getStart() {
                return start;
            }

            @Override
            public int getEnd() {
                return end;
            }
            
            @Override
            public int  getValue() {
                return getEnd();
            }
            
            public Info(int s, int e) {
                start = s;
                end = e;
            }
            
            @Override
            public String toString() {
                return "["+start+","+end+"]";
            }
        }
        
        int l = (int)(Math.random()*1000);
        int r = (int)(Math.random()*1000);
        IntervalTree t = new IntervalTree("test");
        t.add(new Info(l, l+r));
        long t1 = System.currentTimeMillis();
        int NUM_INTERVALS = 1000000;
        int NUM_POINT_QUERIES = 100000;
        int NUM_INT_QUERIES = 100000;
        try {
            FileWriter fw = new FileWriter("c:/temp/x");
            for(int i=0; i<NUM_INTERVALS; i++) {
                l = (int)(Math.random()*10000000);
                do {
                    r = (int)(Math.random()*1000);
                } while(t.find(l+r) != null);
                t.add(new Info(l, l+r));
            }
            long t2 = System.currentTimeMillis();
            System.out.println(NUM_INTERVALS+" intervals populated in "+((t2-t1)/1000.0)+"s");
            // POINT QUERIES
            int[] points = new int[NUM_POINT_QUERIES];
            ArrayList<?>[] z = new ArrayList<?>[NUM_POINT_QUERIES];
            for(int i=0; i<NUM_POINT_QUERIES; i++) {
                points[i] =(int)(Math.random()*10000000);
            }
            t1 = System.currentTimeMillis();
            for(int i=0; i<NUM_POINT_QUERIES; i++) {
                z[i] = t.getContainingIntervals(points[i]);
            }
            t2 = System.currentTimeMillis();
            System.out.println(NUM_POINT_QUERIES+" point-queries executed in "+((t2-t1)/1000.0)+"s");
            for(int i=0; i<NUM_POINT_QUERIES; i++) {
                fw.write(points[i]+":\n");
                for(int j=0; j<z[i].size(); j++) {
                    fw.write("\t"+z[i].get(j)+"\n");
                }
            }
            
            // INTERVAL QUERIES
            z = new ArrayList<?>[NUM_INT_QUERIES];
            int[] left = new int[NUM_INT_QUERIES];
            int[] right = new int[NUM_INT_QUERIES];
            for(int i=0; i<NUM_INT_QUERIES; i++) {
                left[i] = (int)(Math.random()*10000000);
                right[i] = left[i] + (int)(Math.random()*1000);
            }
            t1 = System.currentTimeMillis();
            for(int i=0; i<NUM_INT_QUERIES; i++) {
                z[i] = t.getIntersectingIntervals(left[i], right[i]);
            }
            t2 = System.currentTimeMillis();
            System.out.println(NUM_INT_QUERIES+" int-queries executed in "+((t2-t1)/1000.0)+"s");
            for(int i=0; i<NUM_INT_QUERIES; i++) {
                fw.write("["+left[i]+","+right[i]+"]:\n");
                for(int j=0; j<z[i].size(); j++) {
                    fw.write("\t"+z[i].get(j)+"\n");
                }
            }
            fw.close();
        }catch(IOException ex) {
            System.out.println(ex);
        }        
    }

   

    protected boolean pathToStart(int index, ArrayList<String> s) {
        Interval ite = (Interval)data;
        boolean res = false;
        if (ite.getStart() == index) {
            res = true;
        } else {
            if (left != null) {
                res = ((IntervalTreeNode)left).pathToStart(index, s);
                if (res)
                    s.add("-L->");
            }
            if (right  != null && ! res) {
                res = ((IntervalTreeNode)right).pathToStart(index, s);
                if (res)
                    s.add("-R->");
            }
        }
        if (res) {
            String curr = toString();
            s.add(curr);
        }
        return res;
    }
    
    public String toString() {
        Interval in = (Interval)data;
        return "["+in.getStart()+","+in.getEnd()+", min="+minStart+"]";
    }

}
