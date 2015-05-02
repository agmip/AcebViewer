package org.agmip.ui.acebviewer;

import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;

public class AcebViewerApp extends Application.Adapter {

    private AcebViewerWindow window = null;

    @Override
    public void startup(Display display, Map<String, String> props) throws Exception {
        BXMLSerializer bxml = new BXMLSerializer();
        window = (AcebViewerWindow) bxml.readObject(getClass().getResource("/aceb_viewer.bxml"));
        window.open(display);
    }

    @Override
    public boolean shutdown(boolean opt) {
        if (window != null) {
            window.close();
        }
        return false;
    }

    public static void main(String[] args) {
        boolean cmdFlg = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-cli")) {
                cmdFlg = true;
                break;
            }
        }
        if (cmdFlg) {
//            AcebViewerCmdLine cmd = new AcebViewerCmdLine();
//            cmd.run(args);

        } else {
            DesktopApplicationContext.main(AcebViewerApp.class, args);
        }
    }
}
