package com.cisco.mscviewer.gui.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.cisco.mscviewer.graph.GraphData;
import com.cisco.mscviewer.graph.Point;
import com.cisco.mscviewer.gui.MSCRenderer;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.model.Event;


@SuppressWarnings("serial")
public class GraphWindow extends JFrame {
    private GraphData[] data;
    private CheckBoxList list;
    private JTextField xtf;
    private GraphPanel gp;
    private JPanel right;
    private JPanel YLabels;
    
//    public static void main(String args[]) {
//        GraphData gd = new GraphData("test");
//        for(int i=0; i<10; i++) {
//            int x = i*5;
//            float y = (float)Math.random();
//            gd.add(x, y);
//        }
//        GraphWindow f = new GraphWindow(new GraphData[]{gd});
//        f.setVisible(true);
//    }
    
    private JComponent mkLegend() {
        list = new CheckBoxList();
        list.setBackground(Color.black);
        EntityCheckBox[] cbs = new EntityCheckBox[data.length];
        int idx = 0;
        for (GraphData gd: data) {
            cbs[idx] = new EntityCheckBox(gd);
            cbs[idx].setBackground(Color.black);
            cbs[idx].setForeground(gp.getForeground(gd));
            cbs[idx].setSelected(true);
            cbs[idx].addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent ev) {
                    EntityCheckBox c = (EntityCheckBox)ev.getItem();
                    gp.setEnabled(c.data, ev.getStateChange() == ItemEvent.SELECTED);
                    right.remove(YLabels);
                    YLabels = gp.getYLabelsPanel();
                    if (YLabels != null) {
                        right.add(YLabels, BorderLayout.WEST);
                    }
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

    
    
    public GraphWindow(GraphData[] graphArr) {
        data = graphArr;
        gp = new GraphCloudPanel();
        final MSCRenderer mscRenderer = MainFrame.getInstance().getMainPanel().getMSCRenderer();
        float step = 1.0f/graphArr.length;
        float h = 0f;
        for (GraphData d: graphArr) {            
            gp.addGraph(d);
            gp.setForeground(d, Color.getHSBColor(h, 1.0f, 1.0f));
            h += step;
        }
        gp.addListener(new GraphCursorListener() {                      
            @Override
            public void cursorChanged(GraphData gd, int idx) {
                if (idx >= 0) {
                    Point p = gd.point(idx);
                    if (gd.getXType().equals("time"))
                        xtf.setText(MainFrame.getInstance().getMainPanel().getMSCRenderer().getTimeRepr(p.x));
                    else
                        xtf.setText(""+p.x);

                    Object o = p.getObject();
                    if (o instanceof Event)
                        mscRenderer.setSelectedEvent((Event)o);
                }
            }
        });
        
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                gp.grabFocus();
            }
            
            @Override
            public void mouseDragged(MouseEvent ev) {
                gp.setCursorScreenX(ev.getX());
                repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent ev) {
                gp.grabFocus();
                gp.setCursorScreenX(ev.getX());
                repaint();
            }
        };
        gp.addMouseListener(ma);
        gp.addMouseMotionListener(ma);
        
        JComponent left = new JScrollPane(mkLegend());
        right = new JPanel();
        right.setLayout(new BorderLayout());
        YLabels = gp.getYLabelsPanel();
        if (YLabels != null)
            right.add(YLabels, BorderLayout.WEST);
        right.add(gp,  BorderLayout.CENTER);
        
        JSplitPane split = new JSplitPane();
        setContentPane(split);
        split.add(left, JSplitPane.LEFT);
        JPanel rright = new JPanel();
        rright.setLayout(new BorderLayout());
        rright.add(new JScrollPane(right), BorderLayout.CENTER);

        JPanel valueP = new JPanel();
        JLabel xtl = new JLabel(gp.getXType());
        xtf = new JTextField();
        xtf.setColumns(32);
        xtf.setEditable(false);
        valueP.add(xtl);
        valueP.add(xtf);
        valueP.setBorder(BorderFactory.createEtchedBorder());
        rright.add(valueP, BorderLayout.SOUTH);
        split.add(rright, JSplitPane.RIGHT);

        setSize(640, 480);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setVisible(true);                
            }
        });
        
    }
    

}
