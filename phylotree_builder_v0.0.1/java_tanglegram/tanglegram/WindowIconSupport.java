package tanglegram;

import java.awt.Image;
import java.awt.Window;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

public final class WindowIconSupport {
    private static final String[] ICON_RESOURCES = {
            "/tanglegram/eGPS_logo16x16.png",
            "/tanglegram/eGPS_logo32x32.png",
            "/tanglegram/eGPS_logo72x72.png"
    };

    private WindowIconSupport() {
    }

    public static void apply(Window window) {
        if (window == null) {
            return;
        }
        List<Image> images = loadIconImages();
        if (!images.isEmpty()) {
            window.setIconImages(images);
        }
    }

    static List<Image> loadIconImagesForTest() {
        return loadIconImages();
    }

    private static List<Image> loadIconImages() {
        List<Image> images = new ArrayList<>();
        for (String resourcePath : ICON_RESOURCES) {
            URL resource = WindowIconSupport.class.getResource(resourcePath);
            if (resource != null) {
                images.add(new ImageIcon(resource).getImage());
            }
        }
        return images;
    }
}
