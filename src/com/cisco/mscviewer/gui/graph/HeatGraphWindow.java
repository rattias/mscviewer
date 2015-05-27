package com.cisco.mscviewer.gui.graph;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.cisco.mscviewer.graph.Graph;
import com.cisco.mscviewer.graph.GraphSeries;
import com.cisco.mscviewer.graph.Point;
import com.cisco.mscviewer.gui.MSCRenderer;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.model.Event;

@SuppressWarnings({ "serial" })
public class HeatGraphWindow extends JFrame {
    private final Graph graph;
    private CheckBoxList list;
    private JTextField xtf;
    private final GraphPanel gp;
/*
    private JComponent mkLegend() {
        list = new CheckBoxList();
        list.setBackground(Color.black);
        final EntityCheckBox[] cbs = new EntityCheckBox[graph.getSeries().size()];
        int idx = 0;
        for (final GraphSeries gd : graph.getSeries()) {
            cbs[idx] = new EntityCheckBox(gd);
            cbs[idx].setBackground(Color.black);
            cbs[idx].setForeground(gp.getForeground(gd));
            cbs[idx].setSelected(true);
            cbs[idx].addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent ev) {
                    final EntityCheckBox c = (EntityCheckBox) ev.getItem();
                    gp.setEnabled(c.data,
                            ev.getStateChange() == ItemEvent.SELECTED);
                    revalidate();
                    gp.repaint();
                }
            });
            idx++;
        }
        list.setListData(cbs);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return list;
    }
*/
    public HeatGraphWindow(Graph graph) {
        this.graph = graph;
        gp = new HeatGraphPanel();
        final MSCRenderer mscRenderer = MainFrame.getInstance().getMainPanel()
                .getMSCRenderer();
        final float step = 1.0f / graph.getSeries().size();
        float h = 0f;
        for (final GraphSeries d : graph.getSeries()) {
            gp.addGraph(d);
            gp.setForeground(d, Color.getHSBColor(h, 1.0f, 1.0f));
            h += step;
        }
        gp.addListener(new GraphCursorListener() {
            @Override
            public void cursorChanged(GraphSeries gd, int idx) {
                if (idx >= 0) {
                    final Point p = gd.point(idx);
                    if (xtf != null) {
                        if (gd.getXType().equals("time"))
                            xtf.setText(MainFrame.getInstance().getMainPanel()
                                    .getMSCRenderer().getTimeRepr(p.x()));
                        else
                            xtf.setText("" + p.x());
                    }

                    final Object o = p.getObject();
                    if (o instanceof Event)
                        mscRenderer.setSelectedEvent((Event) o);
                }
            }
        });

        setContentPane(new JScrollPane(gp));

        setSize(640, 480);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setVisible(true);
            }
        });

    }

}
