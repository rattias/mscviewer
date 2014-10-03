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
import com.cisco.mscviewer.gui.renderer.*;
import com.cisco.mscviewer.model.*;
import com.cisco.mscviewer.util.JSonParser;
import com.cisco.mscviewer.util.ProgressReport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class JsonSyslogLoader implements Loader {
    public enum TypeEn {

        SOURCE, SINK, LOCAL
    };
    private static final String MSC_EVENT = "@msc_event";

    private CountDownLatch latch;

    private static boolean isIdentifierStart(String str, int pos) {
        char c = str.charAt(pos);
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(String str, int pos) {
        char c = str.charAt(pos);
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int skipSpaces(String str, int start, String fname, int lineNum) {
        int i;
        for (i = start; Character.isWhitespace(str.charAt(i)); i++)
            ;
        return i;
    }

    private static int expect(String str, int pos, char expected, String fname, int lineNum) {
        pos = skipSpaces(str, pos, fname, lineNum);
        if (str.charAt(pos) != expected) {
            throw new IllegalArgumentException(fname + ":" + lineNum + ":" + pos + ":Expecting " + expected + ", found '" + str.charAt(pos) + "'.");
        }
        return pos + 1;
    }

    
    @SuppressWarnings("unused")
    private static Properties parseProps(String fname, int lineNum, String str, int pos, int stop) {
        int start;
        String expected = "{";
        try {
            pos = expect(str, pos, '{', fname, lineNum) + 1;
            Properties prop = new Properties();
            while (true) {
                expected = "letter or underscore";
                pos = skipSpaces(str, pos, fname, lineNum);
                if (!isIdentifierStart(str, pos)) {
                    throw new IllegalArgumentException(fname + ":" + lineNum + ":" + pos + ":Expected " + expected + ", found '" + str.charAt(pos) + "'.");
                }
                start = pos;
                // PARSE KEY
                pos++;
                while (isIdentifierPart(str, pos)) {
                    pos++;
                }
                String key = str.substring(start, pos);
                pos = expect(str, pos, ':', fname, lineNum);
                pos = expect(str, pos, '"', fname, lineNum);
                // PARSE DOUBLE-QUOTE-ENCLOSED VALUE
                StringBuilder sb = new StringBuilder();
                while (true) {
                    char c = str.charAt(pos);
                    pos++;
                    if (c == '"') {
                        char cc = str.charAt(pos - 1);
                        if (cc == '\\') {
                            sb.setCharAt(sb.length() - 1, c);
                        } else {
                            break;
                        }
                    } else {
                        sb.append(c);
                    }
                }
                prop.put(key, sb.toString());
                expected = "',' or '}'";
                skipSpaces(str, pos, fname, lineNum);
                if (str.charAt(pos) == '}') {
                    return prop;
                }
                if (str.charAt(pos) == ',') {
                    pos++;
                } else {
                    throw new IllegalArgumentException(fname + ":" + lineNum + ":" + pos + ":Expecting " + expected + ", found end-of-line.");
                }
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(fname + ":" + lineNum + ":" + pos + ":Expecting " + expected + ", found end-of-line.");
        }
    }

    private static Interaction createInteraction(MSCDataModel dm, String pairingId, JSonObject props, Event ev,
            TypeEn type, int index, String fname, int lineNum) throws IOException {
        Interaction inter;
        String t = (props != null) ? (String) props.get("type") : "DefaultInteraction";
        InteractionRenderer irenderer;
        if (t == null) {
            irenderer = new DefaultInteractionRenderer();
        } else {
            String irendererName = t + "Renderer";
            try {
                irenderer = (InteractionRenderer) Class.forName("com.cisco.mscviewer.gui.renderer." + irendererName).newInstance();
            } catch (ClassNotFoundException e) {
                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + irendererName + ".", e);
            } catch (InstantiationException e) {
                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + irendererName + ".", e);
            } catch (IllegalAccessException e) {
                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + irendererName + ".", e);
            }
        }
        if (type == TypeEn.SOURCE) {
            inter = new Interaction(dm, index, -1, irenderer);
            //ev.addOutgoingInteraction(inter);
        } else {
            inter = new Interaction(dm, -1, index, irenderer);
            //ev.setIncomingInteraction(inter);
        }
        irenderer.initialize(inter, props, ev);
        return inter;
    }

    private static Date parseDate(String s) {
        String formats[] = {
            "MMM d HH:mm:ss.SSS",
            "MMM d HH:mm:ss",
            "HH:mm:ss",
        };
        Date d = null;
        for (String f: formats) {
            SimpleDateFormat formatter = new SimpleDateFormat(f);
            try {
                d = formatter.parse(s);
                return d;
            }catch(ParseException ex){
            }
        }
        return null;
    }
    
    private static void loadInternal(String fname, MSCDataModel dm) throws IOException {
        HashMap<String, Interaction> pendingSourced = new HashMap<String, Interaction>();
        HashMap<String, Interaction> pendingSinked = new HashMap<String, Interaction>();
        File file = new File(fname);
        int flen = (int) file.length();
        dm.enableNotification(false);
        BufferedReader fr = new BufferedReader(new FileReader(file));
        String line;
        int lineNum = 0;
        int readCnt = 0;
        ProgressReport pr = new ProgressReport("Loading file", fname, 0, flen-1);
        try {
            int x = 0;
            while ((line = fr.readLine()) != null) {
                readCnt += line.length();
                x++;
                if (x % 1024 == 0) {
                    pr.progress(readCnt);
                }
                lineNum++;
                int start = line.indexOf(MSC_EVENT);
                if (start >= 0) {
                    start += MSC_EVENT.length() + 1;
                    JSonObject jo;
                    try {
                        jo = JSonParser.parseObject(line.substring(start), fname, lineNum);
                    }catch(JSonException ex) {
                        throw new IOException(ex);
                    }
                    String entityPath = (String)jo.get("entity");
                    Entity entity = dm.addEntity(entityPath, null);
                    Entity parentEntity = entity.getParentEntity();

                    String label = (String)jo.get("label");
                    if (label == null) {
                        label = "";
                    }

                    long ts = -1;
                    String time = (String)jo.get("time");
                    if (time != null) {
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
                                        ts = Long.parseLong(time.substring(0, len - 1)) * 1000000000;
                                    } else {
                                        throw new IOException(fname + ":" + lineNum + ":Invalid time unit specifier");
                                    }
                            }
                        } else {
                            try {
                                ts = Long.parseLong(time);
                            }catch(NumberFormatException ex) {
                                Date d = parseDate(time);
                                if (d != null) {
                                    ts = d.getTime()*1000000;                                
                                    System.out.println("d = "+d+", time = "+time+", ts = "+ts);
                                }
                            }
                        }
                    } else {
                        // parse date from syslog timestamp
                        Date d = parseDate(line);
                        if (d != null)
                            ts = d.getTime()*1000000;
                    }
                    String t = (String)jo.get("type");
                    EventRenderer renderer = null;
                    if (t != null) {
                        renderer = ImageRenderer.get(t);
                        if (renderer == null) {
                            Class<?> c = null;
                            String rendererName = "com.cisco.mscviewer.gui.renderer." + t + "Renderer";
                            try {
                                c = Class.forName(rendererName);
                            } catch (ClassNotFoundException e) {
                                System.err.println(fname + ":" + lineNum + ": Neither an image renderer "+t+", nor a class "+rendererName+" was found.");
                                rendererName = "com.cisco.mscviewer.gui.renderer.DefaultEventRenderer";
                                try {
                                    c = Class.forName(rendererName);
                                } catch (ClassNotFoundException e1) {
                                    throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + rendererName + ".", e);
                                }
                            }
                            try {
                                renderer = (EventRenderer) c.newInstance();
                            } catch (InstantiationException e) {
                                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + rendererName + ".", e);
                            } catch (IllegalAccessException e) {
                                throw new IOException(fname + ":" + lineNum + ":Unable to instantiate class " + rendererName + ".", e);
                            }
                        }
                    }
                    String pushSourceVal = (String)jo.get("push_source");
                    if (pushSourceVal != null && parentEntity != null) {
                        parentEntity.pushSourceEntityForFromEvents(entity);
                    }
                    String popSourceVal = (String)jo.get("pop_source");
                    if (popSourceVal != null && parentEntity != null) {
                        parentEntity.popSourceEntityForFromEvents();
                    }
                    entity = entity.getSourceEntityForFromEvents();
                    Event ev = new Event(dm, ts, entity, label, lineNum, renderer, jo);
                    int evIndex = dm.addEvent(ev);

                    JSonObject data = (JSonObject)jo.get("data");
                    if (data != null) {
                        ev.setData(data);
                    }
                    String begb = (String)jo.get("begin");
                    if (begb != null) {
                        JSonObject ps = new JSonObject();
                        ps.set("type", "BlockInteraction");
                        ps.set("color", "000000");
                        String pairingId = entity.getId() + "/block";
                        Interaction inter = createInteraction(dm, pairingId, ps, ev, TypeEn.SOURCE, dm.getEventCount(), fname, lineNum);
                        pendingSourced.put(pairingId, inter);
                    }
                    String endb = (String)jo.get("end");
                    if (endb != null) {
                        Interaction inter = pendingSourced.get(entity.getId() + "/block");
                        if (inter == null) {
                            JSonObject ps = new JSonObject();
                            ps.set("type", "BlockInteraction");
                            ps.set("color", "000000");
                            String pairingId = entity.getId() + "/block";
                            createInteraction(dm, pairingId, ps, ev, TypeEn.SINK, dm.getEventCount(), fname, lineNum);
                        } else {
                            inter.setToIndex(evIndex);
                            String pairingId = entity.getId() + "/block";
                            pendingSourced.remove(pairingId);
                        }
                    }
                    int evIdx = dm.getEventCount()-1;
                    // HANDLE "source" KEY
                    try {
                        Interaction inter = null;
                        JSonObject interAttrs = null;
                        Object sourceValue = jo.getValueByPath("source");
                        String sourcePairingId = null;
                        if (sourceValue instanceof String) {
                            // this event is source for an interaction with default
                            // attributes
                            sourcePairingId = (String)sourceValue;
                            interAttrs = null;
                        } else if (sourceValue instanceof JSonObject) {
                            // this event is source for an interaction with 
                            // non-default attributes
                            interAttrs = (JSonObject)sourceValue;                        
                            sourcePairingId = (String)interAttrs.get("pairing");
                        }

                        if (sourcePairingId != null) {
                            inter = pendingSourced.remove(sourcePairingId);
                            if (inter != null) {
                                // there is already a pending source for this pairingId. add it to 
                                // model as orphaned (no sink)
                                dm.addInteraction(inter);
                            }
                            inter = pendingSinked.remove(sourcePairingId);
                            // if there is pending sinked interaction with this pairing Id, 
                            // fix the source and add it to the model.
                            if (inter != null) {
                                inter.setFromIndex(evIdx);
                                dm.addInteraction(inter);
                            } else {
                                // new interaction. create and add to pendingSourced
                                inter = createInteraction(dm, sourcePairingId, interAttrs, ev, TypeEn.SOURCE, evIdx, fname, lineNum);
                                pendingSourced.put(sourcePairingId, inter);
                            }
                        }
                    }catch(NoSuchFieldError ex) {                        
                    }
                    // HANDLE "sink" KEY
                    try {
                        Interaction inter = null;
                        JSonObject interAttrs = null;
                        Object sinkValue = jo.getValueByPath("sink");
                        String sinkPairingId = null;
                        if (sinkValue instanceof String) {
                            // this event is sink for an interaction with default
                            // attributes
                            sinkPairingId = (String)sinkValue;
                        } else if (sinkValue instanceof JSonObject) {
                            // this event is sink for an interaction with 
                            // non-default attributes
                            interAttrs = (JSonObject)sinkValue;                        
                            sinkPairingId = (String)interAttrs.get("pairing");
                        }

                        if (sinkPairingId != null) {
                            inter = pendingSinked.remove(sinkPairingId);
                            if (inter != null) {
                                // there is a pending source for this pairingId. add it to 
                                // model as orphaned (no source)
                                dm.addInteraction(inter);
                            }
                            inter = pendingSourced.remove(sinkPairingId);
                            // if there is a pending sourced interaction with this pairing Id, 
                            // fix the sink and add it to the model
                            if (inter != null) {
                                inter.setToIndex(evIdx);
                                dm.addInteraction(inter);
                            } else {
                                // new interaction. create and add to pendingSinked
                                inter = createInteraction(dm, sinkPairingId, interAttrs, ev, TypeEn.SINK, evIdx, fname, lineNum);
                                pendingSinked.put(sinkPairingId, inter);
                            }
                        }
                    }catch(NoSuchFieldError ex) {
                    }
                }
                dm.addDataLine(line);
            }
            if (dm != null) {
                // add all remaining pending
                for(Interaction inter: pendingSourced.values()) {
                    dm.addInteraction(inter);
                }
                for(Interaction inter: pendingSinked.values()) {
                    dm.addInteraction(inter);
                }
                // sort topologically
                dm.topoSort();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException(fname + ":" + lineNum + ":error: at this location", ex);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new IOException(fname + ":" + lineNum + ":error: at this location", ex);
        } finally {
            pr.progressDone();
            fr.close();
        }
    }
    
    @Override
    public void waitIfLoading() {
        if (latch == null) {
            return;
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void load(final String fname, final MSCDataModel dm) throws IOException {
        dm.setOpenPath(new File(fname).getParent());
        if (Main.batchMode()) {
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
                        e.getCause().printStackTrace();
                        String msg = String.format("Unexpected problem: %s",
                                e.getCause().toString());
                        JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                msg, "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (InterruptedException e) {
                        // Process e here
                    }
                    MainFrame.getInstance().setFilename(fname);
                    dm.enableNotification(true);
                    dm.notifyModelChanged();
                    latch.countDown();
                }
            };

            sw.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("progress".equals(evt.getPropertyName())) {
                        //cMain.progress((Integer) evt.getNewValue());
                    }
                }
            });
            sw.execute();
        }
    }

}
