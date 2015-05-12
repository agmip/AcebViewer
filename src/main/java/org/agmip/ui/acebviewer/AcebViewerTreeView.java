package org.agmip.ui.acebviewer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.agmip.ace.AceComponent;
import org.agmip.ace.AceDataset;
import org.agmip.ace.AceEvent;
import org.agmip.ace.AceEventType;
import org.agmip.ace.AceExperiment;
import org.agmip.ace.AceRecord;
import org.agmip.ace.AceSoil;
import org.agmip.ace.AceWeather;
import org.agmip.ace.util.AceFunctions;
import org.agmip.common.Functions;
import static org.agmip.ui.acebviewer.AcebViewerTreeView.BranchType.Experiments;
import static org.agmip.ui.acebviewer.AcebViewerTreeView.BranchType.Soils;
import static org.agmip.ui.acebviewer.AcebViewerTreeView.BranchType.Weathers;
import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.HashMap;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.serialization.SerializationException;
import org.apache.pivot.util.concurrent.TaskExecutionException;
import org.apache.pivot.wtk.Border;
import org.apache.pivot.wtk.BoxPane;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TreeView;
import org.apache.pivot.wtk.TreeViewSelectionListener;
import org.apache.pivot.wtk.content.TreeBranch;
import org.apache.pivot.wtk.content.TreeNode;
import org.apache.pivot.wtk.media.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Meng Zhang
 */
public class AcebViewerTreeView extends TreeView {

    private static final Logger LOG = LoggerFactory.getLogger(AcebViewerTreeView.class);
    private static Image listIcon;
    private static Image nodeIcon;

    public enum BranchType {

        Unknown,
        Experiments,
        Weathers,
        Soils
    }

    public AcebViewerTreeView() throws TaskExecutionException {
        super();
        listIcon = Image.load(getClass().getResource("/folder.png"));
        nodeIcon = Image.load(getClass().getResource("/page_white.png"));
    }

    public AcebViewerTreeView(final AceDataset ace, final Border dataDetailBd) throws TaskExecutionException, IOException {
        this();

        ArrayList<TreeBranch> dataArr = new ArrayList();
        loadTreeBranch(dataArr, ace, Experiments, "exname");
        loadTreeBranch(dataArr, ace, Soils, "soil_id");
        loadTreeBranch(dataArr, ace, Weathers, "wst_id", "clim_id");
        this.setTreeData(dataArr);

        this.getTreeViewSelectionListeners().add(new TreeViewSelectionListener.Adapter() {

            @Override
            public void selectedPathsChanged(TreeView tv, Sequence<Sequence.Tree.Path> sqnc) {
                Object o = tv.getSelectedNode();
                dataDetailBd.setContent(null);
                BXMLSerializer bxml = new BXMLSerializer();
                try {
                    if (o instanceof TreeBranch) {

                        TreeBranch tb = (TreeBranch) o;
                        String tbName = tb.getText();
                        BoxPane tableBP;
                        if (tbName.equals(Experiments.toString())) {
                            tableBP = (BoxPane) bxml.readObject(getClass().getResource("/aceb_viewer_table_exp.bxml"));
                        } else if (tbName.equals(Weathers.toString())) {
                            tableBP = (BoxPane) bxml.readObject(getClass().getResource("/aceb_viewer_table_wth.bxml"));
                        } else if (tbName.equals(Soils.toString())) {
                            tableBP = (BoxPane) bxml.readObject(getClass().getResource("/aceb_viewer_table_soil.bxml"));
                        } else {
                            LOG.warn("Unsupported branch type [" + tbName + "] selected.");
                            return;
                        }

                        TableView table = (TableView) bxml.getNamespace().get("tableView");
                        loadDataList(table, ace, tbName);
                        dataDetailBd.setContent(tableBP);

                    } else if (o instanceof TreeNode) {

                        TreeBranch tb = ((TreeNode) o).getParent();
                        String tbName = tb.getText();
                        String nodeName = ((TreeNode) o).getText();
                        BoxPane tableBP;

                        if (tbName.equals(Experiments.toString())) {

                            // Find experiment data via slected node name
                            AceExperiment exp = (AceExperiment) getData(ace.getExperiments(), nodeName, "exname");
                            if (exp == null) {
                                LOG.warn("Could not find record for [" + nodeName + "]");
                                return;
                            }

                            tableBP = (BoxPane) bxml.readObject(getClass().getResource("/aceb_viewer_detail_exp.bxml"));
                            loadMetaData((TableView) bxml.getNamespace().get("metaDataView"), exp, "exname", "trt_name", "fl_lat", "fl_long", "soil_id", "wst_id");
                            loadMetaData((TableView) bxml.getNamespace().get("icMetaDataView"), exp.getInitialConditions(), "icdat");
                            loadSubListData((TableView) bxml.getNamespace().get("icLayerDataView"), exp.getInitialConditions().getSoilLayers().iterator(), "sllb");
                            loadEventsData((TableView) bxml.getNamespace().get("managementView"), exp.getEvents().iterator());
                            loadMetaData((TableView) bxml.getNamespace().get("observedSummaryView"), exp.getOberservedData());
                            loadSubListData((TableView) bxml.getNamespace().get("observedTimeSeriView"), exp.getOberservedData().getTimeseries().iterator(), "date");

                        } else if (tbName.equals(Weathers.toString())) {

                            // Find weather data via slected node name
                            AceWeather wth = (AceWeather) getData(ace.getWeathers(), nodeName, "wst_id", "clim_id");
                            if (wth == null) {
                                LOG.warn("Could not find record for [" + nodeName + "]");
                                return;
                            }

                            tableBP = (BoxPane) bxml.readObject(getClass().getResource("/aceb_viewer_detail_wth.bxml"));
                            loadMetaData((TableView) bxml.getNamespace().get("metaDataView"), wth, "wst_id", "clim_id");
                            loadSubListData((TableView) bxml.getNamespace().get("dailyWeatherView"), wth.getDailyWeather().iterator(), "w_date");

                        } else if (tbName.equals(Soils.toString())) {

                            // Find soil data via slected node name
                            AceSoil soil = (AceSoil) getData(ace.getSoils(), nodeName, "soil_id");
                            if (soil == null) {
                                LOG.warn("Could not find record for [" + nodeName + "]");
                                return;
                            }

                            tableBP = (BoxPane) bxml.readObject(getClass().getResource("/aceb_viewer_detail_soil.bxml"));
                            loadMetaData((TableView) bxml.getNamespace().get("metaDataView"), soil, "soil_id");
                            loadSubListData((TableView) bxml.getNamespace().get("soilLayerView"), soil.getSoilLayers().iterator(), "sllb");

                        } else {
                            LOG.warn("Unsupported branch type [" + tbName + "] selected.");
                            return;
                        }
                        dataDetailBd.setContent(tableBP);

                    }
                } catch (IOException ex) {
                    LOG.error(ex.getMessage());
                } catch (SerializationException ex) {
                    LOG.error(ex.getMessage());
                }
            }
        });
    }

    private void loadTreeBranch(ArrayList<TreeBranch> dataArr, AceDataset ace, BranchType branchName, String... branchPKs) throws IOException {

        LOG.info("Loading " + branchName + " data...");
        List list;
        if (branchName.equals(Experiments)) {
            list = ace.getExperiments();
        } else if (branchName.equals(Weathers)) {
            list = ace.getWeathers();
        } else if (branchName.equals(Soils)) {
            list = ace.getSoils();
        } else {
            return;
        }

        if (list != null && !list.isEmpty()) {

            TreeBranch tb = new TreeBranch(listIcon, branchName.toString());
            for (Object o : list) {
                AceComponent c = (AceComponent) o;
                String nodeName = "";
                for (String branchPK : branchPKs) {
                    String val = c.getValueOr(branchPK, "");
                    if (!val.equals("")) {
                        nodeName += val + "-";
                    }
                }
                if (!nodeName.isEmpty()) {
                    nodeName = nodeName.substring(0, nodeName.length() - 1);
                } else {
                    nodeName = "N/A";
                }
                tb.add(new TreeNode(nodeIcon, nodeName));
            }
            dataArr.add(tb);

        } else {
            LOG.info(branchName + " data is missing in the dataset");
        }

    }

    private void loadDataList(TableView table, AceDataset ace, String tbName) throws IOException {

        // Get Header info
        int colNum = table.getColumns().getLength();
        String[] headers = new String[colNum];
        for (int i = 0; i < colNum; i++) {
            headers[i] = table.getColumns().get(i).getName();
        }

        // Load data into table
        ArrayList<HashMap> dataList = new ArrayList();
        if (tbName.equals(Experiments.toString())) {

            for (AceExperiment exp : ace.getExperiments()) {
                HashMap m = new HashMap();
                for (String header : headers) {
                    String val;
                    if (header.equals("wst_id") || header.equals("soil_id")) {
                        val = exp.getValueOr(header, "");
                    } else if (header.equals("ir_num")) {
                        val = "" + getEventCount(exp, AceEventType.ACE_IRRIGATION_EVENT);
                    }  else if (header.equals("ir_tot")) {
                        val = getEventAmtTotal(exp, AceEventType.ACE_IRRIGATION_EVENT, header, "irval");
                    } else if (header.equals("fe_num")) {
                        val = "" + getEventCount(exp, AceEventType.ACE_FERTILIZER_EVENT);
                    } else if (header.equals("fen_tot")) {
                        val = getEventAmtTotal(exp, AceEventType.ACE_FERTILIZER_EVENT, header, "feamn");
                    } else if (header.equals("fep_tot")) {
                        val = getEventAmtTotal(exp, AceEventType.ACE_FERTILIZER_EVENT, header, "feamp");
                    }  else if (header.equals("fek_tot")) {
                        val = getEventAmtTotal(exp, AceEventType.ACE_FERTILIZER_EVENT, header, "feamk");
                    }  else if (header.equals("om_tot")) {
                        val = getEventAmtTotal(exp, AceEventType.ACE_ORGANIC_MATTER_EVENT, header, "omamt");
                    }  else if (header.equals("obs_end_of_season")) {
                        val = "" + exp.getOberservedData().keySet().size();
                    }  else if (header.equals("obs_time_series_data")) {
                        val = "" + exp.getOberservedData().getTimeseries().size();
                    } else {
                        val = AceFunctions.deepGetValue(exp, header);
                        if (val == null) {
                            val = "";
                        }
                    }
                    m.put(header, val);
                }
                dataList.add(m);
            }

        } else {

            List dataSet;
            if (tbName.equals(Soils.toString())) {
                dataSet = ace.getSoils();
            } else if (tbName.equals(Weathers.toString())) {
                dataSet = ace.getWeathers();
            } else {
                return;
            }
            for (Object data : dataSet) {
                HashMap m = new HashMap();
                for (String key : headers) {
                    m.put(key, ((AceComponent) data).getValueOr(key, ""));
                }
                dataList.add(m);
            }
        }

        table.setTableData(dataList);
    }

    private AceComponent getData(List list, String nodeName, String... pks) {
        AceComponent c = null;
        for (Object o : list) {
            c = (AceComponent) o;
            try {
                String keyVal = "";
                for (String pk : pks) {
                    String tmp = c.getValueOr(pk, "");
                    if (!tmp.equals("")) {
                        keyVal += "-" + tmp;
                    }
                }
                if (!keyVal.equals("")) {
                    keyVal = keyVal.substring(1);
                }
                if (keyVal.equals(nodeName)) {
                    break;
                } else {
                    c = null;
                }
            } catch (IOException ex) {
                LOG.warn(ex.getMessage());
            }
        }

        return c;
    }

    private void loadMetaData(TableView table, AceComponent acp, String... priorityKeys) {
        ArrayList<HashMap> dataList = new ArrayList();
        try {
            HashSet<String> keys = new HashSet(acp.keySet());
            for (String nodePK : priorityKeys) {
                HashMap m = new HashMap();
                m.put("var", nodePK.toUpperCase());
                m.put("val", acp.getValueOr(nodePK, ""));
                dataList.add(m);
                keys.remove(nodePK);
            }
            for (String key : keys) {
                key = escapeKey(key);
                HashMap m = new HashMap();
                m.put("var", key.toUpperCase());
                m.put("val", acp.getValueOr(key, ""));
                dataList.add(m);
            }
        } catch (IOException ex) {
            LOG.warn(ex.getMessage());
        }

        table.setTableData(dataList);
    }

    private void loadEventsData(TableView table, Iterator<AceEvent> it) throws IOException {
        ArrayList<HashMap> dataList = new ArrayList();
        HashSet<String> headers = new HashSet();

        headers.add("date");
        table.getColumns().add(new TableView.Column("date", "DATE"));
        headers.add("event");
        table.getColumns().add(new TableView.Column("event", "EVENT_TYPE"));

        while (it.hasNext()) {
            AceEvent event = it.next();
            HashMap m = new HashMap();
            int count = 1;
            for (String key : event.keySet()) {
                key = escapeKey(key);
                if (headers.contains(key)) {

                    m.put(key, event.getValueOr(key, ""));

                } else {
                    
                    if (!headers.contains("var_" + count)) {
                        headers.add("var_" + count);
                        table.getColumns().add(new TableView.Column("var_" + count, "VAR_" + count));
                        table.getColumns().add(new TableView.Column("val_" + count, "VAL_" + count));
                    }
                    m.put("var_" + count, key);
                    m.put("val_" + count, event.getValueOr(key, ""));
                    count++;
                }

            }
            dataList.add(m);
        }

        table.setTableData(dataList);
    }

    private void loadSubListData(TableView table, Iterator<AceRecord> it, String... priorityKeys) throws IOException {
        ArrayList<HashMap> dataList = new ArrayList();
        HashSet<String> headers = new HashSet();

        for (String priorityKey : priorityKeys) {
            headers.add(priorityKey);
            table.getColumns().add(new TableView.Column(priorityKey, priorityKey.toUpperCase()));
        }

        while (it.hasNext()) {
            AceRecord record = it.next();
            HashMap m = new HashMap();
            for (String key : record.keySet()) {
                key = escapeKey(key);
                if (!headers.contains(key)) {
                    headers.add(key);
                    table.getColumns().add(new TableView.Column(key, key.toUpperCase()));
                }
                m.put(key, record.getValueOr(key, ""));
            }
            dataList.add(m);
        }

        table.setTableData(dataList);
    }

    private String escapeKey(String key) {
        if (key.contains("#")) {
            key = key.replaceAll("#", "_num");
        }
        if (key.contains("%")) {
            key = key.replaceAll("%", "_pct");
        }
        return key;
    }
    
    private int getEventCount(AceExperiment exp, AceEventType type) {
        int count = 0;
        Iterator<AceEvent> it = exp.getEvents().iterator();
        while (it.hasNext()) {
            AceEvent event = it.next();
            if (event.getEventType().equals(type)) {
                count++;
            }
        }
        return count;
    }
    
    private String getEventAmtTotal(AceExperiment exp, AceEventType type, String totVarName, String amtVarName) throws IOException {
        String totAmt = AceFunctions.deepGetValue(exp, totVarName);
        if (totAmt == null || totAmt.equals("")) {
            totAmt = "0";
            Iterator<AceEvent> it = exp.getEvents().iterator();
            while (it.hasNext()) {
                AceEvent event = it.next();
                if (event.getEventType().equals(type)) {
                    totAmt = Functions.sum(totAmt, event.getValueOr(amtVarName, "0"));
                }
            }
        }
        
        return totAmt;
    }
}
