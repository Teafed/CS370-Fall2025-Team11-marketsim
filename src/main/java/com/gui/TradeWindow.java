package com.gui;

import javax.swing.*;
import java.awt.*;

public class TradeWindow extends JFrame {

    public  TradeWindow() {
        super("Trade Window");
        this.setSize(800, 600);

        createUserInput();

        this.setVisible(true);
    }

    public void createUserInput() {
        JTextField numberOfShares = new JTextField("Input number of shares");
        this.add(numberOfShares);
    }

}
