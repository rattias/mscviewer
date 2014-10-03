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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;


public class ImageRenderer extends EventRenderer {
    private static HashMap<String, ImageRenderer> map = new HashMap<String, ImageRenderer>();
    private static String DEFAULT_RENDERER = "com/cisco/mscviewer/resource/renderer";
    
    private Image img;
    private Dimension dim = null;
    
    private static void readDefaultRenderers() {
        URL resourceURL = ClassLoader.getSystemResource(DEFAULT_RENDERER);
        String urlStr = resourceURL.getPath(); 
        int idx = urlStr.indexOf(".jar!"); 
        try {
            if (idx != -1) {
                String jarPath = urlStr.substring(5, idx+4); // strip leading "file:" and trailing !....
                URL jar;
                jar = new File(jarPath).toURI().toURL();
                ZipInputStream zip = new ZipInputStream(jar.openStream());
                while(true) {
                    ZipEntry e = zip.getNextEntry();
                    if (e == null)
                        break;
                    String name = e.getName();
                    if (name.startsWith(DEFAULT_RENDERER)) {
                        int dotIdx = name.lastIndexOf('.');
                        int l = DEFAULT_RENDERER.length()+1;
                        String key = dotIdx >= 0 ? name.substring(l, dotIdx) : name.substring(l);                        
                        ImageRenderer ir = new ImageRenderer();
                        try {
                            ir.img = ImageIO.read(ClassLoader.getSystemResource(name).openStream());
                        } catch (IOException ex) {
                            Logger.getLogger(ImageRenderer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        map.put(key, ir);
                    }
                }
            } else {
                File f = new File(resourceURL.toURI());
                String[] files = f.list();
                for(String img: files) {
                    ImageRenderer ir = new ImageRenderer();
                    File ff = new File(f, img);
                    ir.img = ImageIO.read(ff);
                    int dotIdx = img.lastIndexOf('.');
                    String key = dotIdx >= 0 ? img.substring(0, dotIdx) : img;
                    map.put(key, ir);
                }
            }
        }catch(MalformedURLException ex) {            
            ex.printStackTrace();
        } catch (URISyntaxException ex) {
            ex.printStackTrace();            
        }catch(IOException ex) { 
            ex.printStackTrace();
        }
    }
    
    public static void init(String plugins) {
        readDefaultRenderers();
        if (plugins == null)
            return;
        for(String path: plugins.split(File.pathSeparator)) {
            File f = new File(path+"/renderer");
            String[] icons = f.list();
            for(String s: icons) {
                int dotIdx = s.lastIndexOf('.');
                String key = dotIdx >= 0 ? s.substring(0, dotIdx) : s;
                ImageRenderer ir = new ImageRenderer();
                try {
                    ir.img = ImageIO.read(new File(f, s));
                } catch (IOException ex) {
                    Logger.getLogger(ImageRenderer.class.getName()).log(Level.SEVERE, null, ex);
                }
                map.put(key, ir);
            }
        }
    }
    
    public static ImageRenderer get(String type) {
        return map.get(type);
    }
    

    private void computeDimension(int h) {
        double scalingFactor = ((double)h)/img.getHeight(null);
        dim = new Dimension((int)(img.getWidth(null)*scalingFactor),
            (int)(img.getHeight(null)*scalingFactor));
    }
    
    @Override
    public void render(Graphics2D g2d, Dimension maxDim) {
        if (dim == null)
            computeDimension(maxDim.height);        
        g2d.drawImage(img, -dim.width/2, -dim.height/2, dim.width, dim.height, null);
    }


    @Override
    public Rectangle getBoundingBox(Dimension maxDim, int x, int y, Rectangle bb) {
        if (dim == null)
            computeDimension(maxDim.height);
        if (bb == null)
            bb = new Rectangle();
        bb.x = x-dim.width/2;
        bb.y = y-dim.height/2;
        bb.width = dim.width;
        bb.height = dim.height;
        return bb;
    }


}
