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
package com.cisco.mscviewer.model.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.util.ProgressReport;
import com.cisco.mscviewer.util.Utils;

public class TopologyGraph {

    class P {
        int node;
        int arc;

        public P(int n, int a) {
            node = n;
            arc = a;
        }
    }

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
    private ProgressReport pr = null;

    /**
     * creates a graph where:
     * <ul>
     * <li>Each node represents a sequence of events with same timestamp and
     * entity
     * <li>there is an edge between each pair of consecutive (by timestamp)
     * nodes for the same entity
     * <li>there is an edge between each pair of consecutive (by timestamp)
     * nodes not belonging to the same entity and with different timestamp
     * </ul>
     * note: this code assumes that all events belonging to the same entity and
     * with the same timestamp are going to appear contiguously in the global
     * array of events in the datamodel.
     *
     * @param dm
     * @param obs
     */
    public TopologyGraph(MSCDataModel dm) {
        this.dm = dm;
        final int evCount = dm.getEventCount();
        if (evCount == 0) {
            return;
        }
        final int interCount = dm.getInteractionCount();
        fromNode = new I[evCount][];
        toNode = new I[evCount][];
        final HashMap<Entity, Integer> enToEvIdx = new HashMap<Entity, Integer>();
        ProgressReport subPr = null;
        try {
            pr = new ProgressReport("Topological Sorting", "", 0, 100);
            subPr = pr.subReport("Creating graph", "edges", 20, 0, evCount,
                    true);
            for (int i = 0; i < evCount; i++) {
                subPr.progress(i);
                final Event ev = dm.getEventAt(i);
                final Entity en = ev.getEntity();
                final Integer prevIdx = enToEvIdx.get(en);
                if (prevIdx != null) {
                    // create an edge between last event on this entity and this
                    // one
                    addEdge(prevIdx, i, true);
                }
                enToEvIdx.put(en, i);
            }
            
            subPr.progressDone();
            int i = 0;
            subPr = pr.subReport("Creating graph", "Interactions", 20, 0,
                    interCount, true);
            for (final Iterator<Interaction> it = dm.getInteractionIterator(); it
                    .hasNext();) {
                subPr.progress(i);
                i++;
                final Interaction in = it.next();
                final int from = in.getFromIndex();
                final int to = in.getToIndex();
//                System.out.println("INTER "+from+"->"+to);
                if (from != -1 && to != -1)
                    addEdge(from, to, false);
            }
            subPr.progressDone();
        }catch(Exception ex) {
            // in case of exception close progress
            if (subPr != null)
                subPr.progressDone();
            if (pr != null)
                pr.progressDone();
            throw ex;
        }            
    }

    private ArrayList<P> findLoop() {
        final int evCount = dm.getEventCount();
        final ArrayList<P> loop = new ArrayList<P>();
        int idx;
        for (idx = 0; idx < evCount; idx++) {
            if (fromNode[idx] != null && hasIncomingEdges(idx))
                break;
        }
        if (idx == evCount)
            throw new Error("shouldn't happen");
        loop.add(new P(idx, -1));
        int lastIdx = 0;
        while (true) {
            P latest = null;
            while (true) {
                latest = loop.get(lastIdx);
                latest.arc++;
                if (latest.arc < fromNode[latest.node].length) {
                    if (fromNode[latest.node][latest.arc].v != -1) {
                        // found next arc to traverse
                        break;
                    }
                } else {
                    // backtrack one node
                    lastIdx--;
                    if (lastIdx < 0)
                        return null;
                }
            }
            lastIdx++;
            final P p = new P(fromNode[latest.node][latest.arc].v, -1);
            if (lastIdx == loop.size())
                loop.add(p);
            else
                loop.set(lastIdx, p);
            for (int i = 0; i < lastIdx; i++)
                if (loop.get(i).node == p.node)
                    return loop;
        }
    }

    public int[] topoSort() throws TopologyError {
        ProgressReport subPr = null;
        try {
            final int evCount = dm.getEventCount();
            final int[] L = new int[evCount];
            final TreeSet<Integer> S = new TreeSet<Integer>(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    int n1Idx = ((Integer)o1).intValue();
                    int n2Idx = ((Integer)o2).intValue();
                    long ts1 = dm.getEventAt(n1Idx).getTimestamp();
                    long ts2 = dm.getEventAt(n2Idx).getTimestamp();
                    if (ts1 == ts2)
                        return Integer.compare(n1Idx, n2Idx);
                    return Long.compare(ts1, ts2);                
                }
            });
            // populate S with nodes with no incoming edges
            if (evCount == 0)
                return null;
            subPr = pr.subReport("Sorting Topology...", "phase one",
                    30, 0, evCount, true);
            for (int i = 0; i < evCount; i++) {
                subPr.progress(i); // first 20%
                Event ev = dm.getEventAt(i);
                if (!hasIncomingEdges(i)) {
                    S.add(i);
                }
    
            }
            subPr.progressDone();
            subPr = pr.subReport("Sorting Topology...", "phase two", 30, 0,
                    evCount, true);
            int idx = 0;
            while (!S.isEmpty()) {
                final Integer n = S.pollFirst();
                L[idx++] = n;
                subPr.progress(idx);
                if (toNode[n] != null) {
                    for (final I m : toNode[n]) {
                        if (m.v < 0) {
                            continue;
                        }
                        final int v = m.v;
                        removeEdge(n, v);
                        if (!hasIncomingEdges(v)) {
                            // we want to insert in order, so that when traversing
                            // we loosely
                            // consider original order. Traverse list backwards from
                            // end because
                            // elements are more likely to be towards the end.
                            S.add(v);
                        }
                    }
                }
            }
            subPr.progressDone();
            if (idx != evCount) {
                String filePath = Utils.getWorkDirPath()+"/causality_looo.msc";
                final ArrayList<P> al = findLoop();
                if (al != null) {
                    PrintWriter pw;
                    try {
                        pw = new PrintWriter(new FileWriter(new File(dm.getCausalityLoopFileName())));
                        HashSet<Event> set = new HashSet<Event>();
                        for (int eidx = al.size()-1; eidx>=0; eidx--) {
                            P p = al.get(eidx);
                            Event ev = dm.getEventAt(p.node);
                            if (set.contains(ev))
                                continue;
                            set.add(ev);
                            pw.print("@event {\"entity\":\""+ev.getEntity().getPath()+"\"");
                            pw.print(", \"time\":\""+ev.getTimestamp()+"\"");
                            pw.print(", \"label\":\"["+ev.getLineIndex()+"] "+ev.getLabel()+"\"");
    
                            Interaction ins[] = ev.getOutgoingInteractions(); 
                            if (ins.length == 1) {
                                pw.print(", \"src\":\""+ev.getEntity().getPath()+"/"+ins[0].getToIndex()+"\"");
                            } else if (ins.length > 1) {
                                pw.print(", \"src\":\"{");
                                for(int i=0; i<ins.length; i++) {
                                    if (i>0)
                                        pw.print(", ");
                                    pw.print(""+ev.getEntity().getPath()+"/"+ins[0].getToIndex()+"\"");
                                }
                                pw.print("}\"");                        
                            }
    
                            ins = ev.getIncomingInteractions();                  
                            if (ins.length == 1) {
                                Event srcEv = ins[0].getFromEvent();
                                if (srcEv != null) {                                
                                    pw.print(", \"dst\":\""+srcEv.getEntity().getPath()+"/"+ins[0].getToIndex()+"\"");
                                }
                            } else if (ins.length > 1) {
                                pw.print(", \"dst\":\"{");
                                for(int i=0; i<ins.length; i++) {
                                    if (i>0)
                                        pw.print(", ");
                                    pw.print(""+ev.getEntity().getPath()+"/"+ins[0].getToIndex()+"\"");
                                }
                                pw.print("}\"");                        
                            }
                            pw.println("}");
                        }
                        pw.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } 
                }
                throw new TopologyError("Failed to sort topologically:\n A reduced file containing only the loop events has been saved to "+filePath);
            } else {
            }
    
            // outer:
            // for (int i = 0; i < evCount; i++) {
            // for (int k=0; k<idx; k++) {
            // if (L[k] == i) {
            // System.out.println("SORTED: " + nodeToString(i));
            // break outer;
            // }
            // }
            // System.out.println("UNSORTED: " + nodeToString(i));
            // }
            // throw new TopologyError("Topology Error.");
    
            pr.progressDone();
            pr = null;
            return L;
        } finally {
            if (subPr != null)
                subPr.progressDone();
            if (pr != null)
                pr.progressDone();
            pr = null;
        }
    }

    private void addEdge(int fromIdx, int toIdx, boolean cause) {
        if (fromIdx == toIdx) {
            throw new Error("Self looping edge at index " + fromIdx);
        }
        if (hasEdge(fromIdx, toIdx)) {
            return;
        }
        I[] fe = fromNode[toIdx];
        if (fe == null) {
            fe = new I[1];
            fe[0] = new I(fromIdx, cause);
        } else {
            final int oldLen = fe.length;
            final I[] tmp = new I[oldLen + 1];
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
            final int oldLen = te.length;
            final I[] tmp = new I[oldLen + 1];
            System.arraycopy(te, 0, tmp, 0, oldLen);
            te = tmp;
            te[oldLen] = new I(toIdx, cause);
        }
        toNode[fromIdx] = te;
    }

    private boolean hasEdge(int fromIdx, int toIdx) {
        int i;
        final I[] fn = fromNode[toIdx];
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
        final I[] tn = toNode[fromIdx];
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
        final I[] fn = fromNode[toIdx];
        if (fn == null) {
            throw new Error("no edge to remove");
        }
        for (i = 0; i < fn.length; i++) {
            if (fn[i].v == fromIdx) {
                fn[i].v = -1; // -fromIdx;
                break;
            }
        }
        if (i == fn.length) {
            throw new Error("node " + toIdx + " has no arc from " + fromIdx);
        }
        final I[] tn = toNode[fromIdx];
        if (tn == null) {
            throw new Error("no edge to remove");
        }
        for (i = 0; i < tn.length; i++) {
            if (tn[i].v == toIdx) {
                tn[i].v = -1; // -toIdx;
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
        for (final I item : fromNode[evIdx]) {
            if (item.v >= 0) {
                return true;
            }
        }
        return false;
    }

    String nodeToString(int n) {
        final StringBuilder sb = new StringBuilder();
        sb.append(n).append("(").append(dm.getEventAt(n).getEntity().getPath())
                .append(")");
        final I[] fr = fromNode[n];
        if (fr != null) {
            sb.append(", from=(");
            for (final I fr1 : fr) {
                sb.append(fr1);
                sb.append(" ");
            }
            sb.append(") ");
        }
        final I[] to = toNode[n];
        if (to != null) {
            sb.append(", to=(");
            for (final I to1 : to) {
                sb.append(to1);
                sb.append(" ");
            }
            sb.append(") ");
        }
        return sb.toString();
    }

}
