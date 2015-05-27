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
package com.cisco.mscviewer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

class FromEv {

    String srcEn;
    String pairingId;

    FromEv(String en, String id) {
        srcEn = en;
        this.pairingId = id;
    }
}

public class TBGen {

    static int entityCount, evCount;

    private static void usage() {
        System.out
                .println("java TBGen <entity-count> <ev-count> [<file name>]");
    }

    public static void main(String args[]) throws FileNotFoundException {
        if (args.length < 2 || args.length > 3) {
            usage();
        }
        entityCount = Integer.parseInt(args[0]);
        evCount = Integer.parseInt(args[1]);
        PrintStream out;
        if (args.length == 3) {
            out = new PrintStream(new File(args[2]));
        } else {
            out = System.out;
        }
        try {
            for (int i = 0; i < entityCount; i++) {
                out.println("@msc_entity id=\"en" + i + "\" display_name=\"en/"
                        + i + "\"");
            }
            final ArrayList<FromEv> al = new ArrayList<FromEv>();
            int t = 0;
            int id = 0;
            do {
                int l = al.size();
                boolean cons;
                do {
                    final double f = Math.random();
                    cons = (l > 10 || f < l / 10.0);
                    if (cons) {
                        final int dstEnIdx = (int) (Math.random() * entityCount);
                        final FromEv ev = al.remove(0);
                        out.println("@msc_event type=\"sink\" entity_id=\"en"
                                + dstEnIdx + "\" time=\"" + t
                                + "\" pairing_id=\"" + ev.pairingId + "\"");
                        t += 1 + (int) (10 * Math.random());
                        l--;
                    }
                } while (cons);
                final double r = Math.random();
                final int enIdx = (int) (r * entityCount);
                out.println("@msc_event type=\"source\" entity_id=\"en" + enIdx
                        + "\" time=\"" + t + "\" label=\"foo" + id
                        + "\" pairing_id=\"en" + enIdx + "/" + id + "\"");
                al.add(new FromEv("en" + enIdx, "en" + enIdx + "/" + id));
                id++;
                t += 1 + (int) (10 * Math.random());
            } while (id < evCount);
            for (final FromEv ev : al) {
                final int dstEnIdx = (int) ((Math.random()) * entityCount);
                out.println("@msc_event type=\"sink\" entity_id=\"en"
                        + dstEnIdx + "\" time=\"" + t + "\" pairing_id=\""
                        + ev.pairingId + "\"");
                t += 1 + (int) (10 * Math.random());
            }
        } finally {
            if (out != System.out)
                out.close();
        }
    }

}
