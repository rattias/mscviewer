package com.cisco.mscviewer.util;

import java.io.File;

public class Prefs {
    private static String WORKDIR_PATH = System.getProperties().getProperty("user.home")+"/.msc";
    
    public static String getWorkDirPath() {
        File f = new File(WORKDIR_PATH);
        if (! f.exists()) {
            f.mkdirs();
        }
        return WORKDIR_PATH;
    }
}
