package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JToolBar;



@SuppressWarnings("serial")
public class ColorPicker extends JPanel {
    public static final int TYPE_FOREGROUND = 1;
    public static final int TYPE_BACKGROUND = 2;

    class Letter extends JPanel {
        private int type;
        private Font f;
        private int height;
        private String txt;
        private Color c;
        
        public Letter(int type, String txt, int height, Color color) {
            c = color;
            this.height = height;
            this.txt = txt;
            this.type = type;
            f = new Font("Serif", Font.BOLD, height);
            setPreferredSize(new Dimension(height*3/2, height));
        }
        
        public void setColor(Color c) {
            this.c = c;
        }
        
        @Override
        public void paintComponent(Graphics g) {
            g.setColor(c);
            if (type == TYPE_FOREGROUND) {
                g.fillRect(1, getHeight()-4, getWidth()-2, getHeight()-2);
            } else {
                g.fillRect(3, 3, getWidth()-4, getHeight()-4);                
            }
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(txt, g);
            int x = (int)(getWidth()-r.getWidth())/2;
            int y = (int)(getHeight()-r.getHeight())/2+fm.getAscent();
            g.setColor(Color.white);
            g.drawString(txt, x, y);
            g.setColor(Color.black);
            g.drawString(txt, x+2, y+2);
        }
    };

    
    
    public final int W = 30;
    private Letter B;
    private JButton arr;
//    private JPanel coloredLine;
    private Color selected = null;
    private Vector<ColorSelectionListener> listeners = new Vector<ColorSelectionListener>();
    private JLabel B1;



    private Component getView(Container c) {
        if (c instanceof JViewport)
            return c.getComponent(0);
        for(int i=0; i<c.getComponentCount(); i++) {
            Component sub = c.getComponent(i);
            if (sub instanceof Container) { 
                Component res = getView((Container)sub);
                if (res != null)
                    return res;
            }            
        }
        return null;
    }

    private Point getPopupLocation(Component owner, Component popup) {
        Point p = owner.getLocation();
        p.y += owner.getHeight();
        JFrame f = (JFrame)SwingUtilities.getRoot(this);
        JComponent gp = (JComponent)f.getGlassPane();
        p = SwingUtilities.convertPoint(owner.getParent(), p, gp);
        if (p.x+popup.getWidth() > gp.getWidth())
            p.x = gp.getWidth() - popup.getWidth();
        if (p.y+popup.getHeight() > gp.getHeight())
            p.y = gp.getHeight() - popup.getHeight();
        return p;
    }
    
    public void setEnabled(boolean v) {
        super.setEnabled(v);
        B.setEnabled(v);
        arr.setEnabled(v);
        //coloredLine.setEnabled(v);
    }
    
    public ColorPicker(int type, Color color) {
        if (type != TYPE_FOREGROUND && type != TYPE_BACKGROUND)
            throw new Error("Invalid type "+type);
        Color[][] colors = {
                {new Color(0x000000), Color.red,    Color.red.darker(),     Color.red.darker().darker(),    Color.red.darker().darker().darker()},
                {new Color(0x202020), Color.pink,   Color.pink.darker(),    Color.pink.darker().darker(),   Color.pink.darker().darker().darker()},
                {new Color(0x404040), Color.orange, Color.orange.darker(),  Color.orange.darker().darker(), Color.orange.darker().darker().darker()},
                {new Color(0x606060), Color.yellow, Color.yellow.darker(),  Color.yellow.darker().darker(), Color.yellow.darker().darker().darker()},
                {new Color(0x808080), Color.green,  Color.green.darker(),   Color.green.darker().darker(),  Color.green.darker().darker().darker()},
                {new Color(0xa0a0a0), Color.magenta,Color.magenta.darker(), Color.magenta.darker().darker(),Color.magenta.darker().darker().darker()},
                {new Color(0xc0c0c0), Color.cyan,   Color.cyan.darker(),    Color.cyan.darker().darker(),   Color.cyan.darker().darker().darker()},
                {new Color(0xf0f0f0), Color.blue,   Color.blue.darker(),    Color.blue.darker().darker(),   Color.blue.darker().darker().darker()}
        };                
 
        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(60,1000));
        B = new Letter(type, "A", 16, color);
        selected = color;
        arr = new JButton() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Dimension d = getSize();
                int w = getWidth();
                int h = getHeight();
                g.setColor(isEnabled() ? Color.black : Color.gray);
                g.fillPolygon(new int[]{w/4, 3*w/4, w/2}, new int[]{h/2-w/4, h/2-w/4, h/2+w/4}, 3);
            }
        };
        arr.setPreferredSize(new Dimension(16,2));
        this.add(B, BorderLayout.CENTER);
        this.add(arr, BorderLayout.EAST);
        arr.addActionListener((ev) -> {
            JPanel d = new JPanel();
            d.setLayout(new GridLayout(colors.length, colors[0].length));
            for(int i=0; i<colors.length; i++) {
                for(int j=0; j<colors[i].length; j++) {
                    JButton b = new JButton() {
                        public void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            if (getModel().isRollover()){
                                g.setColor(Color.yellow);
                                g.drawRect(0, 0,  getWidth()-1, getHeight()-1);
                            }
                            g.setColor(getBackground());
                            g.fillRect(1,  1, getWidth()-2, getHeight()-2);
                        }
                    };
                    b.setRolloverEnabled(true);
                    b.setPreferredSize(new Dimension(W, W));
                    b.setBackground(colors[i][j]);
                    b.setOpaque(true);
                    d.add(b);
                    b.addActionListener(                           
                            (ev1) -> {
                                JComponent gp = (JComponent)SwingUtilities.getRootPane(this).getGlassPane();
                                gp.remove(d);
                                gp.setVisible(false);  
                                selected = b.getBackground();
                                B.setColor(selected);
                                B.repaint();
                                for(ColorSelectionListener l: listeners)
                                    l.colorSelected(selected);
                            });

                }
            }
            d.setSize(d.getPreferredSize());
            JComponent gp = (JComponent)SwingUtilities.getRootPane(this).getGlassPane();
            Point p = getPopupLocation(arr, d);
            d.setLocation(p);
            gp.setLayout(null);          
            gp.add(d);
            gp.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    gp.remove(d);
                    gp.setVisible(false);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    gp.remove(d);
                    gp.setVisible(false);
                }

                @Override
                public void mouseReleased(MouseEvent e) {}

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}

            });
            gp.addComponentListener(new ComponentListener() {

                @Override
                public void componentResized(ComponentEvent e) {
                    Point p = getPopupLocation(arr, d);
                    d.setLocation(p);
                }

                @Override
                public void componentMoved(ComponentEvent e) {}

                @Override
                public void componentShown(ComponentEvent e) {}

                @Override
                public void componentHidden(ComponentEvent e) {}
                
            });
            gp.setVisible(true);
        });
    }

    public static void main(String args[]) {
        JFrame f = new JFrame();
        f.setSize(1024, 768);
        Container c = f.getContentPane();
        c.setLayout(new BorderLayout());
        JToolBar b = new JToolBar();
        c.add(b, BorderLayout.NORTH);
        b.add(new ColorPicker(TYPE_FOREGROUND, Color.black));
        f.setVisible(true);
    }

    public void addColorSelectionListener(ColorSelectionListener l) {
        listeners.add(l);
    }
    
    public void setToolTipText(String str) {
        super.setToolTipText(str);
        setToolTipText(this, str);
    }

    private void setToolTipText(JComponent c, String str) {
        for (Component cc : c.getComponents())
            if (cc instanceof JComponent)
                setToolTipText((JComponent) cc, str);   
    }
}
