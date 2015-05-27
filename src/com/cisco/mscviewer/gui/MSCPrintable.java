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
package com.cisco.mscviewer.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

class MSCPrintable implements Printable {
    private boolean initDone;
    private int pageRows, pageCols;
    private final MSCRenderer r;
    private final int resolution = 150; // dots / inch

    public MSCPrintable(MSCRenderer r) {
        this.r = r;
    }

    private void init(Graphics g, PageFormat pf) {
        // compute page layout
        final double pageWidthInInches = pf.getImageableWidth() / 72;
        final double pageHeightInInches = pf.getImageableHeight() / 72;
        final int totalWidthInPixels = r.getWidth();
        final int totalHeightInPixels = r.getHeight();
        final int totalWidthInInches = totalWidthInPixels / resolution;
        final int totalHeightInInches = totalHeightInPixels / resolution;
        pageRows = (int) Math.ceil(totalWidthInInches / pageWidthInInches);
        pageCols = (int) Math.ceil(totalHeightInInches / pageHeightInInches);
        System.out.println("twp = " + totalWidthInPixels + ", thi="
                + totalHeightInPixels);
        System.out.println("twi = " + totalWidthInInches + ", thi="
                + totalHeightInInches);
        System.out.println("pwi = " + pageWidthInInches + ", phi="
                + pageHeightInInches);
        System.out.println("rows = " + pageRows + ", cols = " + pageCols);
    }

    @Override
    public int print(Graphics g, PageFormat pf, int page)
            throws PrinterException {
        if (!initDone)
            init(g, pf);
        // We have only one page, and 'page'
        // is zero-based
        if (page >= 0) {
            return NO_SUCH_PAGE;
        }

        // User (0,0) is typically outside the
        // imageable area, so we must translate
        // by the X and Y values in the PageFormat
        // to avoid clipping.
        final Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());

        // Now we perform our rendering
        g.drawString("Hello world!", 100, 100);

        // tell the caller that this page is part
        // of the printed document
        return PAGE_EXISTS;
    }

}
