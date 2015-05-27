package com.cisco.mscviewer;

public class TestVersion {
    private static final int REQUIRED_JAVA_MAJ_VER = 1;
    private static final int REQUIRED_JAVA_MIN_VER = 8;
    
    public static void main(String args[]) {
        String ver = System.getProperty("java.version");
        int i;
        int maj = 0;
        int min = 0;
        for(i=0; i<ver.length() && Character.isDigit(ver.charAt(i)); i++) {
            maj = maj*10+(ver.charAt(i)-'0');        
        }
        for(i++; i<ver.length() && Character.isDigit(ver.charAt(i)); i++) {
            min = min*10+(ver.charAt(i)-'0');        
        }
        if (maj<REQUIRED_JAVA_MAJ_VER || (maj == REQUIRED_JAVA_MAJ_VER && min < REQUIRED_JAVA_MIN_VER)) {
            System.err.println("ERROR: Java interpreter V"+maj+"."+min+" in path is too old. Please make sure the java interpreter in the path is V"+REQUIRED_JAVA_MAJ_VER+"."+REQUIRED_JAVA_MIN_VER+" or greater.");
            System.exit(1);
        }
    }
}
