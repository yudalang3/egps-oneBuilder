package tanglegram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

final class TreeLeafArrangementDialog extends JDialog {
    private final TreeLeafArrangementControlPanel controlPanel;
    private final Consumer<TreeLeafArrangementOptions> applyCallback;

    private TreeLeafArrangementDialog(
            Window owner,
            TreeLeafArrangementOptions currentOptions,
            Consumer<TreeLeafArrangementOptions> applyCallback) {
        super(owner, UiText.text("Tree leaf arrangement", "树叶节点排列"), Dialog.ModalityType.DOCUMENT_MODAL);
        this.applyCallback = applyCallback;
        this.controlPanel = new TreeLeafArrangementControlPanel(currentOptions);
        WindowIconSupport.apply(this);
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(controlPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton(UiText.text("Apply", "应用"));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton(UiText.text("Cancel", "取消"));
        applyButton.addActionListener(event -> applyValues());
        okButton.addActionListener(event -> {
            applyValues();
            dispose();
        });
        cancelButton.addActionListener(event -> dispose());
        buttonPanel.add(applyButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(860, 580));
        pack();
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(owner);
    }

    static void showDialog(
            Window owner,
            TreeLeafArrangementOptions currentOptions,
            Consumer<TreeLeafArrangementOptions> applyCallback) {
        new TreeLeafArrangementDialog(owner, currentOptions, applyCallback).setVisible(true);
    }

    private void applyValues() {
        applyCallback.accept(controlPanel.toOptions().withEnabled(true));
    }
}

final class TreeLeafArrangementControlPanel extends JPanel {
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(216, 224, 235);
    private static final Color SELECTED = new Color(41, 112, 255);
    private static final Color TEXT = new Color(35, 43, 55);
    private static final Color MUTED = new Color(101, 116, 138);

    private final List<TreeLeafArrangementRule> ruleOrder;
    private final List<RuleCardPanel> ruleCards;
    private final JPanel pipelinePanel;
    private final JScrollPane pipelineScrollPane;
    private final JTextArea detailArea;
    private final JScrollPane detailScrollPane;
    private final JRadioButton upRadioButton;
    private final JRadioButton downRadioButton;
    private int selectedIndex;

    TreeLeafArrangementControlPanel(TreeLeafArrangementOptions currentOptions) {
        super(new BorderLayout(14, 14));
        TreeLeafArrangementOptions effectiveOptions = currentOptions == null
                ? TreeLeafArrangementOptions.defaults()
                : currentOptions;
        this.ruleOrder = new ArrayList<>(effectiveOptions.ruleOrder());
        this.ruleCards = new ArrayList<>();
        this.selectedIndex = 0;

        setOpaque(true);
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        add(createIntroPanel(), BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(1, 2, 14, 0));
        boardPanel.setOpaque(false);

        pipelinePanel = new ViewportWidthTrackingPanel();
        pipelinePanel.setOpaque(false);
        pipelinePanel.setLayout(new BoxLayout(pipelinePanel, BoxLayout.Y_AXIS));
        pipelineScrollPane = createContentScrollPane(pipelinePanel);

        JPanel pipelineColumn = createColumnPanel(
                "Sorting pipeline",
                "Drag cards to change priority. Top card runs first.");
        pipelineColumn.add(pipelineScrollPane, BorderLayout.CENTER);
        pipelineColumn.add(createMoveButtonPanel(), BorderLayout.SOUTH);

        JPanel sideColumn = createColumnPanel(
                "Direction and details",
                "Choose global ordering direction, then inspect each rule.");
        JPanel sideContent = new JPanel(new BorderLayout(0, 12));
        sideContent.setOpaque(false);

        upRadioButton = new JRadioButton("UP", effectiveOptions.direction() == TreeLeafArrangementDirection.UP);
        downRadioButton = new JRadioButton("DOWN", effectiveOptions.direction() == TreeLeafArrangementDirection.DOWN);
        ButtonGroup directionGroup = new ButtonGroup();
        directionGroup.add(upRadioButton);
        directionGroup.add(downRadioButton);
        sideContent.add(createDirectionPanel(), BorderLayout.NORTH);

        detailArea = createDetailArea();
        detailScrollPane = createContentScrollPane(detailArea);
        sideContent.add(createDetailsPanel(detailScrollPane), BorderLayout.CENTER);
        sideColumn.add(sideContent, BorderLayout.CENTER);

        boardPanel.add(pipelineColumn);
        boardPanel.add(sideColumn);
        add(boardPanel, BorderLayout.CENTER);

        rebuildCards();
        updateSelection(0);
    }

    TreeLeafArrangementOptions toOptions() {
        TreeLeafArrangementDirection direction = downRadioButton.isSelected()
                ? TreeLeafArrangementDirection.DOWN
                : TreeLeafArrangementDirection.UP;
        return new TreeLeafArrangementOptions(true, ruleOrder, direction);
    }

    void moveRule(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= ruleOrder.size()) {
            return;
        }
        int boundedToIndex = Math.max(0, Math.min(toIndex, ruleOrder.size() - 1));
        if (fromIndex == boundedToIndex) {
            updateSelection(boundedToIndex);
            return;
        }
        TreeLeafArrangementRule rule = ruleOrder.remove(fromIndex);
        ruleOrder.add(boundedToIndex, rule);
        selectedIndex = boundedToIndex;
        rebuildCards();
        updateSelection(boundedToIndex);
    }

    List<TreeLeafArrangementRule> ruleOrderForTest() {
        return List.copyOf(ruleOrder);
    }

    List<String> cardTitlesForTest() {
        List<String> titles = new ArrayList<>();
        for (RuleCardPanel card : ruleCards) {
            titles.add(card.titleForTest());
        }
        return titles;
    }

    String detailTextForTest() {
        return detailArea.getText();
    }

    void selectRuleForTest(TreeLeafArrangementRule rule) {
        int index = ruleOrder.indexOf(rule);
        if (index >= 0) {
            updateSelection(index);
        }
    }

    void moveSelectedUpForTest() {
        moveSelected(-1);
    }

    void moveSelectedDownForTest() {
        moveSelected(1);
    }

    void setDirectionForTest(TreeLeafArrangementDirection direction) {
        if (direction == TreeLeafArrangementDirection.DOWN) {
            downRadioButton.setSelected(true);
        } else {
            upRadioButton.setSelected(true);
        }
    }

    JScrollPane cardListScrollPaneForTest() {
        return pipelineScrollPane;
    }

    JScrollPane detailScrollPaneForTest() {
        return detailScrollPane;
    }

    private JPanel createIntroPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel("Arrange leaves without changing topology");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 2.0f));
        titleLabel.setForeground(TEXT);
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea note = new JTextArea("Cards are evaluated from top to bottom. This only changes branch display order in the 2D viewer; tree topology and Newick files stay unchanged.");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setForeground(MUTED);
        note.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.add(note, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createColumnPanel(String title, String subtitle) {
        JPanel column = new JPanel(new BorderLayout(0, 12));
        column.setBackground(SURFACE);
        column.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 1.0f));
        titleLabel.setForeground(TEXT);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(MUTED);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.CENTER);
        column.add(header, BorderLayout.NORTH);
        return column;
    }

    private JPanel createMoveButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        JButton moveUpButton = new JButton("Move up");
        JButton moveDownButton = new JButton("Move down");
        moveUpButton.addActionListener(event -> moveSelected(-1));
        moveDownButton.addActionListener(event -> moveSelected(1));
        panel.add(moveUpButton);
        panel.add(moveDownButton);
        return panel;
    }

    private JPanel createDirectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel label = new JLabel("Direction");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(TEXT);
        panel.add(label, BorderLayout.NORTH);

        JPanel togglePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        togglePanel.setOpaque(false);
        styleDirectionButton(upRadioButton, "smaller / earlier first");
        styleDirectionButton(downRadioButton, "larger / later first");
        togglePanel.add(upRadioButton);
        togglePanel.add(downRadioButton);
        panel.add(togglePanel, BorderLayout.CENTER);
        return panel;
    }

    private static void styleDirectionButton(JRadioButton button, String tooltip) {
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setToolTipText(tooltip);
        button.setMargin(new Insets(8, 10, 8, 10));
    }

    private JPanel createDetailsPanel(JScrollPane detailScrollPane) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel label = new JLabel("Rule details");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(detailScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JTextArea createDetailArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(TEXT);
        area.setBackground(new Color(250, 252, 255));
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 241)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        area.setMinimumSize(new Dimension(280, 160));
        return area;
    }

    private JScrollPane createContentScrollPane(java.awt.Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 241)));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        return scrollPane;
    }

    private void rebuildCards() {
        pipelinePanel.removeAll();
        ruleCards.clear();
        for (int index = 0; index < ruleOrder.size(); index++) {
            RuleCardPanel card = new RuleCardPanel(index, ruleOrder.get(index));
            ruleCards.add(card);
            pipelinePanel.add(card);
            if (index < ruleOrder.size() - 1) {
                pipelinePanel.add(Box.createVerticalStrut(10));
            }
        }
        pipelinePanel.revalidate();
        pipelinePanel.repaint();
    }

    private void updateSelection(int index) {
        selectedIndex = Math.max(0, Math.min(index, ruleOrder.size() - 1));
        for (int cardIndex = 0; cardIndex < ruleCards.size(); cardIndex++) {
            ruleCards.get(cardIndex).setSelected(cardIndex == selectedIndex);
        }
        detailArea.setText(detailText(ruleOrder.get(selectedIndex)));
        detailArea.setCaretPosition(0);
        scrollSelectedCardIntoView();
    }

    private void moveSelected(int offset) {
        moveRule(selectedIndex, selectedIndex + offset);
    }

    private void moveDraggedCard(int fromIndex, int mouseYOnPipeline) {
        int targetIndex = Math.max(0, ruleOrder.size() - 1);
        for (int index = 0; index < ruleCards.size(); index++) {
            RuleCardPanel card = ruleCards.get(index);
            int midpoint = card.getY() + (card.getHeight() / 2);
            if (mouseYOnPipeline < midpoint) {
                targetIndex = index;
                break;
            }
        }
        moveRule(fromIndex, targetIndex);
    }

    private void scrollSelectedCardIntoView() {
        if (selectedIndex < 0 || selectedIndex >= ruleCards.size()) {
            return;
        }
        pipelinePanel.scrollRectToVisible(ruleCards.get(selectedIndex).getBounds());
    }

    private static String summaryText(TreeLeafArrangementRule rule) {
        if (rule == TreeLeafArrangementRule.CLADE_SIZE) {
            return "Number of leaves in this clade";
        }
        if (rule == TreeLeafArrangementRule.LEAF_NAME_STRING) {
            return "Cached sorted leaf-name list";
        }
        if (rule == TreeLeafArrangementRule.BRANCH_LENGTH) {
            return "Direct branch length to parent";
        }
        return "Sorting rule";
    }

    private static String detailText(TreeLeafArrangementRule rule) {
        if (rule == TreeLeafArrangementRule.CLADE_SIZE) {
            return "Clade size\n\nCompares the number of leaves contained by each child clade. Smaller clades are drawn first with UP; larger clades are drawn first with DOWN.";
        }
        if (rule == TreeLeafArrangementRule.LEAF_NAME_STRING) {
            return "Leaf name string\n\nEach clade uses a cached sorted list of all leaf names. Comparison is item-by-item, not only the min or max leaf name.\n\nExample: [Abc, Bee] sorts before [Abd, Zed].";
        }
        if (rule == TreeLeafArrangementRule.BRANCH_LENGTH) {
            return "Branch length\n\nCompares the direct branch length from the parent to each child. Shorter branches are drawn first with UP; longer branches are drawn first with DOWN.";
        }
        return "No details available.";
    }

    private final class RuleCardPanel extends JPanel {
        private final int cardIndex;
        private final TreeLeafArrangementRule rule;
        private boolean selected;
        private int dragStartY;

        private RuleCardPanel(int cardIndex, TreeLeafArrangementRule rule) {
            super(new BorderLayout(10, 0));
            this.cardIndex = cardIndex;
            this.rule = rule;
            setOpaque(true);
            setBackground(SURFACE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
            setPreferredSize(new Dimension(320, 92));

            JLabel priority = new JLabel(String.valueOf(cardIndex + 1), SwingConstants.CENTER);
            priority.setOpaque(true);
            priority.setBackground(new Color(232, 238, 248));
            priority.setForeground(new Color(55, 76, 110));
            priority.setFont(priority.getFont().deriveFont(Font.BOLD));
            priority.setPreferredSize(new Dimension(34, 34));

            JPanel textPanel = new JPanel(new BorderLayout(0, 4));
            textPanel.setOpaque(false);
            JLabel title = new JLabel(rule.toString());
            title.setForeground(TEXT);
            title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1.0f));
            JLabel summary = new JLabel(summaryText(rule));
            summary.setForeground(MUTED);
            textPanel.add(title, BorderLayout.NORTH);
            textPanel.add(summary, BorderLayout.CENTER);

            JLabel dragHandle = new JLabel("::", SwingConstants.CENTER);
            dragHandle.setForeground(new Color(140, 153, 174));
            dragHandle.setPreferredSize(new Dimension(26, 34));

            add(priority, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
            add(dragHandle, BorderLayout.EAST);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    dragStartY = event.getYOnScreen();
                    updateSelection(cardIndex);
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    int dragDistance = Math.abs(event.getYOnScreen() - dragStartY);
                    if (dragDistance < 6) {
                        return;
                    }
                    MouseEvent converted = javax.swing.SwingUtilities.convertMouseEvent(
                            RuleCardPanel.this,
                            event,
                            pipelinePanel);
                    moveDraggedCard(cardIndex, converted.getY());
                }
            };
            addMouseListener(mouseAdapter);
            updateBorder();
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
            updateBorder();
        }

        private String titleForTest() {
            return rule.toString();
        }

        private void updateBorder() {
            Color borderColor = selected ? SELECTED : BORDER;
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, selected ? 2 : 1),
                    BorderFactory.createEmptyBorder(selected ? 11 : 12, selected ? 11 : 12, selected ? 11 : 12, selected ? 11 : 12)));
        }
    }
}
