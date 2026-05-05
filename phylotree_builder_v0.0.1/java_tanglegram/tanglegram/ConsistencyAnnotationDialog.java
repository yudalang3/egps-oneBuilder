package tanglegram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

final class ConsistencyAnnotationDialog extends JDialog {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final Consumer<List<ConsistencyAnnotation>> applyCallback;
    private Path lastTsvPath;

    private ConsistencyAnnotationDialog(
            Window owner,
            List<ConsistencyAnnotation> currentAnnotations,
            Consumer<List<ConsistencyAnnotation>> applyCallback) {
        super(owner, UiText.text("Consistency annotation", "一致性注释"), Dialog.ModalityType.DOCUMENT_MODAL);
        this.applyCallback = applyCallback;
        this.tableModel = createTableModel();
        this.table = new JTable(tableModel);
        WindowIconSupport.apply(this);

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(900, 560));
        setMinimumSize(new Dimension(760, 460));

        add(createIntroPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        replaceRows(currentAnnotations == null ? List.of() : currentAnnotations);
        pack();
        setLocationRelativeTo(owner);
    }

    static void showDialog(
            Window owner,
            List<ConsistencyAnnotation> currentAnnotations,
            Consumer<List<ConsistencyAnnotation>> applyCallback) {
        new ConsistencyAnnotationDialog(owner, currentAnnotations, applyCallback).setVisible(true);
    }

    private static DefaultTableModel createTableModel() {
        return new DefaultTableModel(new Object[] { "Clade/Cluster", "Color with alpha", "Ribbon width" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
    }

    private JPanel createIntroPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        JTextArea note = new JTextArea(
                "Each row connects exactly matching clades or clusters across the ordered trees. "
                        + "Use comma-separated leaf names, #RRGGBBAA colors, and a ribbon width, for example: Dog,Cow,Frog    #FFA234A2    5");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setForeground(new Color(80, 91, 107));
        panel.add(note, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createTablePanel() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TableColumn cladeColumn = table.getColumnModel().getColumn(0);
        cladeColumn.setMinWidth(420);
        cladeColumn.setPreferredWidth(560);
        TableColumn colorColumn = table.getColumnModel().getColumn(1);
        colorColumn.setMinWidth(180);
        colorColumn.setPreferredWidth(220);
        TableColumn widthColumn = table.getColumnModel().getColumn(2);
        widthColumn.setMinWidth(110);
        widthColumn.setPreferredWidth(130);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 12, 0, 12),
                BorderFactory.createLineBorder(new Color(226, 232, 241))));
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton importButton = new JButton("Import TSV");
        JButton exportButton = new JButton("Export TSV");
        JButton addButton = new JButton("Add row");
        JButton removeButton = new JButton("Remove selected");
        importButton.addActionListener(event -> importTsv());
        exportButton.addActionListener(event -> exportTsv());
        addButton.addActionListener(event -> tableModel.addRow(new Object[] { "", "#4F8CFFA0", "5" }));
        removeButton.addActionListener(event -> removeSelectedRows());
        editPanel.add(importButton);
        editPanel.add(exportButton);
        editPanel.add(addButton);
        editPanel.add(removeButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton applyButton = new JButton(UiText.text("Apply", "应用"));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton(UiText.text("Cancel", "取消"));
        applyButton.addActionListener(event -> applyValues());
        okButton.addActionListener(event -> {
            if (applyValues()) {
                dispose();
            }
        });
        cancelButton.addActionListener(event -> dispose());
        actionPanel.add(applyButton);
        actionPanel.add(okButton);
        actionPanel.add(cancelButton);

        outerPanel.add(editPanel, BorderLayout.WEST);
        outerPanel.add(actionPanel, BorderLayout.EAST);
        return outerPanel;
    }

    private void importTsv() {
        JFileChooser fileChooser = new JFileChooser(lastTsvPath == null ? null : lastTsvPath.toFile());
        fileChooser.setDialogTitle("Import consistency annotation TSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("TSV files", "tsv", "txt"));
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedPath = fileChooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        try {
            List<ConsistencyAnnotation> annotations = ConsistencyAnnotationIO.readTsv(selectedPath);
            replaceRows(annotations);
            lastTsvPath = selectedPath;
        } catch (IOException exception) {
            showError("Could not import the annotation TSV.", exception.getMessage());
        }
    }

    private void exportTsv() {
        List<ConsistencyAnnotation> annotations;
        try {
            annotations = annotationsFromTable();
        } catch (IOException exception) {
            showError("Could not export the annotation TSV.", exception.getMessage());
            return;
        }
        JFileChooser fileChooser = new JFileChooser(lastTsvPath == null ? null : lastTsvPath.toFile());
        fileChooser.setDialogTitle("Export consistency annotation TSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("TSV files", "tsv", "txt"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selectedPath = fileChooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        try {
            ConsistencyAnnotationIO.writeTsv(selectedPath, annotations);
            lastTsvPath = selectedPath;
        } catch (IOException exception) {
            showError("Could not export the annotation TSV.", exception.getMessage());
        }
    }

    private boolean applyValues() {
        try {
            applyCallback.accept(annotationsFromTable());
            return true;
        } catch (IOException exception) {
            showError("Could not apply consistency annotations.", exception.getMessage());
            return false;
        }
    }

    private void removeSelectedRows() {
        int[] selectedRows = table.getSelectedRows();
        for (int index = selectedRows.length - 1; index >= 0; index--) {
            tableModel.removeRow(table.convertRowIndexToModel(selectedRows[index]));
        }
    }

    private void replaceRows(List<ConsistencyAnnotation> annotations) {
        tableModel.setRowCount(0);
        for (ConsistencyAnnotation annotation : annotations) {
            tableModel.addRow(new Object[] { annotation.leafNamesText(), annotation.colorText(), annotation.ribbonWidthText() });
        }
        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[] { "", "#4F8CFFA0", "5" });
        }
    }

    private List<ConsistencyAnnotation> annotationsFromTable() throws IOException {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        List<String[]> rows = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            rows.add(new String[] { valueAt(row, 0), valueAt(row, 1), valueAt(row, 2) });
        }
        return ConsistencyAnnotationIO.parseTableRows(rows);
    }

    private String valueAt(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message == null ? title : message, title, JOptionPane.ERROR_MESSAGE);
    }
}
