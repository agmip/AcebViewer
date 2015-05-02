package org.agmip.ui.acebviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.agmip.ace.AceDataset;
import org.agmip.ace.io.AceParser;
import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskExecutionException;
import org.apache.pivot.wtk.Border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadDataTask extends Task<AcebViewerTreeView> {

    private static final Logger LOG = LoggerFactory.getLogger(ReadDataTask.class);
    private final File dataFile;
    private final Border dataDetailBd;

    public ReadDataTask(File dataFile, Border dataDetailBd) {
        this.dataFile = dataFile;
        this.dataDetailBd = dataDetailBd;
    }

    @Override
    public AcebViewerTreeView execute() {

        AcebViewerTreeView tree = null;
        AceDataset ace;
        try {
            if (dataFile.getName().toLowerCase().endsWith(".aceb")) {
                ace = AceParser.parseACEB(new FileInputStream(dataFile));
            } else {
                ace = AceParser.parse(new FileInputStream(dataFile));
            }
            LOG.info("Build Tree menu...");
            tree = new AcebViewerTreeView(ace, dataDetailBd);
            LOG.info("Build Tree menu completely");

        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            LOG.error("Failed to read file [" + dataFile.getPath() + "]");
        } catch (TaskExecutionException ex) {
            LOG.error(ex.getMessage());
            LOG.error("Failed to read file [" + dataFile.getPath() + "]");
        }

        return tree;
    }
}
