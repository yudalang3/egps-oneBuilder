package tanglegram;

import javax.swing.JComponent;

interface ExportableView {
    JComponent getExportComponent();

    default boolean canExport() {
        return getExportComponent() != null;
    }

    default Class<?> getExportContextClass() {
        return getClass();
    }
}
