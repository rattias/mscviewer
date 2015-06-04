package com.cisco.mscviewer.io;

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
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;

public class Session {
    private static String EL_MSC_SESSION = "msc-session";
    private static String ATTR_MSC_PATH = "msc-path";
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
        Element e = null;
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

            // open entities
            Element entities = dom.createElement(EL_ENTITIES);
            root.appendChild(entities);
            EntityHeader hd = MainFrame.getInstance().getEntityHeader();
            for(int i=0; i<hd.getComponentCount(); i++) {
                Element entity = dom.createElement(EL_ENTITY);
                entities.appendChild(entity);
                entity.setAttribute(ATTR_ID, hd.getEntity(i).getId());
            }
            // selected event or interaction 
            Event ev = MainFrame.getInstance().getMainPanel().getMSCRenderer().getSelectedEvent();
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
    
    
    public static void loadAsync() {
        if (! SwingUtilities.isEventDispatchThread())
            throw new Error("loadAsync should be called only from EDT");
        if (jfc == null)
            jfc = new JFileChooser(System.getProperty("user.dir"));
        int res = jfc.showOpenDialog(null);
        if (res != JFileChooser.APPROVE_OPTION)
            return;
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
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                } catch (InterruptedException e) {
                    // Process e here
                }
            }
        };
        sw.execute();
    }
   
    public static void load(String sessionFilePath) {
        if (SwingUtilities.isEventDispatchThread())
            throw new Error("load should never be called only from EDT");
        loadInternal(sessionFilePath);
    }
    
    private static void loadInternal(String sessionFilePath) {
        MSCDataModel model = MSCDataModel.getInstance();
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
            if (model != null && !path.equals(model.getFilePath())) {
                int res = JOptionPane.showConfirmDialog(null, "You're loading a session for model file "+path+", while the loaded model is from file "+model.getFilePath()+". This will load the former model. Do you want to continue?");
                if (res != JOptionPane.YES_OPTION) 
                    return;
                MainFrame.getInstance().getViewModel().reset();          
                JsonLoader l = new JsonLoader();
                l.load(path, model, false);
            } else if (model == null) {
                MainFrame.getInstance().getViewModel().reset();          
                JsonLoader l = new JsonLoader();
                l.load(path, model, false);
            } else {
                MainFrame.getInstance().getViewModel().reset();          
            }
            Element entitiesEl = (Element)root.getElementsByTagName(EL_ENTITIES).item(0);
            NodeList entities = root.getElementsByTagName(EL_ENTITY);
            for(int i=0; i<entities.getLength(); i++) {
                Element entityEl = (Element)entities.item(i);
                String id = entityEl.getAttribute(ATTR_ID);
                Main.open(id);
            }
            Element markersEl = (Element)root.getElementsByTagName(EL_MARKERS).item(0);
            NodeList markers = root.getElementsByTagName(EL_MARKER);
            for(int i=0; i<markers.getLength(); i++) {
                Element markerEl = (Element)markers.item(i);
                
                int index = Integer.parseInt(markerEl.getAttribute(ATTR_INDEX));
                String color = markerEl.getChildNodes().item(0).getTextContent();
                model.getEventAt(index).setMarker(Marker.valueOf(color));
            }
            Element notesEl = (Element)root.getElementsByTagName(EL_NOTES).item(0);
            NodeList notes = root.getElementsByTagName(EL_NOTE);
            for(int i=0; i<notes.getLength(); i++) {
                Element noteEl = (Element)notes.item(i);
                int index = Integer.parseInt(noteEl.getAttribute(ATTR_INDEX));
                String html = noteEl.getChildNodes().item(0).getTextContent();
                Event ev = model.getEventAt(index);
                ev.setNote(html);
                ev.setNoteVisible(noteEl.getAttribute(ATTR_VISIBLE).equalsIgnoreCase("yes"));
            }
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
