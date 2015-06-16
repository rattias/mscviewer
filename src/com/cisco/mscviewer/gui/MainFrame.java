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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.expression.ParsedExpression;
import com.cisco.mscviewer.gui.colorpicker.HighlighterColorPicker;
import com.cisco.mscviewer.io.JsonLoader;
import com.cisco.mscviewer.io.PNGSaver;
import com.cisco.mscviewer.io.Session;
import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.JSonObject;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.ViewModel;
import com.cisco.mscviewer.script.PythonFunction;
import com.cisco.mscviewer.util.PNGSnapshotTarget;
import com.cisco.mscviewer.util.PersistentPrefs;
import com.cisco.mscviewer.util.Resources;
import com.cisco.mscviewer.util.Utils;
import com.sun.javafx.geom.RoundRectangle2D;

@SuppressWarnings("serial")
public class MainFrame extends JFrame implements PNGSnapshotTarget {
    private final static double VER_SPLIT = 0.6;

    private static MainFrame mf;
    private final ViewModel viewModel;
    private final EntityTree entityTree;
    private final EntityList entityList;
    private final PyScriptTree scriptTree;
    private final EntityHeader entityHeader;
    private final MainPanel mainPanel;
    private final JScrollPane jsp;
    private final LogList logList;
    private final MarkerBar markerBar;
    private final ResultPanel results;
    private final DataPanel data;
    private final JLabel info;
    final private JFileChooser jfc;
    private JButton reloadButton;
    private JMenuItem reloadMI;
    
    private final JSlider zoom;
    private Vector<Vector<String>> filters = new Vector<Vector<String>>();
    private final FilterPanel filterP;
    private Marker currentMarker = null;
    private final String progName;
    private String fname;
    private boolean isMarking;
    private final JTabbedPane jtabbed;
    private final JSplitPane leftRightSplitPane, leftSplitPane, rightSplitPane;
    private NotesPanel notes;
    private JMenu recentFilesMenu;
    private JMenu recentSessionMenu;

    private JButton clearHighlightsButton;
    private JMenuItem clearHighlightsMI;

    class MouseHandler implements MouseListener, MouseMotionListener {

        private void popupMenu(final MouseEvent me) {
            final MSCRenderer r = mainPanel.getMSCRenderer();
            final JPopupMenu jpm = new JPopupMenu();
            JMenuItem it = new JMenuItem("Close Entity");
            it.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final Point p = new Point(me.getPoint().x, 0);
                    final Entity en = entityHeader.getEntityAt(p);
                    if (en != null) {
                        viewModel.remove(en);
                    }
                }
            });
            jpm.add(it);
            it = new JMenuItem("Close All Entities");
            it.addActionListener(new ActionListener() {
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
                        it.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.add(viewModel.indexOf(ten), fen);
                                if (inter.getFromEvent() != null
                                        && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getToIndex());
                            }
                        });
                        jpm.add(it);
                    } else {
                        it = new JMenuItem("Close Source");
                        it.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.remove(fen);
                                // entityTree.updateTreeForEntityChange(fen);
                                if (inter.getFromEvent() != null
                                        && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getToIndex());
                            }
                        });
                        jpm.add(it);
                    }
                }
                if (ten != null) {
                    if (viewModel.indexOf(ten) == -1) {
                        it = new JMenuItem("Open Sink");
                        it.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.add(viewModel.indexOf(fen) + 1, ten);
                                // entityTree.updateTreeForEntityChange(ten);
                                if (inter.getFromEvent() != null
                                        && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getToIndex());
                            }
                        });
                        jpm.add(it);
                    } else {
                        it = new JMenuItem("Close Sink");
                        it.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                viewModel.remove(ten);
                                // entityTree.updateTreeForEntityChange(ten);
                                if (inter.getFromEvent() != null
                                        && inter.getFromEvent().getEntity() != null)
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getFromIndex());
                                else
                                    mainPanel.makeEventWithIndexVisible(inter
                                            .getToIndex());
                            }
                        });
                        jpm.add(it);
                    }
                }
            }
            final Point p = new Point(me.getPoint().x, 0);
            final Entity en = entityHeader.getEntityAt(p);
            if (en != null) {
                it = new JMenuItem("Select first event");
                it.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        r.setSelectedEventByViewIndex(-1);
                        final int idx = viewModel.getFirstEventIndexForEntity(en);
                        r.setSelectedEventByViewIndex(idx);
                        updateInfo(null);
                    }
                });
                jpm.add(it);
                it = new JMenuItem("Select last event");
                it.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        r.setSelectedEventByViewIndex(-1);
                        final int idx = viewModel.getLastEventIndexForEntity(en);
                        r.setSelectedEventByViewIndex(idx);
                        updateInfo(null);
                    }
                });
                jpm.add(it);

                it = new JMenuItem("Select previous highlighted");
                it.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        Event ev = r.getSelectedEvent();
                        if (ev != null) {
                            for (int idx = viewModel
                                    .getViewIndexFromModelIndex(ev.getIndex()) - 1; idx >= 0; idx--) {
                                ev = viewModel.getEventAt(idx);
                                if (ev.getMarker() != null
                                        && ev.getEntity() == en) {
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
                it.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        Event ev = r.getSelectedEvent();
                        if (ev != null) {
                            for (int idx = viewModel
                                    .getViewIndexFromModelIndex(ev.getIndex()) + 1; idx < viewModel
                                    .getEventCount(); idx++) {
                                ev = viewModel.getEventAt(idx);
                                if (ev.getMarker() != null
                                        && ev.getEntity() == en) {
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
                it.setEnabled(r.getSelectedEvent() != null
                        || r.getSelectedInteraction() != null);
                it.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        mainPanel.scrollToSelected();
                    }
                });
                jpm.add(it);
            }
            jpm.addSeparator();
            Event ev = r.getSelectedEvent();
            it = new JMenuItem("Add Note");
            it.setEnabled(ev != null && ev.getNote() == null);
            it.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    notes.makeVisible();
                    NoteEditor ne = notes.getEditor();
                    ne.setTextAndSelect("Modify me in Event Note Tab");
                    notes.selectionChanged(ev);
                    mainPanel.repaint();
                }
            });
            jpm.add(it);
            it = new JMenuItem("Remove Note");
            it.setEnabled(ev != null && ev.getNote() != null);
            it.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    ev.setNote(null);
                    notes.selectionChanged(ev);
                    mainPanel.repaint();
                }
            });
            jpm.add(it);

            if (jpm.getComponentCount() > 0) {
                jpm.show((Component) me.getSource(), me.getX(), me.getY());
            }

        }

        @Override
        public void mouseClicked(MouseEvent me) {
        }

        @Override
        public void mouseEntered(MouseEvent arg0) {
        }

        @Override
        public void mouseExited(MouseEvent arg0) {
        }

        private boolean toggleMarker(MouseEvent me, boolean toggle, boolean set) {
            boolean res = true;
            final MainPanel mp = getMainPanel();
            final JViewport jvp = (JViewport) mp.getParent();
            final Rectangle rec = jvp.getViewRect();
            // mp.getMSCRenderer().selectClosest(me.getX(), me.getY(), rec.y,
            // rec.height);
            final Object obj = mp.getMSCRenderer().getClosest(me.getX(), me.getY(),
                    rec.y, rec.height);
            if (obj != null) {
                if (obj instanceof Event) {
                    final Event ev = (Event) obj;
                    if (toggle) {
                        if (ev.getMarker() == null) {
                            ev.setMarker(currentMarker);
                            clearHighlightsButton.setEnabled(true);
                            clearHighlightsMI.setEnabled(true);
                            res = true;
                        } else {
                            ev.setMarker(null);
                            res = false;
                        }
                    } else {
                        if (set) {
                            ev.setMarker(currentMarker);
                            clearHighlightsButton.setEnabled(true);
                            clearHighlightsMI.setEnabled(true);
                        }else
                            ev.setMarker(null);
                    }
                } else {
                    if (obj instanceof Interaction) {
                        final Interaction in = mp.getMSCRenderer()
                                .getSelectedInteraction();
                        if (in != null) {
                            if (toggle) {
                                if (in.getMarker() == null) {
                                    in.setMarker(currentMarker);
                                    clearHighlightsButton.setEnabled(true);
                                    clearHighlightsMI.setEnabled(true);
                                } else
                                    in.setMarker(null);
                            } else {
                                if (set) {
                                    in.setMarker(currentMarker);
                                    clearHighlightsButton.setEnabled(true);
                                    clearHighlightsMI.setEnabled(true);
                                } else
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
            final MainPanel mp = getMainPanel();
            if (currentMarker == null) {
                final JViewport jvp = (JViewport) mp.getParent();
                final Rectangle rec = jvp.getViewRect();
                mp.requestFocus();
                mp.getMSCRenderer().selectClosest(me.getX(), me.getY(), rec.y,
                        rec.height);
                repaint();
            } else {
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
            if (currentMarker == null) {
                // mainPanel.setSelectionRectangle(getRect(x0, y0, me.getX(),
                // me.getY()));
            } else {
                toggleMarker(me, false, isMarking);
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {
            final MainPanel mp = getMainPanel();
            final JViewport jvp = (JViewport) mp.getParent();
            final Rectangle rec = jvp.getViewRect();
            final Object obj = mp.getMSCRenderer().getClosest(me.getX(), me.getY(),
                    rec.y, rec.height);
            if (obj instanceof Event) {
                updateInfo((Event) obj);
            }
        }

    }

    private void loadFile() {
        final MSCDataModel dm = MSCDataModel.getInstance();
        final String curDir = dm.getOpenPath();
        if (curDir != null)
            jfc.setCurrentDirectory(new File(curDir));
        final int result = jfc.showOpenDialog(null);
        dm.setOpenPath(jfc.getCurrentDirectory().getAbsolutePath());
        switch (result) {
        case JFileChooser.APPROVE_OPTION:
            final File f = jfc.getSelectedFile();
            try {
                loadFile(f.getPath());                
                break;
            } catch (final Exception e1) {
                e1.printStackTrace();
            }
            break;
        case JFileChooser.CANCEL_OPTION:
            break;
        case JFileChooser.ERROR_OPTION:
        default:
            System.out.println("Error");
            break;
        }
    }

    private void loadFile(String path) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new Error("loadFile() should be called in EDT");
        final MSCDataModel dm = MSCDataModel.getInstance();
        // we want to execute both viewModel and DataModel reset in the context of 
        //event dispatch thread!
        viewModel.reset();            
        dm.reset();
        try {
            new JsonLoader().loadAsync(path, dm, false);
            reloadButton.setEnabled(true);
            reloadMI.setEnabled(true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        addRecentModel(path);
    }
    
    private void reloadFile() {
        final MSCDataModel dm = MSCDataModel.getInstance();
        try {
            viewModel.reset();
            dm.reset();
            new JsonLoader().load(dm.getFilePath(), dm, false);
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
    }

    private void exportPNG() {
        final Object[] choices = { "Highlighted Elements", "Open Entities",
        "Entire Model" };
        final String s = (String) JOptionPane.showInputDialog(MainFrame.this,
                "What image do you want to capture?", "Choose ",
                JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        int mode;
        if (choices[0].equals(s))
            mode = PNGSaver.SAVE_MARKED;
        else if (choices[1].equals(s))
            mode = PNGSaver.SAVE_OPENED;
        else if (choices[2].equals(s))
            mode = PNGSaver.SAVE_ALL;
        else
            return;
        final int result = jfc.showSaveDialog(null);
        switch (result) {
        case JFileChooser.APPROVE_OPTION:
            final File f = jfc.getSelectedFile();
            try {
                PNGSaver.saveMSCasPNG(f.getAbsolutePath(),
                        mainPanel.getMSCRenderer(), mode);
            } catch (final IOException e1) {
                e1.printStackTrace();
            }
            break;
        case JFileChooser.CANCEL_OPTION:
            break;
        case JFileChooser.ERROR_OPTION:
            System.out.println("Error");
        }
    }

    private void clearHighlights() {
        final int res = JOptionPane.showConfirmDialog(MainFrame.this,
                "Are you sure you want to remove all highlights?",
                "Clear Highlights", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            final MSCDataModel dm = MSCDataModel.getInstance();
            dm.clearMarkers();
            clearHighlightsButton.setEnabled(false);
            clearHighlightsMI.setEnabled(false);
            repaint();
        }
    }

    private void runLatestScript() {
        scriptTree.runLatestFunction();
    }

    private void openPreferences() {
        final MSCRenderer r = mainPanel.getMSCRenderer();
        final PrefsDialog d = new PrefsDialog(MainFrame.this, r.getTimeScaleFactor(),
                r.getAbsTimeUnit(), r.getDeltaTimeUnit());
        // d.setInputUnit(r.getTimestampUnit());
        d.setAbsoluteOutputUnit(r.getAbsTimeUnit());
        d.setDeltaOutputUnit(r.getDeltaTimeUnit());
        d.setShowUnits(r.getShowUnits());
        d.setShowDate(r.getShowDate());
        d.setShowLeadingZeroes(r.getShowLeadingZeroes());
        d.setCompactView(r.getCompactView());
        d.setVisible(true);
        if (d.approved()) {
            // r.setTimestampUnit(d.getInputUnit());
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

    public MainFrame(int x, int y, int w, int h) {
        MainFrame.mf = this;
        setName("MainFrame");
        setBounds(x, y, w, h);
        progName = "MSCViewer v" + Main.VERSION;
        updateTitle();

        final List<Image> icons = new ArrayList<Image>();
        icons.add(Resources.getImageIcon("64x64/mscviewer.png", "mscviewer")
                .getImage());
        setIconImages(icons);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jfc = new JFileChooser();
        getContentPane().setLayout(new BorderLayout());
        final JToolBar toolBar = new JToolBar("jtb");
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.LOWERED));
        getContentPane().add(toolBar, BorderLayout.NORTH);
        leftRightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        getContentPane().add(leftRightSplitPane, BorderLayout.CENTER);
        info = new JLabel("");
        info.setBorder(BorderFactory.createEtchedBorder());
        getContentPane().add(info, BorderLayout.SOUTH);
        viewModel = new ViewModel(MSCDataModel.getInstance());
        entityHeader = new EntityHeader(viewModel);
        entityTree = new EntityTree(viewModel);
        entityList = new EntityList(viewModel);
        viewModel.addListener(entityList);
        viewModel.setFilter(new CompactViewFilter(viewModel, null));

        mainPanel = new MainPanel(this, entityHeader, viewModel);
        scriptTree = new PyScriptTree(mainPanel);
        final JTabbedPane topLeft = new TopLeftPane() {
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
        final JPanel bottomLeft = new JPanel();
        bottomLeft.setLayout(new BorderLayout());
        bottomLeft.add(new JLabel("Open Entities"), BorderLayout.NORTH);
        bottomLeft.add(new JScrollPane(entityList), BorderLayout.CENTER);
        leftSplitPane = new LeftPane(topLeft, bottomLeft);
        leftSplitPane.setOneTouchExpandable(true);
        leftSplitPane.setDividerLocation((int) (VER_SPLIT * h));
        rightSplitPane.setResizeWeight(1.0);

        leftRightSplitPane.setLeftComponent(leftSplitPane);
        leftRightSplitPane.setRightComponent(rightSplitPane);
        leftRightSplitPane.setResizeWeight(0.3);
        mainPanel.getMSCRenderer().addSelectionListener(
                new SelectionListener() {
                    @Override
                    public void eventSelected(MSCRenderer renderer,
                            Event selectedEvent, int viewEventIndex,
                            int modelEventIndex) {
                        final String status = mainPanel.getMSCRenderer()
                                .getSelectedStatusString();
                        info.setText(status);
                        notes.selectionChanged(selectedEvent);
                    }

                    @Override
                    public void interactionSelected(MSCRenderer renderer,
                            Interaction selectedInteraction) {
                        final String status = mainPanel.getMSCRenderer()
                                .getSelectedStatusString();
                        info.setText(status);
                        notes.selectionChanged(null);
                    }

                });
        final MouseHandler mh = new MouseHandler();
        mainPanel.addMouseListener(mh);
        mainPanel.addMouseMotionListener(mh);
        final JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        final FindPanel findP = new FindPanel(mainPanel.getMSCRenderer());
        final JPanel tb = new JPanel();
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
                final int v = zoom.getValue();
                mainPanel.setZoomFactor(v);
                repaint();
            }
        });
        tb.add(zoom);
        tb.add(Box.createHorizontalStrut(20));
        p.add(tb, BorderLayout.NORTH);
        jsp = new CustomJScrollPane(mainPanel);
        jsp.setName("MainPanelJSP");

        // jsp.setLayout(new com.cisco.mscviewer.gui.ScrollPaneLayout());
        // following is needed to resize the MainPanel when the view is resized
        // jsp.getViewport().addChangeListener(new ChangeListener(){
        // @Override
        // public void stateChanged(ChangeEvent e) {
        // mainPanel.updateView();
        // }
        // });
        p.add(jsp, BorderLayout.CENTER);
        markerBar = new MarkerBar(entityHeader, viewModel,
                mainPanel.getMSCRenderer());
        viewModel.addListener(markerBar);
        p.add(markerBar, BorderLayout.EAST);
        jsp.setColumnHeaderView(entityHeader);
        jsp.validate();
        rightSplitPane.setTopComponent(p);
        logList = new LogList(MSCDataModel.getInstance(), viewModel);
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final Object o = e.getSource();
                @SuppressWarnings("unchecked")
                final
                JList<String> ls = (JList<String>) o;
                final int idx = ls.getSelectedIndex();
                mainPanel.getMSCRenderer().selectByLineNumber(idx + 1);

            }
        });

        mainPanel.getMSCRenderer().addSelectionListener(logList);
        final JPanel logPanel = new JPanel();
        logPanel.setLayout(new BorderLayout());
        final FindSourcePanel fsp = new FindSourcePanel(logList);
        logPanel.add(fsp, BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logList), BorderLayout.CENTER);

        results = new ResultPanel(MSCDataModel.getInstance());
        data = new DataPanel();
        mainPanel.getMSCRenderer().addSelectionListener(data);
        notes = new NotesPanel(mainPanel);

        jtabbed = new CustomJTabbedPane();
        jtabbed.setName("bottom-right");
        jtabbed.add("Log File", logPanel);
        jtabbed.add(ResultPanel.NAME, results);
        jtabbed.add(DataPanel.NAME, data);
        jtabbed.add(NotesPanel.NAME, notes);
        rightSplitPane.setBottomComponent(jtabbed);
        rightSplitPane.setDividerLocation((int) (VER_SPLIT * h));
        rightSplitPane.setResizeWeight(1.0);
        final JMenuBar jmb = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem mi = new JMenuItem(new AbstractAction("Load Log File...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadFile();
                
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                ActionEvent.ALT_MASK));
        fileMenu.add(mi);

        reloadMI = new JMenuItem(new AbstractAction("Reload Log File") {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadFile();
            }
        });
        reloadMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                ActionEvent.ALT_MASK));
        fileMenu.add(reloadMI);

        recentFilesMenu = new JMenu("Recent Log Files");
        updateRecentModelsMenu();
        fileMenu.add(recentFilesMenu);

        fileMenu.addSeparator();
        mi = new JMenuItem(new AbstractAction("Load Session...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = Session.loadAsync();
                addRecentSession(path);
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
        fileMenu.add(mi);

        mi = new JMenuItem(new AbstractAction("Save Session") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Session.save();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.ALT_MASK));
        fileMenu.add(mi);

        
        mi = new JMenuItem(new AbstractAction("Save Session as...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Session.saveAs();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
        fileMenu.add(mi);

        recentSessionMenu = new JMenu("Recent session");
        updateRecentSessionsMenu();
        fileMenu.add(recentSessionMenu);
        fileMenu.addSeparator();



        mi = new JMenuItem(new AbstractAction("Export PNG...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportPNG();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                ActionEvent.ALT_MASK));
        fileMenu.add(mi);

        mi = new JMenuItem(new AbstractAction("Print...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PrinterJob pj = PrinterJob.getPrinterJob();
                pj.setPrintable(new MSCPrintable(mainPanel.getMSCRenderer()));
                if (pj.printDialog()) {
                    try {
                        pj.print();
                    } catch (final PrinterException exc) {
                        System.out.println(exc);
                    }
                }
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                ActionEvent.ALT_MASK));
        fileMenu.add(mi);
        fileMenu.addSeparator();
        mi = new JMenuItem(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                quit();
            }
        });
        fileMenu.add(mi);

        final JMenu editMenu = new JMenu("Edit");
        mi = new JMenuItem(new AbstractAction("Find...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final FindDialog fd = new FindDialog(MainFrame.this);
                fd.setVisible(true);
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                ActionEvent.ALT_MASK));
        editMenu.add(mi);

        clearHighlightsMI = new JMenuItem(new AbstractAction("Clear Highlights") {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearHighlights();
            }
        });
        clearHighlightsMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,
                ActionEvent.ALT_MASK));
        editMenu.add(clearHighlightsMI);

        final JMenu viewMenu = new JMenu("View");
        mi = new JMenuItem(new AbstractAction("Preferences...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPreferences();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
        viewMenu.add(mi);

        viewMenu.add(new JMenuItem(new AbstractAction("Filters...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final FilterDialog d = new FilterDialog(MainFrame.this, filters);
                d.setVisible(true);
                if (d.approved()) {
                    filters = d.getFilters();
                }
            }
        }));
        final JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem(new AbstractAction("About MSCViewer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(Desktop.isDesktopSupported()) {
                    try {
                        String path = Utils.getInstallDir()+"/doc/release.html";
                        URI uri = new File(path).toURI();
                        Desktop.getDesktop().browse(uri);
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } 

                }
            }
        }));
        helpMenu.add(new JMenuItem(new AbstractAction("About Memory") {
            @Override
            public void actionPerformed(ActionEvent e) {
                long free = Runtime.getRuntime().freeMemory()/(1024*1024);
                long total = Runtime.getRuntime().totalMemory()/(1024*1024);
                long used = total - free;
                JOptionPane.showMessageDialog(MainFrame.this, new String[]{"<html><b>Total Memory:</b> "+total+"Mb", "<html><b>Used Memory:</b> "+used+"Mb", "<html><b>Free Memory:</b> "+free+"Mb"});
            }
        }));
        jmb.add(fileMenu);
        jmb.add(editMenu);
        jmb.add(viewMenu);
        jmb.add(helpMenu);
        setJMenuBar(jmb);
        populateToolbar(toolBar);
        setMainPanelCursor(Resources.getImageIcon("32x32/select.png", "")
                .getImage(), 0, 0);

    }

    private void setMainPanelCursor(Image img, int px, int py) {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Cursor c = toolkit.createCustomCursor(img, new Point(px, py), "img");
        mainPanel.setCursor(c);
    }

    private JToggleButton addToggleButton(JToolBar bar, String resource,
            String description, boolean isSelected) {
        final ImageIcon ii = Resources.getImageIcon(resource, description);
        if (ii == null)
            throw new Error("missing " + resource);
        StyledToggleButton jtb = new StyledToggleButton();
        jtb.setToolTipText(description);
        jtb.setIcon(ii);
        jtb.setSelected(isSelected);
        bar.add(jtb);
        bar.add(Box.createHorizontalStrut(2));
        return jtb;
    }

    private JButton addButton(JToolBar bar, String resource,
            String description, boolean isSelected, boolean isEnabled) {
        return addButton(bar, resource, description, isSelected, isEnabled, null);
    }

    private JButton addButton(JToolBar bar, String resource,
            String description, boolean isSelected, boolean isEnabled, JButton btn) {
        final ImageIcon ii = Resources.getImageIcon(resource, description);
        if (ii == null)
            throw new Error("missing " + resource);
        final JButton jtb = btn != null ? btn : new StyledButton();
        jtb.setIcon(ii);
        jtb.setToolTipText(ii.getDescription());
        jtb.setSelected(isSelected);
        jtb.setEnabled(isEnabled);
        bar.add(jtb);
        bar.add(Box.createHorizontalStrut(2));
        return jtb;
    }

    private JComponent addHighlighter(JToolBar bar) {
        HighlighterColorPicker hcp = new HighlighterColorPicker(32);
        hcp.addColorSelectionListener((c) -> {
            Color[] colors = HighlighterColorPicker.colors;
            for(int i=0; i<colors.length; i++) {
                if (colors[i].equals(c)) {
                    Marker m = HighlighterColorPicker.markers[i];
                    String name = m.getName();
                    setMainPanelCursor(Resources.getImageIcon("32x32/highlight_"+name+".png", name).getImage(), 0, 0);                
                    currentMarker = m;
                }
            }
        });
        ToolTipManager.sharedInstance().registerComponent(hcp);
        hcp.setToolTipText("Highlighter");
        bar.add(hcp);
        bar.add(Box.createHorizontalStrut(2));
        return hcp.getButton();
    }

    private void populateToolbar(JToolBar bar) {
        bar.add(Box.createHorizontalStrut(10));
        final ButtonGroup toolGroup = new ButtonGroup();

        JButton jb = addButton(bar, "load.png", "Load File", true, true);
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadFile();
            }
        });
        reloadButton = addButton(bar, "reload.png", "Reload File", true, false);
        reloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadFile();
            }
        });

        jb = addButton(bar, "camera.png", "Capture Screenshot", true, true);
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportPNG();
            }
        });
        clearHighlightsButton = addButton(bar, "clear_highlights.png", "Clear Highlights",
                true, false);
        clearHighlightsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearHighlights();
            }
        });

        jb = addButton(bar, "run.png", "Rerun Latest Script", true, false);
//        jb.
//                new JButton() {
//            @Override
//            public String getToolTipText() {
//                super.getToolTipText();
//                final PythonFunction fun = scriptTree.getLatestFunction();
//                return (fun != null) ? "Rerun Latest Script: "
//                + fun.getName() : "Rerun Latest Script: <none>";
//            }
//        });
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runLatestScript();
            }
        });

        jb = addButton(bar, "options.png", "Preferences", true, true);
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPreferences();
            }
        });
        Dimension separatorDim = new Dimension(16, 32);
        bar.addSeparator(separatorDim);
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        
        sep.setPreferredSize(separatorDim);
        sep.setMaximumSize(separatorDim);
        bar.add(sep);
        JToggleButton jtb = addToggleButton(bar, "select.png", "select",
                true);
        toolGroup.add(jtb);
        jtb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JToggleButton jtb = (JToggleButton) e.getSource();
                if (jtb.isSelected()) {
                    setMainPanelCursor(
                            Resources.getImageIcon("32x32/select.png", "")
                            .getImage(), 0, 0);
                    currentMarker = null;
                }
            }
        });

        JToggleButton tb = (JToggleButton)addHighlighter(bar);
        toolGroup.add(tb);
        bar.addSeparator(separatorDim);
        sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(separatorDim);
        sep.setMaximumSize(separatorDim);
        bar.add(sep);

        jtb = addToggleButton(bar, "blocks.png", "show blocks", true);
        jtb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.getMSCRenderer().setShowBlocks(
                        ((JToggleButton) e.getSource()).isSelected());
                mainPanel.repaint();
            }
        });
        jtb = addToggleButton(bar, "time.png", "show timestamps", true);
        jtb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.getMSCRenderer().setShowTime(
                        ((JToggleButton) e.getSource()).isSelected());
                mainPanel.repaint();
            }
        });
        jtb = addToggleButton(bar, "label.png", "show labels", true);
        jtb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.getMSCRenderer().setShowLabels(
                        ((JToggleButton) e.getSource()).isSelected());
                mainPanel.repaint();
            }
        });
        jtb = addToggleButton(bar, "note.png", "show notes", true);
        jtb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.getMSCRenderer().setShowNotes(
                        ((JToggleButton) e.getSource()).isSelected());
                mainPanel.repaint();
            }
        });
        
    }

    int getLifeLineY(int entityIdx) {
        return entityHeader.getEntityCenterX(entityIdx);
    }

    int getEntitiesTotalWidth() {
        return entityHeader.getPreferredWidth();
    }

    public MainPanel getMainPanel() {
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
            final char[] charArray = new char[fname.length()];
            Arrays.fill(charArray, ' ');
            final String pad = new String(charArray);
            setTitle(pad + "      " + progName + "      " + fname);
        } else
            setTitle(progName);
    }

    public void setFilename(String fname) {
        this.fname = fname;
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
        final StringBuilder sb = new StringBuilder();
        final MSCRenderer r = mainPanel.getMSCRenderer();
        final Event ev = r.getSelectedEvent();
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
                long x = hovering.getTimestamp() - ev.getTimestamp();
                final long hr = TimeUnit.NANOSECONDS.toHours(x);
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

    public void showTab(String s) {
        final int idx = jtabbed.indexOfTab(s);
        if (idx < 0)
            throw new Error("Invalid tab name '" + s + "'");
        jtabbed.setSelectedIndex(idx);
    }

    public EntityTree getEntityTree() {
        return entityTree;
    }

    public JSplitPane getLeftSplitPane() {
        return leftSplitPane;
    }

    public JSplitPane getRightSplitPane() {
        return rightSplitPane;
    }

    public JSplitPane getLeftRightSplitPane() {
        return leftRightSplitPane;
    }

    private void quit() {
        if (! Session.isUpToDate()) {
            String[] options = new String[]{"Save & Quit", "Quit", "Cancel"};
            int v = JOptionPane.showOptionDialog(null,
                    "Some notes or markers have changed since session was last saved." +
                            " Do you want to save the session, quit without saving, or cancel?",
                            "Quit", 0, JOptionPane.INFORMATION_MESSAGE, null, options, null);
            switch(v){
            case 0:
                Session.save();
                System.exit(0);
                break;
            case 1:
                System.exit(0);
                break;
            case 2:
                break;
            }
        } else
            System.exit(0);
    }
    
    private void addRecentModel(String path) {
        ArrayList<String> items = PersistentPrefs.getInstance().addRecentModel(path);
        updateRecentModelsMenu();
    }

    public void addRecentSession(String path) {
        ArrayList<String> items = PersistentPrefs.getInstance().addRecentSession(path);
        updateRecentSessionsMenu();
    }

    private void updateRecentModelsMenu() {
        ArrayList<String> models = PersistentPrefs.getInstance().getRecentModels();
        recentFilesMenu.removeAll();
        for(int i=0; i<models.size(); i++) {
            String item = models.get(i);
            JMenuItem mi = new JMenuItem(new AbstractAction(item) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    loadFile(item);
                }
            });
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0+i, ActionEvent.ALT_MASK));
            recentFilesMenu.add(mi);
        }
    }
    
    private void updateRecentSessionsMenu() {
        ArrayList<String> sessions = PersistentPrefs.getInstance().getRecentSessions();
        recentSessionMenu.removeAll();
        for(int i=0; i<sessions.size(); i++) {
            String item = sessions.get(i);
            JMenuItem mi = new JMenuItem(new AbstractAction(item) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    loadFile(item);
                }
            });
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0+i, ActionEvent.ALT_MASK|ActionEvent.SHIFT_MASK));
            recentSessionMenu.add(mi);
        }
    }
}
