package com.cisco.mscviewer.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.ToolTipManager;



@SuppressWarnings("serial")
public class StyledButton extends JButton {
    public StyledButton() {
        setFocusable(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(false);
        setBackground(null);
        ToolTipManager.sharedInstance().registerComponent(this);

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
    
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        if (getModel().isRollover()) {
            g2.setPaint(Styles.buttonHoverColor);
            g2.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
            g2.setColor(Styles.buttonHoverBorderColor);
            g2.drawRect(0,  0,  getWidth()-1,  getHeight()-1);
        } 
        int w = getWidth();
        int h = getHeight();
        super.paintComponent(g);
    }

}
