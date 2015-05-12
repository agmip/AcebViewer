package org.agmip.ui.acebviewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.prefs.Preferences;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.serialization.SerializationException;
import org.apache.pivot.util.Filter;
import org.apache.pivot.util.Resources;
import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskListener;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.ActivityIndicator;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.Border;
import org.apache.pivot.wtk.BoxPane;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.FileBrowserSheet;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.MessageType;
import org.apache.pivot.wtk.ScrollPane;
import org.apache.pivot.wtk.Sheet;
import org.apache.pivot.wtk.SheetCloseListener;
import org.apache.pivot.wtk.TaskAdapter;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Meng Zhang
 */
public class AcebViewerWindow extends Window implements Bindable {

    private static final Logger LOG = LoggerFactory.getLogger(AcebViewerWindow.class);
    private Properties versionProperties = new Properties();
    private Preferences pref = null;
    private String viewerVersion = "";
    private Button browseAcebButton = null;
    private TextInput acebText = null;
    private Border dataListBd = null;
    private Border dataDetailBd = null;
    private Label txtVersion = null;

    public AcebViewerWindow() {
        try {
            InputStream versionFile = getClass().getClassLoader().getResourceAsStream("product.properties");
            versionProperties.load(versionFile);
            versionFile.close();
            StringBuilder avv = new StringBuilder();
            String buildType = versionProperties.getProperty("product.buildtype");
            avv.append("Version ");
            avv.append(versionProperties.getProperty("product.version"));
            avv.append("-").append(versionProperties.getProperty("product.buildversion"));
            avv.append("(").append(buildType).append(")");
            if (buildType.equals("dev")) {
                avv.append(" [").append(versionProperties.getProperty("product.buildts")).append("]");
            }
            viewerVersion = avv.toString();
        } catch (IOException ex) {
            LOG.error("Unable to load version information, version will be blank.");
        }

        Action.getNamedActions().put("fileQuit", new Action() {
            @Override
            public void perform(Component src) {
                DesktopApplicationContext.exit();
            }
        });
    }

    @Override
    public void initialize(Map<String, Object> ns, URL url, Resources rsrcs) {

        browseAcebButton = (Button) ns.get("browseAcebButton");
        acebText = (TextInput) ns.get("acebText");
        dataListBd = (Border) ns.get("dataList");
        dataDetailBd = (Border) ns.get("dataDetail");
        txtVersion = (Label) ns.get("txtVersion");

        txtVersion.setText(viewerVersion);
        LOG.info("AcebViewer {} lauched with JAVA {} under OS {}", viewerVersion, System.getProperty("java.runtime.version"), System.getProperty("os.name"));

        try {
            pref = Preferences.userNodeForPackage(getClass());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }

        browseAcebButton.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {
                final FileBrowserSheet browse = openFileBrowserSheet("last_input_aceb");
                browse.setDisabledFileFilter(new Filter<File>() {

                    @Override
                    public boolean include(File file) {
                        return (file.isFile()
                                //                                && !file.getName().toLowerCase().endsWith(".zip")
                                && !file.getName().toLowerCase().endsWith(".json")
                                && !file.getName().toLowerCase().endsWith(".aceb"));
                    }
                });
                browse.open(AcebViewerWindow.this, new SheetCloseListener() {
                    @Override
                    public void sheetClosed(Sheet sheet) {
                        if (sheet.getResult()) {
                            File dataFile = browse.getSelectedFile();
                            acebText.setText(dataFile.getPath());
                            if (pref != null) {
                                pref.put("last_input_aceb", dataFile.getPath());
                            }
                            readData(dataFile);
                        }
                    }
                });
            }
        });

    }

    private void readData(File dataFile) {

        LOG.info("Loading data from [" + dataFile.getPath() + "]..."); 
        ActivityIndicator indicator = new ActivityIndicator();
        indicator.setActive(true);
        indicator.setHeightLimits(10, 16);
        indicator.setWidthLimits(10, 16);
        try {
            indicator.setStyles("{color:'#777777'}");
        } catch (SerializationException ex) {
            LOG.error(ex.getMessage());
        }
        dataListBd.setContent(indicator);
        dataDetailBd.setContent(null);

        ReadDataTask task = new ReadDataTask(dataFile, dataDetailBd);
        TaskListener<AcebViewerTreeView> listener = new TaskListener<AcebViewerTreeView>() {

            @Override
            public void taskExecuted(Task<AcebViewerTreeView> task) {

                AcebViewerTreeView tree = task.getResult();
                if (tree != null) {
//                    
                    ScrollPane sp = new ScrollPane();
                    sp.setMaximumHeight(500);
                    sp.setMaximumWidth(360);
                    BoxPane bp = new BoxPane();
                    try {
                        bp.setStyles("{padding:{right:10}}");
                    } catch (SerializationException ex) {
                        LOG.error(ex.getMessage());
                    }
                    bp.add(tree);
                    sp.setView(bp);
                    dataListBd.setContent(sp);

                } else {
                    LOG.info("Loading data failed, no tree built");
                    Alert.alert(MessageType.ERROR, "Failed to read data, no tree built", AcebViewerWindow.this);
                }
            }

            @Override
            public void executeFailed(Task<AcebViewerTreeView> task) {
                LOG.info("Loading data failed");
                Alert.alert(MessageType.ERROR, "Failed to read data", AcebViewerWindow.this);
            }
        };
        task.execute(new TaskAdapter(listener));
    }

    private FileBrowserSheet openFileBrowserSheet(String lastPathId) {
        if (!acebText.getText().equals("")) {
            try {
                String path = new File(acebText.getText()).getCanonicalFile().getParent();
                return new FileBrowserSheet(FileBrowserSheet.Mode.OPEN, path);
            } catch (IOException ex) {
                return new FileBrowserSheet(FileBrowserSheet.Mode.OPEN);
            }
        } else {
            String lastPath = "";
            if (pref != null) {
                lastPath = pref.get(lastPathId, "");
            }
            File tmp = new File(lastPath);
            if (lastPath.equals("") || !tmp.exists()) {
                return new FileBrowserSheet(FileBrowserSheet.Mode.OPEN);
            } else {
                if (!tmp.isDirectory()) {
                    lastPath = tmp.getParentFile().getPath();
                }
                return new FileBrowserSheet(FileBrowserSheet.Mode.OPEN, lastPath);
            }
        }
    }
}
