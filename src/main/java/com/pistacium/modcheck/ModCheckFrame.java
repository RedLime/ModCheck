package com.pistacium.modcheck;

import com.pistacium.modcheck.mod.ModData;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.util.ModCheckStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModCheckFrame extends JFrame {

    private final JPanel mainPanel;
    private JProgressBar progressBar;
    private JComboBox<ModVersion> versionSelection;
    private JPanel versionJPanel;
    private JScrollPane versionScrollPane;
    private final HashMap<ModData, JCheckBox> modCheckBoxes = new HashMap<>();
    private JButton downloadButton;
    private JTextField pathField;

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    ModCheckFrame() {
        super("ModCheck v"+ ModCheckConstants.APPLICATION_VERSION + " by RedLime");

        mainPanel = new JPanel(new BorderLayout());

        initHeaderLayout();

        initCenterLayout();

        initBottomLayout();

        initMenuBar();

        getContentPane().add(mainPanel);

        setSize(800, 550);
        setResizable(false);
        setVisible(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initHeaderLayout() {
        JPanel instanceSelectPanel = new JPanel();

        JLabel headerLabel = new JLabel("Instance Path: ");
        pathField = new JTextField(30);
        JButton selectPathButton = new JButton("Select Path");
        selectPathButton.addActionListener(e -> {
            JFileChooser pathSelector = new JFileChooser();
            pathSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            pathSelector.showSaveDialog(null);
            if (pathSelector.getSelectedFile() != null) {
                pathField.setText(pathSelector.getSelectedFile().getPath());
            }
        });

        instanceSelectPanel.add(headerLabel);
        instanceSelectPanel.add(pathField);
        instanceSelectPanel.add(selectPathButton);
        mainPanel.add(instanceSelectPanel, BorderLayout.NORTH);
    }

    private void initBottomLayout() {
        JPanel instanceBottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 100, 5));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Idle...");
        progressBar.setPreferredSize(new Dimension(300, 20));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
        JCheckBox jCheckBox = new JCheckBox("Delete all old .jar files");
        downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> {
            downloadButton.setEnabled(false);

            Path instancePath;
            try {
                instancePath = Path.of(pathField.getText());
            } catch (InvalidPathException exception) {
                JOptionPane.showMessageDialog(this, "Failed to parsing Instance path!", "Please try again", JOptionPane.ERROR_MESSAGE);
                downloadButton.setEnabled(true);
                return;
            }
            if (!instancePath.toString().endsWith("mods")) instancePath = instancePath.resolve("mods");
            File instanceDir = instancePath.toFile();
            //noinspection ResultOfMethodCallIgnored
            instanceDir.mkdirs();
            if (!instanceDir.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Please select a instance path(directory)!", "Please try again", JOptionPane.ERROR_MESSAGE);
                downloadButton.setEnabled(true);
                return;
            }

            if (jCheckBox.isSelected()) {
                File[] modFiles = instanceDir.listFiles();
                if (modFiles == null) return;
                for (File file : modFiles) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }


            if (this.versionSelection.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "??? What are you doing");
                downloadButton.setEnabled(true);
                return;
            }

            ModVersion mcVersion = (ModVersion) this.versionSelection.getSelectedItem();
            this.progressBar.setValue(0);
            ModCheck.setStatus(ModCheckStatus.DOWNLOADING_MOD_FILE);

            Path finalInstancePath = instancePath;
            ArrayList<ModData> targetMods = new ArrayList<>();
            int maxCount = 0;
            for (Map.Entry<ModData, JCheckBox> modEntry : modCheckBoxes.entrySet()) {
                if (modEntry.getValue().isSelected() && modEntry.getValue().isEnabled()) {
                    targetMods.add(modEntry.getKey());
                    maxCount++;
                }
            }

            int finalMaxCount = maxCount;
            ModCheck.THREAD_EXECUTOR.submit(() -> {
                int count = 0;
                ArrayList<ModData> failedMods = new ArrayList<>();
                for (ModData targetMod : targetMods) {
                    this.progressBar.setString("Downloading '" + targetMod.getName() + "'");
                    if (!targetMod.downloadModJarFile(mcVersion, finalInstancePath)) {
                        failedMods.add(targetMod);
                    }
                    this.progressBar.setValue((int) ((++count / (finalMaxCount * 1f)) * 100));
                }
                this.progressBar.setValue(100);
                ModCheck.setStatus(ModCheckStatus.IDLE);

                for (ModData failedMod : failedMods) {
                    JOptionPane.showMessageDialog(this, "Failed to download '" + failedMod.getName() +"'.", "Please try again", JOptionPane.ERROR_MESSAGE);
                }
                JOptionPane.showMessageDialog(this, "All selected mods have been downloaded!");
                downloadButton.setEnabled(true);
            });
        });
        downloadButton.setEnabled(false);

        instanceBottomPanel.add(progressBar);
        buttonPanel.add(jCheckBox);
        buttonPanel.add(downloadButton);
        instanceBottomPanel.add(buttonPanel);
        mainPanel.add(instanceBottomPanel, BorderLayout.SOUTH);
    }

    private void initCenterLayout() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JPanel versionSelectPanel = new JPanel();
        JLabel headerLabel = new JLabel("Minecraft Version: ");
        versionSelection = new JComboBox<>();
        versionSelection.addActionListener(e -> updateModList());

        versionJPanel = new JPanel();
        versionJPanel.setLayout(new BoxLayout(versionJPanel, BoxLayout.Y_AXIS));
        versionScrollPane = new JScrollPane(versionJPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        versionScrollPane.setPreferredSize(new Dimension(600, 300));
        versionScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel selectButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> {
            for (Map.Entry<ModData, JCheckBox> entry : modCheckBoxes.entrySet()) {
                if (entry.getValue().isEnabled() && entry.getKey().getWarningMessage().isEmpty()) {
                    entry.getValue().setSelected(true);
                }
            }
        });
        JButton deselectAllButton = new JButton("Deselect All");
        deselectAllButton.addActionListener(e -> {
            for (JCheckBox cb : modCheckBoxes.values()) {
                cb.setSelected(false);
                cb.setEnabled(true);
            }
        });
        selectButtonPanel.add(selectAllButton);
        selectButtonPanel.add(deselectAllButton);

        versionSelectPanel.add(headerLabel);
        versionSelectPanel.add(versionSelection);
        centerPanel.add(versionSelectPanel);
        centerPanel.add(selectButtonPanel);
        centerPanel.add(versionScrollPane);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
    }

    public void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu githubSource = new JMenu("Github...");
        githubSource.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/RedLime/ModCheck"));
            } catch (Exception ignored) {
            }
        });
        menuBar.add(githubSource);

        this.setJMenuBar(menuBar);
    }

    public void updateVersionList() {
        versionSelection.removeAllItems();
        for (ModVersion availableVersion : ModCheck.AVAILABLE_VERSIONS) {
            versionSelection.addItem(availableVersion);
        }
        versionSelection.setSelectedItem(ModCheck.AVAILABLE_VERSIONS.get(0));
        updateModList();
    }

    public void updateModList() {
        versionJPanel.removeAll();
        modCheckBoxes.clear();

        if (this.versionSelection.getSelectedItem() == null) return;

        ModVersion mcVersion = (ModVersion) this.versionSelection.getSelectedItem();

        for (ModData modData : ModCheck.AVAILABLE_MODS) {
            if (modData.getLatestVersionResource(mcVersion) != null) {
                JPanel modPanel = new JPanel();
                modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));

                JCheckBox checkBox = new JCheckBox(modData.getName());
                checkBox.addActionListener(i -> {
                    boolean isSelected = checkBox.isSelected();
                    for (String incompatibleMod : modData.getIncompatibleMods()) {
                        for (Map.Entry<ModData, JCheckBox> entry : modCheckBoxes.entrySet()) {
                            if (Objects.equals(entry.getKey().getName(), incompatibleMod)) {
                                entry.getValue().setEnabled(!isSelected);
                            }
                        }
                    }

                    if (isSelected && !modData.getWarningMessage().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "<html><body>" + modData.getWarningMessage() + "<br>If you didn't follow this warning, your run being may rejected.</body></html>", "WARNING!", JOptionPane.WARNING_MESSAGE);
                    }
                });

                JLabel description = new JLabel("<html><body>" + modData.getDescription().replace("\n", "<br>") + "</body></html>");
                description.setMaximumSize(new Dimension(500, 60));
                description.setBorder(new EmptyBorder(0, 15,0, 0));
                Font f = description.getFont();
                description.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));

                modPanel.add(checkBox);
                modPanel.add(description);
                modPanel.setMaximumSize(new Dimension(600, 60));
                modPanel.setBorder(new EmptyBorder(0, 10,10, 0));

                versionJPanel.add(modPanel);
                modCheckBoxes.put(modData, checkBox);
            }
        }
        versionJPanel.updateUI();
        versionScrollPane.updateUI();
        downloadButton.setEnabled(true);
    }
}
