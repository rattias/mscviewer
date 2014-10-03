/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jan 2014
 */
package com.cisco.mscviewer.graph;

import com.cisco.mscviewer.util.ProgressReport;
import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;


public class Graph {
    private ProgressReport pr;
    
    class I {

        int v;
        boolean entity;

        public I(int v, boolean entity) {
            this.v = v;
            this.entity = entity;
        }

        @Override
        public String toString() {
            return "(" + v + ", " + (entity ? "E)" : "I)");
        }
    }
    I[][] fromNode;
    I[][] toNode;

    private final MSCDataModel dm;

    /**
     * creates a graph where:
     * <ul>
     * <li> Each node represents a sequence of events with same timestamp and
     * entity
     * <li> there is an edge between each pair of consecutive (by timestamp)
     * nodes for the same entity
     * <li> there is an edge between each pair of consecutive (by timestamp)
     * nodes not belonging to the same entity and with different timestamp
     * </ul>
     * note: this code assumes that all events belonging to the same entity and
     * with the same timestamp are going to appear contiguously in the global
     * array of events in the datamodel.
     *
     * @param dm
     * @param obs
     */
    public Graph(MSCDataModel dm) {
        this.dm = dm;
        final int evCount = dm.getEventCount();
        if (evCount == 0) {
            return;
        }
        int interCount = dm.getInteractionCount();
        fromNode = new I[evCount][];
        toNode = new I[evCount][];
        HashMap<Entity, Integer> enToEvIdx = new HashMap<Entity, Integer>();
        pr = null;
        ProgressReport subPr = null;
        try {
            pr = new ProgressReport("Topological Sorting", "", 0, 100);
            subPr = pr.subReport("Creating graph", "edges", 20, 0, evCount, true);
            for (int i = 0; i < evCount; i++) {
                subPr.progress(i);
                Event ev = dm.getEventAt(i);
                Entity en = ev.getEntity();
                Integer prevIdx = enToEvIdx.get(en);
                if (prevIdx != null) {
                    // create an edge between last event on this entity and this one 
                    addEdge(prevIdx, i, true);
                }
                enToEvIdx.put(en, i);
            }
            subPr.progressDone();
            int i = 0;
            subPr = pr.subReport("Creating graph", "Interactions", 20, 0, interCount, true);
            for(Iterator<Interaction> it = dm.getInteractionIterator(); it.hasNext();) {
                subPr.progress(i);
                i++;
                Interaction in = it.next();
                int from = in.getFromIndex();
                int to = in.getToIndex();
                if (from != -1 && to != -1)
                    addEdge(from, to, false);
            }
            subPr.progressDone();
        } finally {
            if (subPr != null)
                subPr.progressDone();
            if (pr != null)
                pr.progressDone();
        }
    }

    public int[] topoSort() throws TopologyError {
        int evCount = dm.getEventCount();
        int[] L = new int[evCount];
        TreeSet<Integer> S = new TreeSet<Integer>();
        // populate S with nodes with no incoming edges
        if (evCount == 0)
            return null;
        ProgressReport subPr = pr.subReport("Sorting Topology...", "phase one", 30, 0, evCount, true);
        for (int i = 0; i < evCount; i++) {
            subPr.progress(i); //first 20%
            if (!hasIncomingEdges(i)) {
                S.add(i);
            }

        }
        subPr.progressDone();
        subPr = pr.subReport("Sorting Topology...", "phase two", 30, 0, evCount, true);
        int idx = 0;
        while (!S.isEmpty()) {
            Integer n = S.pollFirst();
            L[idx++] = n;
            subPr.progress(idx);
            if (toNode[n] != null) {
                for (I m : toNode[n]) {
                    if (m.v < 0) {
                        continue;
                    }
                    int v = m.v;
                    removeEdge(n, v);
                    if (!hasIncomingEdges(v)) {
                        // we want to insert in order, so that when traversing we loosely 
                        // consider original order. Traverse list backwards from end because
                        // elements are more likely to be towards the end.
                        S.add(v);
                    }
                }
            }
        }
        subPr.progressDone();
        if (idx != evCount) {
            System.out.println("SORTING INCOMPLETE: " + idx + "/" + evCount);
            outer:
            for (int i = 0; i < evCount; i++) {
                for (int k=0; k<idx; k++) {
                    if (L[k] == i) {
                        System.out.println("SORTED: " + nodeToString(i));
                        break outer;
                    }
                }
                System.out.println("UNSORTED: " + nodeToString(i));
            }
            throw new TopologyError("Topology Error.");
        }
        pr.progressDone();
        return L;
    }

    private void addEdge(int fromIdx, int toIdx, boolean cause) {
        if (fromIdx == toIdx) {
            throw new Error("Self looping edge at index "+fromIdx);
        }
        if (hasEdge(fromIdx, toIdx)) {
            return;
        }
        I[] fe = fromNode[toIdx];
        if (fe == null) {
            fe = new I[1];
            fe[0] = new I(fromIdx, cause);
        } else {
            int oldLen = fe.length;
            I[] tmp = new I[oldLen + 1];
            System.arraycopy(fe, 0, tmp, 0, oldLen);
            fe = tmp;
            fe[oldLen] = new I(fromIdx, cause);
        }
        fromNode[toIdx] = fe;
        I[] te = toNode[fromIdx];
        if (te == null) {
            te = new I[1];
            te[0] = new I(toIdx, cause);
        } else {
            int oldLen = te.length;
            I[] tmp = new I[oldLen + 1];
            System.arraycopy(te, 0, tmp, 0, oldLen);
            te = tmp;
            te[oldLen] = new I(toIdx, cause);
        }
        toNode[fromIdx] = te;
    }

    private boolean hasEdge(int fromIdx, int toIdx) {
        int i;
        I[] fn = fromNode[toIdx];
        if (fn == null) {
            return false;
        }
        for (i = 0; i < fn.length; i++) {
            if (fn[i].v == fromIdx) {
                break;
            }
        }
        if (i == fn.length) {
            return false;
        }
        I[] tn = toNode[fromIdx];
        if (tn == null) {
            return false;
        }
        for (i = 0; i < tn.length; i++) {
            if (tn[i].v == toIdx) {
                return true;
            }
        }
        return false;
    }

    private void removeEdge(int fromIdx, int toIdx) {
        if (fromIdx < 0 || toIdx < 0) {
            throw new Error("can't remove (" + fromIdx + ", " + toIdx + ")");
        }
        int i;
        I[] fn = fromNode[toIdx];
        if (fn == null) {
            throw new Error("no edge to remove");
        }
        for (i = 0; i < fn.length; i++) {
            if (fn[i].v == fromIdx) {
                fn[i].v = -1; //-fromIdx;
                break;
            }
        }
        if (i == fn.length) {
            throw new Error("node " + toIdx + " has no arc from " + fromIdx);
        }
        I[] tn = toNode[fromIdx];
        if (tn == null) {
            throw new Error("no edge to remove");
        }
        for (i = 0; i < tn.length; i++) {
            if (tn[i].v == toIdx) {
                tn[i].v = -1; //-toIdx;
                return;
            }
        }
        if (i == tn.length) {
            throw new Error("node " + fromIdx + " has no arc to " + toIdx);
        }
    }

    public boolean hasIncomingEdges(int evIdx) {
        if (fromNode[evIdx] == null) {
            return false;
        }
        for (I item : fromNode[evIdx]) {
            if (item.v >= 0) {
                return true;
            }
        }
        return false;
    }

    String nodeToString(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n).append("(").append(dm.getEventAt(n).getEntity().getPath()).append(")");
        I[] fr = fromNode[n];
        if (fr != null) {
            sb.append(", from=(");
            for (I fr1 : fr) {
                sb.append(fr1);
                sb.append(" ");
            }
            sb.append(") ");
        }
        I[] to = toNode[n];
        if (to != null) {
            sb.append(", to=(");
            for (I to1 : to) {
                sb.append(to1);
                sb.append(" ");
            }
            sb.append(") ");
        }
        return sb.toString();
    }

}
