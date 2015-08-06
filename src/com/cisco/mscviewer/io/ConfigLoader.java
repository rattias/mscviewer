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
package com.cisco.mscviewer.io;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.model.MSCDataModel;

@SuppressWarnings("serial")
class UserAbortException extends SAXException {
}

public class ConfigLoader {
    /**
     * loads a configuration and applies it to the passed model if non-null, or
     * tries to load the model referred to by the configuration first.
     * 
     * @param fname
     * @param mod
     * @param obs
     * @return
     * @throws java.io.IOException
     */
    public static MSCDataModel load(String fname, MSCDataModel mod)
            throws IOException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = null;
        try {
            saxParser = factory.newSAXParser();
        } catch (final ParserConfigurationException e1) {
            throw new IOException(e1);
        } catch (final SAXException e1) {
            throw new IOException(e1);
        }
        final String cfgPath = fname;
        final MSCDataModel min = mod;

        final DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName,
                    String qName, Attributes attributes) throws SAXException {
                if (qName.equalsIgnoreCase(Config.EL_MSCCONFIG)) {
                    final String logPath = attributes
                            .getValue(Config.ATTR_MSCCONFIG_LOG);
                    final String dateString = attributes
                            .getValue(Config.ATTR_MSCCONFIG_DATE);

                    long t = 0;
                    try {
                        t = Config.getTimeSinceEpoch(dateString);
                    } catch (final ParseException e) {
                        Logger logger = Logger.getLogger(Main.class.getName());
                        logger.log(Level.INFO,"startElement() failed:", e);                        
                    }
                    final File f = new File(logPath);
                    final long lastModified = f.lastModified();
                    if (min != null) {
                        if (!min.getFilePath().equals(logPath)) {
                            final int res = JOptionPane
                                    .showConfirmDialog(
                                            null,
                                            "Config  file \""
                                                    + cfgPath
                                                    + "\" refer to log file \""
                                                    + logPath
                                                    + "\", which"
                                                    + "is different from currently loaded log file \""
                                                    + min.getFilePath()
                                                    + ". "
                                                    + "Do want to load config and referred log (Yes), load config and apply "
                                                    + "to current log (No), or Cancel?",
                                            "Load Configuration",
                                            JOptionPane.YES_NO_CANCEL_OPTION);
                            switch (res) {
                            case JOptionPane.CANCEL_OPTION:
                                throw new UserAbortException();
                            case JOptionPane.YES_OPTION:
                                try {
                                    final Loader l = new LegacyLoader();
                                    l.load(logPath, min, false);
                                } catch (final IOException e) {
                                    throw new SAXException(e);
                                }
                                break;
                            case JOptionPane.NO_OPTION:
                                break;
                            }
                        } else if (lastModified != t) {
                            final int res = JOptionPane
                                    .showConfirmDialog(
                                            null,
                                            "Currently loaded log file  \""
                                                    + logPath
                                                    + "\" has different timestamp "
                                                    + "than the copy on disk. "
                                                    + "Do you want to: reload log file from disk (Yes), "
                                                    + "keep current model and apply config (markers may be out of sync) (No)",
                                            "or Cancel?",
                                            JOptionPane.YES_NO_CANCEL_OPTION);
                            switch (res) {
                            case JOptionPane.CANCEL_OPTION:
                                throw new UserAbortException();
                            case JOptionPane.YES_OPTION:
                                try {
                                    final Loader l = new LegacyLoader();
                                    l.load(logPath, min, false);
                                } catch (final IOException e) {
                                    throw new SAXException(e);
                                }
                                break;
                            case JOptionPane.NO_OPTION:
                                break;
                            }
                        }
                    }
                    if (lastModified != t) {
                        final int res = JOptionPane
                                .showConfirmDialog(
                                        null,
                                        "Log file \""
                                                + logPath
                                                + "\"referred to by config  file \""
                                                + cfgPath
                                                + "\" has been modified since when the config file was saved."
                                                + " Do you still want to load it (markers may be out of sync)?",
                                        "Load Configuration",
                                        JOptionPane.YES_NO_OPTION);
                        if (res == JOptionPane.CANCEL_OPTION) {
                            throw new UserAbortException();
                        }
                    }
                }
                if (qName.equalsIgnoreCase("LASTNAME")) {
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
            public void endElement(String uri, String localName, String qName)
                    throws SAXException {

                System.out.println("End Element :" + qName);

            }

            @Override
            public void characters(char ch[], int start, int length)
                    throws SAXException {
                final boolean bfname = false;
                if (bfname) {
                    System.out.println("First Name : "
                            + new String(ch, start, length));
                }

            }

        };
        try {
            saxParser.parse(fname, handler);
        } catch (final UserAbortException e) {
            Logger logger = Logger.getLogger(Main.class.getName());
            logger.log(Level.INFO,"Load operation aborted by user", e);                        
        } catch (final SAXException e) {
            throw new IOException(e);
        }

        return null;
    }
}