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
import com.cisco.mscviewer.util.Resources;
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
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

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
    private final JLabel info;
    final private JFileChooser jfc;
    private JToggleButton selectBtn, showBlocksBtn;
    private JSlider zoom;
    private Vector<Vector<String>> filters = new Vector<Vector<String>>();
    private final FilterPanel filterP;
    private Marker currentMarker = null;
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
                        updateInfo(null);
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
                        updateInfo(null);
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
                                    updateInfo(null);                                    
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
                                    updateInfo(null);
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
            } else {
            }
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
            } else if (currentMarker != null) {
                mp.requestFocus();
                isMarking = toggleMarker(me, true, false);
            }
            if (me.isPopupTrigger())
                popupMenu(me);
        }
        
        
        @Override
        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger())
                popupMenu(me);
        }
        
        @Override
        public void mouseDragged(MouseEvent me) {
            if (selectBtn.isSelected()) {
                //mainPanel.setSelectionRectangle(getRect(x0, y0, me.getX(), me.getY()));
            } else  if (currentMarker != null) {
                toggleMarker(me, false, isMarking);
            }
        }
        
        @Override
        public void mouseMoved(MouseEvent me) {
            MainPanel mp = getMainPanel();
            JViewport jvp = (JViewport)mp.getParent();
            Rectangle rec = jvp.getViewRect();
            Object obj = mp.getMSCRenderer().getClosest(me.getX(), me.getY(), rec.y, rec.height);
            if (obj instanceof Event) {
                updateInfo((Event)obj);
            }
        }
        
    }
    
    
    public MainFrame(int x, int y, int w, int h) {
        MainFrame.mf = this;
        setName("MainFrame");
        setBounds(x, y, w, h);
        progName = "MSCViewer v"+Main.VERSION;
        updateTitle();
        
        List<Image> icons = new ArrayList<Image>();
        icons.add(Resources.getImageIcon("64x64/mscviewer.png", "mscviewer").getImage());
        setIconImages(icons);
        
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
        info = new JLabel("");
        info.setBorder(BorderFactory.createEtchedBorder());
        getContentPane().add(info, BorderLayout.SOUTH);
        viewModel = new ViewModel(Main.getDataModel());
        entityHeader = new EntityHeader(viewModel);
        entityTree = new EntityTree(viewModel);
        entityList = new EntityList(viewModel);
        viewModel.addListener(entityList);
        viewModel.setFilter(new CompactViewFilter(viewModel, null));
        
        scriptTree = new PyScriptTree(mainPanel);
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
                String status = mainPanel.getMSCRenderer().getSelectedStatusString();
                info.setText(status);
            }
            
            @Override
            public void interactionSelected(MSCRenderer renderer, Interaction selectedInteraction) {
                String status = mainPanel.getMSCRenderer().getSelectedStatusString();
                info.setText(status);
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
        JMenuItem mi = new JMenuItem(new AbstractAction("Load...") {
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
                            viewModel.reset();
                            dm.reset();
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
            }});
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
        fileMenu.add(mi);
        
        mi = new JMenuItem(new AbstractAction("Reload") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MSCDataModel dm = Main.getDataModel();
                try {
                    dm.reset();
                    Main.getLoader().load(dm.getFilePath(), dm);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }            
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
        fileMenu.add(mi);

        mi = new JMenuItem(new AbstractAction("Export PNG...") {
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
                        break;
                    case JFileChooser.ERROR_OPTION:
                        System.out.println("Error");
                }
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        fileMenu.add(mi);
        
        mi = new JMenuItem(new AbstractAction("Print...") {
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
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
        fileMenu.add(mi);
        
                
        JMenu editMenu = new JMenu("Edit");
        mi = new JMenuItem(new AbstractAction("Find...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FindDialog fd = new FindDialog(MainFrame.this);
                fd.setVisible(true);
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK));
        editMenu.add(mi);
        
        mi = new JMenuItem(new AbstractAction("Clear Highlights") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int res = JOptionPane.showConfirmDialog(MainFrame.this, "Are you sure you want to remove all highlights?", "Clear Highlights", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    MSCDataModel dm = mainPanel.getDataModel();
                    dm.clearMarkers();
                    repaint();
                }
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
        editMenu.add(mi);

        JMenu viewMenu = new JMenu("View");
        mi = new JMenuItem(new AbstractAction("Preferences...") {
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
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
        viewMenu.add(mi);
        
        viewMenu.add(new JMenuItem(new AbstractAction("Filters...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FilterDialog d = new FilterDialog(MainFrame.this, filters);
                d.setVisible(true);
                if (d.approved()) {
                    filters = d.getFilters();
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
        setMainPanelCursor(Resources.getImageIcon("32x32/select.png", "").getImage(), 0, 0);

    }
    
//    private void initFilterCB() {
//        filterCB.addItem("<none>");
//        for(int i=0; i<filters.size(); i++) {
//            filterCB.addItem(filters.elementAt(i).elementAt(0));
//        }
//    }

    private void setMainPanelCursor(Image img, int px, int py) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
//        BufferedImage b_argb = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
//        b_argb.getGraphics().drawImage(img, 0,0, null);
//        for(int y=1; y<31; y++) {
//        	for(int x=1; x<31; x++) {
//    			int pxl = b_argb.getRGB(x, y);
//    			int alpha = (pxl>>24) & 0xFF;
//        	    if (alpha <128)
//        	    	alpha = 0;
//        	    else {
//                	if (alpha <255)
//                		pxl = 0xFFFF;
//        	    	alpha = 255;
//        	    }
//        	    b_argb.setRGB(x, y, (pxl & 0xFFFFFF) | (alpha<<24));
//        	}
//        }
        Cursor c = toolkit.createCustomCursor(img , new Point(px, py), "img");
        mainPanel.setCursor(c);
    }
    
 
    
    private void populateToolbar(JToolBar bar){
        final String sz = "32x32/";
        final String hsz = "16x16/";
        bar.add(Box.createHorizontalStrut(10));

        ButtonGroup toolGroup = new ButtonGroup();
        
        ImageIcon ii = Resources.getImageIcon(sz+"select.png", "select");
        selectBtn = new JToggleButton(ii);        
        selectBtn.setMargin(new Insets(1, 1, 1, 1));
        //selectBtn.setBorder(BorderFactory.createRaisedBevelBorder());
        selectBtn.setToolTipText(ii.getDescription());
        selectBtn.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectBtn.isSelected()) {
                    setMainPanelCursor(Resources.getImageIcon(sz+"select.png", "").getImage(), 0, 0);                
                    currentMarker = null;
                }
            }            
        });        toolGroup.add(selectBtn);
        bar.add(selectBtn);
        selectBtn.setSelected(true);
        String colors[] = {"Green", "Blue", "Yellow", "Red"};
        for(String c: colors) {
        	ii = Resources.getImageIcon(sz+"highlight_"+c.toLowerCase()+".png", c+" Highlighter");
        	JToggleButton highlightBtn = new JToggleButton(ii);
        	highlightBtn.setName(c);
        	highlightBtn.setMargin(new Insets(1, 1, 1, 1));        
        	highlightBtn.setToolTipText(ii.getDescription());
        	toolGroup.add(highlightBtn);
        	bar.add(highlightBtn);
            highlightBtn.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                	JToggleButton jtb = (JToggleButton)e.getSource();
                    if (jtb.isSelected()) {
                    	String color = jtb.getName().toLowerCase();
                        setMainPanelCursor(Resources.getImageIcon(sz+"highlight_"+color+".png", "").getImage(), 0, 0);
                        if (color.equals("green"))
                        	currentMarker = Marker.GREEN;
                        else if (color.equals("blue"))
                        	currentMarker = Marker.BLUE;
                        else if (color.equals("yellow"))
                        	currentMarker = Marker.YELLOW;
                        else if (color.equals("red"))
                        	currentMarker = Marker.RED;
                    }
                }            
            });
        }


        bar.add(Box.createHorizontalStrut(100));

        ii = Resources.getImageIcon(sz+"blocks.png", "show blocks");
        if (ii == null)
        	throw new Error("missing blocks.png");
        showBlocksBtn = new JToggleButton(ii);
        bar.add(showBlocksBtn);
        showBlocksBtn.setMargin(new Insets(1, 1, 1, 1));        
        showBlocksBtn.setToolTipText(ii.getDescription());
        showBlocksBtn.setSelected(true);
        showBlocksBtn.addActionListener(new ActionListener(){
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		mainPanel.getMSCRenderer().setShowBlocks(showBlocksBtn.isSelected());
        		mainPanel.repaint();
        	}
        });
        ii = Resources.getImageIcon(sz+"time.png", "show timestamps");
        if (ii == null)
        	throw new Error("missing time.png");
        JToggleButton btn = new JToggleButton(ii);
        bar.add(btn);
        btn.setMargin(new Insets(1, 1, 1, 1));        
        btn.setToolTipText(ii.getDescription());
        btn.setSelected(true);
        btn.addActionListener(new ActionListener(){
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		mainPanel.getMSCRenderer().setShowTime(((JToggleButton)e.getSource()).isSelected());
        		mainPanel.repaint();
        	}
        });

        
        selectBtn.setSelected(true);

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
    
    public void updateInfo(Event hovering) {
        StringBuilder sb = new StringBuilder();
        MSCRenderer r = mainPanel.getMSCRenderer();
        Event ev = r.getSelectedEvent();
        if (ev != null) {
            sb.append("selected event: ");
            sb.append(ev.getTimestampRepr());
            sb.append(": ");
            sb.append(ev.getLabel());
            sb.append("    ");
        }
        if (hovering != null) {
            sb.append("hovering event: ");
            sb.append(hovering.getTimestampRepr());
            sb.append(": ");
            sb.append(hovering.getLabel());
            if (ev != null) {
                sb.append("    ");
                sb.append("elapsed: ");
                long x = hovering.getTimestamp()-ev.getTimestamp();
                final long hr =  TimeUnit.NANOSECONDS.toHours(x);
                x -= TimeUnit.HOURS.toNanos(hr);
                final long min = TimeUnit.NANOSECONDS.toMinutes(x);
                x -= TimeUnit.MINUTES.toNanos(min);
                final long sec = TimeUnit.NANOSECONDS.toSeconds(x);
                x -= TimeUnit.SECONDS.toNanos(sec);
                sb.append(String.format("%02d:%02d:%02d.%09d", hr, min, sec, x));
            }
        }
        info.setText(sb.toString());
    }
}
