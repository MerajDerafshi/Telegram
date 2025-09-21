package ToolBox;

import javafx.scene.Parent;
import javafx.scene.Scene;
import java.net.URL;
import java.util.Objects;

/**
 * Manages the theme (light/dark mode) for the application.
 */
public class ThemeManager {

    public static boolean isLightMode = false;

    // Corrected to use absolute paths from the classpath root. This is much more reliable.
    private static final String LIGHT_MODE_CSS = Objects.requireNonNull(ThemeManager.class.getResource("/resources/css/lightMode.css")).toExternalForm();
    private static final String DARK_MODE_CSS = Objects.requireNonNull(ThemeManager.class.getResource("/resources/css/darkMode.css")).toExternalForm();

    /**
     * Applies the currently selected theme to a given scene.
     * It intelligently removes the other theme's stylesheet if present and adds the current one.
     * @param scene The scene to apply the theme to.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        // Remove the other theme's stylesheet and add the current one.
        if (isLightMode) {
            scene.getStylesheets().remove(DARK_MODE_CSS);
            if (!scene.getStylesheets().contains(LIGHT_MODE_CSS)) {
                scene.getStylesheets().add(LIGHT_MODE_CSS);
            }
        } else {
            scene.getStylesheets().remove(LIGHT_MODE_CSS);
            if (!scene.getStylesheets().contains(DARK_MODE_CSS)) {
                scene.getStylesheets().add(DARK_MODE_CSS);
            }
        }
    }

    /**
     * Applies the currently selected theme to the scene of a given root node.
     * @param root The root node of the scene.
     */
    public static void applyTheme(Parent root) {
        if (root != null) {
            applyTheme(root.getScene());
        }
    }
}

