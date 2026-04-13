package tanglegram;

import com.formdev.flatlaf.FlatLightLaf;

public final class FlatLafBootstrap {
    public static final String USE_NATIVE_LIBRARY_PROPERTY = "flatlaf.useNativeLibrary";

    private FlatLafBootstrap() {
    }

    public static void prepareSystemProperties() {
        System.setProperty(USE_NATIVE_LIBRARY_PROPERTY, "false");
    }

    public static void setupFlatLaf() {
        prepareSystemProperties();
        FlatLightLaf.setup();
    }
}
