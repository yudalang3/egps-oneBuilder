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
    private Path lastRunningResultDir;
    private Path lastTsvConfigPath;
    private Path lastTreeFilePath;
    private ImportSourceKind currentSourceKind;
    private String currentSourceName;

    TanglegramWelcomePanel(Consumer<LoadedImportSession> loadConsumer) {
        super(new BorderLayout(0, 12));
        this.loadConsumer = loadConsumer;
        this.tableModel = createTableModel();
        this.table = new JTable(tableModel);
        this.statusLabel = new JLabel("Add at least two trees, or import a running result / TSV config.");
        this.errorLogArea = new JTextArea();
        this.errorLogToggleButton = new JToggleButton();
        this.errorLogPanel = new JPanel(new BorderLayout());
        this.lastRunningResultDir = UiPreferenceStore.loadRecentRunningResultDir();
        this.lastTsvConfigPath = UiPreferenceStore.loadRecentConfigFile();
        this.lastTreeFilePath = UiPreferenceStore.loadRecentTreeFileDir();
        this.currentSourceKind = ImportSourceKind.MANUAL;
        this.currentSourceName = "manual";
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
            setStatus("Loaded startup tree summary: " + treeSummaryDir);
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
        JLabel titleLabel = new JLabel("Import Tree Data");
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            titleLabel.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 4f));
        }
        JLabel subtitleLabel = new JLabel("Load running results, TSV configs, or custom tree files and open a result tab.");
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

        JButton loadRunningResultButton = new JButton("Load Running Result");
        loadRunningResultButton.addActionListener(event -> chooseRunningResult());

        JButton loadTsvButton = new JButton("Load Config File");
        loadTsvButton.setToolTipText("Lines starting with # are comments. Column 1 is label name. Column 2 is the tree path.");
        loadTsvButton.addActionListener(event -> chooseTsvConfig());

        JButton exportTsvButton = new JButton("Export Config File");
        exportTsvButton.addActionListener(event -> exportTsvConfig());

        JButton addTreesButton = new JButton("Add Trees...");
        addTreesButton.addActionListener(event -> addTrees());

        JButton browseSelectedButton = new JButton("Browse Selected...");
        browseSelectedButton.addActionListener(event -> browseSelectedRow());

        JButton removeSelectedButton = new JButton("Remove Selected");
        removeSelectedButton.addActionListener(event -> removeSelectedRows());

        JButton loadButton = new JButton("Load Tree Data");
        loadButton.addActionListener(event -> loadCurrentTable());

        buttonPanel.add(loadRunningResultButton);
        buttonPanel.add(loadTsvButton);
        buttonPanel.add(exportTsvButton);
        buttonPanel.add(addTreesButton);
        buttonPanel.add(browseSelectedButton);
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
        errorLogArea.setText("No problems yet. If loading fails, this area will explain what happened and what to do next.");
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
        updateErrorLogState(false, false, "No problems yet. If loading fails, this area will explain what happened and what to do next.");

        bottomPanel.add(actionRowPanel, BorderLayout.NORTH);
        bottomPanel.add(errorLogPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void chooseRunningResult() {
        JFileChooser fileChooser = new JFileChooser(lastRunningResultDir == null ? null : lastRunningResultDir.toFile());
        fileChooser.setDialogTitle("Select running result folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int selection = fileChooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedDirectory = fileChooser.getSelectedFile().toPath();
        runInBackground("Loading running result", () -> {
            TreeSummaryLoadResult loadResult = TreeSummaryLoader.loadRunResult(selectedDirectory);
            return importedTreesFromLoadResult(loadResult);
        }, importedTrees -> {
            lastRunningResultDir = selectedDirectory.toAbsolutePath().normalize();
            UiPreferenceStore.saveRecentRunningResultDir(lastRunningResultDir);
            currentSourceKind = ImportSourceKind.RUNNING_RESULT;
            Path fileName = lastRunningResultDir.getFileName();
            currentSourceName = fileName == null ? lastRunningResultDir.toString() : fileName.toString();
            replaceRows(importedTrees);
            showSuccess("Loaded running result config from " + lastRunningResultDir + ".");
        });
    }

    private void chooseTsvConfig() {
        JFileChooser fileChooser = new JFileChooser(lastTsvConfigPath == null ? null : lastTsvConfigPath.toFile());
        fileChooser.setDialogTitle("Open config file");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(createConfigFileFilter());
        int selection = fileChooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedFile = fileChooser.getSelectedFile().toPath();
        runInBackground("Loading TSV config", () -> TreeImportConfigIO.readTsv(selectedFile), importedTrees -> {
            lastTsvConfigPath = selectedFile.toAbsolutePath().normalize();
            UiPreferenceStore.saveRecentConfigFile(lastTsvConfigPath);
            currentSourceKind = ImportSourceKind.TSV;
            Path fileName = lastTsvConfigPath.getFileName();
            currentSourceName = fileName == null ? lastTsvConfigPath.toString() : fileName.toString();
            replaceRows(importedTrees);
            showSuccess("Loaded TSV config from " + lastTsvConfigPath + ".");
        });
    }

    private void exportTsvConfig() {
        JFileChooser fileChooser = new JFileChooser(lastTsvConfigPath == null ? null : lastTsvConfigPath.toFile());
        fileChooser.setDialogTitle("Export config file");
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
            showUserError("Could not export the TSV file.", exception.getMessage());
            return;
        }
        runInBackground("Exporting TSV config", () -> {
            TreeImportConfigIO.writeTsv(selectedFile, importedTrees);
            return selectedFile.toAbsolutePath().normalize();
        }, exportedPath -> {
            lastTsvConfigPath = exportedPath;
            UiPreferenceStore.saveRecentConfigFile(lastTsvConfigPath);
            showSuccess("Exported TSV config to " + exportedPath + ".");
        });
    }

    private void addTrees() {
        JFileChooser fileChooser = new JFileChooser(lastTreeFilePath == null ? null : lastTreeFilePath.toFile());
        fileChooser.setDialogTitle("Choose Newick tree files");
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
        showSuccess("Added " + fileChooser.getSelectedFiles().length + " tree file(s).");
    }

    private void browseSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showUserError(
                    "No row is selected.",
                    "Select one row first, then click Browse Selected... or double-click the Tree Path cell you want to fill.");
            return;
        }
        String currentPathText = TreeImportConfigIO.normalizePathText(valueAt(selectedRow, 1));
        Path initialPath = initialTreeChooserPath(currentPathText);
        JFileChooser fileChooser = new JFileChooser(initialPath == null ? null : initialPath.toFile());
        fileChooser.setDialogTitle("Choose tree file for selected row");
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
        showSuccess("Updated row " + (selectedRow + 1) + " with " + selectedPath.getFileName() + ".");
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
            showUserError("Nothing was removed.", "Select one or more rows, then click Remove Selected.");
            return;
        }
        for (int index = selectedRows.length - 1; index >= 0; index--) {
            tableModel.removeRow(selectedRows[index]);
        }
        showSuccess("Removed " + selectedRows.length + " row(s).");
    }

    private void loadCurrentTable() {
        List<ImportedTreeSpec> importedTrees;
        try {
            importedTrees = collectImportedTreesFromTable();
        } catch (Exception exception) {
            showUserError("Tree data could not be loaded.", exception.getMessage());
            return;
        }
        if (importedTrees.size() < 2) {
            showUserError(
                    "Tree data could not be loaded.",
                    "At least two readable tree files are required.\n\nWhat to do:\n- Add another tree file, or\n- Remove incomplete rows, then click Load Tree Data again.");
            return;
        }
        List<TreePairSpec> pairSpecs = buildPairSpecs(importedTrees);
        String sourceName = deriveSourceName(importedTrees);
        ImportSourceKind sourceKind = deriveSourceKind(importedTrees);
        runInBackground("Loading tree data", () -> new LoadedImportSession(
                sourceName,
                sourceKind,
                resolveImportedTrees(importedTrees),
                pairSpecs,
                List.of()), session -> {
            loadConsumer.accept(session);
            showSuccess("Opened a result tab for " + sourceName + " with " + pairSpecs.size() + " pairwise comparisons.");
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
        updateErrorLogState(false, false, "No problems found.\n\nYou can continue with the next step.");
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
        errorLogToggleButton.setText((expanded ? "▼" : "▶") + " Error Log" + indicator);
        if (errorLogPanel.getComponentCount() > 1) {
            errorLogPanel.getComponent(1).setVisible(expanded);
        }
        errorLogPanel.revalidate();
        errorLogPanel.repaint();
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
                return "Config files (*.tsv, *.txt, or files without extension)";
            }
        };
    }

    private String buildUserHelpMessage(String rawMessage) {
        String message = rawMessage == null || rawMessage.isBlank() ? "The selected data could not be used." : rawMessage;
        String lowerCaseMessage = message.toLowerCase();

        if (lowerCaseMessage.contains("missing required directories")) {
            return "The selected folder is not a complete oneBuilder result folder.\n\nWhat to do:\n- Choose the main output folder of oneBuilder.\n- Make sure it contains bayesian_method, distance_method, maximum_likelihood, parsimony_method, and tree_summary.";
        }
        if (lowerCaseMessage.contains("missing required tree files")) {
            return "Some expected tree files are missing from the selected result folder.\n\nWhat to do:\n- Confirm the run finished successfully.\n- Check that NJ, ML, BI, and MP tree files were generated.\n- If only some methods are available, import the existing tree files manually instead.";
        }
        if (lowerCaseMessage.contains("invalid tsv row")) {
            return "The TSV file format is not correct.\n\nWhat to do:\n- Lines starting with # are comments.\n- Column 1 must be the label name.\n- Column 2 must be the tree file path.\n- Save the file as tab-separated text, then import it again.";
        }
        if (lowerCaseMessage.contains("missing a label name")) {
            return "One row is missing a label name.\n\nWhat to do:\n- Fill in the Label Name column for that row.\n- Example labels: NJ, ML, BI, MP, or any custom display name you want to show in the result tabs.";
        }
        if (lowerCaseMessage.contains("missing a tree path")) {
            return "One row is missing a tree path.\n\nWhat to do:\n- Fill in the Tree Path column for that row.\n- You can use Add Trees... or Browse Selected... to avoid typing mistakes.";
        }
        if (lowerCaseMessage.contains("does not exist at row")) {
            return "One of the tree file paths does not point to a real file.\n\nWhat to do:\n- Check the path in that row.\n- Use Browse Selected... or double-click the Tree Path cell to choose the file again.\n- If you pasted the path manually, remove extra spaces or quotes and try again.";
        }
        if (lowerCaseMessage.contains("directory does not exist")) {
            return "The selected folder cannot be found.\n\nWhat to do:\n- Check whether the folder was moved or renamed.\n- Choose the folder again from the file dialog.";
        }

        return message + "\n\nWhat to do:\n- Check the selected file or folder again.\n- Prefer using the file dialog instead of manual path typing.\n- Make sure the file is a readable tree file or a valid TSV config.";
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
