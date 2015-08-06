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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cisco.mscviewer.util.MSCViewerError;

/**
 *
 * @author rattias
 */
public class AVLTree  {
    protected AVLTreeNode root;

    public AVLTree(String name) {
       
    }

    public AVLTreeNode getRoot() {
        return root;
    }

    public void add(Value p) {
        final AVLTreeNode tn = newNode(p);
        if (root == null)
            root = tn;
        else {
            root.add(tn);
        }
    }

    /**
     * (see http://en.wikipedia.org/wiki/AVL_tree) 1. If node X is a leaf or has
     * only one child, skip to step 5. (node Z will be node X) 2. Otherwise,
     * determine node Y by finding the largest node in node X's left sub tree
     * (in-order predecessor) or the smallest in its right sub tree (in-order
     * successor). 3. Replace node X with node Y (remember, tree structure
     * doesn't change here, only the values). In this step, node X is
     * essentially deleted when its internal values were overwritten with node
     * Y's. 4. Choose node Z to be the old node Y. 5. Attach node Z's subtree to
     * its parent (if it has a subtree). If node Z's parent is null, update
     * root. (node Z is currently root) 6. Delete node Z. 7. rebalance
     * 
     * @param value
     */
    public Object remove(int value) {
        AVLTreeNode prev = null;
        AVLTreeNode curr = root;
        AVLTreeNode y;
        AVLTreeNode zchild;
        AVLTreeNode yparent = curr;
        Object res;
        final List<Object> path = new ArrayList<Object>();
        while (curr != null && value != curr.data.getValue()) {
            path.add(curr);
            prev = curr;
            if (value < curr.data.getValue())
                curr = curr.left;
            else
                curr = curr.right;
        }
        if (curr != null) {
            res = curr.data;
            // node found, delete it
            // * 1. If node X is a leaf or has only one child, skip to step 5.
            // (node Z will be node X)
            if (curr.left != null && curr.right != null) {
                // * 2. Otherwise, determine node Y by finding the largest node
                // in node X's left sub tree (in-order predecessor) or the
                // smallest in its right sub tree (in-order successor).
                final int lh = curr.left.getHeight();
                final int rh = curr.right.getHeight();
                if (lh > rh) {
                    for (y = curr.left; y.right != null; y = y.right)
                        yparent = y;
                    zchild = y.left;
                } else {
                    for (y = curr.right; y.left != null; y = y.left)
                        yparent = y;
                    zchild = y.right;
                }
            } else {
                yparent = prev;
                y = curr;
                if (curr.left != null)
                    zchild = curr.left;
                else if (curr.right != null)
                    zchild = curr.right;
                else
                    zchild = null;
            }
            // * 3. Replace node X with node Y (remember, tree structure doesn't
            // change here, only the values). In this step, node X is
            // essentially deleted when its internal values were overwritten
            // with node Y's.
            curr.data = y.data;
            if (yparent.left == y) {
                yparent.left = zchild;
            } else if (yparent.right == y) {
                yparent.right = zchild;
            }
            for (int idx = path.size() - 1; idx >= 0; idx--)
                ((AVLTreeNode) path.get(idx)).balance();
            return res;
        } else
            throw new MSCViewerError("Internal error: node " + value + " not found");
    }

    public Value find(int r) {
        AVLTreeNode tn = root;
        while (tn != null) {
            final int d = tn.data.getValue();
            if (r == d)
                return tn.data;
            else if (r < d) {
                if (tn.left == null)
                    return null;
                tn = tn.left;
            } else {
                if (tn.right == null)
                    return null;
                tn = tn.right;
            }
        }
        return null;
    }

    public int count() {
        return (root != null) ? root.count(0) : 0;
    }

    public String[] pathsToValue(int v) {
        if (root == null)
            return new String[0];
        final List<String> al = new ArrayList<String>();
        root.pathsToValue(v, "", al);
        return al.toArray(new String[al.size()]);
    }

    protected AVLTree newTree(String name) {
        return new AVLTree(name);
    }

    protected AVLTreeNode newNode(Value d) {
        return new AVLTreeNode(d);
    }

    protected void verify(String msg) {
        if (root == null)
            return;
        final Set<Object> hs = new HashSet<Object>();
        root.verify(root, hs, msg, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static void getLevels(String[] s, AVLTreeNode t, int pos) {
        if (t == null)
            return;
        s[pos] = t.nodeRep();
        if (t.left != null)
            getLevels(s, t.left, pos * 2 + 1);
        if (t.right != null)
            getLevels(s, t.right, pos * 2 + 2);
    }

    /**
     * ________aaaa__________ / \ ___aaaa___ ___aaaa__ / \ / \ aaaa aaaa aaaa
     * aaaa / \ / \ / \ / \ aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa
     * 
     * @return
     */
    public void layout(PrintStream pw) {
        if (root == null)
            return;
        final int height = root.getHeight();
        final String[] vals = new String[(1 << height) - 1];
        getLevels(vals, root, 0);
        final int w = root.getMaxWidth() + 2;
        final int totalw = w * (1 << (height - 1));
        for (int level = 0; level < height; level++) {
            final int nn = 1 << level;
            for (int peer = 0; peer < nn; peer++) {
                String v = vals[(1 << level) - 1 + peer];
                if (v == null)
                    v = "-";
                final int kk = totalw / nn - v.length();
                final int spc = peer == 0 ? kk / 2 : kk;
                for (int i = 0; i < spc; i++)
                    pw.append(' ');
                pw.append(v);
            }
            pw.append('\n');
        }
    }

    /**
     * Rebuilds the tree from scratch using the data currently in the nodes.
     */
    public boolean preorder(Visitor v) {
        if (root == null)
            return true;
        return root.preorder(v);
    }

    public boolean inorder(Visitor v) {
        if (root == null)
            return true;
        return root.inorder(v);
    }

    public boolean postorder(Visitor v) {
        if (root == null)
            return true;
        return root.postorder(v);
    }
}
