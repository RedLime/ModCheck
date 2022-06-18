package com.pistacium.modcheck.util;
/*
 * @(#)SwingUtils.java	1.02 11/15/08
 *
 */

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of utility methods for Swing.
 *
 * @author Darryl Burke
 */
public final class SwingUtils {

    private SwingUtils() {
        throw new Error("SwingUtils is just a container for static methods");
    }

    public static <T extends JComponent> List<T> getDescendantsOfType(
            Class<T> clazz, Container container) {
        return getDescendantsOfType(clazz, container, true);
    }


    public static <T extends JComponent> List<T> getDescendantsOfType(
            Class<T> clazz, Container container, boolean nested) {
        List<T> tList = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (clazz.isAssignableFrom(component.getClass())) {
                tList.add(clazz.cast(component));
            }
            if (nested || !clazz.isAssignableFrom(component.getClass())) {
                tList.addAll(SwingUtils.getDescendantsOfType(clazz,
                        (Container) component, nested));
            }
        }
        return tList;
    }
}