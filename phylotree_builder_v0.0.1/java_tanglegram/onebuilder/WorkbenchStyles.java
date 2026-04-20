package onebuilder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.LayoutManager;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

final class WorkbenchStyles {
    static final Color CANVAS_BACKGROUND = new Color(243, 247, 252);
    static final Color SURFACE_BACKGROUND = Color.WHITE;
    static final Color ACCENT = new Color(39, 102, 191);
    static final Color ACCENT_SOFT = new Color(231, 239, 252);
    static final Color ACCENT_BORDER = new Color(193, 211, 242);
    static final Color TEXT_PRIMARY = new Color(29, 43, 64);
    static final Color TEXT_SECONDARY = new Color(94, 112, 137);
    static final Color DIVIDER = new Color(220, 228, 238);
    static final Color SUCCESS = new Color(40, 125, 76);
    static final Color WARNING = new Color(185, 125, 0);
    static final Color DANGER = new Color(181, 61, 61);
    static final EmptyBorder PAGE_PADDING = new EmptyBorder(20, 20, 20, 20);
    private static final Border CARD_BORDER = new CompoundBorder(
            new LineBorder(ACCENT_BORDER, 1, true),
            new EmptyBorder(18, 18, 18, 18));

    private WorkbenchStyles() {
    }

    static JPanel createCanvasPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(CANVAS_BACKGROUND);
        return panel;
    }

    static JPanel createSurfacePanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        applyCard(panel);
        return panel;
    }

    static void applyCanvas(JComponent component) {
        component.setOpaque(true);
        component.setBackground(CANVAS_BACKGROUND);
    }

    static void applyCard(JComponent component) {
        component.setOpaque(true);
        component.setBackground(SURFACE_BACKGROUND);
        component.setBorder(CARD_BORDER);
    }

    static void applyInsetCard(JComponent component, int top, int left, int bottom, int right) {
        component.setOpaque(true);
        component.setBackground(SURFACE_BACKGROUND);
        component.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_BORDER, 1, true),
                new EmptyBorder(top, left, bottom, right)));
    }

    static JLabel createPageTitle(String text) {
        JLabel label = new JLabel(text);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            label.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 10f));
        }
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            label.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D() + 3f));
        }
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    static JLabel createSubtitleLabel(String text) {
        JLabel label = new JLabel(text);
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            label.setFont(baseFont.deriveFont(Font.PLAIN, Math.max(11f, baseFont.getSize2D() - 1f)));
        }
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    static JTextArea createNoteArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setForeground(TEXT_SECONDARY);
        return area;
    }

    static JLabel createStatusChip(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(ACCENT_SOFT);
        label.setForeground(ACCENT);
        label.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ACCENT_BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont != null) {
            label.setFont(baseFont.deriveFont(Font.BOLD, Math.max(11f, baseFont.getSize2D() - 1f)));
        }
        return label;
    }

    static void updateStatusChip(JLabel label, String text) {
        String safeText = text == null || text.isBlank() ? "Idle" : text.trim();
        label.setText(safeText);
        if ("Completed".equalsIgnoreCase(safeText) || "Complete".equalsIgnoreCase(safeText)) {
            label.setBackground(new Color(230, 244, 236));
            label.setForeground(SUCCESS);
        } else if ("Failed".equalsIgnoreCase(safeText)) {
            label.setBackground(new Color(253, 236, 236));
            label.setForeground(DANGER);
        } else if ("Interrupted".equalsIgnoreCase(safeText) || "Stopped".equalsIgnoreCase(safeText)) {
            label.setBackground(new Color(255, 244, 224));
            label.setForeground(WARNING);
        } else {
            label.setBackground(ACCENT_SOFT);
            label.setForeground(ACCENT);
        }
    }

    static void styleRailButton(AbstractButton button) {
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setBackground(CANVAS_BACKGROUND);
        button.setForeground(TEXT_PRIMARY);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
    }

    static void styleMonospaceLog(JTextArea textArea) {
        Font baseFont = UIManager.getFont("Label.font");
        int size = baseFont == null ? 12 : Math.max(12, baseFont.getSize());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, size));
    }

    static void applyPanelTreeBackground(Component component) {
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;
            if (!(jComponent instanceof javax.swing.JScrollPane)
                    && !(jComponent instanceof javax.swing.JViewport)
                    && !(jComponent instanceof org.jdesktop.swingx.JXTaskPane)) {
                if (!(jComponent instanceof javax.swing.JTextArea)
                        && !(jComponent instanceof javax.swing.JTextField)
                        && !(jComponent instanceof javax.swing.JComboBox)
                        && !(jComponent instanceof javax.swing.JSpinner)
                        && !(jComponent instanceof javax.swing.JButton)
                        && !(jComponent instanceof javax.swing.JCheckBox)) {
                    jComponent.setOpaque(true);
                    if (jComponent.getBackground() == null || UIManager.getColor("Panel.background").equals(jComponent.getBackground())) {
                        jComponent.setBackground(SURFACE_BACKGROUND);
                    }
                }
            }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyPanelTreeBackground(child);
            }
        }
    }
}
