package onebuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import tanglegram.UiPreferenceStore;

final class InputAlignPanel extends JPanel {
    private static final int INPUT_TYPE_DETECTION_CHAR_LIMIT = 20000;
    private final JComboBox<InputType> inputTypeCombo;
    private final JTextField inputFileField;
    private final JTextField outputDirField;
    private final JTextField outputPrefixField;
    private final JCheckBox runAlignmentCheckBox;
    private final JCheckBox exportConfigCheckBox;
    private final JComboBox<String> alignStrategyCombo;
    private final JSpinner maxiterateSpinner;
    private final JSpinner alignThreadsSpinner;
    private final JCheckBox reorderCheckBox;
    private final JTextArea alignExtraArgsArea;
    private final JTextArea alignmentGuidanceArea;
    private final JLabel inputTypeHintLabel;
    private final JLabel alignedPreviewValue;
    private final JButton reuseLastButton;
    private final PlatformSupport platformSupport;
    private final Runnable inputChangedCallback;
    private final Supplier<PipelineRuntimeConfig> runtimeConfigSupplier;
    private Path lastInputBrowseDir;
    private Path lastOutputBrowseDir;
    private boolean running;
    private boolean alignmentSelectionAutoControlled = true;
    private long inputTypeDetectionToken;
    private Path lastScheduledInputTypeDetectionPath;

    InputAlignPanel(
            PlatformSupport platformSupport,
            Runnable inputChangedCallback,
            Supplier<PipelineRuntimeConfig> runtimeConfigSupplier) {
        super(new BorderLayout(12, 12));
        this.platformSupport = platformSupport;
        this.inputChangedCallback = inputChangedCallback;
        this.runtimeConfigSupplier = runtimeConfigSupplier;
        this.lastInputBrowseDir = UiPreferenceStore.loadRecentOneBuilderInputDir();
        this.lastOutputBrowseDir = UiPreferenceStore.loadRecentOneBuilderOutputDir();
        WorkbenchStyles.applyCanvas(this);

        JPanel introCard = WorkbenchStyles.createSurfacePanel(new BorderLayout());
        introCard.add(WorkbenchStyles.createNoteArea(buildIntroText(platformSupport)), BorderLayout.CENTER);
        add(introCard, BorderLayout.NORTH);

        JPanel formPanel = WorkbenchStyles.createSurfacePanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        inputTypeCombo = new JComboBox<>(InputType.values());
        reuseLastButton = new JButton("Reuse Last");
        inputFileField = new JTextField();
        outputDirField = new JTextField();
        outputPrefixField = new JTextField();
        runAlignmentCheckBox = new JCheckBox("Run multiple sequence alignment first", false);
        runAlignmentCheckBox.setToolTipText(
                "Enable this when your input file contains raw sequences that still need MAFFT alignment before tree building. Leave it off when the file is already an aligned MSA.");
        alignmentGuidanceArea = WorkbenchStyles.createNoteArea(
                "Turn this on when the input FASTA contains raw sequences and is not aligned yet. oneBuilder auto-enables it when multiple sequences have different lengths.");
        exportConfigCheckBox = new JCheckBox("Export config file when running", true);
        alignStrategyCombo = new JComboBox<>(new String[] {"localpair", "genafpair", "auto", "globalpair"});
        maxiterateSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 1000000, 100));
        alignThreadsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 512, 1));
        reorderCheckBox = new JCheckBox("Reorder sequences", true);
        alignExtraArgsArea = new JTextArea(5, 28);
        alignExtraArgsArea.setEditable(true);
        alignExtraArgsArea.setBackground(WorkbenchStyles.SURFACE_BACKGROUND);
        alignExtraArgsArea.setForeground(WorkbenchStyles.TEXT_PRIMARY);
        alignExtraArgsArea.setLineWrap(false);
        inputTypeHintLabel = WorkbenchStyles.createSubtitleLabel("Auto-detected: choose an input FASTA/MSA file first.");
        alignedPreviewValue = new JLabel("-");

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Input type"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(inputTypeCombo, constraints);
        constraints.gridx = 2;
        constraints.weightx = 0.0;
        reuseLastButton.setToolTipText(
                "Reuse the most recent Input / Align setup saved by oneBuilder. This restores the last input FASTA/MSA file, output base directory, output prefix, and input type so repeated testing can start from the same paths without typing them again. After restoring, you can still use Browse or edit any field normally before running or exporting.");
        reuseLastButton.addActionListener(event -> reuseLastInputs());
        formPanel.add(reuseLastButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel(""), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(inputTypeHintLabel, constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 2;
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
        constraints.gridy = 3;
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
        constraints.gridy = 4;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Output prefix"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(outputPrefixField, constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Alignment"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(buildAlignmentSelectionPanel(), constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("MAFFT strategy"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(alignStrategyCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Maxiterate"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(maxiterateSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("MAFFT threads"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(alignThreadsSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Alignment flags"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(reorderCheckBox, constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Advanced MAFFT"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(buildAdvancedAlignmentPanel(), constraints);
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 11;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Config export"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(buildConfigExportPanel(), constraints);
        constraints.gridwidth = 1;

        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Expected aligned file"), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(alignedPreviewValue, constraints);
        constraints.gridwidth = 1;

        add(formPanel, BorderLayout.CENTER);

        JPanel nextStepCard = WorkbenchStyles.createSurfacePanel(new BorderLayout());
        nextStepCard.add(
                WorkbenchStyles.createNoteArea(
                        "Next step: open Tree Parameters to adjust the method tree, then use Tree Build to export the full config or run the pipeline."),
                BorderLayout.CENTER);
        add(nextStepCard, BorderLayout.SOUTH);

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
        DocumentListener inputFileDocumentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                handleInputFileChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                handleInputFileChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                handleInputFileChanged();
            }
        };
        inputFileField.getDocument().addDocumentListener(inputFileDocumentListener);
        outputDirField.getDocument().addDocumentListener(documentListener);
        outputPrefixField.getDocument().addDocumentListener(documentListener);
        inputTypeCombo.addActionListener(event -> notifyInputChanged());
        runAlignmentCheckBox.addActionListener(event -> {
            alignmentSelectionAutoControlled = false;
            notifyInputChanged();
        });
        alignStrategyCombo.addActionListener(event -> notifyInputChanged());
        maxiterateSpinner.addChangeListener(event -> notifyInputChanged());
        alignThreadsSpinner.addChangeListener(event -> notifyInputChanged());
        reorderCheckBox.addActionListener(event -> notifyInputChanged());
        alignExtraArgsArea.getDocument().addDocumentListener(documentListener);
        exportConfigCheckBox.addActionListener(event -> notifyInputChanged());
        toggleAlignmentControls();
        refreshReuseLastButtonState();
        WorkbenchStyles.applyPanelTreeBackground(this);
    }

    InputType selectedInputType() {
        return (InputType) inputTypeCombo.getSelectedItem();
    }

    boolean hasCompleteInput() {
        return !inputFileField.getText().trim().isEmpty() && !outputDirField.getText().trim().isEmpty();
    }

    String navigationBlockingMessage() {
        String inputText = inputFileField.getText().trim();
        String outputDirText = outputDirField.getText().trim();
        boolean missingInput = inputText.isEmpty();
        boolean missingOutput = outputDirText.isEmpty();
        if (missingInput && missingOutput) {
            return "Finish the required fields in Input / Align first: choose an input FASTA/MSA file and an output base directory.";
        }
        if (missingInput) {
            return "Finish the required fields in Input / Align first: choose an input FASTA/MSA file.";
        }
        if (missingOutput) {
            return "Finish the required fields in Input / Align first: choose an output base directory.";
        }
        try {
            Path inputPath = Paths.get(inputText).toAbsolutePath().normalize();
            if (!Files.exists(inputPath) || !Files.isRegularFile(inputPath)) {
                return "Finish the required fields in Input / Align first: choose an existing input FASTA/MSA file.";
            }
        } catch (InvalidPathException exception) {
            return "Finish the required fields in Input / Align first: choose a valid input FASTA/MSA path.";
        }
        if (containsInvalidPathControlCharacters(outputDirText)) {
            return "Finish the required fields in Input / Align first: choose a valid output base directory path.";
        }
        try {
            Paths.get(outputDirText).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            return "Finish the required fields in Input / Align first: choose a valid output base directory path.";
        }
        return null;
    }

    RunRequest buildRunRequestForExecution() {
        return buildRunRequest(exportConfigCheckBox.isSelected());
    }

    RunRequest buildRunRequestForExport() {
        return buildRunRequest(true);
    }

    String buildRunDraftSummary(PipelineRuntimeConfig runtimeConfig) {
        StringBuilder builder = new StringBuilder();
        builder.append("Input type: ").append(selectedInputType().displayName()).append(System.lineSeparator());
        builder.append("Input FASTA/MSA: ").append(textOrDash(inputFileField.getText())).append(System.lineSeparator());
        builder.append("Output base dir: ").append(textOrDash(outputDirField.getText())).append(System.lineSeparator());

        String prefix = outputPrefixField.getText().trim();
        if (prefix.isEmpty()) {
            try {
                Path inputPath = resolvedInputPathOrNull();
                prefix = inputPath == null ? "-" : defaultOutputPrefix(inputPath);
            } catch (InvalidPathException exception) {
                prefix = "-";
            }
        }
        builder.append("Output prefix: ").append(textOrDash(prefix)).append(System.lineSeparator());
        builder.append("Run multiple sequence alignment before tree building: ")
                .append(runAlignmentCheckBox.isSelected() ? "Yes" : "No, use the input file as an existing alignment"
                )
                .append(System.lineSeparator());
        builder.append("MAFFT threads: ").append(integerTextOrAuto((Integer) alignThreadsSpinner.getValue())).append(System.lineSeparator());
        builder.append("Expected aligned file: ").append(textOrDash(alignedPreviewValue.getText())).append(System.lineSeparator());
        builder.append("Keep config file when running: ").append(exportConfigCheckBox.isSelected() ? "Yes" : "No").append(System.lineSeparator());
        builder.append(System.lineSeparator()).append("Method tree").append(System.lineSeparator());
        builder.append("- Distance Method: ").append(runtimeConfig.distance().enabled() ? "Enabled" : "Disabled").append(System.lineSeparator());
        builder.append("- Maximum Likelihood: ").append(runtimeConfig.maximumLikelihood().enabled() ? "Enabled" : "Disabled").append(System.lineSeparator());
        builder.append("- Bayes Method: ").append(runtimeConfig.bayesian().enabled() ? "Enabled" : "Disabled").append(System.lineSeparator());
        builder.append("- Maximum Parsimony: ").append(runtimeConfig.parsimony().enabled() ? "Enabled" : "Disabled").append(System.lineSeparator());
        builder.append("- Protein Structure: ")
                .append(selectedInputType() == InputType.PROTEIN
                        ? (runtimeConfig.proteinStructure().enabled() ? "Foldseek enabled" : "Disabled")
                        : "Protein only")
                .append(System.lineSeparator());
        if (selectedInputType() == InputType.PROTEIN && runtimeConfig.proteinStructure().enabled()) {
            builder.append("  Protein structure TSV: ")
                    .append(runtimeConfig.proteinStructure().useStructureManifest()
                            ? textOrDash(runtimeConfig.proteinStructure().structureManifestFile())
                            : "Not used; FASTA-only ProstT5/3Di mode")
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    void setRunning(boolean running) {
        this.running = running;
        inputTypeCombo.setEnabled(!running);
        reuseLastButton.setEnabled(!running && hasReusableLastInputs());
        inputFileField.setEnabled(!running);
        outputDirField.setEnabled(!running);
        outputPrefixField.setEnabled(!running);
        runAlignmentCheckBox.setEnabled(!running);
        exportConfigCheckBox.setEnabled(!running);
        alignStrategyCombo.setEnabled(!running && runAlignmentCheckBox.isSelected());
        maxiterateSpinner.setEnabled(!running && runAlignmentCheckBox.isSelected());
        alignThreadsSpinner.setEnabled(!running && runAlignmentCheckBox.isSelected());
        reorderCheckBox.setEnabled(!running && runAlignmentCheckBox.isSelected());
        alignExtraArgsArea.setEnabled(!running);
        alignExtraArgsArea.setEditable(!running);
    }

    void setAlignedPreview(Path alignedOutput) {
        alignedPreviewValue.setText(alignedOutput == null ? "-" : alignedOutput.toString());
    }

    private void browseForInputFile() {
        Path initialPath = initialInputChooserPath();
        JFileChooser chooser = new JFileChooser(initialPath == null ? null : initialPath.toFile());
        int selection = chooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            Path selectedInput = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            inputFileField.setText(selectedInput.toString());
            lastInputBrowseDir = selectedInput.getParent();
            UiPreferenceStore.saveRecentOneBuilderInputDir(lastInputBrowseDir);
            UiPreferenceStore.saveLastOneBuilderInputFile(selectedInput);
            UiPreferenceStore.saveLastOneBuilderInputType(selectedInputType().name());
            if (outputPrefixField.getText().trim().isEmpty()) {
                outputPrefixField.setText(defaultOutputPrefix(Paths.get(inputFileField.getText().trim())));
            }
            saveLastUsedInputSettings();
        }
    }

    private void browseForOutputDirectory() {
        Path initialPath = initialOutputChooserPath();
        JFileChooser chooser = new JFileChooser(initialPath == null ? null : initialPath.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int selection = chooser.showOpenDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            Path selectedOutputDir = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            outputDirField.setText(selectedOutputDir.toString());
            lastOutputBrowseDir = selectedOutputDir;
            UiPreferenceStore.saveRecentOneBuilderOutputDir(lastOutputBrowseDir);
            UiPreferenceStore.saveLastOneBuilderOutputDir(selectedOutputDir);
            saveLastUsedInputSettings();
        }
    }

    Path initialInputChooserPathForTest() {
        return initialInputChooserPath();
    }

    void setInputFilePathForTest(String value) {
        inputFileField.setText(value);
    }

    boolean isRunAlignmentSelectedForTest() {
        return runAlignmentCheckBox.isSelected();
    }

    Path initialOutputChooserPathForTest() {
        return initialOutputChooserPath();
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

        Path inputPath;
        try {
            inputPath = Paths.get(inputText).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Input FASTA/MSA path is invalid: " + inputText, exception);
        }
        if (!Files.exists(inputPath) || !Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }

        if (containsInvalidPathControlCharacters(outputDirectoryText)) {
            throw new IllegalArgumentException("Output base directory path is invalid: " + outputDirectoryText);
        }
        Path outputDirectory;
        try {
            outputDirectory = Paths.get(outputDirectoryText).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Output base directory path is invalid: " + outputDirectoryText, exception);
        }
        String prefix = outputPrefixField.getText().trim();
        if (prefix.isEmpty()) {
            prefix = defaultOutputPrefix(inputPath);
            outputPrefixField.setText(prefix);
        }

        PipelineRuntimeConfig runtimeConfig = runtimeConfigSupplier.get();
        validateProteinStructureConfig(runtimeConfig);

        RunRequest request = RunRequest.builder()
                .inputType(selectedInputType())
                .inputFile(inputPath)
                .outputDirectory(outputDirectory)
                .outputPrefix(prefix)
                .language(UiPreferenceStore.load().uiLanguage())
                .exportConfigFile(exportConfigFile)
                .runAlignmentFirst(runAlignmentCheckBox.isSelected())
                .alignOptions(new AlignmentOptions(
                        String.valueOf(alignStrategyCombo.getSelectedItem()),
                        ((Integer) maxiterateSpinner.getValue()).intValue(),
                        integerOrNull((Integer) alignThreadsSpinner.getValue()),
                        reorderCheckBox.isSelected(),
                        TextListCodec.splitLines(alignExtraArgsArea.getText())))
                .runtimeConfig(runtimeConfig)
                .build();
        saveLastUsedInputSettings();
        setAlignedPreview(request.runAlignmentFirst()
                ? ExecutionPlanBuilder.alignedOutputPath(request.inputFile())
                : request.inputFile());
        return request;
    }

    private void validateProteinStructureConfig(PipelineRuntimeConfig runtimeConfig) {
        if (selectedInputType() != InputType.PROTEIN || runtimeConfig == null || !runtimeConfig.proteinStructure().enabled()) {
            return;
        }
        ProteinStructureConfig proteinStructure = runtimeConfig.proteinStructure();
        if (!proteinStructure.useStructureManifest()) {
            return;
        }
        String manifestText = proteinStructure.structureManifestFile();
        if (manifestText == null || manifestText.isBlank()) {
            throw new IllegalArgumentException("Protein structure TSV is required when structure mapping is enabled.");
        }
        Path manifestPath;
        try {
            manifestPath = Paths.get(manifestText).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Protein structure TSV path is invalid: " + manifestText, exception);
        }
        if (!Files.exists(manifestPath) || !Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException("Protein structure TSV does not exist: " + manifestPath);
        }
    }

    private void reuseLastInputs() {
        StoredInputSelection lastSelection = loadLastInputSelection();
        if (lastSelection == null) {
            Toolkit.getDefaultToolkit().beep();
            refreshReuseLastButtonState();
            return;
        }
        if (lastSelection.inputType != null && inputTypeCombo.isEnabled()) {
            inputTypeCombo.setSelectedItem(lastSelection.inputType);
        }
        if (lastSelection.inputFile != null) {
            inputFileField.setText(lastSelection.inputFile.toString());
            lastInputBrowseDir = lastSelection.inputFile.getParent();
            UiPreferenceStore.saveRecentOneBuilderInputDir(lastInputBrowseDir);
        }
        if (lastSelection.outputDir != null) {
            outputDirField.setText(lastSelection.outputDir.toString());
            lastOutputBrowseDir = lastSelection.outputDir;
            UiPreferenceStore.saveRecentOneBuilderOutputDir(lastOutputBrowseDir);
        }
        outputPrefixField.setText(lastSelection.outputPrefix == null ? "" : lastSelection.outputPrefix);
        saveLastUsedInputSettings();
    }

    static InputType detectInputTypeFromFasta(Path inputPath) {
        return detectSequenceContentFromFasta(inputPath).inputType();
    }

    private static SequenceContentDetection detectSequenceContentFromFasta(Path inputPath) {
        int informativeResidues = 0;
        int sequenceCount = 0;
        int currentSequenceLength = 0;
        int firstSequenceLength = -1;
        boolean sequenceLengthsDiffer = false;
        boolean insideSequence = false;
        boolean proteinResidueSeen = false;
        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null && informativeResidues < INPUT_TYPE_DETECTION_CHAR_LIMIT) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.charAt(0) == '>') {
                    if (insideSequence) {
                        sequenceLengthsDiffer = updateSequenceLengthDifference(
                                firstSequenceLength,
                                currentSequenceLength,
                                sequenceLengthsDiffer);
                        if (firstSequenceLength < 0) {
                            firstSequenceLength = currentSequenceLength;
                        }
                    }
                    insideSequence = true;
                    sequenceCount++;
                    currentSequenceLength = 0;
                    continue;
                }
                insideSequence = true;
                for (int index = 0; index < trimmed.length() && informativeResidues < INPUT_TYPE_DETECTION_CHAR_LIMIT; index++) {
                    char residue = Character.toUpperCase(trimmed.charAt(index));
                    if (Character.isWhitespace(residue)) {
                        continue;
                    }
                    currentSequenceLength++;
                    if (residue == '-' || residue == '.' || residue == '*' || residue == '?') {
                        continue;
                    }
                    if (isDnaResidue(residue)) {
                        informativeResidues++;
                        continue;
                    }
                    if (Character.isLetter(residue)) {
                        proteinResidueSeen = true;
                    }
                }
            }
            if (insideSequence) {
                sequenceLengthsDiffer = updateSequenceLengthDifference(
                        firstSequenceLength,
                        currentSequenceLength,
                        sequenceLengthsDiffer);
            }
        } catch (IOException exception) {
            return new SequenceContentDetection(null, false);
        }
        return new SequenceContentDetection(
                proteinResidueSeen ? InputType.PROTEIN : (informativeResidues > 0 ? InputType.DNA_CDS : null),
                sequenceCount > 1 && sequenceLengthsDiffer);
    }

    void autoDetectInputTypeForCurrentFileForTest() {
        Path inputPath = resolvedInputPathOrNull();
        if (inputPath == null || !Files.isRegularFile(inputPath)) {
            return;
        }
        applyAutoDetectedSequenceContent(inputPath, selectedInputType(), detectSequenceContentFromFasta(inputPath));
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
        Path inputPath = resolvedInputPathOrNull();
        setAlignedPreview(inputPath == null
                ? null
                : (runAlignmentCheckBox.isSelected() ? ExecutionPlanBuilder.alignedOutputPath(inputPath) : inputPath));
        inputChangedCallback.run();
    }

    private void handleInputFileChanged() {
        scheduleInputTypeDetection();
        notifyInputChanged();
    }

    private void scheduleInputTypeDetection() {
        Path inputPath = resolvedInputPathOrNull();
        if (inputPath == null || !Files.isRegularFile(inputPath)) {
            lastScheduledInputTypeDetectionPath = null;
            inputTypeHintLabel.setText("Auto-detected: choose an input FASTA/MSA file first.");
            return;
        }
        if (inputPath.equals(lastScheduledInputTypeDetectionPath)) {
            return;
        }
        lastScheduledInputTypeDetectionPath = inputPath;
        alignmentSelectionAutoControlled = true;
        inputTypeHintLabel.setText("Auto-detected: checking sequence content...");
        long detectionToken = ++inputTypeDetectionToken;
        InputType selectedAtSchedule = selectedInputType();
        Thread detectionThread = new Thread(() -> {
            SequenceContentDetection detection = detectSequenceContentFromFasta(inputPath);
            SwingUtilities.invokeLater(() -> {
                if (detectionToken != inputTypeDetectionToken) {
                    return;
                }
                if (!inputPath.equals(resolvedInputPathOrNull())) {
                    return;
                }
                applyAutoDetectedSequenceContent(inputPath, selectedAtSchedule, detection);
            });
        }, "onebuilder-input-type-detector");
        detectionThread.setDaemon(true);
        detectionThread.start();
    }

    private void applyAutoDetectedSequenceContent(Path inputPath, InputType selectedAtSchedule, SequenceContentDetection detection) {
        if (!inputPath.equals(resolvedInputPathOrNull())) {
            return;
        }
        InputType detectedInputType = detection.inputType();
        if (detectedInputType == null) {
            inputTypeHintLabel.setText("Auto-detected: could not determine input type, keep manual selection.");
            return;
        }
        if (selectedInputType() != selectedAtSchedule) {
            inputTypeHintLabel.setText("Auto-detected: " + detectedInputType.displayName() + " (manual selection kept).");
            return;
        }
        if (inputTypeCombo.isEnabled() && detectedInputType != selectedAtSchedule) {
            inputTypeCombo.setSelectedItem(detectedInputType);
        }
        if (alignmentSelectionAutoControlled) {
            runAlignmentCheckBox.setSelected(detection.needsAlignment());
            notifyInputChanged();
        }
        inputTypeHintLabel.setText("Auto-detected: " + detectedInputType.displayName()
                + (detection.needsAlignment() ? "; raw sequence lengths differ, alignment enabled." : "; alignment status not forced."));
    }

    private static boolean updateSequenceLengthDifference(
            int firstSequenceLength,
            int currentSequenceLength,
            boolean sequenceLengthsDiffer) {
        return sequenceLengthsDiffer || (firstSequenceLength >= 0 && currentSequenceLength != firstSequenceLength);
    }

    private Path resolvedInputPathOrNull() {
        String inputText = inputFileField.getText().trim();
        if (inputText.isEmpty()) {
            return null;
        }
        try {
            return Paths.get(inputText).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private static boolean containsInvalidPathControlCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            if (Character.isISOControl(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private Path initialInputChooserPath() {
        String inputText = inputFileField.getText().trim();
        if (!inputText.isEmpty()) {
            try {
                Path currentPath = Paths.get(inputText).toAbsolutePath().normalize();
                if (Files.isRegularFile(currentPath)) {
                    return currentPath.getParent();
                }
                if (Files.isDirectory(currentPath)) {
                    return currentPath;
                }
            } catch (Exception ignored) {
            }
        }
        return lastInputBrowseDir;
    }

    private Path initialOutputChooserPath() {
        String outputDirText = outputDirField.getText().trim();
        if (!outputDirText.isEmpty()) {
            try {
                Path currentPath = Paths.get(outputDirText).toAbsolutePath().normalize();
                if (Files.isDirectory(currentPath)) {
                    return currentPath;
                }
            } catch (Exception ignored) {
            }
        }
        return lastOutputBrowseDir;
    }

    private void toggleAlignmentControls() {
        boolean alignmentEnabled = runAlignmentCheckBox.isSelected();
        alignStrategyCombo.setEnabled(alignmentEnabled && !running);
        maxiterateSpinner.setEnabled(alignmentEnabled && !running);
        alignThreadsSpinner.setEnabled(alignmentEnabled && !running);
        reorderCheckBox.setEnabled(alignmentEnabled && !running);
        alignExtraArgsArea.setEnabled(!running);
        alignExtraArgsArea.setEditable(!running);
    }

    private void refreshReuseLastButtonState() {
        reuseLastButton.setEnabled(!running && hasReusableLastInputs());
        if (reuseLastButton.isEnabled()) {
            reuseLastButton.setToolTipText(
                    "Reuse the most recent Input / Align setup saved by oneBuilder. This restores the last input FASTA/MSA file, output base directory, output prefix, and input type so repeated testing can start from the same paths without typing them again. After restoring, you can still use Browse or edit any field normally before running or exporting.");
            return;
        }
        reuseLastButton.setToolTipText(
                "Reuse the most recent Input / Align setup saved by oneBuilder. No previous input file and output directory pair has been saved yet, so there is nothing to restore right now.");
    }

    private boolean hasReusableLastInputs() {
        StoredInputSelection lastSelection = loadLastInputSelection();
        return lastSelection != null;
    }

    private StoredInputSelection loadLastInputSelection() {
        Path inputFile = UiPreferenceStore.loadLastOneBuilderInputFile();
        Path outputDir = UiPreferenceStore.loadLastOneBuilderOutputDir();
        if (inputFile == null || outputDir == null) {
            return null;
        }
        String storedInputType = UiPreferenceStore.loadLastOneBuilderInputType();
        InputType inputType = null;
        if (storedInputType != null) {
            try {
                inputType = InputType.valueOf(storedInputType);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new StoredInputSelection(
                inputType,
                inputFile,
                outputDir,
                UiPreferenceStore.loadLastOneBuilderOutputPrefix());
    }

    private void saveLastUsedInputSettings() {
        Path inputPath = resolvedInputPathOrNull();
        if (inputPath != null) {
            UiPreferenceStore.saveLastOneBuilderInputFile(inputPath);
            UiPreferenceStore.saveLastOneBuilderInputType(selectedInputType().name());
        }
        String outputDirText = outputDirField.getText().trim();
        if (!outputDirText.isEmpty() && !containsInvalidPathControlCharacters(outputDirText)) {
            try {
                UiPreferenceStore.saveLastOneBuilderOutputDir(Paths.get(outputDirText).toAbsolutePath().normalize());
            } catch (InvalidPathException ignored) {
            }
        }
        UiPreferenceStore.saveLastOneBuilderOutputPrefix(outputPrefixField.getText().trim());
        refreshReuseLastButtonState();
    }

    private static final class StoredInputSelection {
        private final InputType inputType;
        private final Path inputFile;
        private final Path outputDir;
        private final String outputPrefix;

        private StoredInputSelection(InputType inputType, Path inputFile, Path outputDir, String outputPrefix) {
            this.inputType = inputType;
            this.inputFile = inputFile;
            this.outputDir = outputDir;
            this.outputPrefix = outputPrefix;
        }
    }

    boolean isRunSupported() {
        return platformSupport.supportsPipelineExecution();
    }

    boolean isExportSelected() {
        return exportConfigCheckBox.isSelected();
    }

    private static String buildIntroText(PlatformSupport platformSupport) {
        if (platformSupport.supportsPipelineExecution()) {
            return "Provide one FASTA/MSA input. On Linux, oneBuilder can run the existing MAFFT wrapper and the four-tree pipeline directly. After this page, use Tree Parameters to tune methods and Tree Build to run or export the config.";
        }
        return "Provide one FASTA/MSA input. Actual alignment and tree construction are Linux-only. On Windows, continue through Tree Parameters and use Tree Build to export a reusable config file.";
    }

    private static String textOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private static String integerTextOrAuto(Integer value) {
        return value == null || value.intValue() <= 0 ? "Auto/default" : Integer.toString(value.intValue());
    }

    private static Integer integerOrNull(Integer value) {
        return value == null || value.intValue() <= 0 ? null : Integer.valueOf(value.intValue());
    }

    private static boolean isDnaResidue(char residue) {
        switch (residue) {
            case 'A':
            case 'C':
            case 'G':
            case 'T':
            case 'U':
            case 'R':
            case 'Y':
            case 'S':
            case 'W':
            case 'K':
            case 'M':
            case 'B':
            case 'D':
            case 'H':
            case 'V':
            case 'N':
                return true;
            default:
                return false;
        }
    }

    private JPanel buildAdvancedAlignmentPanel() {
        JTextArea note = WorkbenchStyles.createNoteArea(
                "Advanced MAFFT flags. Use this box only when you need extra MAFFT options beyond the common controls above. The common controls already cover the usual localpair, genafpair, auto, globalpair, and thread-count setup. Enter one token per line here only for less common MAFFT flags. These tokens are appended after the standard MAFFT arguments when the alignment step is enabled. If your input file is already an aligned MSA and the alignment checkbox stays off, these extra MAFFT flags will not be used.");
        JScrollPane extraArgsScrollPane = new JScrollPane(alignExtraArgsArea);
        extraArgsScrollPane.setBorder(BorderFactory.createLineBorder(WorkbenchStyles.ACCENT_BORDER, 1, true));
        extraArgsScrollPane.getViewport().setBackground(WorkbenchStyles.SURFACE_BACKGROUND);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.add(extraArgsScrollPane, BorderLayout.CENTER);
        content.add(note, BorderLayout.SOUTH);
        return TaskPaneFactory.createBlueTaskPane("Advanced Parameters", content, true);
    }

    private JPanel buildAlignmentSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        panel.add(runAlignmentCheckBox, BorderLayout.NORTH);
        panel.add(alignmentGuidanceArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildConfigExportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(exportConfigCheckBox, BorderLayout.NORTH);
        panel.add(
                WorkbenchStyles.createNoteArea(
                        "When this option is enabled, clicking Run will also write a reusable JSON config file for the current job. That file records the selected input, alignment setup, output prefix, and all tree-method parameters so the same workflow can be rerun later from the command line on Linux, shared with collaborators, or kept as an exact record of how this analysis was configured."),
                BorderLayout.CENTER);
        return panel;
    }

    private static final class SequenceContentDetection {
        private final InputType inputType;
        private final boolean needsAlignment;

        private SequenceContentDetection(InputType inputType, boolean needsAlignment) {
            this.inputType = inputType;
            this.needsAlignment = needsAlignment;
        }

        private InputType inputType() {
            return inputType;
        }

        private boolean needsAlignment() {
            return needsAlignment;
        }
    }
}
