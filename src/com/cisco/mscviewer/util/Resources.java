package com.cisco.mscviewer.util;

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
import javax.swing.ImageIcon;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.gui.renderer.EventRenderer;
import com.cisco.mscviewer.gui.renderer.ImageRenderer;

public class Resources {
    private static final String RESOURCE_PATH = "com/cisco/mscviewer/resources";
    private static final String ICON_PATH = RESOURCE_PATH + "/icons";
    private static final String RENDERER_PATH = RESOURCE_PATH + "/renderers";
    private static HashMap<String, ImageIcon> imgIcons;
    private static HashMap<String, ImageRenderer> imgRenderers;

    public static void init(String plugins) {
        int W = 25, H = 25;
        getImageIcon("highlight_green.png"       , "green", W, H);
        getImageIcon("highlight_green32x32.png"  , "green");
        getImageIcon("highlight_yellow.png"     , "yellow", W, H);
        getImageIcon("highlight_yellow32x32.png", "yellow");
        getImageIcon("highlight_blue.png"       , "blue", W, H);
        getImageIcon("highlight_blue32x32.png"  , "blue");
        getImageIcon("highlight_red.png"        , "red", W, H);
        getImageIcon("highlight_red.png"        , "red");
        getImageIcon("select.png"               , "select", W, H);
        getImageIcon("select32x32.png"      , "select");
        getImageIcon("blocks32x32.png"      , "blocks");

        readDefaultRenderers();
        if (plugins == null)
            return;
        imgRenderers = new HashMap<String, ImageRenderer>();
        for(String path: plugins.split(File.pathSeparator)) {
            File f = new File(path+"/renderer");
            String[] icons = f.list();
            for(String s: icons) {
                int dotIdx = s.lastIndexOf('.');
                String key = dotIdx >= 0 ? s.substring(0, dotIdx) : s;

                try {
                    ImageRenderer ir = new ImageRenderer(ImageIO.read(new File(f, s)));
                    imgRenderers.put(key, ir);
                } catch (IOException ex) {
                    Logger.getLogger(ImageRenderer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }




    public static ImageIcon getImageIcon(String path, String description) {
        return getImageIcon(path, description, -1, -1);
    }

    public static ImageIcon getImageIcon(String path, String description, int width, int height) {
        java.net.URL imgURL;
        String filePath;
        boolean shouldRescale = false;
        if (imgIcons == null)
            imgIcons = new HashMap<String, ImageIcon>();
        filePath = RESOURCE_PATH + "/icons/" + path;
        ImageIcon im = imgIcons.get(filePath);
        if (im == null) {
            if (width >=0) {
                int idx = filePath.lastIndexOf(".");
                if (idx >= 0 )
                    filePath = filePath.substring(0, idx)+width+"x"+height+filePath.substring(idx);
                else
                    filePath = filePath+width+"x"+height;
            }
            imgURL = ClassLoader.getSystemResource(filePath);
            if (imgURL == null && width >= 0) {
                shouldRescale = true;
                filePath = path;
                imgURL = ClassLoader.getSystemResource(ICON_PATH + "/" + filePath);
            }
            if (imgURL == null) {
                System.err.println("Couldn't find file: " + filePath+" (width = "+width+", height = "+height+")");
                return null;
            }
            im = new ImageIcon(imgURL, description);
            if (shouldRescale)
                im.setImage(im.getImage().getScaledInstance(width, height, 0));
            imgIcons.put(filePath, im);
        }
        return im;
    }


    private static void readDefaultRenderers() {
        if (imgRenderers == null)
            imgRenderers = new HashMap<String, ImageRenderer>();
        
        URL resourceURL = ClassLoader.getSystemResource(RENDERER_PATH);
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
                    if (name.startsWith(RENDERER_PATH)) {
                        int dotIdx = name.lastIndexOf('.');
                        int l = RENDERER_PATH.length()+1;
                        String key = dotIdx >= 0 ? name.substring(l, dotIdx) : name.substring(l);                        
                        try {
                            ImageRenderer ir = new ImageRenderer(ImageIO.read(ClassLoader.getSystemResource(name).openStream()));                            
                            imgRenderers.put(key, ir);
                        } catch (IOException ex) {
                            Logger.getLogger(ImageRenderer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            } else {
                File f = new File(resourceURL.toURI());
                String[] files = f.list();
                for(String img: files) {
                    File ff = new File(f, img);
                    ImageRenderer ir = new ImageRenderer(ImageIO.read(ff));
                    int dotIdx = img.lastIndexOf('.');
                    String key = dotIdx >= 0 ? img.substring(0, dotIdx) : img;
                    imgRenderers.put(key, ir);
                }
            }
        }catch(MalformedURLException ex) {                        ex.printStackTrace();
        } catch (URISyntaxException ex) {
            ex.printStackTrace();            
        }catch(IOException ex) { 
            ex.printStackTrace();
        }
    }




    public static EventRenderer getImageRenderer(String t) {
        return imgRenderers.get(t);
    }    
}
