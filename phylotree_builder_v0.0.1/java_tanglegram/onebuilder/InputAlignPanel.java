package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class InputAlignPanel extends JPanel {
    private final JComboBox<InputType> inputTypeCombo;
    private final JTextField inputFileField;
    private final JTextField outputDirField;
    private final JTextField outputPrefixField;
    private final JCheckBox runAlignmentCheckBox;
    private final JCheckBox exportConfigCheckBox;
    private final JComboBox<String> alignStrategyCombo;
    private final JSpinner maxiterateSpinner;
    private final JCheckBox reorderCheckBox;
    private final JLabel alignedPreviewValue;
    private final JButton runButton;
    private final JButton stopButton;
    private final JButton exportButton;
    private final PlatformSupport platformSupport;
    private final Runnable inputChangedCallback;
    private final Consumer<RunRequest> runRequestedCallback;
    private final Consumer<RunRequest> exportRequestedCallback;
    private final Runnable stopRequestedCallback;
    private final Supplier<PipelineRuntimeConfig> runtimeConfigSupplier;
    private boolean running;

    InputAlignPanel(
            PlatformSupport platformSupport,
            Runnable inputChangedCallback,
            Consumer<RunRequest> runRequestedCallback,
            Consumer<RunRequest> exportRequestedCallback,
            Runnable stopRequestedCallback,
            Supplier<PipelineRuntimeConfig> runtimeConfigSupplier) {
        super(new BorderLayout(12, 12));
        this.platformSupport = platformSupport;
        this.inputChangedCallback = inputChangedCallback;
        this.runRequestedCallback = runRequestedCallback;
        this.exportRequestedCallback = exportRequestedCallback;
        this.stopRequestedCallback = stopRequestedCallback;
        this.runtimeConfigSupplier = runtimeConfigSupplier;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextArea intro = new JTextArea(
                buildIntroText(platformSupport));
        intro.setEditable(false);
        intro.setLineWrap(true);
        intro.setWrapStyleWord(true);
        intro.setOpaque(false);
        intro.setBorder(null);
        add(intro, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        inputTypeCombo = new JComboBox<>(InputType.values());
        inputFileField = new JTextField();
        outputDirField = new JTextField();
        outputPrefixField = new JTextField();
        runAlignmentCheckBox = new JCheckBox("Run alignment first", false);
        exportConfigCheckBox = new JCheckBox("Export config file when running", true);
        alignStrategyCombo = new JComboBox<>(new String[] {"localpair", "auto", "globalpair"});
        maxiterateSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 1000000, 100));
        reorderCheckBox = new JCheckBox("Reorder sequences", true);
        alignedPreviewValue = new JLabel("-");
        runButton = new JButton("Run");
        stopButton = new JButton("Stop");
        exportButton = new JButton("Export JSON");
        stopButton.setEnabled(false);

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Input type"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(inputTypeCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Input FASTA/MSA"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(inputFileField, constraints);
        constraints.gridx = 2;
        constraints.weightx = 0.0;
        JButton inputBrowseButton = new JButton("Browse");
        inputBrowseButton.addActionListener(event -> browseForInputFile());
        formPanel.add(inputBrowseButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Output base dir"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(outputDirField, constraints);
        constraints.gridx = 2;
        constraints.weightx = 0.0;
        JButton outputBrowseButton = new JButton("Browse");
        outputBrowseButton.addActionListener(event -> browseForOutputDirectory());
        formPanel.add(outputBrowseButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Output prefix"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(outputPrefixField, constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Alignment"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(runAlignmentCheckBox, constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("MAFFT strategy"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(alignStrategyCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("maxiterate"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(maxiterateSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Alignment flags"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(reorderCheckBox, constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Config export"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(exportConfigCheckBox, constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Expected aligned file"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(alignedPreviewValue, constraints);
        constraints.gridwidth = 1;

        add(formPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        JPanel leftActions = new JPanel();
        leftActions.add(runButton);
        leftActions.add(stopButton);
        leftActions.add(exportButton);
        actions.add(leftActions, BorderLayout.WEST);
        add(actions, BorderLayout.SOUTH);

        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                notifyInputChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                notifyInputChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                notifyInputChanged();
            }
        };
        inputFileField.getDocument().addDocumentListener(documentListener);
        outputDirField.getDocument().addDocumentListener(documentListener);
        outputPrefixField.getDocument().addDocumentListener(documentListener);
        inputTypeCombo.addActionListener(event -> notifyInputChanged());
        runAlignmentCheckBox.addActionListener(event -> notifyInputChanged());
        alignStrategyCombo.addActionListener(event -> notifyInputChanged());
        maxiterateSpinner.addChangeListener(event -> notifyInputChanged());
        reorderCheckBox.addActionListener(event -> notifyInputChanged());
        exportConfigCheckBox.addActionListener(event -> notifyInputChanged());

        runButton.addActionListener(event -> submitRunRequest());
        exportButton.addActionListener(event -> submitExportRequest());
        stopButton.addActionListener(event -> stopRequestedCallback.run());

        if (!platformSupport.supportsPipelineExecution()) {
            runButton.setEnabled(false);
            stopButton.setEnabled(false);
        }
        toggleAlignmentControls();
    }

    InputType selectedInputType() {
        return (InputType) inputTypeCombo.getSelectedItem();
    }

    boolean hasCompleteInput() {
        return !inputFileField.getText().trim().isEmpty() && !outputDirField.getText().trim().isEmpty();
    }

    void setRunning(boolean running) {
        this.running = running;
        boolean pipelineExecution = platformSupport.supportsPipelineExecution();
        runButton.setEnabled(pipelineExecution && !running);
        stopButton.setEnabled(pipelineExecution && running);
        inputTypeCombo.setEnabled(!running);
        inputFileField.setEnabled(!running);
        outputDirField.setEnabled(!running);
        outputPrefixField.setEnabled(!running);
        runAlignmentCheckBox.setEnabled(!running);
        exportConfigCheckBox.setEnabled(!running);
        exportButton.setEnabled(!running);
        alignStrategyCombo.setEnabled(!running && runAlignmentCheckBox.isSelected());
        maxiterateSpinner.setEnabled(!running && runAlignmentCheckBox.isSelected());
        reorderCheckBox.setEnabled(!running && runAlignmentCheckBox.isSelected());
    }

    void setAlignedPreview(Path alignedOutput) {
        alignedPreviewValue.setText(alignedOutput == null ? "-" : alignedOutput.toString());
    }

    private void browseForInputFile() {
        JFileChooser chooser = new JFileChooser();
        if (!inputFileField.getText().trim().isEmpty()) {
            chooser.setSelectedFile(Paths.get(inputFileField.getText().trim()).toFile());
        }
        int selection = chooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            inputFileField.setText(chooser.getSelectedFile().toPath().toString());
            if (outputPrefixField.getText().trim().isEmpty()) {
                outputPrefixField.setText(defaultOutputPrefix(Paths.get(inputFileField.getText().trim())));
            }
        }
    }

    private void browseForOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!outputDirField.getText().trim().isEmpty()) {
            chooser.setSelectedFile(Paths.get(outputDirField.getText().trim()).toFile());
        }
        int selection = chooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    private void submitRunRequest() {
        try {
            runRequestedCallback.accept(buildRunRequest(exportConfigCheckBox.isSelected()));
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "eGPS oneBuilder", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitExportRequest() {
        try {
            exportRequestedCallback.accept(buildRunRequest(true));
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "eGPS oneBuilder", JOptionPane.ERROR_MESSAGE);
        }
    }

    private RunRequest buildRunRequest(boolean exportConfigFile) {
        String inputText = inputFileField.getText().trim();
        String outputDirectoryText = outputDirField.getText().trim();
        if (inputText.isEmpty()) {
            throw new IllegalArgumentException("Input FASTA/MSA is required.");
        }
        if (outputDirectoryText.isEmpty()) {
            throw new IllegalArgumentException("Output base directory is required.");
        }

        Path inputPath = Paths.get(inputText).toAbsolutePath().normalize();
        if (!Files.exists(inputPath) || !Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }

        Path outputDirectory = Paths.get(outputDirectoryText).toAbsolutePath().normalize();
        String prefix = outputPrefixField.getText().trim();
        if (prefix.isEmpty()) {
            prefix = defaultOutputPrefix(inputPath);
            outputPrefixField.setText(prefix);
        }

        RunRequest request = RunRequest.builder()
                .inputType(selectedInputType())
                .inputFile(inputPath)
                .outputDirectory(outputDirectory)
                .outputPrefix(prefix)
                .exportConfigFile(exportConfigFile)
                .runAlignmentFirst(runAlignmentCheckBox.isSelected())
                .alignOptions(new AlignmentOptions(
                        String.valueOf(alignStrategyCombo.getSelectedItem()),
                        ((Integer) maxiterateSpinner.getValue()).intValue(),
                        reorderCheckBox.isSelected()))
                .runtimeConfig(runtimeConfigSupplier.get())
                .build();
        setAlignedPreview(request.runAlignmentFirst()
                ? ExecutionPlanBuilder.alignedOutputPath(request.inputFile())
                : request.inputFile());
        return request;
    }

    private static String defaultOutputPrefix(Path inputPath) {
        String fileName = inputPath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            return fileName.substring(0, extensionIndex) + "_tree";
        }
        return fileName + "_tree";
    }

    private void notifyInputChanged() {
        toggleAlignmentControls();
        Path inputPath = inputFileField.getText().trim().isEmpty()
                ? null
                : Paths.get(inputFileField.getText().trim()).toAbsolutePath().normalize();
        setAlignedPreview(inputPath == null
                ? null
                : (runAlignmentCheckBox.isSelected() ? ExecutionPlanBuilder.alignedOutputPath(inputPath) : inputPath));
        inputChangedCallback.run();
    }

    private void toggleAlignmentControls() {
        boolean alignmentEnabled = runAlignmentCheckBox.isSelected();
        alignStrategyCombo.setEnabled(alignmentEnabled && !running);
        maxiterateSpinner.setEnabled(alignmentEnabled && !running);
        reorderCheckBox.setEnabled(alignmentEnabled && !running);
    }

    boolean isRunSupported() {
        return platformSupport.supportsPipelineExecution();
    }

    boolean isExportSelected() {
        return exportConfigCheckBox.isSelected();
    }

    boolean isExportButtonEnabled() {
        return exportButton.isEnabled();
    }

    private static String buildIntroText(PlatformSupport platformSupport) {
        if (platformSupport.supportsPipelineExecution()) {
            return "Provide one MSA input. On Linux, oneBuilder can run the existing MAFFT wrapper and the four-tree pipeline directly. The same page can also export a reusable JSON config file.";
        }
        return "Provide one MSA input and export a reusable JSON config file. Actual alignment and tree construction are Linux-only. On Windows, use the standalone tanglegram viewer to inspect an existing tree_summary result.";
    }
}
