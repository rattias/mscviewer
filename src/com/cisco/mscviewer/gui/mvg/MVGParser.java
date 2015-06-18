/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Sep 2014
 */
package com.cisco.mscviewer.gui.mvg;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A implementation of {@link Stroke} which transforms another Stroke with an
 * {@link AffineTransform} before stroking with it.
 *
 * This class is immutable as long as the underlying stroke is immutable.
 */
class TransformedStroke implements Stroke {
    /**
     * the AffineTransform used to transform the shape before stroking.
     */
    private final AffineTransform transform;
    /**
     * The inverse of {@link #transform}, used to transform back after stroking.
     */
    private final AffineTransform inverse;

    /**
     * Our base stroke.
     */
    private final Stroke stroke;

    /**
     * Creates a TransformedStroke based on another Stroke and an
     * AffineTransform.
     */
    public TransformedStroke(Stroke base, AffineTransform at)
            throws NoninvertibleTransformException {
        this.transform = new AffineTransform(at);
        this.inverse = transform.createInverse();
        this.stroke = base;
    }

    /**
     * Strokes the given Shape with this stroke, creating an outline.
     *
     * This outline is distorted by our AffineTransform relative to the outline
     * which would be given by the base stroke, but only in terms of scaling
     * (i.e. thickness of the lines), as translation and rotation are undone
     * after the stroking.
     */
    @Override
    public Shape createStrokedShape(Shape s) {
        final Shape sTrans = transform.createTransformedShape(s);
        final Shape sTransStroked = stroke.createStrokedShape(sTrans);
        final Shape sStroked = inverse.createTransformedShape(sTransStroked);
        return sStroked;
    }
}

/**
 *
 * @author rattias
 */
public class MVGParser {
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    private static final String f01Re = "((?:0?\\.[0-9]+)|0|1)";
    // private static final Pattern f01Pat = Pattern.compile(f01Re);
    private static final String sRe = "\\s*";
    private static final String commaRe = sRe + "," + sRe;
    private static final String rectRe = sRe + f01Re + commaRe + f01Re
            + commaRe + f01Re + commaRe + f01Re + sRe;
    private static final Pattern rectPat = Pattern.compile(rectRe);
    private static final String pointRe = sRe + "\\(" + sRe + f01Re + commaRe
            + f01Re + sRe + "\\)" + sRe;
    private static final Pattern pointPat = Pattern.compile(pointRe);
    private static final String polyRe = sRe + pointRe + "(?:" + commaRe
            + pointRe + ")+";
    private static final Pattern polyPat = Pattern.compile(polyRe);

    public static Document parse(File f) throws SAXException, IOException,
            ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(true);
        dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        dbf.setAttribute(JAXP_SCHEMA_SOURCE, new File("tvg.xsd"));

        final DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(f);
    }

    private static void dumpNode(Node n, int indent) {
        final String name = n.getNodeName();
        if (name.startsWith("#"))
            return;
        for (int i = 0; i < indent; i++)
            System.out.print(' ');
        System.out.print("<" + name + " ");
        final NamedNodeMap nnm = n.getAttributes();
        if (nnm != null) {
            final int acnt = nnm.getLength();
            if (acnt > 0) {
                for (int j = 0; j < acnt; j++) {
                    final Node an = nnm.item(j);
                    System.out.print(an.getNodeName() + "=\""
                            + an.getNodeValue() + "\" ");
                }
            }
        }
        final NodeList nl = n.getChildNodes();
        final int cnt = nl.getLength();
        if (cnt > 0) {
            System.out.println(">");
            for (int i = 0; i < cnt; i++) {
                dumpNode(nl.item(i), indent + 2);
            }
            for (int i = 0; i < indent; i++)
                System.out.print(' ');
            System.out.println("</" + n.getNodeName() + ">");
        } else {
            System.out.println("/>");
        }
    }

    private static ArrayList<Primitive> doc2Primitives(Document d) {
        final ArrayList<Primitive> al = new ArrayList<Primitive>();
        final Node root = d.getFirstChild();
        final NodeList nl = root.getChildNodes();
        final int cnt = nl.getLength();
        for (int i = 0; i < cnt; i++) {
            final Node n = nl.item(i);
            final String name = n.getNodeName();
            if (name.startsWith("#"))
                continue;
            if (name.equals("rect"))
                al.add(createRect(n));
            else if (name.equals("oval"))
                al.add(createOval(n));
            else if (name.equals("line"))
                al.add(createLine(n));
            else if (name.equals("poly"))
                al.add(createPoly(n));
            else if (name.equals("image"))
                al.add(createImage(n));
        }
        return al;
    }

    private static float tof(String s) {
        return Float.parseFloat(s);
    }

    private static Color getColorByName(String name) {
        Color color = null;
        try {
            final Field field = Color.class.getField(name);
            color = (Color) field.get(null);
        } catch (final Exception e) {
        }
        return color;
    }

    private static void setProps(Primitive p, NamedNodeMap nnm) {
        Node n = nnm.getNamedItem("stroke");
        if (n != null) {
            final Color c = getColorByName(n.getNodeValue());
            p.setStrokeColor(c);
        }
        n = nnm.getNamedItem("stroke-width");
        float strokeWidth;
        if (n != null)
            strokeWidth = tof(n.getNodeValue());
        else
            strokeWidth = 0;
        n = nnm.getNamedItem("stroke-dash");
        float[] strokeDash;
        if (n != null)
            strokeDash = parseFloatList(n.getNodeValue());
        else
            strokeDash = null;
        p.setStrokeProperties(strokeWidth, strokeDash);

        n = nnm.getNamedItem("fill");
        if (n != null) {
            final Color c = getColorByName(n.getNodeValue());
            p.setFillColor(c);
        }
    }

    private static Primitive createRect(Node n) {
        Rect r = null;
        final NamedNodeMap nnm = n.getAttributes();
        final String data = nnm.getNamedItem("data").getNodeValue();
        final Matcher m = rectPat.matcher(data);
        if (m.matches()) {
            r = new Rect(tof(m.group(1)), tof(m.group(2)), tof(m.group(3)),
                    tof(m.group(4)));
        } else
            throw new Error("No match of rect for data = \"" + data + "\"");
        setProps(r, nnm);
        return r;
    }

    private static Primitive createLine(Node n) {
        Poly l = null;
        final NamedNodeMap nnm = n.getAttributes();
        final String data = nnm.getNamedItem("data").getNodeValue();
        final Matcher m = rectPat.matcher(data);
        if (m.matches()) {
            l = new Poly(tof(m.group(1)), tof(m.group(2)));
            l.addPoint(tof(m.group(3)), tof(m.group(4)));
            setProps(l, nnm);
        } else
            throw new Error("No match of line for data = \"" + data + "\"");
        return l;
    }

    private static Primitive createOval(Node n) {
        Oval e = null;
        final NamedNodeMap nnm = n.getAttributes();
        final String data = nnm.getNamedItem("data").getNodeValue();
        final Matcher m = rectPat.matcher(data);
        if (m.matches()) {
            e = new Oval(tof(m.group(1)), tof(m.group(2)), tof(m.group(3)),
                    tof(m.group(4)));
            setProps(e, nnm);
        } else
            throw new Error("No match of oval for data = \"" + data + "\"");
        return e;
    }

    private static Primitive createPoly(Node n) {
        Poly p = null;
        final NamedNodeMap nnm = n.getAttributes();
        final String data = nnm.getNamedItem("data").getNodeValue();
        final Matcher m = polyPat.matcher(data);
        m.find();
        if (m.matches()) {
            final Matcher m1 = pointPat.matcher(data);
            while (m1.find()) {
                if (p == null)
                    p = new Poly(tof(m1.group(1)), tof(m1.group(2)));
                else
                    p.addPoint(tof(m1.group(1)), tof(m1.group(2)));
            }
            setProps(p, nnm);
        } else
            throw new Error("No match of poly for data = \"" + data + "\"");
        return p;
    }

    @SuppressWarnings("serial")
    public static void main(String args[]) throws SAXException, IOException,
            ParserConfigurationException {
        final Document d = MVGParser.parse(new File("example.xml"));
        final NodeList nl = d.getChildNodes();
        final int cnt = nl.getLength();
        for (int i = 0; i < cnt; i++) {
            dumpNode(nl.item(i), 0);
        }
        final ArrayList<Primitive> prims = doc2Primitives(d);
        final JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setBounds(0, 0, 640, 400);
        f.setContentPane(new JPanel() {
            private int cw, ch;

            @Override
            public void paintComponent(Graphics g) {
                final int w = getWidth();
                final int h = getHeight();
                if (w != cw || h != ch) {
                    cw = w;
                    ch = h;
                    for (final Primitive p : prims)
                        p.setContainerDimension(40, 20);
                }
                final Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setBackground(Color.blue);
                g2d.setColor(Color.red);
                g2d.clearRect(0, 0, getWidth(), getHeight());
                final long t0 = System.currentTimeMillis();
                for (int i = 0; i < 1000; i++) {
                    for (final Primitive p : prims) {
                        p.render(g2d);
                    }
                }
                final long t1 = System.currentTimeMillis();
                System.out.println("rendered in " + (t1 - t0) + "ms");
            }
        });
        f.setVisible(true);
    }

    private static float[] parseFloatList(String s) {
        final ArrayList<Float> f = new ArrayList<Float>();
        final String[] spl = s.split(",");
        for (final String str : spl) {
            final float fv = tof(str);
            System.out.println("f = " + fv);
            f.add(fv);
        }
        final float[] res = new float[f.size()];
        int idx = 0;
        System.out.println("returning: [");
        for (final Float ff : f) {
            res[idx++] = ff;
            System.out.println(ff);
        }
        System.out.println("]");
        return res;
    }

    private static Primitive createImage(Node n) {
        final NamedNodeMap nnm = n.getAttributes();
        float x0, y0, w, h;
        Image img = null;
        final String data = nnm.getNamedItem("data").getNodeValue();
        final Matcher m = rectPat.matcher(data);
        if (m.matches()) {
            x0 = tof(m.group(1));
            y0 = tof(m.group(2));
            w = tof(m.group(3));
            h = tof(m.group(4));
        } else
            throw new Error("No match of oval for data = \"" + data + "\"");
        final String imgPath = nnm.getNamedItem("path").getNodeValue();
        try {
            img = ImageIO.read(new File(imgPath));
        } catch (final IOException ex) {
            Logger.getLogger(MVGParser.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
        return new Img(x0, y0, w, h, img);
    }
}
