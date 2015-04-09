/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.io;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.util.ProgressReport;
import com.cisco.mscviewer.gui.renderer.*;
import com.cisco.mscviewer.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Loader for traditional MSCViewer file format
 * @author rattias
 */
public class LegacyLoader implements Loader {

    public enum TypeEn {

        SOURCE, SINK, LOCAL
    };
    private static final String MSC_ENTITY = "@msc_entity";
    private static final String MSC_EVENT = "@msc_event";
    private static final String MSC_SOURCE = "@msc_source";
    private static final String MSC_SINK = "@msc_sink";

    private CountDownLatch latch;

    private static void loadInternal(String fname, MSCDataModel dm) throws IOException {
        HashMap<String, Interaction> pending = new HashMap<String, Interaction>();
        File file = new File(fname);
        int flen = (int) file.length();
        dm.enableNotification(false);
        BufferedReader fr = new BufferedReader(new FileReader(file));
        HashMap<String, Class<?>> classCache = new HashMap<String, Class<?>>();
        String line;
        int lineNum = 0;
        int readCnt = 0;
        String msg = "Loading file " + fname + "...";
        long t0 = System.currentTimeMillis();
        ProgressReport pr = new ProgressReport("Loading", msg, 0, flen);
        try {
            int x = 0;
            while ((line = fr.readLine()) != null) {
                readCnt += line.length();
                x++;
                if (x % 1024 == 0) {
                    pr.progress(msg, readCnt);
                }

                lineNum++;
                int end = line.length();
                int start = line.indexOf(MSC_ENTITY);
                if (start >= 0) {
                    start += MSC_ENTITY.length() + 1;

                    //event properties
                    JSonObject props = new JSonObject();
                    start = parseProps(fname, lineNum, line, start, end, props);
                    String entityPath = props.get("id").toString();
                    if (entityPath == null) {
                        throw new IOException(fname + ":" + lineNum + ":Missing entity_id specification.");
                    }

                    String displayName = props.get("display_name").toString();
                    Entity en = dm.addEntity(entityPath, displayName);
                    String descr = props.get("description").toString();
                    if (descr != null) {
                        en.setDescription(descr);
                    }
                    // currently we mark root entities as clocks source. Ideally it should be
                    // specified in the file which entities are clock sources.
                    if (en.getParentEntity() == null) {
                        en.setAsClockSource(true);
                    }

                } else {
                    start = line.indexOf(MSC_EVENT);
                    if (start >= 0) {
                        start += MSC_EVENT.length() + 1;
                        //event properties
                        JSonObject props = new JSonObject();
                        start = parseProps(fname, lineNum, line, start, end, props);

                        String entityPath = props.get("entity_id").toString();
                        if (entityPath == null) {
                            throw new IOException(fname + ":" + lineNum + ":Missing entity specification.");
                        }
                        props.remove("entity_id");
                        Entity entity = dm.addEntity(entityPath, null);
                        Entity parentEntity = entity.getParentEntity();

                        String label = props.get("label").toString();
                        if (label == null) {
                            label = "";
                        }
                        props.remove("label");

                        long ts = -1;
                        String time = props.get("time").toString();
                        if (time != null) {
                            props.remove("time");
                            if (time.endsWith("s")) {
                                int len = time.length();
                                char pre = time.charAt(len - 2);
                                switch (pre) {
                                    case 'n':
                                        ts = Long.parseLong(time.substring(0, len - 2));
                                        break;
                                    case 'u':
                                        ts = Long.parseLong(time.substring(0, len - 2)) * 1000;
                                        break;
                                    case 'm':
                                        ts = Long.parseLong(time.substring(0, len - 2)) * 1000000;
                                        break;
                                    default:
                                        if (Character.isDigit(pre)) {
                                            ts = Long.parseLong(time.substring(0, len - 2)) * 1000000;
                                        } else {
                                            throw new IOException(fname + ":" + lineNum + ":Invalid time unit specifier");
                                        }
                                }
                            } else {
                                ts = Long.parseLong(time);
                            }
                        }

                        String t = props.get("type").toString();
                        EventRenderer renderer = null;
                        if (t != null) {
                            props.remove("type");
                            String rendererName = t + "Renderer";
                            String fqn = "com.cisco.mscviewer.gui.renderer." + rendererName;
                            Class<?> cl = classCache.get(fqn);
                            if (cl == null) {
                                try {
                                    cl = Class.forName(fqn);
                                    classCache.put(fqn, cl);
                                } catch (ClassNotFoundException e) {
                                    throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + rendererName + ".", e);
                                }
                            }
                            try {
                                renderer = (EventRenderer)cl.newInstance();
                            } catch (IllegalAccessException e) {
                                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + rendererName + ".", e);
                            } catch (InstantiationException e) {
                                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + rendererName + ".", e);
                            }
                        }
                        String pushSourceVal = props.get("push_source").toString();
                        if (pushSourceVal != null && parentEntity != null) {
                            parentEntity.pushSourceEntityForFromEvents(entity);
                        }
                        String popSourceVal = props.get("pop_source").toString();
                        if (popSourceVal != null && parentEntity != null) {
                            parentEntity.popSourceEntityForFromEvents();
                        }
                        entity = entity.getSourceEntityForFromEvents();
                        Event ev = new Event(dm, ts, entity, label, lineNum, renderer, props);
                        int evIndex = dm.addEvent(ev);
                        String note = props.get("note").toString();
                        if (note != null) {
                            ev.setNote(note);
                            props.remove("note");
                        }
                        String begb = props.get("begin").toString();
                        if (begb != null) {
//                            ev.setBegin(true);
                            JSonObject ps = new JSonObject();
                            ps.set("type", new JSonStringValue("BlockInteraction"));
                            ps.set("color", new JSonStringValue("000000"));
                            String pairingId = entity.getId() + "/block";
                            Interaction inter = createInteraction(dm, pairingId, ps, ev, TypeEn.SOURCE, evIndex, fname, lineNum);
                            pending.put(pairingId, inter);
                        }
                        String endb = props.get("end").toString();
                        if (endb != null) {
                            String pairingId = entity.getId() + "/block";
                            Interaction inter = pending.remove(pairingId);
                            if (inter == null) {
                                JSonObject ps = new JSonObject();
                                ps.set("type", new JSonStringValue("BlockInteraction"));
                                ps.set("color", new JSonStringValue("000000"));
                                inter = createInteraction(dm, pairingId, ps, ev, TypeEn.SINK, evIndex, fname, lineNum);
                                dm.addInteraction(inter);
                            } else {
                                inter.setToIndex(evIndex);
                            }
                        }

                        while (true) {
                            while (start < end && Character.isWhitespace(line.charAt(start))) {
                                start++;
                            }
                            if (start >= end) {
                                break;
                            }
                            TypeEn type;
                            if (line.substring(start).startsWith(MSC_SOURCE)) {
                                type = TypeEn.SOURCE;
                                start += MSC_SOURCE.length();
                            } else if (line.substring(start).startsWith(MSC_SINK)) {
                                type = TypeEn.SINK;
                                start += MSC_SINK.length();
                            } else {
                                throw new IOException(fname + ":" + lineNum + ": invalid tag " + line.substring(start) + ".");
                            }

                            props = new JSonObject();
                            start = parseProps(fname, lineNum, line, start, end, props);
                            String pairingId = props.get("pairing_id").toString();
                            if (pairingId == null) {
                                throw new IOException(fname + ":" + lineNum + ":error: missing pairing_id in source/sink definition");
                            }
                            //props.remove("pairing_id");
                            Interaction inter = pending.remove(pairingId);
                            if (inter == null) {
                                inter = createInteraction(dm, pairingId, props, ev, type, evIndex, fname, lineNum);
                                pending.put(pairingId, inter);
                            } else {
                                // a source or sink for this interaction was found earlier
                                TypeEn prevType;
                                Entity prevEn;
                                Event fromEv = inter.getFromEvent();
                                Event toEv = inter.getToEvent();
                                if (fromEv != null) {
                                    prevType = TypeEn.SOURCE;
                                    prevEn = fromEv.getEntity();
                                } else if (toEv != null) {
                                    prevType = TypeEn.SINK;
                                    prevEn = toEv.getEntity();
                                } else {
                                    throw new Error("Unexpected: interaction without source or sink event");
                                }
                                if (prevType == type) {
                                    //ignore previous event stored...must have been orphaned
                                    inter = createInteraction(dm, pairingId, props, ev, type, evIndex, fname, lineNum);
                                    pending.put(pairingId, inter);
                                } else if (prevType == TypeEn.SINK && type == TypeEn.SOURCE
                                        && prevEn == ev.getEntity()) {
                                    //we are a processing a source, and there was a pending sink with
                                    //the same pairing ID ON THE SAME ENTITY. That sink is orphaned, ignore it
                                    inter = createInteraction(dm, pairingId, props, ev, type, evIndex, fname, lineNum);
                                    pending.put(pairingId, inter);
                                } else {
                                    if (type == TypeEn.SOURCE) {
                                        inter.setFromIndex(evIndex);
                                    } else {
                                        inter.setToIndex(evIndex);
                                    }
                                    dm.addInteraction(inter);
                                    inter.getIRenderer().initialize(inter, props, ev);
                                }
                            }
                        }
                    }
                }
                dm.addDataLine(line);
            }
            pr.progress(flen);
            for (Interaction in : pending.values()) {
                dm.addInteraction(in);
            }
            //dm.interactions.verifyIntegrity();
            dm.topoSort();
        } catch (IOException ex) {
            throw new IOException(fname + ":" + lineNum + ":error: at this location", ex);
        } catch (NumberFormatException ex) {
            throw new IOException(fname + ":" + lineNum + ":error: at this location", ex);
        } finally {
            fr.close();
        }
        long t1 = System.currentTimeMillis();
        float elapsed = (t1-t0)/1000.0f;
        pr.progressDone();
        System.out.println("Loaded " + dm.getEntityCount() + " entities, " + dm.getEventCount() + " events, " + dm.getInteractionCount() + " interactions in "+elapsed+"s.");
    }

    @Override
    public void waitIfLoading() {
        if (latch == null) {
            return;
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void load(final String fname, final MSCDataModel dm, boolean batchMode) throws IOException {
        if (batchMode) {
            loadInternal(fname, dm);
        } else {
            latch = new CountDownLatch(1);
            SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    loadInternal(fname, dm);
                    return null;
                }

                @Override
                public void done() {
                    try {
                        get();
                    } catch (ExecutionException e) {
                        String msg = String.format("Unexpected problem: %s",
                                e.getCause().toString());
                        JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                msg, "Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // Process e here
                    }
                    MainFrame.getInstance().setFilename(fname);
                    dm.enableNotification(true);
                    dm.notifyModelChanged();
                    latch.countDown();
                }
            };

//            sw.addPropertyChangeListener(new PropertyChangeListener() {
//                @Override
//                public void propertyChange(PropertyChangeEvent evt) {
//                    if ("progress".equals(evt.getPropertyName())) {
//                        MSC.progress((Integer) evt.getNewValue());
//                    }
//                }
//            });
            sw.execute();
        }
    }

    
    private static Interaction createInteraction(MSCDataModel dm, String pairingId, JSonObject props, Event ev,
            TypeEn type, int index, String fname, int lineNum) throws IOException {
        Interaction inter;
        String t = props.get("type").toString();
        InteractionRenderer irenderer;
        if (t == null) {
            irenderer = new DefaultInteractionRenderer();
        } else {
            String irendererName = t + "Renderer";
            try {
                irenderer = (InteractionRenderer) Class.forName("com.cisco.mscviewer.gui.renderer." + irendererName).newInstance();
            } catch (ClassNotFoundException e) {
                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + irendererName + ".", e);
            } catch (IllegalAccessException e) {
                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + irendererName + ".", e);
            } catch (InstantiationException e) {
                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + irendererName + ".", e);
            }
        }
        if (type == TypeEn.SOURCE) {
            inter = new Interaction(dm, index, -1, irenderer);
        } else {
            inter = new Interaction(dm, -1, index, irenderer);
        }
        irenderer.initialize(inter, props, ev);
        return inter;
    }

    public static int parseProps(String fname, int lineNum, String str, int start, int stop, JSonObject prop) {

        int i;
        while (true) {
            //skip spaces
            for (i = start; i < stop && i < stop && Character.isWhitespace(str.charAt(i)); i++) {
            }
            if (i >= stop) {
                return i;
            }
            if (str.charAt(i) == '@') {
                return i;
            }

            // first key char must be a letter or underscore
            if ((!Character.isLetter(str.charAt(i)) && str.charAt(i) != '_')) {
                throw new IllegalArgumentException(fname + ":" + lineNum + ":character at offset " + start + " should be letter or underscore.");
            }
            StringBuilder bb = new StringBuilder();
            bb.append(str.charAt(i++));
            while (i < stop && Character.isLetterOrDigit(str.charAt(i)) || str.charAt(i) == '_') {
                bb.append(str.charAt(i++));
            }
            String key = bb.toString();
            if (i == stop || str.charAt(i++) != '=') {
                throw new IllegalArgumentException(fname + ":" + lineNum + ":missing '=' after key " + key + " in string " + str.substring(start, stop));
            }
            bb = new StringBuilder();
            if (i == stop || str.charAt(i++) != '"') {
                throw new IllegalArgumentException(fname + ":" + lineNum + ":missing '\"' after '=' in string " + str.substring(start, stop));
            }
            while (i < stop && str.charAt(i) != '"') {
                bb.append(str.charAt(i++));
            }
            if (i == stop || str.charAt(i++) != '"') {
                throw new IllegalArgumentException(fname + ":" + lineNum + ":missing '\"' after value '" + bb.toString() + "' in string " + str.substring(start, stop));
            }
            String value = bb.toString();
            prop.set(key, new JSonStringValue(value));
            start = i + 1;
        }
    }
}
