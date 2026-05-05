package tanglegram;

import java.awt.Color;
import java.util.List;

record ConsistencyAnnotation(List<String> leafNames, Color color, double ribbonWidth) {
    static final double DEFAULT_RIBBON_WIDTH = 5.0d;

    ConsistencyAnnotation {
        leafNames = List.copyOf(leafNames);
        color = color == null ? new Color(79, 140, 255, 160) : color;
        ribbonWidth = Double.isFinite(ribbonWidth) && ribbonWidth > 0.0d ? ribbonWidth : DEFAULT_RIBBON_WIDTH;
    }

    ConsistencyAnnotation(List<String> leafNames, Color color) {
        this(leafNames, color, DEFAULT_RIBBON_WIDTH);
    }

    String leafNamesText() {
        return String.join(",", leafNames);
    }

    String colorText() {
        return String.format(
                "#%02X%02X%02X%02X",
                Integer.valueOf(color.getRed()),
                Integer.valueOf(color.getGreen()),
                Integer.valueOf(color.getBlue()),
                Integer.valueOf(color.getAlpha()));
    }

    String ribbonWidthText() {
        if (Math.rint(ribbonWidth) == ribbonWidth) {
            return String.valueOf((int) ribbonWidth);
        }
        return String.valueOf(ribbonWidth);
    }
}
