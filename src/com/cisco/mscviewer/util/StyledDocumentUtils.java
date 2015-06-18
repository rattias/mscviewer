package com.cisco.mscviewer.util;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;

public class StyledDocumentUtils {

    public static String exportXML(StyledDocument doc) {
        if (doc.getLength() == 0)
            return null;
        StringBuilder sb = new StringBuilder();
        int cnt = doc.getLength();
        Element el = null;
        int soff = 0, eoff;
        String prevAttrs = null;
        sb.append("<Note>");
        while(soff < cnt) {
            Element tmp = doc.getCharacterElement(soff);
            if (tmp != el) {
                soff = tmp.getStartOffset();
                eoff = tmp.getEndOffset();
                AttributeSet as = tmp.getAttributes();
                String attrs = "";
                if (StyleConstants.isBold(as))
                    attrs += 'b';
                if (StyleConstants.isItalic(as))
                    attrs += 'i';
                if (StyleConstants.isUnderline(as))
                    attrs += 'u';
                Color c = StyleConstants.getForeground(as);
                if (c != Color.black)
                    attrs += "c["+String.format("%06x", c.getRGB() & 0xFFFFFF)+"]";
                Color C = StyleConstants.getBackground(as);
                if (C != Color.black)
                    attrs += "C["+String.format("%06x", C.getRGB() & 0xFFFFFF)+"]";
                String txt = "";
                try {
                    int len = eoff-soff;
                    if (eoff > cnt)
                        len--;
                    txt = doc.getText(soff, len);
                } catch (BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }                
                if (attrs.equals(prevAttrs))
                    sb.append(txt);
                else {
                    if (prevAttrs != null)
                        sb.append("</Element>\n");
                    sb.append("  <Element start='"+soff+"' end='"+eoff+"' attrs='"+attrs+"'>");
                    sb.append(txt);
                    prevAttrs = attrs;
                }
                soff = eoff;
            }
            
        }
        if (prevAttrs != null)
            sb.append("</Element>\n");
        sb.append("</Note>");
        return sb.toString();
    }
    
    public static void  importXML(String xml, StyledDocument doc) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // use the factory to take an instance of the document builder
        org.w3c.dom.Document dom = null;
        DocumentBuilder db;
        try {
            doc.remove(0, doc.getLength());
            if (xml == null)
                return;
            db = dbf.newDocumentBuilder();
            dom = db.parse(new ByteArrayInputStream(xml.getBytes()));
            org.w3c.dom.Element root = dom.getDocumentElement();
            NodeList els = root.getElementsByTagName("Element");
            SimpleAttributeSet as = new SimpleAttributeSet();
            for(int i=0; i<els.getLength(); i++) {
                org.w3c.dom.Element n = (org.w3c.dom.Element)els.item(i);
                String attrs = n.getAttribute("attrs");
                boolean bold = false;
                boolean underline = false;
                boolean italic = false;
                Color fg = Color.black;
                Color bg = Color.white;
                for(int a=0; a<attrs.length(); a++) {
                    switch(attrs.charAt(a)) {
                    case 'b': bold = true;      break;
                    case 'u': underline = true; break;
                    case 'i': italic = true;    break;
                    case 'c':
                        String arg = attrs.substring(a+2, attrs.indexOf(']', a+2));
                        fg = new Color(Integer.parseInt(arg, 16));
                        a += 6;
                        break;
                    case 'C': 
                        arg = attrs.substring(a+2, attrs.indexOf(']', a+2));
                        bg = new Color(Integer.parseInt(arg, 16));
                        a += 6;
                        break;
                    }
                }
                StyleConstants.setBold(as, bold);
                StyleConstants.setUnderline(as, underline);
                StyleConstants.setItalic(as, italic);
                StyleConstants.setForeground(as, fg);
                StyleConstants.setBackground(as, bg);
                String txt = n.getChildNodes().item(0).getTextContent();
                doc.insertString(doc.getLength(), txt, as);
            }
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (org.xml.sax.SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
