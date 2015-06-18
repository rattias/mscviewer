package com.cisco.mscviewer.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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


public class PersistentPrefs {
    private static String EL_MSC_PREFS = "msc-prefs";
    private static String EL_RECENT_FILES = "recent-files";
    private static String EL_FILE = "file";
    private static String EL_RECENT_SESSIONS = "recent-sessions";
    private static String EL_SESSION = "session";
    private static String ATTR_PATH = "path";
    private ArrayList<String> recentFiles = new ArrayList<String>();
    private ArrayList<String> recentSessions = new ArrayList<String>();
    private static PersistentPrefs instance = new PersistentPrefs();
    
    public static PersistentPrefs getInstance() {
        return instance;
    }
    
    
    public PersistentPrefs() {
        loadPrefs();
    }
    
    private void loadPrefs() {
        
        String path = getPrefsFile();
        if (! (new File(path).exists()))
            return;
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

    private void savePrefs() {
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
        savePrefs();
        return recentFiles;
    }
    
    public ArrayList<String> addRecentSession(String path) {
        addRecent(recentSessions, path);
        savePrefs();
        return recentSessions;
    }


    public ArrayList<String> getRecentModels() {
        return recentFiles;
    }

    public ArrayList<String> getRecentSessions() {
        return recentSessions;
    }

    
}
