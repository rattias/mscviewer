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
    private JTextField xtf;
    private final GraphPanel gp;

    public HeatGraphWindow(Graph graph) {
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
