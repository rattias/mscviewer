package com.cisco.mscviewer.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Added comment to test commit close */
public class ConvertFormat {
    private static Pattern pv1 = Pattern.compile("entity_id\\s*=\\s*\".+\"");
    private static Pattern pv1_el = Pattern
            .compile("(@msc_entity)|(@msc_event)|(@msc_source)|(@msc_sink)|([a-zA-Z_]+)\\s*=\\s*\"([^\"]+)\"");
    private final static int IN_FMT_V1 = 1; // v1.0

    private final static int GRP_ENTITY = 1;
    private final static int GRP_EVENT = 2;
    private final static int GRP_SOURCE = 3;
    private final static int GRP_SINK = 4;
    private final static int GRP_KEY = 5;
    private final static int GRP_VALUE = 6;
    private static boolean append = false;
    
    private static int parseOptions(String[] args) {
        int i;
        for(i=0; i<args.length && args[i].startsWith("-"); i++) {
            if (args[i].equals("-a"))
                append = true;
        }
        return i;
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
        final BufferedReader br = new BufferedReader(new FileReader(fin));
        String line = null;
        PrintStream ps;
        ps = new PrintStream(args[optEnd+1]);
        System.out.println("converting file " + args[optEnd] + "...");
        try {
            while ((line = br.readLine()) != null) {
                if (line.indexOf("@msc") == -1) {
                    ps.println(line);
                    continue;
                }
                if (append)
                    ps.println(line.replace("@msc_event", "@msc-event").replace("@msc_entity", "@msc-entity"));
                if (fmt == 0) {
                    if (line.indexOf("@msc_event") != -1
                            && pv1.matcher(line).find()) {
                        // v1.0 format
                        fmt = IN_FMT_V1;
                    }
                }
                abstract class Info {
                }
                ;
                class EventInfo extends Info {
                    String srcOrSink;
                    String pairingId;
                    ArrayList<String> kv = new ArrayList<String>();
                }
                ;
                class EntityInfo extends Info {
                    String id;
                    String name;
                }
                ;
                final ArrayList<Info> els = new ArrayList<Info>();
                switch (fmt) {
                case IN_FMT_V1:
                    final Matcher m = pv1_el.matcher(line);
                    Info curr = null;
                    while (m.find()) {
                        if (m.group(GRP_ENTITY) != null) {
                            curr = new EntityInfo();
                            els.add(curr);
                        } else if (m.group(GRP_EVENT) != null) {
                            curr = new EventInfo();
                            els.add(curr);
                        } else if (m.group(GRP_SOURCE) != null) {
                            curr = new EventInfo();
                            els.add(curr);
                            ((EventInfo) curr).srcOrSink = "src";
                        } else if (m.group(GRP_SINK) != null) {
                            curr = new EventInfo();
                            els.add(curr);
                            ((EventInfo) curr).srcOrSink = "dst";
                        } else if (m.group(GRP_KEY) != null) {
                            String key = m.group(GRP_KEY);
                            final String value = m.group(GRP_VALUE);
                            if (curr instanceof EventInfo) {
                                if (key.equals("pairing_id"))
                                    ((EventInfo) curr).pairingId = value;
                                else {
                                    if (key.equals("entity_id"))
                                        key = "entity";
                                    ((EventInfo) curr).kv.add(key);
                                    ((EventInfo) curr).kv.add(value);
                                }
                            } else if (curr instanceof EntityInfo) {
                                if (key.equals("id")) {
                                    ((EntityInfo) curr).id = value;
                                } else if (key.equals("display_name")) {
                                    ((EntityInfo) curr).name = value;
                                }
                            }
                        }
                    }
                    for (final Info inf : els) {
                        if (inf instanceof EventInfo) {
                            final EventInfo ei = (EventInfo) inf;
                            if (ei.srcOrSink != null) {
                                if (ei.kv.size() > 0)
                                    ps.print(", \"" + ei.srcOrSink
                                            + "\":{\"id\":\"" + ei.pairingId
                                            + "\", ");
                                else
                                    ps.print(", \"" + ei.srcOrSink + "\":\""
                                            + ei.pairingId + "\"");
                            } else
                                ps.print("@msc_event {");

                            for (int i = 0; i < ei.kv.size(); i += 2) {
                                if (i != 0)
                                    ps.print(", ");
                                final String k = ei.kv.get(i);
                                final String v = ei.kv.get(i + 1);
                                ps.print("\"" + k + "\":\"" + v + "\"");
                            }
                            if (ei.srcOrSink != null && ei.kv.size() > 0)
                                ps.print("}");
                        } else if (inf instanceof EntityInfo) {
                            final EntityInfo ei = (EntityInfo) inf;
                            ps.print("@msc_entity { \"id\":\"" + ei.id
                                    + "\", \"name\":\"" + ei.name + "\"}");
                        }
                    }
                    ps.println("}");
                    break;
                }

            }
            System.out.println("conversion completed.");
        } catch (final Exception ex) {
            System.err.println("exception while processing line " + line);
        } finally {
            if (ps != System.out)
                ps.close();
            br.close();

        }
    }
}
