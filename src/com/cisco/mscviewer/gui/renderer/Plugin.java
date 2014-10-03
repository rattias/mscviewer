/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jan 2012
 */
package com.cisco.mscviewer.gui.renderer;

import com.cisco.mscviewer.model.JSonObject;
import com.cisco.mscviewer.model.MSCDataModel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author rattias
 */
public class Plugin {
    private static HashMap<HashMap<String, String>, EventRenderer> eventRenderers =
            new HashMap<HashMap<String, String>, EventRenderer>();
    @SuppressWarnings("unused")
    private static HashMap<HashMap<String, String>, InteractionRenderer> interactionRenderers =
            new HashMap<HashMap<String, String>, InteractionRenderer>();
    
    
    private static void parseRenderers(String path) throws IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = null;
        try {
            saxParser = factory.newSAXParser();
        } catch (ParserConfigurationException e1) {
            throw new IOException(e1);
        } catch (SAXException e1) {
            throw new IOException(e1);
        }
        @SuppressWarnings("unused")
        final String filePath = path + "renderers.xml";

        DefaultHandler handler = new DefaultHandler() {
            final static String TAG_EVENT_RENDERER = "event-renderer";
            final static String TAG_RENDER_WITH = "render-with";
            private HashMap<String, String> attrs;
            @Override
            public void startElement(String uri, String localName, String qName, 
                    Attributes attributes) throws SAXException {
                
                if (qName.equalsIgnoreCase(TAG_EVENT_RENDERER)) {
                    attrs = new HashMap<String, String>();
                    for (int i=0; i<attributes.getLength(); i++) {
                        attrs.put(attributes.getLocalName(i), attributes.getValue(i));
                    }        
                } else if (qName.equalsIgnoreCase(TAG_RENDER_WITH)) {
                    String type = attributes.getValue("type");
                    if (type.equals("Image")) {
                        String img = attributes.getValue("img");
                        ImageRenderer r = new ImageRenderer();
                        JSonObject m = new JSonObject();
                        m.set("img", img);
                        r.initialize(m);
                        eventRenderers.put(attrs, r);
                    }
                }

                if (qName.equalsIgnoreCase("NICKNAME")) {
                }

                if (qName.equalsIgnoreCase("SALARY")) {
                }

            }

            @SuppressWarnings("unused")
            private void loadConfigAndLog(MSCDataModel dm) {
            }
            
            @SuppressWarnings("unused")
            private void loadConfig(MSCDataModel dm) {

            }

            @Override
            public void endElement(String uri, String localName,
                    String qName) throws SAXException {

                System.out.println("End Element :" + qName);

            }

            @Override
            public void characters(char ch[], int start, int length) throws SAXException {
                boolean bfname = false;
                if (bfname) {
                    System.out.println("First Name : " + new String(ch, start, length));
                }

            }

        };
        try {
            saxParser.parse(path, handler);
        //}catch (UserAbortException e) {
        } catch (SAXException e) {
            throw new IOException(e);
        }
        
    }
    
    public static void init(String pluginPath) {
        if (pluginPath == null)
            throw new Error("null pluginPath");
        for (String p: pluginPath.split(File.pathSeparator)) {
            try {
                parseRenderers(p);
            } catch (IOException ex) {
                throw new Error(ex);
            }
        }
    }
    
}
