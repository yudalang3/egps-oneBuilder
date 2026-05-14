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
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

final class ThreeDTreeOrderDialog extends JDialog {
    private final ThreeDTreeOrderControlPanel controlPanel;
    private final Consumer<List<ImportedTreeSpec>> applyCallback;

    private ThreeDTreeOrderDialog(
            Window owner,
            List<ImportedTreeSpec> currentTrees,
            Consumer<List<ImportedTreeSpec>> applyCallback) {
        super(owner, UiText.text("Tree order", "树顺序"), Dialog.ModalityType.DOCUMENT_MODAL);
        this.applyCallback = applyCallback;
        this.controlPanel = new ThreeDTreeOrderControlPanel(currentTrees);
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

        setPreferredSize(new Dimension(880, 580));
        pack();
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(owner);
    }

    static void showDialog(
            Window owner,
            List<ImportedTreeSpec> currentTrees,
            Consumer<List<ImportedTreeSpec>> applyCallback) {
        new ThreeDTreeOrderDialog(owner, currentTrees, applyCallback).setVisible(true);
    }

    private void applyValues() {
        applyCallback.accept(controlPanel.toTreeOrder());
    }
}

final class ThreeDTreeOrderControlPanel extends JPanel {
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(216, 224, 235);
    private static final Color SELECTED = new Color(41, 112, 255);
    private static final Color TEXT = new Color(35, 43, 55);
    private static final Color MUTED = new Color(101, 116, 138);

    private final List<ImportedTreeSpec> treeOrder;
    private final List<TreeCardPanel> treeCards;
    private final JPanel pipelinePanel;
    private final JScrollPane pipelineScrollPane;
    private final JTextArea detailArea;
    private final JScrollPane detailScrollPane;
    private int selectedIndex;

    ThreeDTreeOrderControlPanel(List<ImportedTreeSpec> currentTrees) {
        super(new BorderLayout(14, 14));
        this.treeOrder = new ArrayList<>(currentTrees == null ? List.of() : currentTrees);
        this.treeCards = new ArrayList<>();
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

        JPanel orderColumn = createColumnPanel(
                "Tree display order",
                "Drag cards to change the left-to-right order in 3D Alignment.");
        orderColumn.add(pipelineScrollPane, BorderLayout.CENTER);
        orderColumn.add(createMoveButtonPanel(), BorderLayout.SOUTH);

        JPanel detailsColumn = createColumnPanel(
                "Selected tree",
                "Inspect the source for the selected tree card.");
        detailArea = createDetailArea();
        detailScrollPane = createContentScrollPane(detailArea);
        detailsColumn.add(detailScrollPane, BorderLayout.CENTER);

        boardPanel.add(orderColumn);
        boardPanel.add(detailsColumn);
        add(boardPanel, BorderLayout.CENTER);

        rebuildCards();
        updateSelection(0);
    }

    List<ImportedTreeSpec> toTreeOrder() {
        return List.copyOf(treeOrder);
    }

    void moveTree(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= treeOrder.size()) {
            return;
        }
        int boundedToIndex = Math.max(0, Math.min(toIndex, treeOrder.size() - 1));
        if (fromIndex == boundedToIndex) {
            updateSelection(boundedToIndex);
            return;
        }
        ImportedTreeSpec tree = treeOrder.remove(fromIndex);
        treeOrder.add(boundedToIndex, tree);
        selectedIndex = boundedToIndex;
        rebuildCards();
        updateSelection(boundedToIndex);
    }

    List<String> treeLabelsForTest() {
        List<String> labels = new ArrayList<>();
        for (ImportedTreeSpec tree : treeOrder) {
            labels.add(tree.label());
        }
        return labels;
    }

    void moveSelectedUpForTest() {
        moveSelected(-1);
    }

    void moveSelectedDownForTest() {
        moveSelected(1);
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
        JLabel titleLabel = new JLabel("Reorder 3D alignment layers");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 2.0f));
        titleLabel.setForeground(TEXT);
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea note = new JTextArea("This only changes the display order in the current 3D Alignment tab. Imported data and Newick files stay unchanged.");
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
        treeCards.clear();
        for (int index = 0; index < treeOrder.size(); index++) {
            TreeCardPanel card = new TreeCardPanel(index, treeOrder.get(index));
            treeCards.add(card);
            pipelinePanel.add(card);
            if (index < treeOrder.size() - 1) {
                pipelinePanel.add(Box.createVerticalStrut(10));
            }
        }
        pipelinePanel.revalidate();
        pipelinePanel.repaint();
    }

    private void updateSelection(int index) {
        if (treeOrder.isEmpty()) {
            detailArea.setText("No trees are available.");
            return;
        }
        selectedIndex = Math.max(0, Math.min(index, treeOrder.size() - 1));
        for (int cardIndex = 0; cardIndex < treeCards.size(); cardIndex++) {
            treeCards.get(cardIndex).setSelected(cardIndex == selectedIndex);
        }
        ImportedTreeSpec selectedTree = treeOrder.get(selectedIndex);
        detailArea.setText("Tree label\n" + selectedTree.label() + "\n\nSource path\n" + selectedTree.path());
        detailArea.setCaretPosition(0);
        scrollSelectedCardIntoView();
    }

    private void moveSelected(int offset) {
        moveTree(selectedIndex, selectedIndex + offset);
    }

    private void moveDraggedCard(int fromIndex, int mouseYOnPipeline) {
        int targetIndex = Math.max(0, treeOrder.size() - 1);
        for (int index = 0; index < treeCards.size(); index++) {
            TreeCardPanel card = treeCards.get(index);
            int midpoint = card.getY() + (card.getHeight() / 2);
            if (mouseYOnPipeline < midpoint) {
                targetIndex = index;
                break;
            }
        }
        moveTree(fromIndex, targetIndex);
    }

    private void scrollSelectedCardIntoView() {
        if (selectedIndex < 0 || selectedIndex >= treeCards.size()) {
            return;
        }
        pipelinePanel.scrollRectToVisible(treeCards.get(selectedIndex).getBounds());
    }

    private final class TreeCardPanel extends JPanel {
        private final int cardIndex;
        private boolean selected;
        private int dragStartY;

        private TreeCardPanel(int cardIndex, ImportedTreeSpec tree) {
            super(new BorderLayout(10, 0));
            this.cardIndex = cardIndex;
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
            JLabel title = new JLabel(tree.label());
            title.setForeground(TEXT);
            title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1.0f));
            JLabel summary = new JLabel(tree.path() == null ? "Tree source" : tree.path().getFileName().toString());
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
                            TreeCardPanel.this,
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

        private void updateBorder() {
            Color borderColor = selected ? SELECTED : BORDER;
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, selected ? 2 : 1),
                    BorderFactory.createEmptyBorder(selected ? 11 : 12, selected ? 11 : 12, selected ? 11 : 12, selected ? 11 : 12)));
        }
    }
}
