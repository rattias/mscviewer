package com.cisco.mscviewer.util;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Vector;

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
import org.xml.sax.SAXException;

import com.cisco.mscviewer.model.InputUnit;
import com.cisco.mscviewer.model.OutputUnit;

public class PersistentPrefs {
    private static final String EL_INPUT_UNIT = "input-unit";
    private static final String ATTR_FORMAT = "format";
    private static final String EL_MSC_PREFS = "msc-prefs";
    private static final String EL_RECENT_FILES = "recent-files";
    private static final String EL_FILE = "file";
    private static final String EL_RECENT_SESSIONS = "recent-sessions";
    private static final String EL_SESSION = "session";
    private static final String ATTR_PATH = "path";
    private static final String EL_OUTPUT_UNIT= "output-unit";
    private static final String ATTR_DATE_FORMAT = "date-format";
    private static final String ATTR_TIME_FORMAT = "time-format";
    private static final String ATTR_OUTPUT_FLAGS = "flags";
    private static final String EL_ENTITY_FORMAT = "entity-format";
    private static final String ATTR_ENTITY_AS_ID = "show-id";
    private static final String ATTR_ENTITY_FULL_PATH = "show-full-path";
    private static final String ATTR_ENTITY_DESCR = "show-description";
    private static final String EL_COLORS = "colors";
    private static final String ATTR_COLOR_EVENT_BG_ODD = "event-bg-odd";
    private static final String ATTR_COLOR_EVENT_BG_EVEN = "event-bg-odd";
    private static final String ATTR_COLOR_EVENT_LABEL = "event-label";
    private static final String ATTR_COLOR_EVENT_TIMESTAMP = "event-timestamp";
    private static final String ATTR_COLOR_INTERACTION = "interaction";
    private static final String ATTR_COLOR_LOGFILE_FG = "logfile-fg";
    private static final String ATTR_COLOR_LOGFILE_BG = "logfile-bg";
    private static final String ATTR_COLOR_LOGFILE_SELECTED_BG = "logfile-selected-bg";
    private static final String ATTR_COLOR_LOGFILE_COPY = "logfile-copy";
    private static final String ATTR_COLOR_LOGFILE_LINENUM_BG = "logfile-linenum-bg";
    private static final String ATTR_COLOR_LOGFILE_LINENUM_FG = "logfile-linenum-fg";
            
    private static PersistentPrefs instance = new PersistentPrefs();

    private ArrayList<String> recentFiles;
    private ArrayList<String> recentSessions;
    
    private InputUnit inputUnit;
    private boolean showUnits;
    private OutputUnit outputUnit;

    private boolean showEntityFullPath;
    private boolean showEntityAsID;
    private boolean showEntityDescription;

    private Color lifeLineColor;
    private Color oddEventBackgroundColor;
    private Color evenEventBackgroundColor;
    private Color labelColor;
    private Color defaultInteractionColor;
    private Color timestampColor;

    private Color logFileForegroundColor;
    private Color logFileBackgroundColor;
    private Color logFileLineNumberBackgroundColor;
    private Color logFileLineNumberForegroundColor;
    private Color logFileSelectedBackgroundColor;
    private Color logFileCopyPasteSelectionColor;

    private Vector<PersistentPrefsListener> listeners = new Vector<PersistentPrefsListener>();
    //private Prefs savedState;


    public static PersistentPrefs getInstance() {
        return instance;
    }


    public PersistentPrefs() {
        reset();
        // if there is no prefs file already saved,
        // then we should save the initial one, otherwise
        // doing a reset or cancel from the PrefsDialog won't 
        // have the desired effect
        if (!restore())
            persist();
    }
    
    public void reset() {
        recentFiles = new ArrayList<String>();
        recentSessions = new ArrayList<String>();    
        inputUnit = InputUnit.NS;
        outputUnit = new OutputUnit(OutputUnit.DateMode.NO_DATE, OutputUnit.TimeMode.H_M_S_MS, 0);
        
        showEntityFullPath = true;
        showEntityAsID = false;
        showEntityDescription = false;
        
        lifeLineColor = Color.lightGray;
        evenEventBackgroundColor = Color.white;
        oddEventBackgroundColor = new Color(248, 248, 248);
        labelColor = Color.black;
        timestampColor = Color.pink;
        defaultInteractionColor = Color.blue;
        logFileForegroundColor = Color.black;        
        logFileBackgroundColor = Color.white;
        logFileSelectedBackgroundColor = Color.yellow;
        logFileCopyPasteSelectionColor = Color.green;
        logFileLineNumberBackgroundColor = Color.lightGray;
        logFileLineNumberForegroundColor = Color.black;        
    }

    
//    public void saveState() {
//        savedState = new Prefs();
//        for(Field f: Prefs.class.getDeclaredFields()) {
//            f.setAccessible(true);
//            try {
//                f.set(savedState, f.get(this));
//            } catch (Exception e) {
//                throw new Error(e);
//            } 
//        }
//    }
//
//    public void restoreState() { 
//        for(Field f: Prefs.class.getDeclaredFields()) {
//            f.setAccessible(true);
//            try {
//                f.set(this, f.get(savedState));
//            } catch (Exception e) {
//                throw new Error(e);
//            } 
//        }
//        notifyListeners();
//    }
//    
//
    public boolean restore() {
        String path = getPrefsFile();
        if (! (new File(path).exists()))
            return false;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // use the factory to take an instance of the document builder
        Document dom = null;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the    
            // XML file
            String uri = new File(path).toURI().toASCIIString();
            dom = db.parse(uri);
            Element root = dom.getDocumentElement();
            root.normalize();               

            // recent files
            NodeList recentFilesList = root.getElementsByTagName(EL_RECENT_FILES);
            if (recentFilesList.getLength() > 0) {
                recentFiles.clear();
                Element recentFilesEl  = (Element)recentFilesList.item(0);
                NodeList recentFilesNL = recentFilesEl.getElementsByTagName(EL_FILE);
                for(int i=0; i<recentFilesNL.getLength(); i++) {
                    Element fileEl = (Element)recentFilesNL.item(i);
                    String p = fileEl.getAttribute(ATTR_PATH);
                    recentFiles.add(p);                        
                }
            }

            // recent sessions
            NodeList recentSessionsList = root.getElementsByTagName(EL_RECENT_SESSIONS);
            if (recentSessionsList.getLength() > 0) {
                recentSessions.clear();
                Element recentSessionsEl  = (Element)recentSessionsList.item(0);
                NodeList recentSessionsNL = recentSessionsEl.getElementsByTagName(EL_SESSION);
                for(int i=0; i<recentSessionsNL.getLength(); i++) {
                    Element sessionEl = (Element)recentSessionsNL.item(i);
                    String p = sessionEl.getAttribute(ATTR_PATH);
                    recentSessions.add(p);                        
                }
            }

            NodeList list = root.getElementsByTagName(EL_INPUT_UNIT);
            if (list.getLength() > 0) {
                Element el = (Element)list.item(0);
                String unit = el.getAttribute(ATTR_FORMAT);
                inputUnit = InputUnit.byName(unit);
            }                
            list = root.getElementsByTagName(EL_OUTPUT_UNIT);
            if (list.getLength() > 0) {
                Element el = (Element)list.item(0);
                String fmt = el.getAttribute(ATTR_DATE_FORMAT);
                outputUnit.setDateMode(OutputUnit.DateMode.byName(fmt));
                fmt = el.getAttribute(ATTR_TIME_FORMAT);
                outputUnit.setTimeMode(OutputUnit.TimeMode.byName(fmt));
                String flags = el.getAttribute(ATTR_OUTPUT_FLAGS);
                outputUnit.setFlags(Integer.parseInt(flags));
            }                
            list = root.getElementsByTagName(EL_ENTITY_FORMAT);
            if (list.getLength() > 0) {
                Element el = (Element)list.item(0);
                String v = el.getAttribute(ATTR_ENTITY_AS_ID);
                showEntityAsID = "true".equals(v);
                v = el.getAttribute(ATTR_ENTITY_FULL_PATH);
                showEntityFullPath = "true".equals(v);
                v = el.getAttribute(ATTR_ENTITY_DESCR);
                showEntityDescription = "true".equals(v);                                
            }
            list = root.getElementsByTagName(EL_COLORS);
            if (list.getLength() > 0) {
                Element el = (Element)list.item(0);
                String v = el.getAttribute(ATTR_COLOR_EVENT_BG_ODD);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    oddEventBackgroundColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_EVENT_BG_EVEN);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    evenEventBackgroundColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_EVENT_LABEL);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    labelColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_EVENT_TIMESTAMP);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    timestampColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_INTERACTION);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    defaultInteractionColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_LOGFILE_FG);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    logFileForegroundColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_LOGFILE_BG);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    logFileBackgroundColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_LOGFILE_SELECTED_BG);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    logFileSelectedBackgroundColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_LOGFILE_COPY);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    logFileCopyPasteSelectionColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_LOGFILE_LINENUM_BG);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    logFileLineNumberBackgroundColor = new Color(rgb);
                }
                v = el.getAttribute(ATTR_COLOR_LOGFILE_LINENUM_FG);
                if (v != null) {
                    int rgb = Integer.parseInt(v, 16);
                    logFileLineNumberForegroundColor = new Color(rgb);
                }
            }
        } catch (Exception e) {
            Report.exception("Exception while loading preference file '"+path+"':", e);
        }
        return true;
    }

    public void persist() {
        Document dom;
        String path = getPrefsFile();

        // instance of a DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use factory to get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // create instance of DOM
            dom = db.newDocument();

            // create the root element
            Element root = dom.createElement(EL_MSC_PREFS);
            dom.appendChild(root);

            if (recentFiles.size() > 0) {
                Element recentFilesEl = dom.createElement(EL_RECENT_FILES);
                root.appendChild(recentFilesEl);
                for(int i=0; i<recentFiles.size(); i++) {
                    Element file = dom.createElement(EL_FILE);
                    file.setAttribute(ATTR_PATH, recentFiles.get(i));
                    recentFilesEl.appendChild(file);                    
                }
            }            
            if (recentSessions.size() > 0) {
                Element recentSessionsEl = dom.createElement(EL_RECENT_SESSIONS);
                root.appendChild(recentSessionsEl);
                for(int i=0; i<recentSessions.size(); i++) {
                    Element file = dom.createElement(EL_SESSION);
                    file.setAttribute(ATTR_PATH, recentSessions.get(i));
                    recentSessionsEl.appendChild(file);                    
                }
            } 
            Element el = dom.createElement(EL_INPUT_UNIT);
            el.setAttribute(ATTR_FORMAT, inputUnit.toString());
            root.appendChild(el);

            el = dom.createElement(EL_OUTPUT_UNIT);
            el.setAttribute(ATTR_DATE_FORMAT, outputUnit.getDateMode().toString());
            el.setAttribute(ATTR_TIME_FORMAT, outputUnit.getTimeMode().toString());
            el.setAttribute(ATTR_OUTPUT_FLAGS, Integer.toHexString(outputUnit.getFlags()));
            root.appendChild(el);
            
            el = dom.createElement(EL_ENTITY_FORMAT);
            el.setAttribute(ATTR_ENTITY_FULL_PATH, ""+showEntityFullPath);
            el.setAttribute(ATTR_ENTITY_AS_ID, ""+showEntityAsID);
            el.setAttribute(ATTR_ENTITY_DESCR, ""+showEntityDescription);
            root.appendChild(el);
            
            el = dom.createElement(EL_COLORS);
            el.setAttribute(ATTR_COLOR_EVENT_BG_EVEN, Integer.toHexString(0xFFFFFF & evenEventBackgroundColor.getRGB()));
            el.setAttribute(ATTR_COLOR_EVENT_BG_ODD, Integer.toHexString(0xFFFFFF & oddEventBackgroundColor.getRGB()));
            el.setAttribute(ATTR_COLOR_EVENT_LABEL, Integer.toHexString(0xFFFFFF & labelColor.getRGB()));
            el.setAttribute(ATTR_COLOR_EVENT_TIMESTAMP, Integer.toHexString(0xFFFFFF & timestampColor.getRGB()));
            el.setAttribute(ATTR_COLOR_INTERACTION, Integer.toHexString(0xFFFFFF & defaultInteractionColor.getRGB()));
            el.setAttribute(ATTR_COLOR_LOGFILE_BG, Integer.toHexString(0xFFFFFF & logFileBackgroundColor.getRGB()));
            el.setAttribute(ATTR_COLOR_LOGFILE_COPY, Integer.toHexString(0xFFFFFF & logFileCopyPasteSelectionColor.getRGB()));
            el.setAttribute(ATTR_COLOR_LOGFILE_FG, Integer.toHexString(0xFFFFFF & logFileForegroundColor.getRGB()));
            el.setAttribute(ATTR_COLOR_LOGFILE_LINENUM_BG, Integer.toHexString(0xFFFFFF & logFileLineNumberBackgroundColor.getRGB()));
            el.setAttribute(ATTR_COLOR_LOGFILE_LINENUM_FG, Integer.toHexString(0xFFFFFF & logFileLineNumberForegroundColor.getRGB()));
            el.setAttribute(ATTR_COLOR_LOGFILE_SELECTED_BG, Integer.toHexString(0xFFFFFF & logFileSelectedBackgroundColor.getRGB()));
            root.appendChild(el);
            
            try {
                Transformer tr = TransformerFactory.newInstance().newTransformer();
                tr.setOutputProperty(OutputKeys.INDENT, "yes");
                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                //                tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "msc-session.dtd");
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                // send DOM to file
                tr.transform(new DOMSource(dom), 
                        new StreamResult(new FileOutputStream(path)));

            } catch (TransformerException te) {
                System.out.println(te.getMessage());
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }


        } catch (ParserConfigurationException pce) {
            System.out.println("UsersXML: Error trying to instantiate DocumentBuilder " + pce);
        }
    }


    private String getPrefsFile() {
        return Utils.getWorkDirPath()+"/prefs.xml"; 
    }


    private boolean addRecent(ArrayList<String> recents, String path) {
        if (recents.size() > 0 && recents.get(0).equals(path))
            return false;
        for(int i=0; i<recents.size(); i++) {
            if (recents.get(i).equals(path)) {
                recents.remove(i);
                recents.add(0, path);
                return true;
            }
        }
        if (recents.size() >= 10) {
            recents.remove(9);
        }
        recents.add(0, path);
        return true;
    }

    public  ArrayList<String> addRecentModel(String path) {
        addRecent(recentFiles, path);
        persist();
        return recentFiles;
    }

    public ArrayList<String> addRecentSession(String path) {
        addRecent(recentSessions, path);
        persist();
        return recentSessions;
    }


    public ArrayList<String> getRecentModels() {
        return recentFiles;
    }

    public ArrayList<String> getRecentSessions() {
        return recentSessions;
    }

    public void addListener(PersistentPrefsListener l) {
        listeners.add(l);
    }

    public void removeListener(PersistentPrefsListener l) {
        listeners.remove(l);
    }

    private void update() {
        for(PersistentPrefsListener l: listeners)
            l.prefsChanged(this);
    }


    public void setTimeInputUnit(InputUnit iu) {
        inputUnit = iu;
        update();
    }

    public InputUnit getTimeInputUnit() {
        return inputUnit;
    }

    public void setShowTimeUnit(boolean v) {
        showUnits = v;
        update();
    }

    public boolean getShowTimeUnit() {
        return showUnits;
    }

    public void setTimeOutputUnit(OutputUnit ou) {
        outputUnit = ou;
        update();
    }

    public OutputUnit getTimeOutputUnit() {
        return outputUnit;
    }

    public void setLifelineColor(Color c) {
        lifeLineColor = c;
        update();
    }

    public Color getLifelineColor() {
        return lifeLineColor;
    }

    public void setEventLabelColor(Color c) {
        labelColor = c;
        update();
    }

    public Color getEventLabelColor() {
        return labelColor;
    }



    public void setOddEventBackgroundColor(Color c) {
        oddEventBackgroundColor = c;
        update();
    }

    public Color getOddEventBackgroundColor() {
        return oddEventBackgroundColor;
    }

    public void setEvenEventBackgroundColor(Color c) {
        evenEventBackgroundColor = c;
        update();
    }

    public Color getEvenEventBackgroundColor() {
        return evenEventBackgroundColor;
    }


    public void setSHowEntityFullPath(boolean v) {
        showEntityFullPath = v;
        update();
    }

    public boolean getShowEntityFullPath() {
        return showEntityFullPath;
    }

    public void setShowEntityAsID(boolean v) {
        showEntityAsID = v;
        update();
    }

    public boolean getShowEntityAsID() {
        return showEntityAsID;
    }

    public void setShowEntityDescription(boolean v) {
        showEntityDescription = v;
        update();
    }

    public boolean getShowEntityDescription() {
        return showEntityDescription;
    }

    public void setDefaultInteractionColor(Color c) {
        defaultInteractionColor = c;
        update();
    }

    public Color getDefaultInteractionColor() {
        return defaultInteractionColor;
    }

    public void setEventTimestampColor(Color c) {
        timestampColor = c;
        update();
    }

    public Color getEventTimestampColor() {
        return timestampColor;
    }

    public void setLogFileBackgroundColor(Color c) {
        logFileBackgroundColor = c;
        update();
    }

    public Color getLogFileBackgroundColor() {
        return logFileBackgroundColor;
    }

    public void setLogFileSelectedBackgroundColor(Color c) {
        logFileSelectedBackgroundColor = c;
        update();
    }

    public Color getLogFileSelectedBackgroundColor() {
        return logFileSelectedBackgroundColor;
    }

    public void setLogFileLineNumberBackgroundColor(Color c) {
        logFileLineNumberBackgroundColor = c;
        update();
    }

    public Color getLogFileLineNumberBackgroundColor() {
        return logFileLineNumberBackgroundColor;
    }

    public void setLogFileForegroundColor(Color c) {
        logFileForegroundColor = c;
        update();
    }

    public Color getLogFileForegroundColor() {
        return logFileForegroundColor;
    }

    public void setLogFileCopyBackgroundColor(Color c) {
        logFileCopyPasteSelectionColor = c;
        update();
    }

    public Color getLogFileCopyBackgroundColor() {
        return logFileCopyPasteSelectionColor;
    }

    public void setLogFileLineNumberForegroundColor(Color c) {
        logFileLineNumberForegroundColor = c;
        update();
    }

    public Color getLogFileLineNumberForegroundColor() {
        return logFileLineNumberForegroundColor;
    }

}
