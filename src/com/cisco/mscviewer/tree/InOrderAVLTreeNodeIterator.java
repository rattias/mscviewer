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
import java.util.Iterator;

/**
 *
 * @author rattias
 */
public class InOrderAVLTreeNodeIterator implements Iterator<AVLTreeNode> {
    private final ArrayList<AVLTreeNode> stack = new ArrayList<AVLTreeNode>();

    public InOrderAVLTreeNodeIterator(IntervalTree tree) {
        if (tree.getRoot() != null)
            pushLefts(tree.getRoot());
    }

    private void pushLefts(AVLTreeNode tn) {
        while (tn != null) {
            stack.add(tn);
            tn = tn.getLeft();
        }
    }

    @Override
    public boolean hasNext() {
        if (stack.isEmpty())
            return false;
        else
            return true;
    }

    @Override
    public AVLTreeNode next() {
        final AVLTreeNode tn = stack.remove(stack.size() - 1);
        final AVLTreeNode r = tn.getRight();
        if (r != null)
            pushLefts(r);
        return tn;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported.");
    }

}
