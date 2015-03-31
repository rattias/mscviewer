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
import com.cisco.mscviewer.tree.Interval;
import com.cisco.mscviewer.util.JSonParser;
import com.cisco.mscviewer.util.ProgressReport;
import com.cisco.mscviewer.util.Resources;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class JsonLoader implements Loader {
    public enum TypeEn {

        SOURCE, SINK, LOCAL
    };
    private static final String MSC_EVENT = "@msc_event";
    private static final String MSC_EVENT1 = "@event";
    private static final String MSC_ENTITY = "@msc_entity";
    private static final String MSC_ENTITY1 = "@entity";
    private static final String MSC_KEY_SRC= "src";
    private static final String MSC_KEY_DST= "dst";
    private static final String MSC_KEY_INTER_ID = "id";
    private static final String MSC_KEY_ENT_ID= "id";
    private static final String MSC_KEY_ENT_NAME= "name";
    @SuppressWarnings("unused")
    private static final String MSC_KEY_INTER_TYPE = "type";
    private static final String MSC_KEY_BLOCK = "block";
    private static final String MSC_KEY_BLOCK_BEGIN = "begin";
    
    private CountDownLatch latch;
    private static SimpleDateFormat formatter[] = {
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS"),
            new SimpleDateFormat("MMM d HH:mm:ss.SSS"),
            new SimpleDateFormat("MMM d HH:mm:ss"),
            new SimpleDateFormat("HH:mm:ss"),
        };
    
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
        String t;
        if  (props != null && props.get("type") != null)
            t = props.get("type").toString();
        else
            t = "DefaultInteraction";
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
        Date d = null;
        for (int i=0; i<formatter.length; i++) {
            try {
                formatter[i].setTimeZone(TimeZone.getDefault());
                d = formatter[i].parse(s);
                if (i > 0) {
                    SimpleDateFormat f = formatter[i];
                    formatter[i] = formatter[0];
                    formatter[0] = f;
                }
                return d;
            }catch(ParseException ex){
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static void loadInternal(String fname, MSCDataModel dm) throws IOException {
        HashMap<String, Interaction> pendingSourced = new HashMap<String, Interaction>();
        HashMap<String, Interaction> pendingSinked = new HashMap<String, Interaction>();
        HashMap<String, Interval> pendingBlocks = new HashMap<String, Interval>();
        HashMap<String, String> alias = new HashMap<String, String>(); 
        File file = new File(fname);
        int flen = (int) file.length();
        dm.setFilePath(fname);
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
                int eventlen = MSC_EVENT.length();
                if (start < 0) {
                    start = line.indexOf(MSC_EVENT1);
                    eventlen = MSC_EVENT1.length();
                }
                if (start >= 0) {
                    start += eventlen + 1;
                    JSonObject jo;
                    try {
                        jo = JSonParser.parseObject(line.substring(start), fname, lineNum);
                    }catch(JSonException ex) {
                        throw new IOException(ex);
                    }
                    String entityPath = jo.get("entity").toString();
                    String dn = alias.get(entityPath);
                    Entity entity = dm.addEntity(entityPath, dn);
                    Entity parentEntity = entity.getParentEntity();

                    JSonValue jlabel = jo.get("label");
                    String label;
                    if (jlabel == null) {
                        label = "";
                    } else
                        label = jlabel.toString();

                    long ts = -1;
                    JSonValue tm = jo.get("time");
                    if (tm != null) {
                        String time = tm.toString();
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
                    JSonValue tt = jo.get("type");
                    EventRenderer renderer = null;
                    if (tt != null) {
                        String t = tt.toString();
                        renderer = Resources.getImageRenderer(t);
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
                    JSonValue pushSourceVal = jo.get("push_source");
                    if (pushSourceVal != null && parentEntity != null) {
                        parentEntity.pushSourceEntityForFromEvents(entity);
                    }
                    JSonValue popSourceVal = jo.get("pop_source");
                    if (popSourceVal != null && parentEntity != null) {
                        parentEntity.popSourceEntityForFromEvents();
                    }
                    entity = entity.getSourceEntityForFromEvents();
                    Event ev = new Event(dm, ts, entity, label, lineNum, renderer, jo);
//                    if (entity.getPath().equals("XRVR")) {
//                        System.out.println("{XRVR}: "+ts);
//                    }
                    int evIdx = dm.addEvent(ev);
                    
                    JSonValue data = jo.get("data");
                    if (data != null) {
                        ev.setData(data);
                    }
                    
                    JSonValue block = jo.get(MSC_KEY_BLOCK);
                    if (block != null) {
                        if (block.toString().equals(MSC_KEY_BLOCK_BEGIN))
                            ev.setBlockBegin();
                        else
                            ev.setBlockEnd();
                    }
                    
                    // HANDLE "source" KEY
                    try {
                        Interaction inter = null;
                        ArrayList<JSonObject> interAttrs = new ArrayList<JSonObject>();
                        ArrayList<String> sourcePairingId = new ArrayList<String>();
                        JSonValue sourceValue = jo.getValueByPath(MSC_KEY_SRC);
                        if (sourceValue instanceof JSonStringValue) {
                            // this event is source for an interaction with default
                            // attributes
                            sourcePairingId.add(sourceValue.toString());
                            interAttrs.add(null);
                        } else if (sourceValue instanceof JSonObject) {
                            // this event is source for an interaction with 
                            // non-default attributes
                            interAttrs.add((JSonObject)sourceValue);                        
                            sourcePairingId.add(((JSonObject)sourceValue).get(MSC_KEY_INTER_ID).toString());
                        } else if (sourceValue instanceof JSonArrayValue) {
                            // this event is source for multiple interactions
                            ArrayList<JSonValue> al = ((JSonArrayValue)sourceValue).value();
                            for(JSonValue j : al) {
                                interAttrs.add((JSonObject)j);                        
                                sourcePairingId.add(((JSonObject)j).get(MSC_KEY_INTER_ID).toString());      
                            }
                        }

                        for(int i=0; i<sourcePairingId.size(); i++) {
                            String id = sourcePairingId.get(i);
                            JSonObject attrs = interAttrs.get(i);
                            
                            inter = pendingSourced.remove(id);
                            if (inter != null) {
                                // there is already a pending source for this pairingId. add it to 
                                //model as orphaned (no sink)
                                dm.addInteraction(inter);
                            }
                            inter = pendingSinked.remove(id);
                            // if there is pending sinked interaction with this pairing Id:
                            // if it is from the same entity, then a (sink, source) sequence
                            // indicates that sink was orphaned, and we should remove it.
                            // if not on same entity, pair and add
                            // fix the source and add it to the model.
                            if (inter != null && inter.getToEvent().getEntity() != ev.getEntity()) {
                                inter.setFromIndex(evIdx);
                                dm.addInteraction(inter);
                            } else {
                                // new interaction. create and add to pendingSourced
                                inter = createInteraction(dm, id, attrs, ev, TypeEn.SOURCE, evIdx, fname, lineNum);
                                pendingSourced.put(id, inter);
                            }
                        }
                    }catch(NoSuchFieldError ex) {                        
                    }
                    // HANDLE "dst" KEY
                    try {
                        Interaction inter = null;
                        ArrayList<JSonObject> interAttrs = new ArrayList<JSonObject>();
                        ArrayList<String> sinkPairingId = new ArrayList<String>();
                        // next line will throw exception if no such field 
                        JSonValue sinkValue = jo.getValueByPath(MSC_KEY_DST);
                        if (sinkValue instanceof JSonStringValue) {
                            // this event is sink for an interaction with default
                            // attributes
                            sinkPairingId.add(((JSonStringValue)sinkValue).toString());
                            interAttrs.add(null);
                        } else if (sinkValue instanceof JSonObject) {
                            // this event is sink for an interaction with 
                            // non-default attributes
                            JSonObject jo1 = (JSonObject)sinkValue;
                            sinkPairingId.add(jo1.get(MSC_KEY_INTER_ID).toString());
                            interAttrs.add((JSonObject)sinkValue);                        
                        } else if (sinkValue instanceof JSonArrayValue) {
                            // this event is sink for multiple interactions
                            for(JSonObject j : (ArrayList<JSonObject>)sinkValue) {
                                sinkPairingId.add(j.get("id").toString());                                
                                interAttrs.add(j);                        
                            }                            
                        }

                        for(int i=0; i<sinkPairingId.size(); i++) {
                            String id = sinkPairingId.get(i);
                            JSonObject attrs = interAttrs.get(i);
                            
                            inter = pendingSinked.remove(id);
                            if (inter != null) {
                                // there is a pending source for this pairingId. add it to 
                                // model as orphaned (no source)
                                dm.addInteraction(inter);
                            }
                            inter = pendingSourced.remove(id);
                            // if there is a pending sourced interaction with this pairing Id, 
                            // fix the sink and add it to the model
                            if (inter != null) {
                                inter.setToIndex(evIdx);
                                dm.addInteraction(inter);
                            } else {
                                // new interaction. create and add to pendingSinked
                                inter = createInteraction(dm, id, attrs, ev, TypeEn.SINK, evIdx, fname, lineNum);
                                pendingSinked.put(id, inter);
                            }
                        }
                    }catch(NoSuchFieldError ex) {
                    }
                } else {
                    start = line.indexOf(MSC_ENTITY);
                    if (start >= 0)
                        start += MSC_ENTITY.length() + 1;
                    if (start < 0) {
                        start = line.indexOf(MSC_ENTITY1);
                        if (start >= 0)
                            start += MSC_ENTITY1.length() + 1;
                    }
                    if (start >= 0) {
                        JSonObject jo;
                        try {
                            jo = JSonParser.parseObject(line.substring(start), fname, lineNum);
                            alias.put(jo.get("id").toString(), jo.get("name").toString());
                        }catch(JSonException ex) {
                            throw new IOException(ex);
                        }
                        String id = jo.get(MSC_KEY_ENT_ID).toString();
                        if (id == null)
                            throw new IllegalArgumentException(fname + ":" + lineNum + ":Missing \"id\" key");

                        String name = jo.get(MSC_KEY_ENT_NAME).toString();
                        if (name == null)
                            throw new IllegalArgumentException(fname + ":" + lineNum + ":Missing \"name\" key");
                        Entity en = dm.getEntity(id);
                        if (en == null) {
                            en = dm.addEntity(id, name);
                        } else {                            
                            en.setName(name);
                        }
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
                if (Main.WITH_BLOCKS) {
                    for(int i=0; i<dm.getEventCount(); i++) {
                        Event ev = dm.getEventAt(i);
                        Entity en = ev.getEntity();
                        String entityPath = en.getPath();
                        String blkPath = entityPath+"/block";
                        SimpleInterval blk = (SimpleInterval)pendingBlocks.get(blkPath);
                        if (ev.getIncomingInteractions().length > 0|| ev.isBlockBegin()) {
                            if (blk != null) {
                                dm.addBlock(blk);
                            }
                            blk = new SimpleInterval(i, i);
                            pendingBlocks.put(entityPath+"/block", blk);
                        } else if (blk != null){
                            blk.setEnd(i);
                        }
                    }
                    for(Interval inter: pendingBlocks.values()) {
                        dm.addBlock(inter);
                    }
                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException(fname + ":" + lineNum + ":error: at this location", ex);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new IOException(fname + ":" + lineNum + ":error: at this location", ex);
        } catch(Exception ex){
            System.err.println("-----------------");
            ex.printStackTrace();
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
