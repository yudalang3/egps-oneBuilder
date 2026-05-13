package tanglegram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileFilter;

final class TanglegramWelcomePanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;
    private final JTextArea errorLogArea;
    private final JToggleButton errorLogToggleButton;
    private final JPanel errorLogPanel;
    private final Consumer<LoadedImportSession> loadConsumer;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JButton loadRunningResultButton;
    private JButton loadTsvButton;
    private JButton exportTsvButton;
    private JButton addTreesButton;
    private JButton removeSelectedButton;
    private JButton loadButton;
    private Path lastRunningResultDir;
    private Path lastTsvConfigPath;
    private Path lastTreeFilePath;
    private ImportSourceKind currentSourceKind;
    private String currentSourceName;
    private List<TreePairSpec> currentRunningResultPairs;

    TanglegramWelcomePanel(Consumer<LoadedImportSession> loadConsumer) {
        super(new BorderLayout(0, 12));
        this.loadConsumer = loadConsumer;
        this.tableModel = createTableModel();
        this.table = new JTable(tableModel);
        this.statusLabel = new JLabel(defaultStatusText());
        this.errorLogArea = new JTextArea();
        this.errorLogToggleButton = new JToggleButton();
        this.errorLogPanel = new JPanel(new BorderLayout());
        this.lastRunningResultDir = UiPreferenceStore.loadRecentRunningResultDir();
        this.lastTsvConfigPath = UiPreferenceStore.loadRecentConfigFile();
        this.lastTreeFilePath = UiPreferenceStore.loadRecentTreeFileDir();
        this.currentSourceKind = ImportSourceKind.MANUAL;
        this.currentSourceName = "manual";
        this.currentRunningResultPairs = List.of();
        buildUi();
    }

    void populateFromStartupTreeSummary(Path treeSummaryDir) {
        runInBackground("Loading tree summary", () -> {
            TreeSummaryLoadResult loadResult = TreeSummaryLoader.load(treeSummaryDir);
            List<ImportedTreeSpec> importedTrees = resolveImportedTrees(importedTreesFromLoadResult(loadResult));
            return new LoadedImportSession(
                    loadResult.treeSummaryDir().getFileName() == null ? loadResult.treeSummaryDir().toString() : loadResult.treeSummaryDir().getFileName().toString(),
                    ImportSourceKind.RUNNING_RESULT,
                    importedTrees,
                    loadResult.availablePairs(),
                    loadResult.warnings());
        }, session -> {
            setStatus(UiText.text("Loaded startup tree summary: ", "已加载启动树摘要: ") + treeSummaryDir);
            loadConsumer.accept(session);
        });
    }

    private void buildUi() {
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(new WelcomeTreeIcon());
        headerPanel.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new BorderLayout(0, 4));
        textPanel.setOpaque(false);
        titleLabel = new JLabel(UiText.text("Import Tree Data", "导入树数据"));
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            titleLabel.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 4f));
        }
        subtitleLabel = new JLabel(UiText.text(
                "Load running results, TSV configs, or custom tree files and open a result tab.",
                "加载运行结果、TSV 配置或自定义树文件，并打开结果标签页。"));
        subtitleLabel.setForeground(new Color(94, 112, 137));
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(subtitleLabel, BorderLayout.CENTER);
        headerPanel.add(textPanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        TableColumn labelColumn = table.getColumnModel().getColumn(0);
        labelColumn.setMinWidth(320);
        labelColumn.setPreferredWidth(380);
        labelColumn.setMaxWidth(520);
        TableColumn pathColumn = table.getColumnModel().getColumn(1);
        pathColumn.setMinWidth(520);
        pathColumn.setPreferredWidth(880);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.columnAtPoint(event.getPoint()) == 1) {
                    browseSelectedRow();
                }
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 241)));
        add(tableScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setOpaque(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.setOpaque(false);

        loadRunningResultButton = new JButton(UiText.text("Load Running Result", "加载运行结果"));
        loadRunningResultButton.setToolTipText(runningResultTooltip());
        loadRunningResultButton.addActionListener(event -> chooseRunningResult());

        loadTsvButton = new JButton(UiText.text("Load Config File", "加载配置文件"));
        loadTsvButton.setToolTipText(configFileTooltip());
        loadTsvButton.addActionListener(event -> chooseTsvConfig());

        exportTsvButton = new JButton(UiText.text("Export Config File", "导出配置文件"));
        exportTsvButton.setToolTipText(configFileTooltip());
        exportTsvButton.addActionListener(event -> exportTsvConfig());

        addTreesButton = new JButton(UiText.text("Add Trees...", "添加树文件..."));
        addTreesButton.addActionListener(event -> addTrees());

        removeSelectedButton = new JButton(UiText.text("Remove Selected", "移除所选项"));
        removeSelectedButton.addActionListener(event -> removeSelectedRows());

        loadButton = new JButton(UiText.text("Load Tree Data", "加载树数据"));
        loadButton.addActionListener(event -> loadCurrentTable());

        buttonPanel.add(loadRunningResultButton);
        buttonPanel.add(loadTsvButton);
        buttonPanel.add(exportTsvButton);
        buttonPanel.add(addTreesButton);
        buttonPanel.add(removeSelectedButton);

        JPanel primaryActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        primaryActionPanel.setOpaque(false);
        primaryActionPanel.add(loadButton);

        JPanel actionRowPanel = new JPanel(new BorderLayout(12, 0));
        actionRowPanel.setOpaque(false);
        actionRowPanel.add(buttonPanel, BorderLayout.WEST);
        actionRowPanel.add(primaryActionPanel, BorderLayout.EAST);

        errorLogArea.setEditable(false);
        errorLogArea.setLineWrap(true);
        errorLogArea.setWrapStyleWord(true);
        errorLogArea.setRows(4);
        errorLogArea.setText(defaultErrorLogText());
        JScrollPane errorLogScrollPane = new JScrollPane(errorLogArea);

        errorLogToggleButton.setFocusable(false);
        errorLogToggleButton.setSelected(false);
        errorLogToggleButton.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        errorLogToggleButton.setContentAreaFilled(false);
        errorLogToggleButton.setBorderPainted(false);
        errorLogToggleButton.setOpaque(false);
        errorLogToggleButton.addActionListener(event -> updateErrorLogVisibility());

        JPanel errorHeaderPanel = new JPanel(new BorderLayout());
        errorHeaderPanel.setOpaque(false);
        errorHeaderPanel.add(errorLogToggleButton, BorderLayout.WEST);

        errorLogPanel.setOpaque(false);
        errorLogPanel.add(errorHeaderPanel, BorderLayout.NORTH);
        errorLogPanel.add(errorLogScrollPane, BorderLayout.CENTER);
        updateErrorLogState(false, false, defaultErrorLogText());

        bottomPanel.add(actionRowPanel, BorderLayout.NORTH);
        bottomPanel.add(errorLogPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    void applyPreferences(UiPreferences preferences) {
        titleLabel.setText(UiText.text(preferences, "Import Tree Data", "导入树数据"));
        subtitleLabel.setText(UiText.text(
                preferences,
                "Load running results, TSV configs, or custom tree files and open a result tab.",
                "加载运行结果、TSV 配置或自定义树文件，并打开结果标签页。"));
        loadRunningResultButton.setText(UiText.text(preferences, "Load Running Result", "加载运行结果"));
        loadRunningResultButton.setToolTipText(runningResultTooltip(preferences));
        loadTsvButton.setText(UiText.text(preferences, "Load Config File", "加载配置文件"));
        loadTsvButton.setToolTipText(configFileTooltip(preferences));
        exportTsvButton.setText(UiText.text(preferences, "Export Config File", "导出配置文件"));
        exportTsvButton.setToolTipText(configFileTooltip(preferences));
        addTreesButton.setText(UiText.text(preferences, "Add Trees...", "添加树文件..."));
        removeSelectedButton.setText(UiText.text(preferences, "Remove Selected", "移除所选项"));
        loadButton.setText(UiText.text(preferences, "Load Tree Data", "加载树数据"));
        if (statusLabel.getText() == null || statusLabel.getText().isBlank() || statusLabel.getText().equals(defaultStatusText())
                || statusLabel.getText().equals(UiText.text(preferences, "Add at least two trees, or import a running result / TSV config.", "至少添加两棵树，或导入运行结果 / TSV 配置。"))) {
            statusLabel.setText(UiText.text(preferences, "Add at least two trees, or import a running result / TSV config.", "至少添加两棵树，或导入运行结果 / TSV 配置。"));
        }
        if (errorLogArea.getText() == null || errorLogArea.getText().isBlank() || errorLogArea.getText().equals(defaultErrorLogText())) {
            errorLogArea.setText(UiText.text(preferences,
                    "No problems yet. If loading fails, this area will explain what happened and what to do next.",
                    "目前没有问题。如果加载失败，这里会说明发生了什么以及下一步该怎么做。"));
        }
        updateErrorLogVisibility();
    }

    private static String defaultStatusText() {
        return UiText.text("Add at least two trees, or import a running result / TSV config.", "至少添加两棵树，或导入运行结果 / TSV 配置。");
    }

    private static String defaultErrorLogText() {
        return UiText.text(
                "No problems yet. If loading fails, this area will explain what happened and what to do next.",
                "目前没有问题。如果加载失败，这里会说明发生了什么以及下一步该怎么做。");
    }

    private static String runningResultTooltip() {
        return UiText.text(
                "Import a oneBuilder result folder directly. Select the main output folder that contains tree_summary.",
                "直接导入 oneBuilder 结果目录。请选择包含 tree_summary 的主输出目录。");
    }

    private static String runningResultTooltip(UiPreferences preferences) {
        return UiText.text(
                preferences,
                "Import a oneBuilder result folder directly. Select the main output folder that contains tree_summary.",
                "直接导入 oneBuilder 结果目录。请选择包含 tree_summary 的主输出目录。");
    }

    private static String configFileTooltip() {
        return UiText.text(
                "Use a TSV config file for tree labels and tree paths. Lines starting with # are comments; column 1 is label name and column 2 is tree path.",
                "使用 TSV 配置文件记录树标签和树文件路径。以 # 开头的行是注释；第 1 列是标签名，第 2 列是树文件路径。");
    }

    private static String configFileTooltip(UiPreferences preferences) {
        return UiText.text(
                preferences,
                "Use a TSV config file for tree labels and tree paths. Lines starting with # are comments; column 1 is label name and column 2 is tree path.",
                "使用 TSV 配置文件记录树标签和树文件路径。以 # 开头的行是注释；第 1 列是标签名，第 2 列是树文件路径。");
    }

    private void chooseRunningResult() {
        JFileChooser fileChooser = new JFileChooser(lastRunningResultDir == null ? null : lastRunningResultDir.toFile());
        fileChooser.setDialogTitle(UiText.text("Select running result folder", "选择运行结果目录"));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int selection = fileChooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedDirectory = selectedChooserPath(
                fileChooser.getSelectedFile(),
                UiText.text("Could not open the selected running result folder.", "无法打开所选运行结果目录。"));
        if (selectedDirectory == null) {
            return;
        }
        runInBackground("Loading running result", () -> {
            TreeSummaryLoadResult loadResult = TreeSummaryLoader.loadRunResult(selectedDirectory);
            return loadResult;
        }, loadResult -> {
            lastRunningResultDir = selectedDirectory.toAbsolutePath().normalize();
            UiPreferenceStore.saveRecentRunningResultDir(lastRunningResultDir);
            currentSourceKind = ImportSourceKind.RUNNING_RESULT;
            Path fileName = lastRunningResultDir.getFileName();
            currentSourceName = fileName == null ? lastRunningResultDir.toString() : fileName.toString();
            currentRunningResultPairs = loadResult.availablePairs();
            List<ImportedTreeSpec> importedTrees = importedTreesFromLoadResult(loadResult);
            replaceRows(importedTrees);
            showSuccess(UiText.text("Loaded running result config from ", "已加载运行结果配置: ") + lastRunningResultDir + ".");
        });
    }

    private void chooseTsvConfig() {
        JFileChooser fileChooser = new JFileChooser(lastTsvConfigPath == null ? null : lastTsvConfigPath.toFile());
        fileChooser.setDialogTitle(UiText.text("Open config file", "打开配置文件"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(createConfigFileFilter());
        int selection = fileChooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedFile = selectedChooserPath(
                fileChooser.getSelectedFile(),
                UiText.text("Could not open the selected config file.", "无法打开所选配置文件。"));
        if (selectedFile == null) {
            return;
        }
        runInBackground("Loading TSV config", () -> TreeImportConfigIO.readTsv(selectedFile), importedTrees -> {
            lastTsvConfigPath = selectedFile.toAbsolutePath().normalize();
            UiPreferenceStore.saveRecentConfigFile(lastTsvConfigPath);
            currentSourceKind = ImportSourceKind.TSV;
            Path fileName = lastTsvConfigPath.getFileName();
            currentSourceName = fileName == null ? lastTsvConfigPath.toString() : fileName.toString();
            replaceRows(importedTrees);
            showSuccess(UiText.text("Loaded TSV config from ", "已加载 TSV 配置: ") + lastTsvConfigPath + ".");
        });
    }

    private void exportTsvConfig() {
        JFileChooser fileChooser = new JFileChooser(lastTsvConfigPath == null ? null : lastTsvConfigPath.toFile());
        fileChooser.setDialogTitle(UiText.text("Export config file", "导出配置文件"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(createConfigFileFilter());
        int selection = fileChooser.showSaveDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedFile = fileChooser.getSelectedFile().toPath();
        List<ImportedTreeSpec> importedTrees;
        try {
            importedTrees = collectImportedTreesFromTable();
        } catch (Exception exception) {
            showUserError(UiText.text("Could not export the TSV file.", "无法导出 TSV 文件。"), exception.getMessage());
            return;
        }
        runInBackground("Exporting TSV config", () -> {
            TreeImportConfigIO.writeTsv(selectedFile, importedTrees);
            return selectedFile.toAbsolutePath().normalize();
        }, exportedPath -> {
            lastTsvConfigPath = exportedPath;
            UiPreferenceStore.saveRecentConfigFile(lastTsvConfigPath);
            showSuccess(UiText.text("Exported TSV config to ", "已导出 TSV 配置到 ") + exportedPath + ".");
        });
    }

    private void addTrees() {
        JFileChooser fileChooser = new JFileChooser(lastTreeFilePath == null ? null : lastTreeFilePath.toFile());
        fileChooser.setDialogTitle(UiText.text("Choose Newick tree files", "选择 Newick 树文件"));
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Tree files", "nwk", "tree", "tre", "newick", "txt"));
        int selection = fileChooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        for (java.io.File file : fileChooser.getSelectedFiles()) {
            Path path = file.toPath().toAbsolutePath().normalize();
            tableModel.addRow(new Object[] { defaultLabel(path), path.toString() });
            lastTreeFilePath = path.getParent();
            UiPreferenceStore.saveRecentTreeFileDir(lastTreeFilePath);
        }
        currentSourceKind = ImportSourceKind.MANUAL;
        currentSourceName = "manual";
        showSuccess(UiText.text("Added ", "已添加 ") + fileChooser.getSelectedFiles().length + UiText.text(" tree file(s).", " 个树文件。"));
    }

    private void browseSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showUserError(
                    UiText.text("No row is selected.", "尚未选择任何行。"),
                    UiText.text(
                            "Select one row first, then double-click the Tree Path cell you want to fill.",
                            "请先选择一行，再双击你要填写的 Tree Path 单元格。"));
            return;
        }
        String currentPathText = TreeImportConfigIO.normalizePathText(valueAt(selectedRow, 1));
        Path initialPath = initialTreeChooserPath(currentPathText);
        JFileChooser fileChooser = new JFileChooser(initialPath == null ? null : initialPath.toFile());
        fileChooser.setDialogTitle(UiText.text("Choose tree file for selected row", "为所选行选择树文件"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Tree files", "nwk", "tree", "tre", "newick", "txt"));
        int selection = fileChooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedPath = fileChooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        lastTreeFilePath = selectedPath.getParent();
        UiPreferenceStore.saveRecentTreeFileDir(lastTreeFilePath);
        tableModel.setValueAt(selectedPath.toString(), selectedRow, 1);
        String currentLabel = valueAt(selectedRow, 0).trim();
        if (currentLabel.isEmpty()) {
            tableModel.setValueAt(defaultLabel(selectedPath), selectedRow, 0);
        }
        currentSourceKind = ImportSourceKind.MANUAL;
        currentSourceName = "manual";
        showSuccess(UiText.text("Updated row ", "已更新第 ") + (selectedRow + 1)
                + UiText.text(" with ", " 行，文件为 ") + selectedPath.getFileName() + ".");
    }

    private Path initialTreeChooserPath(String currentPathText) {
        if (currentPathText != null && !currentPathText.isBlank()) {
            try {
                Path currentPath = Path.of(currentPathText).toAbsolutePath().normalize();
                if (Files.isRegularFile(currentPath)) {
                    return currentPath;
                }
                if (Files.isDirectory(currentPath)) {
                    return currentPath;
                }
            } catch (Exception ignored) {
            }
        }
        return lastTreeFilePath;
    }

    private void removeSelectedRows() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            showUserError(UiText.text("Nothing was removed.", "没有移除任何内容。"),
                    UiText.text("Select one or more rows, then click Remove Selected.", "请选择一行或多行，然后点击“移除所选项”。"));
            return;
        }
        for (int index = selectedRows.length - 1; index >= 0; index--) {
            tableModel.removeRow(selectedRows[index]);
        }
        showSuccess(UiText.text("Removed ", "已移除 ") + selectedRows.length + UiText.text(" row(s).", " 行。"));
    }

    private void loadCurrentTable() {
        List<ImportedTreeSpec> importedTrees;
        try {
            importedTrees = collectImportedTreesFromTable();
        } catch (Exception exception) {
            showUserError(UiText.text("Tree data could not be loaded.", "无法加载树数据。"), exception.getMessage());
            return;
        }
        if (importedTrees.size() < 2) {
            showUserError(
                    UiText.text("Tree data could not be loaded.", "\u65e0\u6cd5\u52a0\u8f7d\u6811\u6570\u636e\u3002"),
                    UiText.text(
                            "At least two readable tree files are required.\n\nWhat to do:\n- Add another tree file, or\n- Remove incomplete rows, then click Load Tree Data again.",
                            "\u81f3\u5c11\u9700\u8981\u4e24\u4e2a\u53ef\u8bfb\u7684\u6811\u6587\u4ef6\u3002\n\n\u5982\u4f55\u64cd\u4f5c\uff1a\n- \u6dfb\u52a0\u4e00\u4e2a\u6811\u6587\u4ef6\uff0c\u6216\n- \u79fb\u9664\u4e0d\u5b8c\u6574\u7684\u884c\uff0c\u7136\u540e\u518d\u6b21\u70b9\u51fb\u201c\u52a0\u8f7d\u6811\u6570\u636e\u201d\u3002"));
            return;
        }
        List<TreePairSpec> pairSpecs = currentPairSpecs(importedTrees);
        String sourceName = deriveSourceName(importedTrees);
        ImportSourceKind sourceKind = deriveSourceKind(importedTrees);
        runInBackground("Loading tree data", () -> new LoadedImportSession(
                sourceName,
                sourceKind,
                resolveImportedTrees(importedTrees),
                pairSpecs,
                List.of()), session -> {
            loadConsumer.accept(session);
            showSuccess(UiText.text(
                    "Opened a result tab for " + sourceName + " with " + pairSpecs.size() + " pairwise comparisons.",
                    "\u5df2\u4e3a " + sourceName + " \u6253\u5f00\u7ed3\u679c\u6807\u7b7e\u9875\uff0c\u5305\u542b " + pairSpecs.size() + " \u7ec4\u6bd4\u8f83\u3002"));
        });
    }

    private List<ImportedTreeSpec> collectImportedTreesFromTable() {
        List<ImportedTreeSpec> importedTrees = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String labelText = valueAt(row, 0).trim();
            String pathText = TreeImportConfigIO.normalizePathText(valueAt(row, 1));
            if (labelText.isEmpty() && pathText.isEmpty()) {
                continue;
            }
            if (labelText.isEmpty()) {
                throw new IllegalArgumentException("Row " + (row + 1) + " is missing a label name.");
            }
            if (pathText.isEmpty()) {
                throw new IllegalArgumentException("Row " + (row + 1) + " is missing a tree path.");
            }
            Path treePath = Path.of(pathText).toAbsolutePath().normalize();
            if (!Files.isRegularFile(treePath)) {
                throw new IllegalArgumentException("Tree file does not exist at row " + (row + 1) + ": " + treePath);
            }
            importedTrees.add(new ImportedTreeSpec(treePath, labelText));
        }
        return importedTrees;
    }

    private static List<TreePairSpec> buildPairSpecs(List<ImportedTreeSpec> importedTrees) {
        List<TreePairSpec> pairSpecs = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < importedTrees.size(); leftIndex++) {
            ImportedTreeSpec leftTree = importedTrees.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < importedTrees.size(); rightIndex++) {
                ImportedTreeSpec rightTree = importedTrees.get(rightIndex);
                pairSpecs.add(new TreePairSpec(leftTree.label(), rightTree.label(), leftTree.path(), rightTree.path()));
            }
        }
        return pairSpecs;
    }

    private List<TreePairSpec> currentPairSpecs(List<ImportedTreeSpec> importedTrees) {
        if (currentSourceKind == ImportSourceKind.RUNNING_RESULT
                && lastRunningResultDir != null
                && matchesRunningResult(importedTrees, lastRunningResultDir)
                && currentRunningResultPairs.size() > 0) {
            return currentRunningResultPairs;
        }
        return buildPairSpecs(importedTrees);
    }

    private static List<ImportedTreeSpec> importedTreesFromLoadResult(TreeSummaryLoadResult loadResult) {
        List<ImportedTreeSpec> importedTrees = new ArrayList<>();
        for (TreeMethod method : TreeMethod.DISPLAY_ORDER) {
            Path path = loadResult.resolvedTrees().get(method);
            if (path != null) {
                importedTrees.add(new ImportedTreeSpec(path, method.shortLabel()));
            }
        }
        return importedTrees;
    }

    void loadRunningResultForTest(Path outputDirectory, TreeSummaryLoadResult loadResult) {
        lastRunningResultDir = outputDirectory.toAbsolutePath().normalize();
        currentSourceKind = ImportSourceKind.RUNNING_RESULT;
        Path fileName = lastRunningResultDir.getFileName();
        currentSourceName = fileName == null ? lastRunningResultDir.toString() : fileName.toString();
        currentRunningResultPairs = loadResult.availablePairs();
        replaceRows(importedTreesFromLoadResult(loadResult));
    }

    void loadCurrentTableForTest() {
        loadCurrentTable();
    }

    private static List<ImportedTreeSpec> resolveImportedTrees(List<ImportedTreeSpec> importedTrees) throws Exception {
        List<ImportedTreeSpec> resolvedTrees = new ArrayList<>(importedTrees.size());
        for (ImportedTreeSpec importedTree : importedTrees) {
            resolvedTrees.add(TreeDataLoader.loadImportedTree(importedTree));
        }
        return resolvedTrees;
    }

    private void replaceRows(List<ImportedTreeSpec> importedTrees) {
        tableModel.setRowCount(0);
        for (ImportedTreeSpec importedTree : importedTrees) {
            tableModel.addRow(new Object[] { importedTree.label(), importedTree.path().toString() });
        }
    }

    private String deriveSourceName(List<ImportedTreeSpec> importedTrees) {
        if (currentSourceKind == ImportSourceKind.RUNNING_RESULT && lastRunningResultDir != null && matchesRunningResult(importedTrees, lastRunningResultDir)) {
            Path fileName = lastRunningResultDir.getFileName();
            return fileName == null ? lastRunningResultDir.toString() : fileName.toString();
        }
        if (currentSourceKind == ImportSourceKind.TSV && lastTsvConfigPath != null) {
            Path fileName = lastTsvConfigPath.getFileName();
            return fileName == null ? lastTsvConfigPath.toString() : fileName.toString();
        }
        if (currentSourceName != null && !currentSourceName.isBlank()) {
            return currentSourceName;
        }
        Path firstPath = importedTrees.get(0).path().getFileName();
        return firstPath == null ? "manual" : firstPath.toString();
    }

    private ImportSourceKind deriveSourceKind(List<ImportedTreeSpec> importedTrees) {
        if (currentSourceKind == ImportSourceKind.RUNNING_RESULT && lastRunningResultDir != null && matchesRunningResult(importedTrees, lastRunningResultDir)) {
            return ImportSourceKind.RUNNING_RESULT;
        }
        if (currentSourceKind == ImportSourceKind.TSV && lastTsvConfigPath != null) {
            return ImportSourceKind.TSV;
        }
        return ImportSourceKind.MANUAL;
    }

    private static boolean matchesRunningResult(List<ImportedTreeSpec> importedTrees, Path runningResultDir) {
        Path normalizedDir = runningResultDir.toAbsolutePath().normalize();
        for (ImportedTreeSpec importedTree : importedTrees) {
            if (!importedTree.path().startsWith(normalizedDir)) {
                return false;
            }
        }
        return !importedTrees.isEmpty();
    }

    private <T> void runInBackground(String actionLabel, BackgroundSupplier<T> supplier, Consumer<T> successConsumer) {
        setStatus(actionLabel + " ...");
        Thread worker = new Thread(() -> {
            try {
                T result = supplier.get();
                SwingUtilities.invokeLater(() -> successConsumer.accept(result));
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> showUserError(actionLabel + " failed.", exception.getMessage()));
            }
        }, "tanglegram-welcome-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null || message.isBlank() ? " " : message);
    }

    private void showSuccess(String message) {
        setStatus(message);
        updateErrorLogState(false, false, UiText.text(
                "No problems found.\n\nYou can continue with the next step.",
                "未发现问题。\n\n你可以继续下一步。"));
    }

    private void showUserError(String title, String rawMessage) {
        setStatus(title);
        updateErrorLogState(true, true, title + "\n\n" + buildUserHelpMessage(rawMessage));
    }

    private void updateErrorLogState(boolean expanded, boolean highlight, String text) {
        errorLogArea.setText(text);
        errorLogArea.setCaretPosition(0);
        errorLogToggleButton.setSelected(expanded);
        errorLogToggleButton.setForeground(highlight ? new Color(184, 41, 41) : new Color(94, 112, 137));
        updateErrorLogVisibility();
    }

    private void updateErrorLogVisibility() {
        boolean expanded = errorLogToggleButton.isSelected();
        boolean hasError = new Color(184, 41, 41).equals(errorLogToggleButton.getForeground());
        String indicator = hasError ? "  ●" : "";
        errorLogToggleButton.setText((expanded ? "▼" : "▶") + " " + UiText.text("Error Log", "错误日志") + indicator);
        if (errorLogPanel.getComponentCount() > 1) {
            errorLogPanel.getComponent(1).setVisible(expanded);
        }
        errorLogPanel.revalidate();
        errorLogPanel.repaint();
    }

    private Path selectedChooserPath(java.io.File selectedFile, String failureTitle) {
        if (selectedFile == null) {
            showUserError(failureTitle, UiText.text("No file or folder was selected.", "未选择文件或目录。"));
            return null;
        }
        try {
            return selectedFile.toPath().toAbsolutePath().normalize();
        } catch (Exception exception) {
            showUserError(failureTitle, exception.getMessage());
            return null;
        }
    }

    private FileFilter createConfigFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(java.io.File file) {
                if (file == null) {
                    return false;
                }
                if (file.isDirectory()) {
                    return true;
                }
                String name = file.getName().toLowerCase();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex < 0) {
                    return true;
                }
                String suffix = name.substring(dotIndex + 1);
                return suffix.equals("tsv") || suffix.equals("txt");
            }

            @Override
            public String getDescription() {
                return UiText.text("Config files (*.tsv, *.txt, or files without extension)", "配置文件 (*.tsv, *.txt，或无扩展名文件)");
            }
        };
    }

    private String buildUserHelpMessage(String rawMessage) {
        String message = rawMessage == null || rawMessage.isBlank()
                ? UiText.text("The selected data could not be used.", "所选数据无法使用。")
                : rawMessage;
        String lowerCaseMessage = message.toLowerCase();

        if (lowerCaseMessage.contains("missing required directories")) {
            return UiText.text(
                    "The selected folder does not look like a oneBuilder result folder.\n\nWhat to do:\n- Choose the main output folder of oneBuilder.\n- It can contain any two or more available tree results, such as distance_method, maximum_likelihood, bayesian_method, parsimony_method, protein_structure, or tree_summary.",
                    "所选文件夹不像 oneBuilder 结果目录。\n\n如何操作：\n- 选择 oneBuilder 的主输出目录。\n- 其中可以包含任意两个或更多可用树结果，例如 distance_method、maximum_likelihood、bayesian_method、parsimony_method、protein_structure 或 tree_summary。");
        }
        if (lowerCaseMessage.contains("missing required tree files")) {
            return UiText.text(
                    "Some expected tree files are missing from the selected result folder.\n\nWhat to do:\n- Confirm the run finished successfully.\n- The viewer can still load any two or more available trees; remove empty outputs or choose a folder with at least two generated trees.",
                    "在所选结果目录中缺少部分预期的树文件。\n\n如何操作：\n- 确认运行已成功完成。\n- 查看器仍可加载任意两个或更多可用树；请移除空输出，或选择至少包含两棵生成树的目录。");
        }
        if (lowerCaseMessage.contains("at least two readable tree files")) {
            return UiText.text(
                    "The selected result folder does not contain enough readable tree files.\n\nWhat to do:\n- Choose a oneBuilder output folder with at least two generated trees.\n- Protein structure output can be loaded when protein_structure/distance_matrix.tsv exists; the viewer will generate ProteinCluster automatically.",
                    "所选结果目录中可读取的树文件不足。\n\n如何操作：\n- 选择至少包含两棵生成树的 oneBuilder 输出目录。\n- 如果存在 protein_structure/distance_matrix.tsv，查看器会自动生成 ProteinCluster 并导入。");
        }
        if (lowerCaseMessage.contains("invalid tsv row")) {
            return UiText.text(
                    "The TSV file format is not correct.\n\nWhat to do:\n- Lines starting with # are comments.\n- Column 1 must be the label name.\n- Column 2 must be the tree file path.\n- Save the file as tab-separated text, then import it again.",
                    "TSV 文件格式不正确。\n\n如何操作：\n- 以 # 开头的行是注释。\n- 第 1 列必须是标签名。\n- 第 2 列必须是树文件路径。\n- 保存为制表符分隔的文本文件后重新导入。");
        }
        if (lowerCaseMessage.contains("missing a label name")) {
            return UiText.text(
                    "One row is missing a label name.\n\nWhat to do:\n- Fill in the Label Name column for that row.\n- Example labels: NJ, ML, BI, MP, or any custom display name you want to show in the result tabs.",
                    "有一行缺少标签名。\n\n如何操作：\n- 为该行填写 Label Name 列。\n- 标签示例：NJ、ML、BI、MP，或任意自定义显示名称。");
        }
        if (lowerCaseMessage.contains("missing a tree path")) {
            return UiText.text(
                    "One row is missing a tree path.\n\nWhat to do:\n- Fill in the Tree Path column for that row.\n- Use Add Trees... or double-click the Tree Path cell to avoid typing mistakes.",
                    "有一行缺少树文件路径。\n\n如何操作：\n- 为该行填写 Tree Path 列。\n- 使用“添加树文件”按钮或双击 Tree Path 单元格以避免输入错误。");
        }
        if (lowerCaseMessage.contains("does not exist at row")) {
            return UiText.text(
                    "One of the tree file paths does not point to a real file.\n\nWhat to do:\n- Check the path in that row.\n- Double-click the Tree Path cell to choose the file again.\n- If you pasted the path manually, remove extra spaces or quotes and try again.",
                    "某一行的树文件路径指向的文件不存在。\n\n如何操作：\n- 检查该行的路径。\n- 双击 Tree Path 单元格重新选择文件。\n- 如果是手动粘贴的路径，请去除多余的空格或引号后重试。");
        }
        if (lowerCaseMessage.contains("illegal char <") || lowerCaseMessage.contains("invalidpathexception")
                || lowerCaseMessage.contains("malformed input") || lowerCaseMessage.contains("unmappable")) {
            return UiText.text(
                    "The selected path is not valid on this system.\n\nWhat to do:\n- Select the folder or file directly in the dialog.\n- Do not paste a quoted path into the picker.",
                    "所选路径在当前系统上无效。\n\n如何操作：\n- 在对话框里直接选择文件夹或文件。\n- 不要把带引号的路径粘贴到选择器里。");
        }
        if (lowerCaseMessage.contains("directory does not exist")) {
            return UiText.text(
                    "The selected folder cannot be found.\n\nWhat to do:\n- Check whether the folder was moved or renamed.\n- Choose the folder again from the file dialog.",
                    "找不到所选文件夹。\n\n如何操作：\n- 检查文件夹是否被移动或重命名。\n- 从文件对话框重新选择。");
        }

        return message + UiText.text(
                "\n\nWhat to do:\n- Check the selected file or folder again.\n- Prefer using the file dialog instead of manual path typing.\n- Make sure the file is a readable tree file or a valid TSV config.",
                "\n\n如何操作：\n- 再次检查所选文件或文件夹。\n- 建议使用文件对话框而非手动输入路径。\n- 确保文件是可读的树文件或有效的 TSV 配置。");
    }

    private String valueAt(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private static DefaultTableModel createTableModel() {
        return new DefaultTableModel(new Object[] { "Label Name", "Tree Path" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
    }

    private static String defaultLabel(Path path) {
        String fileName = path.getFileName().toString();
        int suffixIndex = fileName.lastIndexOf('.');
        if (suffixIndex > 0) {
            return fileName.substring(0, suffixIndex);
        }
        return fileName;
    }

    record LoadedImportSession(
            String sourceName,
            ImportSourceKind sourceKind,
            List<ImportedTreeSpec> importedTrees,
            List<TreePairSpec> pairSpecs,
            List<String> warnings) {
    }

    @FunctionalInterface
    private interface BackgroundSupplier<T> {
        T get() throws Exception;
    }

    private static final class WelcomeTreeIcon implements Icon {
        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2d.setColor(new Color(45, 118, 214));
            graphics2d.fillRoundRect(x + 2, y + 2, 44, 44, 14, 14);

            graphics2d.setColor(Color.WHITE);
            graphics2d.drawLine(x + 13, y + 24, x + 23, y + 16);
            graphics2d.drawLine(x + 13, y + 24, x + 23, y + 32);
            graphics2d.drawLine(x + 23, y + 16, x + 35, y + 12);
            graphics2d.drawLine(x + 23, y + 16, x + 35, y + 22);
            graphics2d.drawLine(x + 23, y + 32, x + 35, y + 28);
            graphics2d.drawLine(x + 23, y + 32, x + 35, y + 38);

            graphics2d.fillOval(x + 9, y + 20, 8, 8);
            graphics2d.fillOval(x + 19, y + 12, 8, 8);
            graphics2d.fillOval(x + 19, y + 28, 8, 8);
            graphics2d.fillOval(x + 31, y + 8, 8, 8);
            graphics2d.fillOval(x + 31, y + 18, 8, 8);
            graphics2d.fillOval(x + 31, y + 24, 8, 8);
            graphics2d.fillOval(x + 31, y + 34, 8, 8);
            graphics2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return 48;
        }

        @Override
        public int getIconHeight() {
            return 48;
        }
    }
}
