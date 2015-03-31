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
import java.util.HashSet;

/**
 * This class implements an Interval Tree (actually, an Augmented Tree).
 * The implementation is slightly tweaked towards its use for storing 
 * interactions: the binary tree sorting criteria is the end of the interval,
 * because in our model events can have multiple outgoing interaction but only
 * one incoming one. This allows us to use a single IntervalInfo to store
 * interval in nodes instead of an array.
 * @author rattias
 */
public class AVLTreeNode {    
    protected Value data;
    protected AVLTreeNode left;
    protected AVLTreeNode right;
    private int height;
    public static AVLTreeNode singleton;

    public AVLTreeNode(Value d) {
        data = d;      
        height = 1;
    }    

    public AVLTreeNode getLeft() {
        return left;
    }
    
    public AVLTreeNode getRight() {
        return right;
    }
    
    public Value getData() {
        return data;
    }
    
    public int getHeight() {
        return height;
    }
 
    public String nodeRep() {
        return data.toString()+":"+height;
    }
    
    public void print() {
        System.out.println(nodeRep());
    }

    protected int count(int v) {
        v++;
        if (left != null)
            v = left.count(v);
        if (right != null)
            v = right.count(v);
        return v;
    }
            
    @SuppressWarnings("unused")
    private void format(ArrayList<String>[] s, int level) {
        if (s[level] == null)
            s[level] = new ArrayList<String>();
        s[level].add(nodeRep());
        if (left != null)
            left.format(s, level+1);
        else if  (level+1 < s.length && s[level+1] != null)
            s[level+1].add("");
        if (right != null && level < height-1)
            right.format(s, level+1);
        else if  (level+1 < s.length && s[level+1] != null)
            s[level+1].add("");
    }
    
    
    protected int getMaxWidth() {
        int l = nodeRep().length();
        if (left != null)
            l = Math.max(l, left.getMaxWidth());
        if (right != null)
            l = Math.max(l, right.getMaxWidth());
        return l;
    }   
    
        

        
    protected void verify(AVLTreeNode root, HashSet<Object> set, String msg, int min, int max) {
        if (data.getValue() < min || data.getValue() > max) {
            ArrayList<String> al = new ArrayList<String>();
            root.pathsToValue(data.getValue(), "", al);
            System.err.println("Exception: current tree: ");
            throw new Error(msg+":"+al.get(0)+": "+data.getValue()+" should be in range ("+min+","+max+")");
        }
        if (set.contains(this))
            throw new Error("Circular Link: "+msg);
        set.add(this);
        if (set.contains(data))
            throw new Error("Duplicated value: "+msg);
        set.add(data);
        if (left != null) {
            left.verify(root, set, msg, min, data.getValue());
        }
        if (right != null) {
            right.verify(root, set, msg, data.getValue(), max);
        }
    }
 
    
    
    protected void add(AVLTreeNode tn) {
        tn.left = tn.right = null;
        if (tn.data.getValue() < data.getValue()) {
            if (left == null) {
                left = tn;
            } else {
                left.add(tn);
            }
        } else if (tn.data.getValue() > data.getValue()) {
            if (right  == null) {
                right = tn;
            } else {
                right.add(tn);
            }
        } else 
            System.err.println("Unexpected repeated value "+tn.data.getValue());
//        HashSet hs = new HashSet();
//        verify(this, hs, "added "+tn.data.getValue()+", before balance", Integer.MIN_VALUE, Integer.MAX_VALUE);

        balance();
        
//        hs = new HashSet();
//        verify(this, hs, "added "+tn.data.getValue()+", after balance", Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
 
    protected void balance() {
        int lh = left != null ? left.height : 0;
        int rh = right  != null ? right.height : 0;
        if (lh-rh > 1) {
            int llh = left.left != null ? left.left.height : 0;
            int lrh = left.right != null ? left.right.height : 0;
            int d = llh - lrh;
            if (d >= 0) {
                rotateLL();
            } else
                rotateLR();
            lh--;
        } else if (lh-rh < -1) {
            int rlh = right.left != null ? right.left.height : 0;
            int rrh = right.right != null ? right.right.height : 0;
            int d = rlh - rrh;
            if (d >= 0) {
                rotateRL();
            } else
                rotateRR();
            rh--;
        }
        height = Math.max(lh, rh) + 1;
    }
    
    protected void rotateLL() { 
        AVLTreeNode L = left;
        AVLTreeNode R = right;
        AVLTreeNode LL = left.left;
        AVLTreeNode LR = left.right;
        Value tmp = data;
        data  = L.data;
        left = LL;
        right = L;
        right.data = tmp;
        right.left = LR;
        right.right = R;
        right.height = Math.max(right.left != null ? right.left.height : 0,
                                right.right != null? right.right.height: 0)
                       + 1;
        height = Math.max(left.height, right.height)+1;
        //verify("LL");
    }
    
    protected void rotateLR() {
        AVLTreeNode R = right;
        AVLTreeNode LR = left.right;
        AVLTreeNode LRL = left.right.left;
        AVLTreeNode LRR = left.right.right;
        Value tmp = data;
        data = LR.data;
        left.right = LRL;
        right = LR;
        right.data = tmp;
        right.left = LRR;
        right.right = R;
        left.height = Math.max(left.left != null ? left.left.height : 0,
                               left.right != null? left.right.height: 0)
                      + 1;
        right.height = Math.max(right.left != null ? right.left.height : 0,
                                right.right != null? right.right.height: 0)
                       + 1;
        height = Math.max(left.height, right.height)+1;        
        //verify("LR");
    }

    protected void rotateRR() { 
        AVLTreeNode L = left;
        AVLTreeNode R = right;
        AVLTreeNode RL = right.left;
        AVLTreeNode RR = right.right;
        Value tmp = data;
        data = R.data;
        left = R;
        left.data = tmp;
        left.left = L;
        left.right = RL;
        right = RR;
        left.height = Math.max(left.left != null ? left.left.height : 0,
                               left.right!= null ? left.right.height: 0)
                +1;
        height = Math.max(left.height, right.height)+1;
        //verify("RR");
    }

    protected void rotateRL() {
        AVLTreeNode L = left;
        AVLTreeNode RL = right.left;
        AVLTreeNode RLL = right.left.left;
        AVLTreeNode RLR = right.left.right;
        Value tmp = data;
        data = RL.data;
        left = RL;
        left.data = tmp;
        left.left = L;
        left.right = RLL;
        right.left = RLR;
        left.height = Math.max(left.left != null ? left.left.height : 0,
                               left.right!= null ? left.right.height : 0)
                +1;
        right.height = Math.max(right.left != null ? right.left.height : 0,
                                right.right != null? right.right.height : 0)+1;
        height = Math.max(left.height, right.height)+1;        
        //verify("RL");
    }
        
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void visit(ArrayList<Value> al) {
        if (left != null)
            left.visit(al);
        if (al.size()>0) {
            Comparable el1 = data.getValue();
            Comparable el2 = al.get(al.size()-1).getValue();
            if (el1.compareTo(el2) < 0)
                throw new Error("NO : el1="+el1+", el2="+el2);
        }
        al.add(data);
        if (right != null)
            right.visit(al);
    }

    protected boolean pathToNode(AVLTreeNode node, ArrayList<Object> al) {
        if (node == this) {
            al.add(this);
            return true;
        }
        if (left != null) {
            if (left.pathToNode(node, al)) {
                al.add("-L->");
                return true;
            }            
        }
        if (right != null) {
            al.add("-R->");
            if (right.pathToNode(node, al)) 
                return true;
            else {
                al.remove(al.size()-1);
            }
        }
        if (al.size() > 0 && al.get(al.size()-1).toString().endsWith("->"))
            al.remove(al.size()-1);
        return false;
    }


    protected String pathsToValue(int v, String currPath, ArrayList<String> paths) {
        String np = currPath + this;
        if (data.getValue() == v) {
            paths.add(np);
        }
        if (left != null) {
            ((IntervalTreeNode)left).pathsToValue(v, np + "-L->", paths);
        }
        if (right  != null) {
            ((IntervalTreeNode)right).pathsToValue(v, np + "-R->", paths);
        }
        return paths.size()>0 ? paths.get(0) : "<none>";
    }
    
    @SuppressWarnings("unused")
    public static void main(String args[]) {
        class Info implements Value {
            private int start, end;
            
            public int getStart() {
                return start;
            }

            public int getEnd() {
                return end;
            }
            
            @Override
            public int getValue() {
                return getEnd();
            }
            
            public Info(int s, int e) {
                start = s;
                end = e;
            }
            
            @Override
            public String toString() {
                return ""+end;
            }
        }
        AVLTree t;
        if (false) {
            int r = (int) (Math.random() * 100);
            t = new AVLTree("test0");
            t.add(new Info(0, r));
            for(int i=0; i<32; i++) {
                do {
                    r = (int)(Math.random()*100);
                } while(t.find(r) != null);
                t.add(new Info(0, r));
            }
        } else {
            int[] vals = {1, 5, 20, 8, 7, 4, 10};
            t = new AVLTree("test1");
            t.add(new Info(0, 9));
            for (int v : vals) {
                t.add(new Info(0, v));
            }
        }
        t.layout(System.out);
        t.remove(7);
        System.out.println("removed 7");
        t.layout(System.out);
        System.out.println();        
    }

    public void detachChildren() {
        left = null;
        right = null;
    }

    public boolean preorder(Visitor v) {
        return v.visit(this) || 
                (left != null && left.preorder(v)) || 
                (right != null && right.preorder(v));
    }

    public boolean inorder(Visitor v) {
        return (left != null && left.inorder(v)) ||
                v.visit(this) || 
                (right != null && right.inorder(v));
    }

    public boolean postorder(Visitor v) {
        return (left != null && left.postorder(v)) ||
                (right != null && right.postorder(v)) || 
                v.visit(this);
    }

}

