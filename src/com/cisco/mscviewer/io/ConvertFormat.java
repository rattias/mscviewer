package com.cisco.mscviewer.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cisco.mscviewer.Main;

abstract class Info {
    ArrayList<String> kv = new ArrayList<String>();   
}

class Interaction extends Info {
    String pairingId;
}


class EventInfo extends Info {
    Interaction source;
    Interaction sink;
    String entityId;
    ArrayList<String> kv = new ArrayList<String>();
}

class EntityInfo extends Info {
    String id;
    String name;
}


/** Added comment to test commit close */
public class ConvertFormat {
    private static String keyValuePairPattern = "([a-zA-Z_]+)\\s*=\\s*\"([^\"]+)\"";
//    private static Pattern pv1 = Pattern.compile("entity_id\\s*=\\s*\".+\"");
    private static Pattern eventElPat = Pattern
            .compile(keyValuePairPattern+"|(@msc_source)|(@msc_sink)");
    private static Pattern entityElPat = Pattern
            .compile(keyValuePairPattern);
    private final static int IN_FMT_V1 = 1; // v1.0

    private final static int GRP_KEY = 1;
    private final static int GRP_VALUE = 2;
    private final static int GRP_SOURCE = 3;
    private final static int GRP_SINK = 4;
    private static boolean append = false;
    
    private static int parseOptions(String[] args) {
        int i;
        for(i=0; i<args.length && args[i].startsWith("-"); i++) {
            if (args[i].equals("-a"))
                append = true;
        }
        return i;
    }

    private static final boolean isEqual(String str, int off, String str1) {
        for(int i=0; i < str1.length(); i++)
            if (str.charAt(off+i) != str1.charAt(i))
                return false;
        return true;
    }
    
    public static void main(String args[]) throws IOException {
        int fmt = 0;

        int optEnd = parseOptions(args);
        if (args.length - optEnd != 2) {
            System.err.println("Syntax: mscupgrade [options] <input file> <output file>");
            System.err.println("   Supported options:");
            System.err.println("   -a:  append converted lines after corresponding original lines instead of replacing them");
            System.exit(1);
        }

        final String fin = args[optEnd];
        final BufferedReader br = new BufferedReader(new FileReader(fin), 32768);
        String line = null;
        PrintStream ps;
        ps = new PrintStream(args[optEnd+1]);
        System.out.println("converting file " + args[optEnd] + "...");
        long lineCount = 0;
        long t0 = System.currentTimeMillis();
        try {
            while ((line = br.readLine()) != null) {
                lineCount++;
                int idx = line.indexOf("@msc_"); 
                if (idx == -1) {
                    ps.println(line);
                    continue;
                } else if (isEqual(line, idx+5, "event")) {
                    emitEvent(ps, line);
                } else if (isEqual(line, idx+5, "entity")) {
                    emitEntity(ps, line);
                }
            }
            long t1 = System.currentTimeMillis();
            float elapsed = ((float)(t1-t0))/1000;
            int rate = (int)(lineCount/elapsed);
            System.out.println("conversion completed. "+lineCount+" lines processed  in "+elapsed+"s ("+rate+" lines/s)");
        } catch (final Exception ex) {
            Logger logger = Logger.getLogger(Main.class.getName());
            logger.log(Level.INFO,"exception while processing line " + line, ex);
        } finally {
            if (ps != System.out)
                ps.close();
            br.close();

        }
    }

    private static void emitEvent(PrintStream ps, String line) {
        if (append)
            ps.println(line.replace("@msc_event", "@msc-event"));
        EventInfo event = new EventInfo();
        Interaction inter = null;
        final Matcher m = eventElPat.matcher(line);
        while (m.find()) {
            if (m.group(GRP_KEY) != null) {
                String key = m.group(GRP_KEY);
                final String value = m.group(GRP_VALUE);
                if (inter == null) {
                    if (key.equals("entity_id")) {
                        event.entityId = value;
                    } else {
                        event.kv.add(key);
                        event.kv.add(value);
                    }
                } else {
                    if (key.equals("pairing_id"))
                        inter.pairingId = value;
                    else {
                        inter.kv.add(key);
                        inter.kv.add(value);
                    }
                } 
            } else if (m.group(GRP_SOURCE) != null) {
                event.source = inter = new Interaction();
            } else if (m.group(GRP_SINK) != null) {
                event.sink = inter = new Interaction();
            }  
        }
        StringBuilder sb = new StringBuilder("@msc_event {\"entity\":\"");
        sb.append(event.entityId);
        sb.append("\", ");
        
        appendKeyValuePairs(sb, event.kv);
        if (event.source != null) {
            sb.append(", \"src\":");
            if (event.source.kv.size() > 0) {
                sb.append("{\"id\":\"");                            
                sb.append(event.source.pairingId);
                sb.append("\", ");                            
                appendKeyValuePairs(sb, event.source.kv);
                sb.append("}");                            
            } else {
                sb.append("\"");                            
                sb.append(event.source.pairingId);
                sb.append("\"");                            
            }
        }
        if (event.sink != null) {
            sb.append(", \"dst\":");
            if (event.sink.kv.size() > 0) {
                sb.append("{\"id\":\"");                            
                sb.append(event.sink.pairingId);
                sb.append("\", ");                            
                appendKeyValuePairs(sb, event.sink.kv);
                sb.append("}");                            
            } else {
                sb.append("\"");                            
                sb.append(event.sink.pairingId);
                sb.append("\"");                            
            }
        }
        sb.append("}");
        ps.println(sb.toString());
    }
    
    private static final void emitEntity(PrintStream ps, String line) {
        if (append)
            ps.println(line.replace("@msc_entity", "@msc-entity"));
        final Matcher m = entityElPat.matcher(line);
        EntityInfo entity = new EntityInfo();
        while (m.find()) {
            if (m.group(GRP_KEY) != null) {
                String key = m.group(GRP_KEY);
                final String value = m.group(GRP_VALUE);
                if (key.equals("display_name"))
                    key = "name";
                entity.kv.add(key);
                entity.kv.add(value);
                
            }
        }
        StringBuilder sb = new StringBuilder("@msc_entity { ");
        appendKeyValuePairs(sb, entity.kv);
        sb.append("}");
        ps.println(sb.toString());
    }

    public static void appendKeyValuePairs(StringBuilder sb, ArrayList<String> kvp) {
        int sz = kvp.size();
        for(int i=0; i<sz-2; i+= 2) {
            sb.append('\"');
            sb.append(kvp.get(i));
            sb.append("\":\"");
            sb.append(kvp.get(i+1));
            sb.append("\", ");
        }
        sb.append('\"');
        sb.append(kvp.get(sz-2));
        sb.append("\":\"");
        sb.append(kvp.get(sz-1));
        sb.append('\"');
    }
}
