package com.cisco.mscviewer.io;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.gui.EntityHeader;
import com.cisco.mscviewer.gui.MSCRenderer;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.gui.MainPanel;
import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;

public class Session {
    private static String EL_MSC_SESSION = "msc-session";
    private static String ATTR_MSC_PATH = "msc-path";
    private static String EL_FRAME_BOUNDS = "frame-bounds";
    private static String EL_VIEW_POS = "view-position";
    
    private static String ATTR_X = "x";
    private static String ATTR_Y = "y";
    private static String ATTR_WIDTH = "width";
    private static String ATTR_HEIGHT = "height";
    private static String EL_ENTITIES = "entities";
    private static String EL_ENTITY = "entity";
    private static String ATTR_ID = "id";
    private static String EL_SELECTED_EVENT = "selected-event";
    private static String ATTR_INDEX = "index";
    private static String EL_SELECTED_INTERACTION = "selected-interaction";
    private static String ATTR_FROM_INDEX = "from-index";
    private static String ATTR_TO_INDEX = "to-index";
    private static String EL_MARKERS = "markers";
    private static String EL_MARKER = "marker";
    private static String EL_NOTES = "notes";
    private static String EL_NOTE = "note";
    private static String ATTR_VISIBLE = "visible";
    private static JFileChooser jfc;
    private static String savedSessionPath;
    private static boolean upToDate = true;
    
    public static void save(String sessionFilePath) {
        Document dom;
        MSCDataModel m = MSCDataModel.getInstance();
        
        // instance of a DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use factory to get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // create instance of DOM
            dom = db.newDocument();

            // create the root element
            Element root = dom.createElement(EL_MSC_SESSION);
            root.setAttribute(ATTR_MSC_PATH, m.getFilePath());
            dom.appendChild(root);

            MainPanel mp = MainFrame.getInstance().getMainPanel();
            MSCRenderer renderer = mp.getMSCRenderer(); 

            // frame bounds
            Element fbounds = dom.createElement(EL_FRAME_BOUNDS);
            root.appendChild(fbounds);
            Rectangle r = MainFrame.getInstance().getBounds();
            fbounds.setAttribute(ATTR_X, ""+r.x);
            fbounds.setAttribute(ATTR_Y, ""+r.y);
            fbounds.setAttribute(ATTR_WIDTH, ""+r.width);
            fbounds.setAttribute(ATTR_HEIGHT, ""+r.height);
            Point p = mp.getViewPosition();
            
            // view position
            Element viewPos = dom.createElement(EL_VIEW_POS);
            root.appendChild(viewPos);            
            viewPos.setAttribute(ATTR_X, ""+p.x);
            viewPos.setAttribute(ATTR_Y, ""+p.y);
            
            // open entities
            Element entities = dom.createElement(EL_ENTITIES);
            root.appendChild(entities);
            EntityHeader hd = MainFrame.getInstance().getEntityHeader();
            for(int i=0; i<hd.getEntityCount(); i++) {
                Element entity = dom.createElement(EL_ENTITY);
                entities.appendChild(entity);
                entity.setAttribute(ATTR_ID, hd.getEntity(i).getId());
            }
            // selected event or interaction 
            Event ev = renderer.getSelectedEvent();
            if (ev != null) {
                Element  selectedEvent = dom.createElement(EL_SELECTED_EVENT);
                root.appendChild(selectedEvent);
                selectedEvent.setAttribute(ATTR_INDEX, ""+ev.getIndex());
            }
            Interaction in = MainFrame.getInstance().getMainPanel().getMSCRenderer().getSelectedInteraction();
            if (in != null) {
                Element  selectedInteraction = dom.createElement(EL_SELECTED_INTERACTION);
                root.appendChild(selectedInteraction);
                selectedInteraction.setAttribute(ATTR_FROM_INDEX, ""+in.getFromIndex());
                selectedInteraction.setAttribute(ATTR_TO_INDEX, ""+in.getToIndex());
            }
            // markers
            Element markers = dom.createElement(EL_MARKERS);
            root.appendChild(markers);
            for(int i=0; i<m.getEventCount(); i++) {
                ev = m.getEventAt(i);
                Marker mark = ev.getMarker();
                if (mark != null) {
                    Element marker = dom.createElement(EL_MARKER);
                    markers.appendChild(marker);
                    marker.setAttribute(ATTR_INDEX, ""+i);
                    marker.appendChild(dom.createTextNode(mark.toString()));
                }
            }
            // notes
            Element notes = dom.createElement(EL_NOTES);
            root.appendChild(notes);
            for(int i=0; i<m.getEventCount(); i++) {
                ev = m.getEventAt(i);
                String note = ev.getNote();
                if (note != null) {
                    Element n = dom.createElement(EL_NOTE);
                    n.setAttribute(ATTR_INDEX, ""+i);
                    n.setAttribute(ATTR_VISIBLE, ev.noteIsVisible() ? "yes" : "no");
                    notes.appendChild(n);
                    n.appendChild(dom.createTextNode(note));
                }
            }
            try {
                Transformer tr = TransformerFactory.newInstance().newTransformer();
                tr.setOutputProperty(OutputKeys.INDENT, "yes");
                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "msc-session.dtd");
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                // send DOM to file
                tr.transform(new DOMSource(dom), 
                        new StreamResult(new FileOutputStream(sessionFilePath)));

            } catch (TransformerException te) {
                System.out.println(te.getMessage());
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
        } catch (ParserConfigurationException pce) {
            System.out.println("UsersXML: Error trying to instantiate DocumentBuilder " + pce);
        }
        setUpToDate(true);
    }
    
    
    public static String loadAsync() {
        if (! SwingUtilities.isEventDispatchThread())
            throw new Error("loadAsync should be called only from EDT");
        if (jfc == null)
            jfc = new JFileChooser(System.getProperty("user.dir"));
        int res = jfc.showOpenDialog(null);
        if (res != JFileChooser.APPROVE_OPTION)
            return null;
        String sessionFilePath = jfc.getSelectedFile().getPath();
        final SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                loadInternal(sessionFilePath);
                return null;
            }

            @Override
            public void done() {
                try {
                    get();
                    MainFrame.getInstance().addRecentSession(sessionFilePath);
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                } catch (InterruptedException e) {
                    // Process e here
                }
            }
        };
        sw.execute();
        return sessionFilePath;
    }
   
    public static void load(String sessionFilePath) {
        if (SwingUtilities.isEventDispatchThread())
            throw new Error("load should never be called only from EDT");
        loadInternal(sessionFilePath);
    }
    
    private static void loadInternal(String sessionFilePath) {
        MSCDataModel model = MSCDataModel.getInstance();
        MainFrame mf = MainFrame.getInstance();
        MainPanel mp = mf.getMainPanel();
        MSCRenderer renderer = mp.getMSCRenderer();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // use the factory to take an instance of the document builder
        Document dom = null;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException {
                    if (systemId.contains("msc-session.dtd")) {
                        InputStream is = Session.class.getResourceAsStream("msc-session.dtd");
                        return new InputSource(is);
                    } else {
                        return null;
                    }
                }
            });
        // parse using the builder to get the DOM mapping of the    
        // XML file
            String uri = new File(sessionFilePath).toURI().toASCIIString();
            dom = db.parse(uri);
            Element root = dom.getDocumentElement();
            root.normalize();
            String path = root.getAttribute(ATTR_MSC_PATH);
            if (model != null && model.getFilePath() != null && !path.equals(model.getFilePath())) {
                int res = JOptionPane.showConfirmDialog(null, "You're loading a session for log file "+path+", while the loaded model is from file "+model.getFilePath()+". This will load the former log file. Do you want to continue?");
                if (res != JOptionPane.YES_OPTION) 
                    return;
                mf.getViewModel().reset();          
                JsonLoader l = new JsonLoader();
                l.load(path, model, false);
            } else if (model == null || model.getFilePath() == null) {
                mf.getViewModel().reset();          
                JsonLoader l = new JsonLoader();
                l.load(path, model, false);
            } else {
                mf.getViewModel().reset();          
            }
           
            Element entitiesEl = (Element)root.getElementsByTagName(EL_ENTITIES).item(0);
            NodeList entities = entitiesEl.getElementsByTagName(EL_ENTITY);
            for(int i=0; i<entities.getLength(); i++) {
                Element entityEl = (Element)entities.item(i);
                String id = entityEl.getAttribute(ATTR_ID);
                Main.open(id);
            }
            Element markersEl = (Element)root.getElementsByTagName(EL_MARKERS).item(0);
            NodeList markers = markersEl.getElementsByTagName(EL_MARKER);
            for(int i=0; i<markers.getLength(); i++) {
                Element markerEl = (Element)markers.item(i);
                
                int index = Integer.parseInt(markerEl.getAttribute(ATTR_INDEX));
                String color = markerEl.getChildNodes().item(0).getTextContent();
                model.getEventAt(index).setMarker(Marker.valueOf(color));
            }
            Element notesEl = (Element)root.getElementsByTagName(EL_NOTES).item(0);
            NodeList notes = notesEl.getElementsByTagName(EL_NOTE);
            for(int i=0; i<notes.getLength(); i++) {
                Element noteEl = (Element)notes.item(i);
                int index = Integer.parseInt(noteEl.getAttribute(ATTR_INDEX));
                String html = noteEl.getChildNodes().item(0).getTextContent();
                Event ev = model.getEventAt(index);
                ev.setNote(html);
                ev.setNoteVisible(noteEl.getAttribute(ATTR_VISIBLE).equalsIgnoreCase("yes"));
            }
 
            Element frameEl = (Element)root.getElementsByTagName(EL_FRAME_BOUNDS).item(0);
            int x = Integer.parseInt(frameEl.getAttribute(ATTR_X));
            int y = Integer.parseInt(frameEl.getAttribute(ATTR_Y));
            int w = Integer.parseInt(frameEl.getAttribute(ATTR_WIDTH));
            int h = Integer.parseInt(frameEl.getAttribute(ATTR_HEIGHT));
            mf.setBounds(x,  y,  w, h);


            // selected event or interaction 
            NodeList selectedEventEls = root.getElementsByTagName(EL_SELECTED_EVENT);
            if (selectedEventEls.getLength() > 0) {
                Element selectedEventEl = (Element)selectedEventEls.item(0);
                int idx = Integer.parseInt(selectedEventEl.getAttribute(ATTR_INDEX));
                renderer.setSelectedEventByModelIndex(idx);
            }
            NodeList selectedIntEls = root.getElementsByTagName(EL_SELECTED_INTERACTION);
            if (selectedIntEls.getLength() > 0) {
                Element selectedIntEl = (Element)selectedIntEls.item(0);
                int fromIdx = Integer.parseInt(selectedIntEl.getAttribute(ATTR_FROM_INDEX));
                int toIdx = Integer.parseInt(selectedIntEl.getAttribute(ATTR_TO_INDEX));
                if (fromIdx >= 0) {
                    Event ev = model.getEventAt(fromIdx);
                    for(Interaction inter : ev.getOutgoingInteractions()) {
                        if (inter.getToIndex() == toIdx) {
                            renderer.setSelectedInteraction(inter);
                            break;
                        }
                    }
                }
            }

            Element viewPos = (Element)root.getElementsByTagName(EL_VIEW_POS).item(0);
            x = Integer.parseInt(viewPos.getAttribute(ATTR_X));
            y = Integer.parseInt(viewPos.getAttribute(ATTR_Y));
            mp.setViewPosition(x, y);
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void saveAs() {
        if (! SwingUtilities.isEventDispatchThread())
            throw new Error("saveAs should be called only from EDT");
        if (jfc == null)
            jfc = new JFileChooser(System.getProperty("user.dir"));
        int res = jfc.showSaveDialog(null);
        if (res != JFileChooser.APPROVE_OPTION)
            return;
        File f = jfc.getSelectedFile();
        save(f.getPath());
    }
    
    public static void save() {
        if (savedSessionPath == null) {
            saveAs();
        } else {
            save(savedSessionPath);
        }
    }
    
    public static void setUpToDate(boolean v) {
        upToDate = v;
    }
    
    public static boolean isUpToDate() {
        return upToDate;
    }
}
