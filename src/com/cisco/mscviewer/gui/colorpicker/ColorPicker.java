package com.cisco.mscviewer.gui.colorpicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.cisco.mscviewer.gui.StyledButton;



@SuppressWarnings("serial")
public class ColorPicker extends JPanel {
    public final int W = 30;
    private AbstractButton setColorButton;
    private JButton menuButton;
    private Color selected = null;
    private Vector<ColorSelectionListener> listeners = new Vector<ColorSelectionListener>();


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
        setColorButton.setEnabled(v);
        menuButton.setEnabled(v);
    }
    
    public ColorPicker(AbstractButton colorButton, Color colors[][], int selectedColorRow, int selectedColorColumn) {
        if (selectedColorRow >= colors.length || selectedColorRow  < 0 ||
                selectedColorColumn >= colors[selectedColorRow].length || selectedColorColumn < 0)
            throw new Error("Invalid selected color position ("+selectedColorRow+", "+selectedColorColumn+")");
            
        setOpaque(false);
        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(48,1000));
        colorButton.setFocusable(false);
        if (colorButton instanceof AbstractButton) {
            AbstractButton ab = (AbstractButton )colorButton;
            ab.setBorderPainted(false);
            ab.setContentAreaFilled(false);
            ab.setRolloverEnabled(false);
        }
        colorButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) {
                ColorPicker.this.mouseEntered(true);
            }
            public void mouseExited(MouseEvent ev) { 
                ColorPicker.this.mouseEntered(false);
            }
        });


        setColorButton = colorButton;
        selected = colors[selectedColorRow][selectedColorColumn];
        menuButton = new StyledButton() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(isEnabled() ? Color.black : Color.gray);
                int w = getWidth();
                int h = getHeight();
                g.fillPolygon(new int[]{w/4, 3*w/4, w/2}, new int[]{h/2-w/4, h/2-w/4, h/2+w/4}, 3);
            }
        }; 
        menuButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) {
                ColorPicker.this.mouseEntered(true);
            }
            public void mouseExited(MouseEvent ev) { 
                ColorPicker.this.mouseEntered(false);
            }
        });
        menuButton.setPreferredSize(new Dimension(16,2));
        this.add(setColorButton, BorderLayout.CENTER);
        this.add(menuButton, BorderLayout.EAST);
        menuButton.addActionListener((ev) -> {
            JPanel colorPanel = new JPanel();
            colorPanel.setBorder(BorderFactory.createEtchedBorder());
            colorPanel.setLayout(new GridLayout(colors.length, colors[0].length));
            for(int i=0; i<colors.length; i++) {
                for(int j=0; j<colors[i].length; j++) {
                    JButton b = new JButton() {
                        public void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            if (getModel().isRollover()){
                                g.setColor(Color.yellow);
                                g.drawRect(0, 0,  getWidth()-1, getHeight()-1);
                            }
                            Color col = getBackground();
                            g.setColor(col);
                            g.fillRect(1,  1, getWidth()-2, getHeight()-2);
                            if (col.equals(selected)) {
                              int R = col.getRed();
                              int G = col.getGreen();
                              int B = col.getBlue();
                              int avg = (R+G+B)/3;
                              if (avg >128)
                                  g.setColor(Color.black);
                              else
                                  g.setColor(Color.white);
                              g.fillOval(getWidth()/2-3, getHeight()/2-3, 6, 6);
                            }
                        }
                    };
                    b.setRolloverEnabled(true);
                    b.setPreferredSize(new Dimension(W, W));
                    b.setBackground(colors[i][j]);
                    b.setOpaque(true);
                    colorPanel.add(b);
                    b.addActionListener(                           
                            (ev1) -> {
                                JComponent gp = (JComponent)SwingUtilities.getRootPane(this).getGlassPane();
                                gp.remove(colorPanel);
                                gp.setVisible(false);  
                                selected = b.getBackground();
                                notifyColorSelectionListeners();
                            });

                }
            }
            colorPanel.setSize(colorPanel.getPreferredSize());
            JComponent gp = (JComponent)SwingUtilities.getRootPane(this).getGlassPane();
            Point p = getPopupLocation(menuButton, colorPanel);
            colorPanel.setLocation(p);
            gp.setLayout(null);          
            gp.add(colorPanel);
            gp.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    gp.remove(colorPanel);
                    gp.setVisible(false);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    gp.remove(colorPanel);
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
                    Point p = getPopupLocation(menuButton, colorPanel);
                    colorPanel.setLocation(p);
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
    
    protected void notifyColorSelectionListeners() {
        for(ColorSelectionListener l: listeners)
            l.colorSelected(selected);
    }
    
    public Color getSelectedColor() {
        return selected;
    }
    
    public JComponent getButton() {
        return setColorButton;
    }

    public void mouseEntered(boolean b) {
        setColorButton.getModel().setRollover(b);
        menuButton.getModel().setRollover(b);
    }
}
