package com.cisco.mscviewer.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import javax.swing.AbstractListModel;

import com.cisco.mscviewer.util.Utils;

@SuppressWarnings("serial")
public class LogListModel extends AbstractListModel<String> {
    private final static File dir = new File(Utils.getWorkDirPath());
    private final static int BLOCK_SIZE = 1024;
    private RandomAccessFile raf;
    private BufferedWriter fw;
    ArrayList<long[]> offsets;
    private int numLines;
    private long fileSize;
    private long cachedOffset;
    private byte[] cache;
    private boolean loading;
    private File f;

    public LogListModel() {
        reset();
        long[] el = new long[BLOCK_SIZE];
        offsets.add(el);
    }

    public void add(String line) {
        if (! loading)
            throw new Error("Invalid add when mode is not loading");
        try {
            if (fw == null) {
                f = File.createTempFile("temp", ".msc", dir);
                fw = new BufferedWriter(new FileWriter(f));
            }
            long[] last;
            if (numLines == offsets.size()*BLOCK_SIZE) {
                last = new long[BLOCK_SIZE];
                offsets.add(last);                
            } else
                last = offsets.get(offsets.size()-1);
            last[numLines % BLOCK_SIZE] = fileSize;
            numLines++;
            int len = line.length()+1;
            fw.write(line);
            fw.write('\n');
            fileSize += len;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public int getSize() {
        return numLines;
    }


    @Override
    public String getElementAt(int index) {
        if (loading)
            return "";
        String res = null;
        try {
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
            int lineLen = (int)(p1-p+1);
            if (p >= cachedOffset && p1 < cachedOffset + cache.length) {
                return new String(cache, (int)(p-cachedOffset), lineLen-1);
            } else if (lineLen > cache.length) {
                cache = new byte[lineLen];
            }
            if (p < cachedOffset) {
                // we may be traversing the file backward
                // read so that extra data is above
                cachedOffset = p + lineLen - cache.length;
                if (cachedOffset < 0) 
                    cachedOffset = 0;
            } else {
                cachedOffset = p;
            }

            if (p != raf.getFilePointer()) {
                raf.seek(cachedOffset);
            }
            raf.read(cache);
            return new String(cache, (int)(p-cachedOffset), lineLen-1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return res;
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
        File[] files = dir.listFiles();
        for(File f: files) {
            // delete older files. If one is there because
            // opened by another mscviewer, the delete should fails
            // silently.
            if (f.getPath().endsWith(".msc"))
                f.delete();
        }
        if (fw != null) {
            try {
                fw.close();
                fw = null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        offsets = new ArrayList<long[]>();
        numLines = 0;
        fileSize = 0;
        cachedOffset = Long.MIN_VALUE;
        cache = new byte[8192];
        loading = true;
    }

    public void doneLoading() {
        try {
            if (fw != null) {
                fw.close();
                fw = null;
            }
            loading = false;
            raf = new RandomAccessFile(f, "r");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
