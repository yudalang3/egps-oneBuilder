package onebuilder;

import java.util.ArrayList;
import java.util.List;

final class TextListCodec {
    private TextListCodec() {
    }

    static List<String> splitLines(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
        String[] lines = text.replace("\r", "").split("\n");
        List<String> values = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return List.copyOf(values);
    }

    static String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(System.lineSeparator(), values);
    }
}
