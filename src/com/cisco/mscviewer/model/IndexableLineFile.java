package com.cisco.mscviewer.model;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.cisco.mscviewer.util.Report;
import com.cisco.mscviewer.util.Utils;

@SuppressWarnings("serial")
public class IndexableLineFile extends AbstractListModel<String> {
    private final static File dir = new File(Utils.getWorkDirPath());
    private final static int BLOCK_SIZE = 64*1024;
    private BufferedWriter fw;
    ArrayList<long[]> offsets;
    private int numLines;
    private long fileSize;
    private boolean loading;
    private File f;
    private FontMetrics offscreenFontMetrics;
    private Graphics offscreenG;
    private int lineHeight = 0;
    private int lineWidth = 0;
    private int charWidth = 0;
    private int maxLineLen;
    private FileChannel fileChannel;
    private MappedByteBuffer[] byteBuffer;
    private long prevOffset = -1;
    
    public IndexableLineFile() {
        reset();
        long[] el = new long[BLOCK_SIZE];
        offsets.add(el);
    }
    
    public void setFile(String filePath) throws IOException {
        reset();
        File file = new File(filePath);
        fileChannel = new RandomAccessFile(file, "r").getChannel();
        fileSize = fileChannel.size();
        int nChunks = (int)(fileSize/Integer.MAX_VALUE);
        long reminder = fileSize % Integer.MAX_VALUE;
        if (reminder != 0)
            nChunks++;
        byteBuffer = new MappedByteBuffer[nChunks];
        for(int i=0; i<nChunks; i++) {
            long size = (i<nChunks-1 || reminder == 0) ? Integer.MAX_VALUE : reminder;
            byteBuffer[i] = fileChannel.map(FileChannel.MapMode.READ_ONLY, Integer.MAX_VALUE*(long)i, size);
        }
         
    }

    
    public Dimension getMaxLineSize() {
        return new Dimension(lineWidth, lineHeight);
    }
    
    public void addLine(long offset) {
        if (prevOffset >=0) {
            int len = (int)(offset-prevOffset); // don't add 1 cause we skip \n
            if (len > maxLineLen)
                maxLineLen = len;
        }
        prevOffset = offset;
        long[] last;
        if (numLines == offsets.size()*BLOCK_SIZE) {
            last = new long[BLOCK_SIZE];
            offsets.add(last);
        } else
            last = offsets.get(offsets.size()-1);
        last[numLines % BLOCK_SIZE] = offset;
        numLines++;
        if (numLines == Integer.MAX_VALUE)
            throw new Error("Error: File contain more than 2^31 lines are not supported");
    }
    

    @Override
    public int getSize() {
        return numLines;
    }


    @Override
    public String getElementAt(int index) {
        
        if (index >= numLines)
            return null;
        String res = null;
        int idx = index / BLOCK_SIZE;
        int off = index % BLOCK_SIZE;
        long p = offsets.get(idx)[off];
        long p1;
        if (index == numLines - 1) {
            p1 = fileSize; 
        } else {
            if (off < BLOCK_SIZE-1)
                p1 = offsets.get(idx)[off+1];
            else
                p1 = offsets.get(idx+1)[0];
        }
        int lineLen = (int)(p1-p);
        byte[] data = new byte[lineLen];
        int bbIndex = (int)(p/Integer.MAX_VALUE);
        int bbOffset = (int)(p % Integer.MAX_VALUE);
        if (p+lineLen < Integer.MAX_VALUE) {
            byteBuffer[bbIndex].position(bbOffset);
            byteBuffer[bbIndex].get(data, 0, lineLen);
            return new String(data);
        } else {
            byteBuffer[bbIndex].position(bbOffset);
            int len0 = (int)(Integer.MAX_VALUE-bbOffset+1);
            byteBuffer[bbIndex].get(data, 0, len0);
            byteBuffer[bbIndex+1].position(0);                
            byteBuffer[bbIndex+1].get(data, len0, lineLen-len0);
            return new String(data);                
        }
    }

    public long getElementOffset(int index) {
        int idx = index / BLOCK_SIZE;
        int off = index % BLOCK_SIZE;
        long p = offsets.get(idx)[off];        
        return p;
    }

    public void fireContentsChanged() {
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public void reset() {
        offsets = new ArrayList<long[]>();
        numLines = 0;
        fileSize = 0;
        maxLineLen = 0;
        lineWidth = 0;
        if (fileChannel != null) {
            try {
                fileChannel.close();
            } catch (IOException e) {
                Report.exception("Exception while closing fileChannel:", e);
            }
            fileChannel = null;
        }
        loading = true;
    }

    public void doneLoading() {
        loading = false;
        fireContentsChanged();
    }

    public int getMaxLineLen() {
        return maxLineLen;
    }
    
    public boolean loading() {
        return loading;
    }
}
