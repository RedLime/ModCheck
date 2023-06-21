package com.pistacium.modcheck;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.*;
import com.pistacium.modcheck.util.Config;
import com.pistacium.modcheck.util.ModCheckStatus;
import com.pistacium.modcheck.util.ModCheckUtils;
import com.pistacium.modcheck.util.SwingUtils;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.impl.util.version.VersionParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ModCheckFrameForm extends JFrame {

    private static final FontUIResource font = new FontUIResource("SansSerif", Font.BOLD, 15);
    private JProgressBar progressBar;
    private JButton downloadButton;
    private JCheckBox deleteAllJarCheckbox;
    private JButton selectInstancePathsButton;
    private JComboBox<MCVersion> mcVersionCombo;
    private JRadioButton randomSeedRadioButton;
    private JRadioButton setSeedRadioButton;
    private JRadioButton windowsRadioButton;
    private JRadioButton macRadioButton;
    private JRadioButton linuxRadioButton;
    private JCheckBox accessibilityCheckBox;
    private JScrollPane modListScroll;
    private JPanel mainPanel;
    private JLabel selectedDirLabel;
    private JButton deselectAllButton;
    private JButton selectAllRecommendsButton;
    private JPanel modListPanel;
    private JScrollBar scrollBar1;


    private File[] selectDirs = null;
    private final HashMap<ModInfo, JCheckBox> modCheckBoxes = new HashMap<>();
    private String currentOS = ModCheckUtils.getCurrentOS();

    ModCheckFrameForm() throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        setContentPane(mainPanel);
        setTitle("ModCheck v" + ModCheckConstants.APPLICATION_VERSION + " by RedLime");
        setSize(1100, 700);
        setVisible(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Enumeration<?> keys = UIManager.getLookAndFeelDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, font);
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        URL resource = getClass().getClassLoader().getResource("end_crystal.png");
        if (resource != null) setIconImage(new ImageIcon(resource).getImage());

        initMenuBar();

        selectInstancePathsButton.addActionListener(e -> {
            Config instanceDir = ModCheckUtils.readConfig();
            JFileChooser pathSelector = instanceDir == null ? new JFileChooser() : new JFileChooser(instanceDir.getDir());
            pathSelector.setMultiSelectionEnabled(true);
            pathSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            pathSelector.setDialogType(JFileChooser.CUSTOM_DIALOG);
            pathSelector.setDialogTitle("Select Instance Paths");
            JComboBox<?> jComboBox = SwingUtils.getDescendantsOfType(JComboBox.class, pathSelector).get(0);
            jComboBox.setEditable(true);
            jComboBox.setEditor(new BasicComboBoxEditor.UIResource() {
                @Override
                public Object getItem() {
                    try {
                        return new File((String) super.getItem());
                    } catch (Exception e) {
                        return super.getItem();
                    }
                }
            });

            int showDialog = pathSelector.showDialog(this, "Select");
            File[] files = pathSelector.getSelectedFiles();
            if (pathSelector.getSelectedFiles() != null && showDialog == JFileChooser.APPROVE_OPTION) {
                selectDirs = files;
                String parentDir = "";
                StringBuilder stringBuilder = new StringBuilder();
                for (File selectDir : selectDirs) {
                    stringBuilder.append(parentDir.isEmpty() ? selectDir.getPath() : selectDir.getPath().replace(parentDir, "")).append(", ");
                    parentDir = selectDir.getParent();
                }
                selectedDirLabel.setText("<html>Selected Instances : <br>" + stringBuilder.substring(0, stringBuilder.length() - (stringBuilder.length() != 0 ? 2 : 0)) + "</html>");
            }
            ModCheckUtils.writeConfig(files[0].getParentFile());
        });

        progressBar.setString("Idle...");
        downloadButton.addActionListener(e -> {
            if (selectDirs == null || selectDirs.length < 1) return;

            downloadButton.setEnabled(false);
            Stack<File> modsFileStack = new Stack<>();

            int ignoreInstance = -1;

            for (File instanceDir : selectDirs) {
                Path instancePath = instanceDir.toPath();
                File dotMinecraft = instancePath.resolve(".minecraft").toFile();
                if (dotMinecraft.isDirectory()) {
                    instancePath = instancePath.resolve(".minecraft");
                }

                Path modsPath = instancePath.resolve("mods");
                File modsDir = modsPath.toFile();
                if (!modsDir.isDirectory()) {
                    int result = ignoreInstance != -1 ? ignoreInstance : JOptionPane.showConfirmDialog(this, "You have selected a directory but not a minecraft instance directory.\nAre you sure you want to download in this directory?", "Wrong instance directory", JOptionPane.OK_CANCEL_OPTION);

                    System.out.println(result);
                    if (result != 0) {
                        downloadButton.setEnabled(true);
                        return;
                    } else {
                        ignoreInstance = result;
                        modsFileStack.push(instanceDir);
                    }
                } else {
                    modsFileStack.push(modsDir);
                }
            }

            if (mcVersionCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Error: selected item is null");
                downloadButton.setEnabled(true);
                return;
            }

            ArrayList<ModInfo> targetMods = new ArrayList<>();
            int maxCount = 0;
            for (Map.Entry<ModInfo, JCheckBox> modEntry : modCheckBoxes.entrySet()) {
                if (modEntry.getValue().isSelected() && modEntry.getValue().isEnabled()) {
                    System.out.println("Selected " + modEntry.getKey().getName());
                    targetMods.add(modEntry.getKey());
                    maxCount++;
                }
            }
            MCVersion mcVersion = (MCVersion) mcVersionCombo.getSelectedItem();

            for (File instanceDir : modsFileStack) {
                File[] modFiles = instanceDir.listFiles();
                if (modFiles == null) return;
                for (File file : modFiles) {
                    if (file.getName().endsWith(".jar")) {
                        if (deleteAllJarCheckbox.isSelected()) {
                            file.delete();
                        } else {
                            String modFileName = file.getName().split("-")[0].split("\\+")[0];
                            for (ModInfo targetMod : targetMods) {
                                String targetModFileName = targetMod.getFileFromVersion(mcVersion, this.getRuleIndicator()).getName();
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
                ArrayList<ModInfo> failedMods = new ArrayList<>();
                for (ModInfo targetMod : targetMods) {
                    this.progressBar.setString("Downloading '" + targetMod.getName() + "'");
                    System.out.println("Downloading " + targetMod.getName());
                    Stack<File> downloadFiles = new Stack<>();
                    downloadFiles.addAll(modsFileStack);
                    if (!targetMod.downloadFile(mcVersion, this.getRuleIndicator(), downloadFiles)) {
                        System.out.println("Failed to downloading " + targetMod.getName());
                        failedMods.add(targetMod);
                    }
                    this.progressBar.setValue((int) ((++count / (finalMaxCount * 1f)) * 100));
                }
                this.progressBar.setValue(100);
                ModCheck.setStatus(ModCheckStatus.IDLE);

                System.out.println("Downloading mods complete");

                if (failedMods.size() > 0) {
                    StringBuilder failedModString = new StringBuilder();
                    for (ModInfo failedMod : failedMods) {
                        failedModString.append(failedMod.getName()).append(", ");
                    }
                    JOptionPane.showMessageDialog(this, "Failed to download " + failedModString.substring(0, failedModString.length() - 2) + ".", "Please try again", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "All selected mods have been downloaded!");
                }
                downloadButton.setEnabled(true);
            });
        });
        downloadButton.setEnabled(false);

        mcVersionCombo.addActionListener(e -> updateModList());

        selectAllRecommendsButton.addActionListener(e -> {
            for (Map.Entry<ModInfo, JCheckBox> entry : modCheckBoxes.entrySet()) {
                if (!entry.getKey().isRecommended()
                        || entry.getKey().getIncompatible().stream().anyMatch(incompatible ->
                        modCheckBoxes.entrySet().stream().anyMatch(entry2 ->
                                entry2.getKey().getName().equals(incompatible) && entry2.getValue().isSelected()))
                ) continue;

                if (entry.getValue().isEnabled()) {
                    entry.getValue().setSelected(true);
                }
            }
            JOptionPane.showMessageDialog(this, "<html><body>Some mods that have warnings (like noPeaceful)<br> or incompatible with other mods (like Starlight and Phosphor) aren't automatically selected.<br>You have to select them yourself.</body></html>", "WARNING!", JOptionPane.WARNING_MESSAGE);
        });

        deselectAllButton.addActionListener(e -> {
            for (JCheckBox cb : modCheckBoxes.values()) {
                cb.setSelected(false);
                cb.setEnabled(true);
            }
        });

        windowsRadioButton.addActionListener(e -> {
            currentOS = "windows";
            updateModList();
        });
        if (currentOS.equals("windows")) windowsRadioButton.setSelected(true);
        macRadioButton.addActionListener(e -> {
            currentOS = "osx";
            updateModList();
        });
        if (currentOS.equals("osx")) macRadioButton.setSelected(true);
        linuxRadioButton.addActionListener(e -> {
            currentOS = "linux";
            updateModList();
        });
        if (currentOS.equals("linux")) linuxRadioButton.setSelected(true);

        randomSeedRadioButton.addActionListener(e -> updateModList());
        setSeedRadioButton.addActionListener(e -> updateModList());
        accessibilityCheckBox.addActionListener(e -> {
            if (accessibilityCheckBox.isSelected()) {
                String message = "You may utilize these mods ONLY if you tell the MCSR Team about a medical condition that makes them necessary in advance.";
                int result = JOptionPane.showConfirmDialog(this, message, "THIS OPTION IS NOT FOR ALL!", JOptionPane.OK_CANCEL_OPTION);
                if (result == 0) {
                    updateModList();
                } else {
                    accessibilityCheckBox.setSelected(false);
                }
            } else {
                updateModList();
            }
        });
    }


    public void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu source = new JMenu("Info");

        JMenuItem githubSource = new JMenuItem("GitHub...");
        githubSource.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/RedLime/ModCheck"));
            } catch (Exception ignored) {
            }
        });
        source.add(githubSource);

        JMenuItem donateSource = new JMenuItem("Support");
        donateSource.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://ko-fi.com/redlimerl"));
            } catch (Exception ignored) {
            }
        });
        source.add(donateSource);

        JMenuItem checkChangeLogSource = new JMenuItem("Changelog");
        checkChangeLogSource.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/RedLime/ModCheck/releases/tag/" + ModCheckConstants.APPLICATION_VERSION));
            } catch (Exception ignored) {
            }
        });
        source.add(checkChangeLogSource);

        JMenuItem updateCheckSource = new JMenuItem("Check for updates");
        updateCheckSource.addActionListener(e -> {
            try {
                JsonObject jsonObject = JsonParser.parseString(ModCheckUtils.getUrlRequest("https://api.github.com/repos/RedLime/ModCheck/releases/latest")).getAsJsonObject();
                if (VersionParser.parseSemantic(jsonObject.get("tag_name").getAsString()).compareTo((Version) VersionParser.parseSemantic(ModCheckConstants.APPLICATION_VERSION)) > 0) {
                    int result = JOptionPane.showOptionDialog(null, "<html><body>Found new ModCheck update!<br><br>Current Version : " + ModCheckConstants.APPLICATION_VERSION + "<br>Updated Version : " + jsonObject.get("tag_name").getAsString() + "</body></html>", "Update Checker", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Download", "Cancel"}, "Download");
                    if (result == 0) {
                        Desktop.getDesktop().browse(new URI("https://github.com/RedLime/ModCheck/releases/latest"));
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "You are using the latest version!");
                }
            } catch (Exception ignored) {
            }
        });
        source.add(updateCheckSource);

        menuBar.add(source);

        this.setJMenuBar(menuBar);
    }

    public void updateVersionList() {
        mcVersionCombo.removeAllItems();
        for (MCVersion availableVersion : ModCheck.AVAILABLE_VERSIONS) {
            mcVersionCombo.addItem(availableVersion);
        }
        mcVersionCombo.setSelectedItem(ModCheck.AVAILABLE_VERSIONS.get(0));
        updateModList();
    }

    public void updateModList() {
        modListPanel.removeAll();
        modListPanel.setLayout(new BoxLayout(modListPanel, BoxLayout.Y_AXIS));
        modCheckBoxes.clear();

        if (mcVersionCombo.getSelectedItem() == null) return;

        MCVersion mcVersion = (MCVersion) mcVersionCombo.getSelectedItem();

        modFor:
        for (ModInfo modInfo : ModCheck.AVAILABLE_MODS) {
            ModFile modFile = modInfo.getFileFromVersion(mcVersion, this.getRuleIndicator());
            if (modFile != null) {
                if (modFile.getRules() != null) {
                    for (ModRule rule : modFile.getRules()) {
                        boolean allowed = rule.getAction().equals("allow");
                        for (Map.Entry<String, String> entry : rule.getProperties().entrySet()) {
                            if (entry.getKey().equals("category") && !(entry.getValue().equals("rsg") == randomSeedRadioButton.isSelected() == allowed))
                                continue modFor;
                            if (entry.getKey().equals("condition") && !(entry.getValue().equals("medical_issue") == accessibilityCheckBox.isSelected() == allowed))
                                continue modFor;
                            if (entry.getKey().equals("os") && !(entry.getValue().equals(currentOS) == allowed))
                                continue modFor;
                        }
                    }
                }

                JPanel modPanel = new JPanel();
                modPanel.setLayout(new BoxLayout(modPanel, BoxLayout.Y_AXIS));

                String versionName = modFile.getVersion();
                JCheckBox checkBox = new JCheckBox(modInfo.getName() + " (v" + (versionName.substring(versionName.startsWith("v") ? 1 : 0)) + ")");
                checkBox.addChangeListener(i -> {
                    modCheckBoxes.entrySet().stream()
                            .filter(entry -> entry.getKey().getIncompatible().contains(modInfo.getName()) || modInfo.getIncompatible().contains(entry.getKey().getName()))
                            .forEach(entry -> entry.getValue().setEnabled(modCheckBoxes.entrySet().stream()
                                    .noneMatch(entry2 -> (entry.getKey().getIncompatible().contains(entry2.getKey().getName()) || entry2.getKey().getIncompatible().contains(entry.getKey().getName())) && entry2.getValue().isSelected())));
                });

                int line = modInfo.getDescription().split("\n").length;
                JLabel description = new JLabel("<html><body>" + modInfo.getDescription().replaceAll("\n", "<br>").replaceAll("<a ", "<b ").replaceAll("</a>", "</b>") + "</body></html>");
                description.setMaximumSize(new Dimension(800, 60 * line));
                description.setBorder(new EmptyBorder(0, 15, 0, 0));
                Font f = description.getFont();
                description.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));

                modPanel.add(checkBox);
                modPanel.add(description);
                modPanel.setMaximumSize(new Dimension(950, 60 * line));
                modPanel.setBorder(new EmptyBorder(0, 10, 10, 0));

                modListPanel.add(modPanel);
                modCheckBoxes.put(modInfo, checkBox);
            }
        }
        modListPanel.updateUI();
        modListScroll.updateUI();
        downloadButton.setEnabled(true);
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    private RuleIndicator getRuleIndicator() {
        String runType = randomSeedRadioButton.isSelected() ? "rsg" : "ssg";
        return new RuleIndicator(currentOS, runType, accessibilityCheckBox.isSelected());
    }

}
