package com.cisco.mscviewer.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JToggleButton;



@SuppressWarnings("serial")
public class StyledToggleButton extends JToggleButton {
    
    public StyledToggleButton() {
        super();
    }
    
    public StyledToggleButton(String title) {
        this();
        if (title != null)
            setText(title);
        setFocusable(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorderPainted(false);
        setRolloverEnabled(false);
        setBackground(null);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent ev) {
                getModel().setRollover(true);
            }
            @Override
            public void mouseExited(MouseEvent ev) {
                getModel().setRollover(false);
            }
        });
    }
    
    @Override
    public boolean isContentAreaFilled() {
        return false;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        if (getModel().isRollover()) {
            g2.setPaint(Styles.buttonHoverColor);
            g2.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
            g2.setColor(Styles.buttonHoverBorderColor);
            g2.drawRect(0,  0,  getWidth()-1,  getHeight()-1);
        } else if (isSelected()) {
            g2.setPaint(Styles.toggleSelectedGradient);                        
            g2.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        } 
        if (isSelected()) {
            g2.setPaint(Color.black);
            g2.drawRoundRect(2, 2, getWidth()-4, getHeight()-4, 3, 3);
        }
        super.paintComponent(g);
    }

}
