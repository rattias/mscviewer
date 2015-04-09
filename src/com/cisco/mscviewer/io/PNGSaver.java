/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.io;

import com.cisco.mscviewer.gui.MSCRenderer;
import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.util.ProgressReport;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class PNGSaver {
    public final static int SAVE_MARKED = 1;
    public final static int SAVE_OPENED = 2;
    public final static int SAVE_ALL = 3;
    
    public static void saveMSCasPNG(final String path, final MSCRenderer r, int mode) throws IOException {
        Thread t = new Thread("Image Writer") {
            @Override
            public void run() {
                ImageOutputStream ios = null;
                try {
                    BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = tmp.createGraphics();
                    FontMetrics fm = g2d.getFontMetrics();
                    int entityWidth = 0;
                    for(Iterator<Entity> it = MSCDataModel.getInstance().getEntityIterator(false); it.hasNext();) {
                        Entity en = it.next();
                        int w = fm.stringWidth(en.getName());
                        if (w>entityWidth)
                            entityWidth = w;
                    }
                    g2d.dispose();
                    entityWidth += 10;

                    int width = r.getEntityHeaderModel().getTotalWidth();
                    int height = r.getHeight()+20;
                    // Create a buffered image in which to draw
                    System.out.println("PNGSaver: img: "+width+"x"+height);
                    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    // Create a graphics contents on the buffered image
                    g2d = bufferedImage.createGraphics();
                    g2d.setColor(Color.yellow);
                    g2d.fillRect(0, 0, width, height);
                    r.render(g2d, true, 0, height);
                    g2d.dispose();	    

                    // Save as PNG
                    File file = new File(path);
                    Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("png");
                    if (! it.hasNext())
                        throw new Error("No PNG ImageWriter was found");
                    ImageWriter iw = it.next();
                    ios = ImageIO.createImageOutputStream(file);
                    iw.setOutput(ios);
                    final ProgressReport pr = new ProgressReport("Exporting Image", ""+path, 0, 100);
                    iw.addIIOWriteProgressListener(new IIOWriteProgressListener() {
                        
                        @Override
                        public void imageStarted(ImageWriter source, int imageIndex) {
                            pr.progress("Writing "+path, 0);
                        }
                        
                        @Override
                        public void imageProgress(ImageWriter source,
                                float percentageDone) {
                            pr.progress((int)(100*percentageDone));
                        }
                        
                        @Override
                        public void imageComplete(ImageWriter source) {
                            pr.progressDone();
                        }
                        
                        @Override
                        public void thumbnailStarted(ImageWriter source,
                                int imageIndex, int thumbnailIndex) { }
                        
                        @Override
                        public void thumbnailProgress(ImageWriter source,
                                float percentageDone) { }
                        
                        @Override
                        public void thumbnailComplete(ImageWriter source) {}
                        
                        @Override
                        public void writeAborted(ImageWriter source) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    pr.progressDone();
                                    JOptionPane.showMessageDialog(null, "Error while writing image to disk: Write aborted", "Image I/O Error", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }
                    });	    	
                    iw.write(bufferedImage);
                }catch(final IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, "Error while writing image to disk: "+ex.getMessage(), "Image I/O Error", JOptionPane.ERROR_MESSAGE);							
                        }						
                    });					

                }finally{
                    if (ios != null)
                        try {
                            ios.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                }
            }
        };
        t.start();
    }
}
