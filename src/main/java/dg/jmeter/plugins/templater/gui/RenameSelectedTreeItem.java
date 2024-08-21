package dg.jmeter.plugins.templater.gui;

import dg.jmeter.plugins.templater.RunThroughTree;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.gui.util.JMeterToolBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static dg.jmeter.plugins.templater.utils.RenameUtils.CheckCreateRenameConfig;

public class RenameSelectedTreeItem extends JMenuItem implements ActionListener {
    private static final Logger log = LoggerFactory.getLogger(RenameSelectedTreeItem.class);

    public RenameSelectedTreeItem() {
        super("Rename Selected Tree", new ImageIcon(RenameSelectedTreeItem.class.getResource("/renameSelectedTreeIcon16.png")));
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
        JButton button = new JButton(new ImageIcon(RenameSelectedTreeItem.class.getResource("/renameSelectedTreeIcon22.png")));
        button.setToolTipText("Rename Selected Tree");
        button.addActionListener(this);
        return button;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CheckCreateRenameConfig();
        new RunThroughTree(true);
    }
}
