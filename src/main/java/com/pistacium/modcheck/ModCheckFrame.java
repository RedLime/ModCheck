package com.pistacium.modcheck;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.ModData;
import com.pistacium.modcheck.mod.resource.ModResource;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.util.ModCheckStatus;
import com.pistacium.modcheck.util.ModCheckUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ModCheckFrame extends JFrame {

    private static final FontUIResource font = new FontUIResource("SansSerif", Font.BOLD, 15);

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
        setUIFont();

        mainPanel = new JPanel(new BorderLayout());

        initHeaderLayout();

        initCenterLayout();

        initBottomLayout();

        initMenuBar();

        getContentPane().add(mainPanel);

        setSize(1100, 700);
        setResizable(false);
        setVisible(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private static void setUIFont(){
        Enumeration<?> keys = UIManager.getLookAndFeelDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, ModCheckFrame.font);
        }
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
        progressBar.setPreferredSize(new Dimension(500, 30));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
        JCheckBox jCheckBox = new JCheckBox("Delete all old .jar files");
        downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> {
            downloadButton.setEnabled(false);

            Stack<Path> instancePathStack = new Stack<>();
            try {
                String[] pathArr = pathField.getText().split(String.format("\\%s", File.separator));
                String lastPath = pathArr[pathArr.length - 1];
                if (lastPath.contains("*") && lastPath.chars().filter(c -> c == '*').count() == 1) {
                    pathArr[pathArr.length - 1] = "";
                    File[] pathFiles = Paths.get(String.join(File.separator, pathArr)).toFile().listFiles();
                    if (pathFiles == null) {
                        throw new IllegalAccessException();
                    }

                    if (lastPath.equals("*")) {
                        for (File pathFile : pathFiles) {
                            if (pathFile.isDirectory()) instancePathStack.push(pathFile.toPath());
                        }
                    } else if (lastPath.startsWith("*")) {
                        for (File pathFile : pathFiles) {
                            if (pathFile.isDirectory() && pathFile.getName().endsWith(lastPath.replace("*", ""))) instancePathStack.push(pathFile.toPath());
                        }
                    } else if (lastPath.endsWith("*")) {
                        for (File pathFile : pathFiles) {
                            if (pathFile.isDirectory() && pathFile.getName().startsWith(lastPath.replace("*", ""))) instancePathStack.push(pathFile.toPath());
                        }
                    } else {
                        String[] split = lastPath.split("\\*");
                        for (File pathFile : pathFiles) {
                            if (pathFile.isDirectory() && pathFile.getName().startsWith(split[0]) && pathFile.getName().endsWith(split[1])) instancePathStack.push(pathFile.toPath());
                        }
                    }
                } else {
                    instancePathStack.push(Paths.get(String.join(File.separator, pathArr)));
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to parsing Instance path!", "Please try again", JOptionPane.ERROR_MESSAGE);
                downloadButton.setEnabled(true);
                return;
            }

            Stack<File> modsFileStack = new Stack<>();
            for (Path instancePath : instancePathStack) {
                Path modsPath = instancePath.resolve("mods");
                File instanceDir = modsPath.toFile();
                if (!instanceDir.isDirectory()) {
                    JOptionPane.showMessageDialog(this, "Please select a instance path(directory)!", "Please try again", JOptionPane.ERROR_MESSAGE);
                    downloadButton.setEnabled(true);
                    return;
                }
                modsFileStack.push(instanceDir);
            }

            if (this.versionSelection.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "??? What are you doing");
                downloadButton.setEnabled(true);
                return;
            }

            ArrayList<ModData> targetMods = new ArrayList<>();
            int maxCount = 0;
            for (Map.Entry<ModData, JCheckBox> modEntry : modCheckBoxes.entrySet()) {
                if (modEntry.getValue().isSelected() && modEntry.getValue().isEnabled()) {
                    targetMods.add(modEntry.getKey());
                    maxCount++;
                }
            }
            ModVersion mcVersion = (ModVersion) this.versionSelection.getSelectedItem();

            for (File instanceDir : modsFileStack) {
                File[] modFiles = instanceDir.listFiles();
                if (modFiles == null) return;
                for (File file : modFiles) {
                    if (file.getName().endsWith(".jar")) {
                        if (jCheckBox.isSelected()) {
                            file.delete();
                        } else {
                            String modFileName = file.getName().split(ModVersion.versionRegex.pattern())[0]
                                    .split(ModVersion.snapshotRegex.pattern())[0];
                            for (ModData targetMod : targetMods) {
                                String targetModFileName = targetMod.getLatestVersionResource(mcVersion).getFileName();
                                if (targetModFileName.startsWith(modFileName)) {
                                    file.delete();
                                }
                            }
                        }
                    }
                }
            }

            this.progressBar.setValue(0);
            ModCheck.setStatus(ModCheckStatus.DOWNLOADING_MOD_FILE);

            int finalMaxCount = maxCount;
            ModCheck.THREAD_EXECUTOR.submit(() -> {
                int count = 0;
                ArrayList<ModData> failedMods = new ArrayList<>();
                for (ModData targetMod : targetMods) {
                    this.progressBar.setString("Downloading '" + targetMod.getName() + "'");
                    if (!targetMod.downloadModJarFile(mcVersion, modsFileStack)) {
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
        versionScrollPane.setPreferredSize(new Dimension(1000, 500));
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
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
        mainPanel.add(centerPanel, BorderLayout.CENTER);
    }

    public void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu source = new JMenu("Info");

        JMenuItem githubSource = new JMenuItem("Github...");
        githubSource.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/RedLime/ModCheck"));
            } catch (Exception ignored) {
            }
        });
        source.add(githubSource);

        JMenuItem donateSource = new JMenuItem("Support...");
        donateSource.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://ko-fi.com/redlimerl"));
            } catch (Exception ignored) {
            }
        });
        source.add(donateSource);

        JMenuItem checkChangeLogSource = new JMenuItem("Changelog...");
        checkChangeLogSource.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/RedLime/ModCheck/releases/tag/"+ModCheckConstants.APPLICATION_VERSION));
            } catch (Exception ignored) {
            }
        });
        source.add(checkChangeLogSource);

        JMenuItem updateCheckSource = new JMenuItem("Check new update");
        updateCheckSource.addActionListener(e -> {
            try {
                JsonObject jsonObject = JsonParser.parseString(ModCheckUtils.getUrlRequest("https://api.github.com/repos/RedLime/ModCheck/releases/latest")).getAsJsonObject();
                if (ModVersion.of(jsonObject.get("tag_name").getAsString()).compareTo(ModVersion.of(ModCheckConstants.APPLICATION_VERSION)) > 0) {
                    int result = JOptionPane.showOptionDialog(null, "<html><body>Found new ModCheck update!<br><br>Current Version : " + ModCheckConstants.APPLICATION_VERSION + "<br>Updated Version : " + jsonObject.get("tag_name").getAsString() + "</body></html>","Update Checker", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] { "Download", "Cancel" }, "Download");
                    if (result == 0) {
                        Desktop.getDesktop().browse(new URI("https://github.com/RedLime/ModCheck/releases/latest"));
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "You are using latest version!");
                }
            } catch (Exception ignored) {
            }
        });
        source.add(updateCheckSource);

        menuBar.add(source);

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
            ModResource modResource = modData.getLatestVersionResource(mcVersion);
            if (modResource != null) {
                JPanel modPanel = new JPanel();
                modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));

                JCheckBox checkBox = new JCheckBox(modData.getName() + " (v" + (modResource.getModVersion().getVersionName()) + ")");
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
                description.setMaximumSize(new Dimension(800, 60));
                description.setBorder(new EmptyBorder(0, 15,0, 0));
                Font f = description.getFont();
                description.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));

                modPanel.add(checkBox);
                modPanel.add(description);
                modPanel.setMaximumSize(new Dimension(950, 60));
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
