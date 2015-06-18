package com.cisco.mscviewer.util;

import java.awt.Toolkit;
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

import com.cisco.mscviewer.gui.renderer.EventRenderer;
import com.cisco.mscviewer.gui.renderer.ImageRenderer;

public class Resources {
    private static final String RESOURCE_PATH = "com/cisco/mscviewer/resources";
    private static final String ICON_PATH = RESOURCE_PATH + "/icons";
    private static final String RENDERER_PATH = RESOURCE_PATH + "/renderers";
    private static HashMap<String, ImageIcon> imgIcons;
    private static HashMap<String, ImageRenderer> imgRenderers;
    private static String iconSize;

    public static void init(String plugins) {
        imgRenderers = new HashMap<String, ImageRenderer>();

        readDefaultRenderers();
        if (plugins == null)
            return;
        for (final String path : plugins.split(File.pathSeparator)) {
            final File f = new File(path + "/renderer");
            if (f.isDirectory()) {
                final String[] icons = f.list();
                for (final String s : icons) {
                    final int dotIdx = s.lastIndexOf('.');
                    final String key = dotIdx >= 0 ? s.substring(0, dotIdx) : s;
                    
                    try {
                        final ImageRenderer ir = new ImageRenderer(key,
                                ImageIO.read(new File(f, s)));
                        imgRenderers.put(key, ir);
                    } catch (final IOException ex) {
                        Logger.getLogger(ImageRenderer.class.getName()).log(
                                Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    private static String getIconSize() {
        if (iconSize == null) {
            int res = Toolkit.getDefaultToolkit().getScreenResolution();
            if (res >120)
                iconSize = "32x32";
            else
                iconSize = "16x16";
        }
        return iconSize;        
    }
    
    public static ImageIcon getImageIcon(String path, String description) {
        if (path.indexOf('/') == -1)
            path = getIconSize() + "/"+path;
        java.net.URL imgURL;
        if (imgIcons == null)
            imgIcons = new HashMap<String, ImageIcon>();
        ImageIcon im = imgIcons.get(RESOURCE_PATH + path);
        if (im == null) {
            imgURL = ClassLoader.getSystemResource(RESOURCE_PATH + path);
            if (imgURL == null) {
                imgURL = ClassLoader.getSystemResource(ICON_PATH + "/" + path);
            }
            if (imgURL == null) {
                System.err.println("Couldn't find image icon " + RESOURCE_PATH
                        + path + " or " + ICON_PATH + "/" + path);
                final StackTraceElement[] ste = Thread.currentThread()
                        .getStackTrace();
                for (final StackTraceElement el : ste)
                    System.out.println(el);
                return null;
            }
            im = new ImageIcon(imgURL, description);
            imgIcons.put(path, im);
        }
        return im;
    }

    private static void readDefaultRenderers() {
        final URL resourceURL = ClassLoader.getSystemResource(RENDERER_PATH);
        final String urlStr = resourceURL.getPath();
        final int idx = urlStr.indexOf(".jar!");
        try {
            if (idx != -1) {
                final String jarPath = urlStr.substring(5, idx + 4); // strip leading
                                                               // "file:" and
                                                               // trailing !....
                URL jar;
                jar = new File(jarPath).toURI().toURL();
                final ZipInputStream zip = new ZipInputStream(jar.openStream());
                while (true) {
                    final ZipEntry e = zip.getNextEntry();
                    if (e == null)
                        break;
                    final String name = e.getName();
                    if (name.startsWith(RENDERER_PATH)) {
                        final int dotIdx = name.lastIndexOf('.');
                        final int l = RENDERER_PATH.length() + 1;
                        final String key = dotIdx >= 0 ? name.substring(l, dotIdx)
                                : name.substring(l);
                        try {
                            final ImageRenderer ir = new ImageRenderer(key,
                                    ImageIO.read(ClassLoader.getSystemResource(
                                            name).openStream()));
                            imgRenderers.put(key, ir);
                        } catch (final IOException ex) {
                            Logger.getLogger(ImageRenderer.class.getName())
                                    .log(Level.SEVERE, null, ex);
                        }
                    }
                }
            } else {
                final File f = new File(resourceURL.toURI());
                final String[] files = f.list();
                for (final String img : files) {
                    final File ff = new File(f, img);
                    final int dotIdx = img.lastIndexOf('.');
                    final String key = dotIdx >= 0 ? img.substring(0, dotIdx) : img;
                    final ImageRenderer ir = new ImageRenderer(key, ImageIO.read(ff));
                    imgRenderers.put(key, ir);
                }
            }
        } catch (final MalformedURLException ex) {
            ex.printStackTrace();
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    public static EventRenderer getImageRenderer(String t) {
        final EventRenderer r = imgRenderers.get(t);
        return r;
    }

}
