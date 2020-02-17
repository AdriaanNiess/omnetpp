package org.omnetpp.scave.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.ui.ExportChartsDialog;
import org.omnetpp.scave.export.IChartExport;
import org.omnetpp.scave.export.IGraphicalExportFileFormat;
import org.omnetpp.scave.jobs.ExportChartsJob;
import org.omnetpp.scave.model.ModelObject;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model2.FilenameGenerator;
import org.omnetpp.scave.model2.FilenameGenerator.Overwrite;
import org.omnetpp.scave.model2.ScaveModelUtil;

public class ExportChartsAction extends AbstractScaveAction {

    public ExportChartsAction() {
        setText("Export Charts...");
        setToolTipText("Export charts as png/jpg/eps/svg image files.");
    }

    @Override
    protected void doRun(ScaveEditor editor, ISelection selection) throws CoreException {
        // we'll display the list of all charts, and check the selected one(s) and those under selected ones
        final List<Chart> allCharts = ScaveModelUtil.collectCharts(editor.getAnalysis().getCharts().getCharts());
        List<Chart> initialSelection = new ArrayList<Chart>();
        for (Object obj : asStructuredOrEmpty(selection).toArray()) {
            if (obj instanceof Chart)
                initialSelection.add((Chart)obj);
            if (obj instanceof ModelObject)
                initialSelection.addAll(ScaveModelUtil.collectCharts(editor.getAnalysis().getCharts().getCharts()));
        }
        if (initialSelection.isEmpty())
            initialSelection = allCharts;

        final Shell parentShell = editor.getSite().getShell();
        final ExportChartsDialog dialog = new ExportChartsDialog(editor.getSite().getShell(), allCharts, initialSelection, editor);
        int result = dialog.open();
        if (result == Window.OK) {
            try {
                List<Chart> selectedCharts = dialog.getSelectedCharts();
                IChartExport exporter = dialog.getChartExporter();
                IGraphicalExportFileFormat format = dialog.getFileFormat();
                File targetFolder = dialog.getTargetFolder().getLocation().toFile();
                List<File> files = generateFilenames(selectedCharts, targetFolder, format, parentShell);
                if (!selectedCharts.isEmpty() && exporter != null && format != null && targetFolder != null) {
                    ExportChartsJob job = new ExportChartsJob(selectedCharts, exporter, editor.getResultFileManager(), format, files);
                    job.schedule();
                }
            }
            catch (OperationCanceledException e) {
            }
        }
    }

    @Override
    protected boolean isApplicable(ScaveEditor editor, ISelection selection) {
        final List<Chart> charts = ScaveModelUtil.collectCharts(editor.getAnalysis().getCharts().getCharts());
        return !charts.isEmpty();
    }

    private List<File> generateFilenames(List<Chart> charts, File folder, IGraphicalExportFileFormat format, Shell shell) {
        List<String> names = new ArrayList<String>(charts.size());
        for (Chart chart : charts)
            names.add(!StringUtils.isBlank(chart.getName()) ? chart.getName() : "unnamed-"+chart.hashCode());
        List<File> files = new FilenameGenerator(folder, format.getName(), Overwrite.Ask).generateFilenames(names, shell);
        return files;
    }
}
