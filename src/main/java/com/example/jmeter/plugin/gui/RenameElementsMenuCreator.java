package com.example.jmeter.plugin.gui;

import org.apache.jmeter.gui.plugin.MenuCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.MenuElement;


public class RenameElementsMenuCreator implements MenuCreator {
    private static final Logger log = LoggerFactory.getLogger(RenameElementsMenuCreator.class);

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION menuLocation) {
        if (menuLocation == MenuCreator.MENU_LOCATION.RUN)
            try {
                return new JMenuItem[] {new RenameElementsMenuItem()};
            } catch (Throwable e) {
                log.error("Failed to load debugger", e);
                return new JMenuItem[0];
            }
        return new JMenuItem[0];
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menuElement) {
        return false;
    }

    @Override
    public void localeChanged() {

    }
}
