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
import com.cisco.mscviewer.*;
import com.cisco.mscviewer.expression.ParsedExpression;
import com.cisco.mscviewer.io.*;
import com.cisco.mscviewer.model.*;
import com.cisco.mscviewer.util.PNGSnapshotTarget;
import com.cisco.mscviewer.util.Utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings("serial")
public class MainFrame extends JFrame implements PNGSnapshotTarget {
    private final static double VER_SPLIT= 0.6;
    private static MainFrame mf;
    private final ViewModel viewModel;
    private final EntityTree entityTree;
    private final EntityList entityList;
    private PyScriptTree scriptTree;
    private final EntityHeader entityHeader;
    private MainPanel mainPanel;
    private final JScrollPane jsp;
    private final LogList logList;
    private final MarkerBar markerBar;
    private final ResultPanel results;
    private final DataPanel data;
    final private JFileChooser jfc;
    private JToggleButton selectBtn, highlightBtn;
    private JSlider zoom;
    private Vector<Vector<String>> filters = new Vector<Vector<String>>();
    private final FilterPanel filterP;
    private Marker currentMarker = Marker.GREEN;
    private static HashMap<String, ImageIcon> imgIcons;
    private final String progName;
    private String fname;
    private boolean isMarking;

    class MouseHandler implements MouseListener, MouseMotionListener {
        
        private void popupMenu(final MouseEvent me) {
            final MSCRenderer r = mainPanel.getMSCRenderer();
            JPopupMenu jpm = new JPopupMenu();
            JMenuItem it = new JMenuItem("Close Entity");
            it.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    Point p = new Point(me.getPoint().x, 0);
                    Entity en = entityHeader.getEntityAt(p);
                    if (en != null) {
                        viewModel.remove(en);
                    }
                }
            });
            jpm.add(it);
            it = new JMenuItem("Close All Entities");
            it.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    viewModel.reset();
                }
            });
            jpm.add(it);
            
            final Interaction inter = r.getSelectedInteraction();
            if (inter != null) {
                final Entity fen, ten;
                if (inter.getFromEvent() != null)
                    fen = inter.getFromEvent().getEntity();
                else
                    fen = null;
                if (inter.getToEvent() != null)
                    ten = inter.getToEvent().getEntity();
                else
                    ten = null;
                if (fen != null) {
                    if (viewModel.indexOf(fen) == -1) {
                        it = new JMenuItem("Open Source");
                        it.addActionListener(new ActionListener(){
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.add(viewModel.indexOf(ten), fen);
                                if (inter.getFromEvent() != null && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter.getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter.getToIndex());
                            }
                        });
                        jpm.add(it);
                    } else {
                        it = new JMenuItem("Close Source");
                        it.addActionListener(new ActionListener(){
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.remove(fen);
                                //entityTree.updateTreeForEntityChange(fen);
                                if (inter.getFromEvent() != null && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter.getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter.getToIndex());
                            }
                        });
                        jpm.add(it);
                    }
                }
                if (ten != null) {
                    if (viewModel.indexOf(ten) == -1) {
                        it = new JMenuItem("Open Sink");
                        it.addActionListener(new ActionListener(){
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.add(viewModel.indexOf(fen)+1, ten);
                                //entityTree.updateTreeForEntityChange(ten);
                                if (inter.getFromEvent() != null && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter.getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter.getToIndex());
                            }
                        });
                        jpm.add(it);
                    } else {
                        it = new JMenuItem("Close Sink");
                        it.addActionListener(new ActionListener(){
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.remove(ten);
                                //entityTree.updateTreeForEntityChange(ten);
                                if (inter.getFromEvent() != null && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter.getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter.getToIndex());
                            }
                        });
                        jpm.add(it);
                    }
                }
            }
            Point p = new Point(me.getPoint().x, 0);
            final Entity en = entityHeader.getEntityAt(p);
            if (en != null) {
                it = new JMenuItem("Select first event");
                it.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        r.setSelectedEventByViewIndex(-1);
                        int idx = viewModel.getFirstEventIndexForEntity(en);
                        r.setSelectedEventByViewIndex(idx);
                    }
                });
                jpm.add(it);
                it = new JMenuItem("Select last event");
                it.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        r.setSelectedEventByViewIndex(-1);
                        int idx = viewModel.getLastEventIndexForEntity(en);
                        r.setSelectedEventByViewIndex(idx);
                    }
                });
                jpm.add(it);

                it = new JMenuItem("Select previous highlighted");
                it.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        Event ev = r.getSelectedEvent();
                        if (ev != null) {
                            for (int idx = viewModel.getViewIndexFromModelIndex(ev.getIndex())-1;
                                 idx >=0; idx--) {
                                ev = viewModel.getEventAt(idx);
                                if (ev.getMarker() != null && ev.getEntity() == en) {
                                    r.setSelectedEventByViewIndex(idx);
                                    break;
                                }       
                            }
                        }
                    }
                });
                jpm.add(it);
                
                it = new JMenuItem("Select next highlighted");
                it.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        Event ev = r.getSelectedEvent();
                        if (ev != null) {
                            for (int idx = viewModel.getViewIndexFromModelIndex(ev.getIndex())+1;
                                 idx <viewModel.getEventCount(); idx++) {
                                ev = viewModel.getEventAt(idx);
                                if (ev.getMarker() != null && ev.getEntity() == en) {
                                    r.setSelectedEventByViewIndex(idx);
                                    break;
                                }       
                            }
                        }
                    }
                });
                jpm.add(it);

                
                it = new JMenuItem("View Selected");
                it.setEnabled(r.getSelectedEvent() != null || r.getSelectedInteraction() != null);    
                it.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        mainPanel.scrollToSelected();
                    }
                });
                jpm.add(it);
            }
            if (jpm.getComponentCount() > 0) {
                jpm.show((Component)me.getSource(), me.getX(), me.getY());
            }
            
        }
        
        @Override
        public void mouseClicked(MouseEvent me) { }
        @Override
        public void mouseEntered(MouseEvent arg0) { }
        @Override
        public void mouseExited(MouseEvent arg0) { }

        private boolean toggleMarker(MouseEvent me, boolean toggle, boolean set) {
            boolean res = true;
            MainPanel mp = getMainPanel();
            JViewport jvp = (JViewport)mp.getParent();
            Rectangle rec = jvp.getViewRect();
            //mp.getMSCRenderer().selectClosest(me.getX(), me.getY(), rec.y, rec.height);
            Object obj = mp.getMSCRenderer().getClosest(me.getX(), me.getY(), rec.y, rec.height);
            if (obj != null) {
                System.out.println("obj is "+obj.getClass().getName());
                if (obj instanceof Event) {
                    Event ev = (Event)obj;
                    if (toggle) {
                        if (ev.getMarker() == null) {
                            ev.setMarker(currentMarker);
                            res = true;
                        } else {
                            ev.setMarker(null);
                            res = false;
                        }
                    } else {
                        if (set)
                            ev.setMarker(currentMarker);
                        else
                            ev.setMarker(null);
                    }
                } else {
                    if (obj instanceof Interaction) {
                        Interaction in = mp.getMSCRenderer().getSelectedInteraction();
                        if (in != null) {
                            if (toggle) {
                                if (in.getMarker() == null)
                                    in.setMarker(currentMarker);
                                else
                                    in.setMarker(null);
                            } else {
                                if (set)
                                    in.setMarker(currentMarker);
                                else
                                    in.setMarker(null);
                            }
                        }
                    }
                }
            } else
                System.out.println("obj is null");
            repaint();
            return res;
        }
        
        @Override
        public void mousePressed(final MouseEvent me) {
            MainPanel mp = getMainPanel();
            if (selectBtn.isSelected()) {
                JViewport jvp = (JViewport)mp.getParent();
                Rectangle rec = jvp.getViewRect();
                mp.requestFocus();
                mp.getMSCRenderer().selectClosest(me.getX(), me.getY(), rec.y, rec.height);
                repaint();
            } else if (highlightBtn.isSelected()) {
//                JViewport jvp = (JViewport)mp.getParent();
                mp.requestFocus();
                //mp.getMSCRenderer().selectClosest(me.getX(), me.getY(), rec.y, rec.height);
                isMarking = toggleMarker(me, true, false);
            }
            if (me.isPopupTrigger())
                popupMenu(me);
        }
        
//        private Rectangle getRect(int x0, int y0, int x1, int y1) {
//            Rectangle r = new Rectangle();
//            if (x0 < x1) {
//                r.x = x0;
//                r.width = x1-x0+1;
//            } else {
//                r.x = x1;
//                r.width = x0-x1+1;
//            }
//            if (y0 < y1) {
//                r.y = y0;
//                r.height = y1-y0+1;
//            } else {
//                r.y = y1;
//                r.height = y0-y1+1;
//            }
//            return r;
//        }
        
        @Override
        public void mouseReleased(MouseEvent me) {
            if (selectBtn.isSelected()) {
//                mainPanel.setSelectionRectangle(null);
            } else if (highlightBtn.isSelected()) {
                
            }
            if (me.isPopupTrigger())
                popupMenu(me);
        }
        
        @Override
        public void mouseDragged(MouseEvent me) {
            if (selectBtn.isSelected()) {
                //mainPanel.setSelectionRectangle(getRect(x0, y0, me.getX(), me.getY()));
            } else  if (highlightBtn.isSelected()) {
                toggleMarker(me, false, isMarking);
            }
        }
        
        @Override
        public void mouseMoved(MouseEvent e) { }
        
    }
    
    public static ImageIcon getImageIcon(String path, String description) {
        return getImageIcon(path, description, -1, -1);
    }
    
    public static ImageIcon getImageIcon(String path, String description, int width, int height) {
        java.net.URL imgURL;
        String actPath;
        boolean shouldRescale = false;
        
        actPath = "com/cisco/mscviewer/resource/" + path;
        ImageIcon im = imgIcons.get(actPath);
        if (im == null) {
            if (width >=0) {
                int idx = actPath.lastIndexOf(".");
                if (idx >= 0 )
                    actPath = actPath.substring(0, idx)+width+"x"+height+actPath.substring(idx);
                else
                    actPath = actPath+width+"x"+height;
            }
            imgURL = ClassLoader.getSystemResource(actPath);
            if (imgURL == null && width >= 0) {
                shouldRescale = true;
                imgURL = ClassLoader.getSystemResource("com/cisco/mscviewer/resource/" + path);
            }
            if (imgURL == null) {
                System.err.println("Couldn't find file: " + actPath+" or  com/cisco/mscviewer/resource/" + path);
                return null;
            }
            im = new ImageIcon(imgURL, description);
            if (shouldRescale)
                im.setImage(im.getImage().getScaledInstance(width, height, 0));
            imgIcons.put(actPath, im);
        }
        return im;
    }
    
    private void initIcons() {
        int W = 25, H = 25;
        getImageIcon(   "highlight_green.png"       , "green", W, H);
        getImageIcon(   "highlight_green32x32.png"  , "green");
        getImageIcon("highlight_yellow.png"     , "yellow", W, H);
        getImageIcon("highlight_yellow32x32.png", "yellow");
        getImageIcon("highlight_blue.png"       , "blue", W, H);
        getImageIcon("highlight_blue32x32.png"  , "blue");
        getImageIcon("highlight_red.png"        , "red", W, H);
        getImageIcon("highlight_red.png"        , "red");
        getImageIcon("select.png"               , "select", W, H);
        getImageIcon("select32x32.png"      , "select");
    }
    
    public MainFrame(int x, int y, int w, int h) {
        imgIcons = new HashMap<String, ImageIcon>();
        MainFrame.mf = this;
        setName("MainFrame");
        setBounds(x, y, w, h);
        progName = "MSCViewer v"+Main.VERSION;
        updateTitle();
        initIcons();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jfc = new JFileChooser();
        getContentPane().setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar("jtb");
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        getContentPane().add(toolBar, BorderLayout.NORTH);
        JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane rightVerticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        getContentPane().add(horizontalSplitPane, BorderLayout.CENTER);
        viewModel = new ViewModel(Main.getDataModel());
        entityHeader = new EntityHeader(viewModel);
        entityTree = new EntityTree(viewModel);
        entityList = new EntityList(viewModel);
        viewModel.addListener(entityList);
        viewModel.setFilter(new CompactViewFilter(viewModel, null));
        
        scriptTree = new PyScriptTree();
        JTabbedPane topLeft = new TopLeftPane(){
            @Override
            public void setSelectedIndex(int idx) {
                if (idx == 1) {
                    scriptTree.initTreeContent();
                }
                super.setSelectedIndex(idx);
            }
        };
        topLeft.setName("top-left");
        topLeft.add("Entities", new JScrollPane(entityTree));
        topLeft.add("Scripts", new JScrollPane(scriptTree));
        JPanel bottomLeft = new JPanel();
        bottomLeft.setLayout(new BorderLayout());
        bottomLeft.add(new JLabel("Open Entities"), BorderLayout.NORTH);
        bottomLeft.add(new JScrollPane(entityList), BorderLayout.CENTER);
        JSplitPane leftPane = new LeftPane(topLeft, bottomLeft);
        leftPane.setOneTouchExpandable(true);
        leftPane.setDividerLocation((int)(VER_SPLIT*h));
        rightVerticalSplitPane.setResizeWeight(1.0);
        
        horizontalSplitPane.setLeftComponent(leftPane);
        horizontalSplitPane.setRightComponent(rightVerticalSplitPane);
        horizontalSplitPane.setResizeWeight(0.3);
        mainPanel = new MainPanel(this, entityHeader, viewModel);
        mainPanel.getMSCRenderer().addSelectionListener(new SelectionListener() {
            @Override
            public void eventSelected(MSCRenderer renderer, Event selectedEvent,
                    int viewEventIndex, int modelEventIndex) {
//                String status = mainPanel.getMSCRenderer().getSelectedStatusString();
            }
            
            @Override
            public void interactionSelected(MSCRenderer renderer, Interaction selectedInteraction) {
//                String status = mainPanel.getMSCRenderer().getSelectedStatusString();
            }
            
        });
        MouseHandler mh = new MouseHandler();
        mainPanel.addMouseListener(mh);
        mainPanel.addMouseMotionListener(mh);
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        FindPanel findP = new FindPanel(mainPanel.getMSCRenderer());
        JPanel tb = new JPanel();
        tb.setLayout(new BoxLayout(tb, BoxLayout.LINE_AXIS));
        tb.add(findP);
        filterP = new FilterPanel(this);
        tb.add(filterP);
        tb.add(Box.createHorizontalStrut(100));
        zoom = new JSlider(10, 100);
        zoom.setValue(100);
        zoom.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int v = zoom.getValue();
                mainPanel.setZoomFactor(v);
                repaint();
            }
        });
        tb.add(zoom);
        tb.add(Box.createHorizontalStrut(20));
        p.add(tb, BorderLayout.NORTH);
        jsp = new CustomJScrollPane(mainPanel);
        jsp.setName("MainPanelJSP");
        
        
        //jsp.setLayout(new com.cisco.mscviewer.gui.ScrollPaneLayout());
        // following is needed to resize the MainPanel when the view is resized
        //		jsp.getViewport().addChangeListener(new ChangeListener(){
        //			@Override
        //			public void stateChanged(ChangeEvent e) {
        //				mainPanel.updateView();
        //			}
        //		});
        p.add(jsp, BorderLayout.CENTER);
        markerBar = new MarkerBar(entityHeader, viewModel, mainPanel.getMSCRenderer());
        viewModel.addListener(markerBar);
        p.add(markerBar, BorderLayout.EAST);
        jsp.setColumnHeaderView(entityHeader);
        jsp.validate();
        rightVerticalSplitPane.setTopComponent(p);
        logList = new LogList(Main.getDataModel(), viewModel);
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Object o = e.getSource();
                @SuppressWarnings("unchecked")
                JList<String> ls = (JList<String>)o;
                int idx = ls.getSelectedIndex();
                mainPanel.getMSCRenderer().selectByLineNumber(idx+1);
                
            }
        });
        
        mainPanel.getMSCRenderer().addSelectionListener(logList);
        JPanel logPanel = new JPanel();
        logPanel.setLayout(new BorderLayout());
        FindSourcePanel fsp = new FindSourcePanel(logList);
        logPanel.add(fsp, BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logList), BorderLayout.CENTER);
        
        results = new ResultPanel(Main.getDataModel());        
        data = new DataPanel();
        mainPanel.getMSCRenderer().addSelectionListener(data);
        
        JTabbedPane jtabbed = new CustomJTabbedPane();
        jtabbed.setName("bottom-right");
        jtabbed.add("input", logPanel);
        jtabbed.add("results", results);
        jtabbed.add("data", data);
        rightVerticalSplitPane.setBottomComponent(jtabbed);
        rightVerticalSplitPane.setDividerLocation((int)(VER_SPLIT*h));
        rightVerticalSplitPane.setResizeWeight(1.0);
        JMenuBar jmb = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(new AbstractAction("Open...") {
            public void actionPerformed(ActionEvent e) {
                MSCDataModel dm = Main.getDataModel();
                String curDir = dm.getOpenPath();
                if (curDir != null)
                    jfc.setCurrentDirectory(new File (curDir));
                int result = jfc.showOpenDialog(null);
                dm.setOpenPath(jfc.getCurrentDirectory().getAbsolutePath());
                switch (result) {
                    case JFileChooser.APPROVE_OPTION:
                        File f = jfc.getSelectedFile();
                        try {
                            dm.reset();
                            System.out.println("f is "+f);
                            Main.getLoader().load(f.getPath(), dm);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        break;
                    case JFileChooser.CANCEL_OPTION:
                        break;
                    case JFileChooser.ERROR_OPTION:
                        System.out.println("Error");
                }
            }}));
        
        fileMenu.add(new JMenuItem(new AbstractAction("Export PNG...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] choices = {"Highlighted Elements", "Open Entities", "Entire Model"};
                String s = (String)JOptionPane.showInputDialog(
                    MainFrame.this,
                    "What image do you want to capture?",
                    "Choose ",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    choices,
                    choices[0]
                );
                int mode;
                if (choices[0].equals(s))
                    mode = PNGSaver.SAVE_MARKED;
                else if (choices[1].equals(s))
                    mode = PNGSaver.SAVE_OPENED;
                else if (choices[2].equals(s))
                    mode = PNGSaver.SAVE_ALL;
                else
                    return;
                int result = jfc.showSaveDialog(null);
                switch (result) {
                    case JFileChooser.APPROVE_OPTION:
                        File f = jfc.getSelectedFile();
                        try {
                            PNGSaver.saveMSCasPNG(f.getAbsolutePath(), mainPanel.getMSCRenderer(), mode);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        break;
                    case JFileChooser.CANCEL_OPTION:
                        System.out.println("Cancel or the close-dialog icon was clicked");
                        break;
                    case JFileChooser.ERROR_OPTION:
                        System.out.println("Error");
                }
            }
        }));
        fileMenu.add(new JMenuItem(new AbstractAction("Print...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                PrinterJob pj = PrinterJob.getPrinterJob();
                pj.setPrintable(new MSCPrintable(mainPanel.getMSCRenderer()));
                if (pj.printDialog()) {
                    try {pj.print();}
                    catch (PrinterException exc) {
                        System.out.println(exc);
                    }
                }
            }
        }));
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem(new AbstractAction("Find...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FindDialog fd = new FindDialog(MainFrame.this);
                fd.setVisible(true);
            }
        }));
        editMenu.add(new JMenuItem(new AbstractAction("Clear Highlights") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int res = JOptionPane.showConfirmDialog(MainFrame.this, "Are you sure you want to remove all highlights?", "Clear Highlights", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    MSCDataModel dm = mainPanel.getDataModel();
                    dm.clearMarkers();
                    repaint();
                }
            }
        }));
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(new JMenuItem(new AbstractAction("Preferences...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MSCRenderer r = mainPanel.getMSCRenderer();
                PrefsDialog d = new PrefsDialog(MainFrame.this, r.getTimeScaleFactor(), r.getAbsTimeUnit(), r.getDeltaTimeUnit());
//                d.setInputUnit(r.getTimestampUnit());
                d.setAbsoluteOutputUnit(r.getAbsTimeUnit());
                d.setDeltaOutputUnit(r.getDeltaTimeUnit());
                d.setShowUnits(r.getShowUnits());
                d.setShowDate(r.getShowDate());
                d.setShowLeadingZeroes(r.getShowLeadingZeroes());
                d.setCompactView(r.getCompactView());
                d.setVisible(true);
                if (d.approved()) {
//                    r.setTimestampUnit(d.getInputUnit());
                    r.setAbsTimeUnit(d.getAbsoluteOutputUnit());
                    r.setDeltaTimeUnit(d.getDeltaOutputUnit());
                    r.setShowUnits(d.getShowUnits());
                    r.setShowDate(d.getShowDate());
                    r.setShowLeadingZeroes(d.getShowLeadingZeroes());
                    r.updateForTimeUnitChanges();
                    r.setCompactView(d.getCompactView());
                    mainPanel.updateView();
                }
            }
        }));
        viewMenu.add(new JMenuItem(new AbstractAction("Filters...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FilterDialog d = new FilterDialog(MainFrame.this, filters);
                d.setVisible(true);
                if (d.approved()) {
                    filters = d.getFilters();
//                    String curSel = (String)filterCB.getSelectedItem();
//                    filterCB.removeAllItems();
//                    initFilterCB();
//                    filterCB.setSelectedItem(curSel);
                }
            }
        }));
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem(new AbstractAction("About MSCViewer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame f = new HelpFrame();
                f.setVisible(true);
            }
        }));
        if (Main.extra) {
            PNGSnapshotTarget[] comps = Utils.getPNGSnapshotTargets(this);
            viewMenu.addSeparator();
            for (final PNGSnapshotTarget currComp : comps) {
                viewMenu.add(new JMenuItem(new AbstractAction("Capture " + ((Component) currComp).getName()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Utils.getPNGSnapshot(currComp);
                    }
                }));
            }
        }
        jmb.add(fileMenu);
        jmb.add(editMenu);
        jmb.add(viewMenu);
        jmb.add(helpMenu);
        setJMenuBar(jmb);
        populateToolbar(toolBar);
        setMainPanelCursor(getImageIcon("select.png", "", 32, 32).getImage(), 0, 0);

    }
    
//    private void initFilterCB() {
//        filterCB.addItem("<none>");
//        for(int i=0; i<filters.size(); i++) {
//            filterCB.addItem(filters.elementAt(i).elementAt(0));
//        }
//    }

    private void setMainPanelCursor(Image img, int px, int py) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Cursor c = toolkit.createCustomCursor(img , new Point(px, py), "img");
        mainPanel.setCursor(c);
    }
    
    private void updateHighlightToolImage() {
        int W = MSCRenderer.EVENT_HEIGHT;
        switch(currentMarker) {
            case YELLOW:
                highlightBtn.setIcon(getImageIcon("highlight_yellow.png" ,"Yellow Highlighter", W, W ));
                Image img = getImageIcon("highlight_yellow.png", "", 32, 32).getImage();
                setMainPanelCursor(img, 0, img.getHeight(null)-1);
                break;
            case BLUE:
                highlightBtn.setIcon(getImageIcon("highlight_blue.png", "Blue Highlighter", W, W));
                img = getImageIcon("highlight_blue.png", "", 32, 32).getImage();
                setMainPanelCursor(img, 0, img.getHeight(null)-1);
                break;
            case GREEN:
                highlightBtn.setIcon(getImageIcon("highlight_green.png", "Highlighter", W, W));
                img = getImageIcon("highlight_green.png", "", 32, 32).getImage();
                setMainPanelCursor(img, 0, img.getHeight(null)-1);
                break;
            case ERROR:
            case RED:
                highlightBtn.setIcon(getImageIcon("highlight_red.png", "Red Highlighter", W, W));
                img = getImageIcon("highlight_red.png", "", 32, 32).getImage();
                setMainPanelCursor(img, 0, img.getHeight(null)-1);
                break;
        }
        highlightBtn.repaint();
    }
    
    private void populateToolbar(JToolBar bar){
        int W = MSCRenderer.EVENT_HEIGHT;
        ButtonGroup bg = new ButtonGroup();
        
        ImageIcon ii = getImageIcon("select.png", "select", W, W);
        selectBtn = new JToggleButton(ii);        
        selectBtn.setMargin(new Insets(0, 0, 0, 0));        
        selectBtn.setToolTipText(ii.getDescription());
        bg.add(selectBtn);
        bar.add(selectBtn);
        selectBtn.setSelected(true);
        
        ii = getImageIcon("highlight_green.png", "Green Highlighter", W, W);
        highlightBtn = new JToggleButton(ii);
        highlightBtn.setMargin(new Insets(0, 0, 0, 0));        
        highlightBtn.setToolTipText(ii.getDescription());
        bg.add(highlightBtn);
        bar.add(highlightBtn);
        selectBtn.setSelected(true);
        selectBtn.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectBtn.isSelected())
                    setMainPanelCursor(getImageIcon("select.png", "", 32, 32).getImage(), 0, 0);                
            }            
        });
        highlightBtn.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }
            @Override
            public void mousePressed(MouseEvent e) {
                
                class ColorIcon implements Icon {
                    final static int W = 32;
                    final static int H = 16;
                    GradientPaint p;
                    
                    public ColorIcon(Color c1, Color c2) {
                        p = new GradientPaint(0, 0, c1, H, W, c2);
                    }
                    
                    @Override
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        ((Graphics2D)g).setPaint(p);
                        ((Graphics2D)g).fill(new Rectangle2D.Float(0,0, getIconWidth(), getIconHeight()));
                    }
                    
                    @Override
                    public int getIconWidth() {
                        return W;
                    }
                    
                    @Override
                    public int getIconHeight() {
                        return H;
                    }
                }
                JPopupMenu jpm = new JPopupMenu();
                JMenuItem y = new JMenuItem(new ColorIcon(Marker.YELLOW.getColor().darker(), Marker.YELLOW.getColor()));
                y.setAccelerator(KeyStroke.getKeyStroke("control 1"));
                y.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        currentMarker = Marker.YELLOW;
                        updateHighlightToolImage();
                    }
                });
                jpm.add(y);
                JMenuItem b = new JMenuItem(new ColorIcon(Marker.BLUE.getColor().darker(), Marker.BLUE.getColor()));
                b.setAccelerator(KeyStroke.getKeyStroke("control 2"));
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        currentMarker = Marker.BLUE;
                        updateHighlightToolImage();
                    }
                });
                jpm.add(b);
                JMenuItem g = new JMenuItem(new ColorIcon(Marker.GREEN.getColor().darker(), Marker.GREEN.getColor()));
                g.setAccelerator(KeyStroke.getKeyStroke("control 3"));
                g.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        currentMarker = Marker.GREEN;
                        updateHighlightToolImage();
                    }
                });
                jpm.add(g);
                JMenuItem r = new JMenuItem(new ColorIcon(Marker.ERROR.getColor().darker(), Marker.ERROR.getColor()));
                r.setAccelerator(KeyStroke.getKeyStroke("control 4"));
                r.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        currentMarker = Marker.ERROR;
                        updateHighlightToolImage();
                    }
                });
                jpm.add(r);
                highlightBtn.setSelected(true);
                jpm.show((Component)e.getSource(), e.getX(), e.getY());
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub
                
            }
            
        });
        bar.add(Box.createHorizontalStrut(100));
        //		FindPanel findP = new FindPanel(mainPanel.getMSCRenderer());
        //		//findP.setPreferredSize(new Dimension(500, 30));
        //		findP.setMinimumSize(new Dimension(500, 30));
        //		findP.setMaximumSize(new Dimension(500, 30));
        //		bar.add(findP);
        //		bar.add(Box.createHorizontalStrut(100));
        //		filterP = new FilterPanel(this);
        //		filterP.setMinimumSize(new Dimension(500, 30));
        //		filterP.setMaximumSize(new Dimension(500, 30));
        //		bar.add(filterP);
    }
    
    
    public MSCDataModel getDataModel() {
        return Main.getDataModel();
    }
    
    int getLifeLineY(int entityIdx) {
        return entityHeader.getEntityCenterX(entityIdx);
    }
    
    
    int getEntitiesTotalWidth() {
        return entityHeader.getEntitiesTotalWidth();
    }
    
    public MainPanel getMainPanel(){
        return mainPanel;
    }
        
    public EntityHeader getEntityHeader() {
        return entityHeader;
    }
    
    public boolean filteringEnabled() {
        return filterP.enabled();
    }
    
    public ParsedExpression getFilterExpression() {
        return filterP.getParsedExpression();
    }
    
    public void updateTree(Entity en) {
        entityTree.updateTreeForEntityChange(en);
    }
    
    public static MainFrame getInstance() {
        return mf;
    }
    
    public final void updateTitle() {
        if (fname != null) {
            char[] charArray = new char[fname.length()];
            Arrays.fill(charArray,' ');
            String pad = new String(charArray);
            setTitle(pad+"      "+progName+"      "+fname);
        } else
            setTitle(progName);
    }
    public void setFilename(String fname) {
        this.fname =fname;
        updateTitle();
    }
    
    public ResultPanel getResultPanel() {
        return results;
    }
    
    @Override
    public Component getPNGSnapshotClient() {
        return null;
    }
    
    public ViewModel getViewModel() {
        return viewModel;
    }

    public void addResult(String res) {
        getResultPanel().append(res);    
    }
    
    public void showData(JSonObject obj) {
        data.setModel(obj);
    }
}
