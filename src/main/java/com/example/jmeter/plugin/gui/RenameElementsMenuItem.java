package com.example.jmeter.plugin.gui;

import com.example.jmeter.plugin.PrintTreeModel;
import com.example.jmeter.plugin.RenameTreeElements;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.gui.util.JMeterToolBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JButton;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.example.jmeter.plugin.utils.RenameUtils.CheckCreateRenameConfig;

public class RenameElementsMenuItem extends JMenuItem implements ActionListener {
    private static final Logger log = LoggerFactory.getLogger(RenameElementsMenuItem.class);

    public RenameElementsMenuItem() {
        super("Rename elements", new ImageIcon(RenameElementsMenuItem.class.getResource("/renameIcon16.png")));
        addActionListener(this);
        addToolbarIcon();
    }

    private void addToolbarIcon() {
        GuiPackage guiPackage = GuiPackage.getInstance();
        if (guiPackage != null) {
            final MainFrame mainFrame = guiPackage.getMainFrame();
            final ComponentFinder<JMeterToolBar> finder = new ComponentFinder<>(JMeterToolBar.class);
            SwingUtilities.invokeLater(() -> {
                JMeterToolBar toolbar = null;
                while (toolbar == null) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        log.debug("Did not add btn to toolbar", e);
                    }
                    log.debug("Searching for toolbar");
                    toolbar = finder.findComponentIn(mainFrame);
                }
                int pos = 14;
                Component toolbarButton = getToolbarButton();
                toolbarButton.setSize(toolbar.getComponent(pos).getSize());
                toolbar.add(toolbarButton, pos);
            });
        }
    }

    private Component getToolbarButton() {
        JButton button = new JButton(new ImageIcon(RenameElementsMenuItem.class.getResource("/renameIcon22.png")));
        button.setToolTipText("MyCustomButton");
        button.addActionListener(this);
        return button;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CheckCreateRenameConfig();
        new RenameTreeElements();
    }
}
