package com.pistacium.modcheck;

import javax.swing.*;

public class ModCheckFrame extends JFrame {

    ModCheckFrame() {
        super("ModCheck v"+ ModCheckConstants.APPLICATION_VERSION);
        setSize(800, 500);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

}
