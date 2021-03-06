/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.editors.datatable;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.omnetpp.common.Debug;
import org.omnetpp.common.ui.FilterCombo;
import org.omnetpp.common.ui.FocusManager;
import org.omnetpp.common.ui.IHasFocusManager;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engineext.ResultFileManagerEx;
import org.omnetpp.scave.model.ResultType;
import org.omnetpp.scave.model2.FilterField;
import org.omnetpp.scave.model2.FilterHints;

/**
 * Displays a data control of vectors/scalars/histograms with filter
 * combo boxes.
 *
 * This class is reusable, which means it only knows that it has to
 * display an IDList belonging to a particular ResultFileManager,
 * and has absolutely no reference to the editor, or EMF model objects,
 * or any widgets outside -- nothing.
 *
 * The user is responsible to keep contents up-to-date in case
 * ResultFileManager or IDList contents change. Refreshing can be
 * done by calling setIDList().
 *
 * @author andras
 */
public class FilteredDataPanel extends Composite implements IHasFocusManager {

    private FilterBar filterBar;
    private IDataControl dataControl;
    private IDList idlist; // the unfiltered data list
    private ResultType type;
    private FocusManager focusManager;
    private int itemLimit = 100_000_000; // some sensible(?) limit to the number of data items displayed
    private boolean truncated = false; // whether table/tree content has been truncated (to itemLimit items)

    public FilteredDataPanel(Composite parent, int style, ResultType type) {
        super(parent, style);
        this.type = type;
        initialize(type);
        configureFilterBar();
    }

    public FilterBar getFilterPanel() {
        return filterBar;
    }

    public IDataControl getDataControl() {
        return dataControl;
    }

    public void setIDList(IDList idlist) {
        this.idlist = idlist;
        runFilter();
    }

    public IDList getIDList() {
        return idlist;
    }

    public int getItemLimit() {
        return itemLimit;
    }

    public void setItemLimit(int itemLimit) {
        this.itemLimit = itemLimit;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setResultFileManager(ResultFileManagerEx manager) {
        dataControl.setResultFileManager(manager);
    }

    public ResultFileManager getResultFileManager() {
        return dataControl.getResultFileManager();
    }

    public ResultType getType() {
        return type;
    }

    protected void initialize(ResultType type) {
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);
        filterBar = new FilterBar(this, SWT.NONE);
        filterBar.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        if (type == null)
            dataControl = new DataTree(this, SWT.MULTI);
        else
            dataControl = new DataTable(this, SWT.MULTI, type);

        dataControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        filterBar.getToggleFilterTypeButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (filterBar.isShowingAdvancedFilter())
                    trySwitchToSimpleFilter();
                else
                    switchToAdvancedFilter();
            }
        });

        focusManager = new FocusManager(this);
    }

    protected void configureFilterBar() {
        //TODO configure filterBar.getAdvancedFilterText()
        configureFilterCombo(filterBar.getExperimentCombo(), FilterField.EXPERIMENT);
        configureFilterCombo(filterBar.getMeasurementCombo(), FilterField.MEASUREMENT);
        configureFilterCombo(filterBar.getReplicationCombo(), FilterField.REPLICATION);
        configureFilterCombo(filterBar.getModuleNameCombo(), FilterField.MODULE);
        configureFilterCombo(filterBar.getNameCombo(), FilterField.NAME);
    }

    protected void configureFilterCombo(FilterCombo filterCombo, FilterField filterField) {
        filterCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // check the filter string
                if (!filterBar.isFilterPatternValid()) {
                    MessageDialog.openWarning(getShell(), "Error in Filter Expression", "Syntax error in filter expression, panel contents unchanged.");
                    return;
                }
                runFilter();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);  //delegate
            }
        });

        filterCombo.setContentproposalProvider(new IContentProposalProvider() {
            @Override
            public IContentProposal[] getProposals(String contents, int position) {
                String prefix = contents.substring(0, position);
                return wrapIntoProposals(computeHintsFor(filterCombo, filterField, prefix));
            }
        });
    }

    protected String[] computeHintsFor(FilterCombo filterCombo, FilterField filterField, String prefix) {
        String filter = filterBar.getSimpleFilterExcluding(filterField);
        IDList filteredIDList = computeFilteredIDList(filter, Math.max(itemLimit, 1_000_000));
        return FilterHints.computeHints(dataControl.getResultFileManager(), filteredIDList, filterField, prefix); //TODO hintsLimit!
    }

    protected IContentProposal[] wrapIntoProposals(String[] hints) {
        IContentProposal[] proposals = new IContentProposal[hints.length];
        for (int i=0; i<hints.length; i++)
            proposals[i] = new ContentProposal(hints[i]);
        return proposals;
    }

    protected void runFilter() {
        Assert.isTrue(idlist!=null);

        Debug.time("runFilter() including setItemCount()", 10, () -> {
            IDList filteredIDList = computeFilteredIDList(filterBar.getFilterIfValid(), itemLimit);
            dataControl.setIDList(filteredIDList);

            if (getParent() instanceof FilteredDataTabFolder)
                ((FilteredDataTabFolder)getParent()).refreshPanelTitles();
        });
    }

    // Side effect: sets the 'truncated' flag
    protected IDList computeFilteredIDList(String filter, int itemLimit) {
        ResultFileManagerEx manager = dataControl.getResultFileManager();
        truncated = false;
        if (manager == null) {
            return new IDList();
        }
        else if (filter != null) {
            IDList filtered = manager.filterIDList(idlist, filter, itemLimit+1);
            if (filtered.size() == itemLimit+1) {
                filtered.erase(itemLimit); // remove last one
                truncated = true;
            }
            return filtered;
        }
        else { // no or invalid filter
            if (idlist.size() <= itemLimit)
                return idlist;
            else {
                truncated = true;
                return idlist.getRange(0, itemLimit);
            }
        }
    }

    public String getFilter() {
        return filterBar.getFilter();
    }

    public void setFilterParams(String filter) {
        // an arbitrary pattern can only be shown in advanced view -- switch there
        if (!filterBar.isShowingAdvancedFilter())
            filterBar.showAdvancedFilter();
        filterBar.getAdvancedFilterText().setText(filter);
        runFilter();
    }

    /**
     * Switches the filter from "Advanced" to "Basic" mode. If this cannot be done
     * (filter string invalid or too complex), the user is prompted with a dialog,
     * and switching may or may not actually take place depending on the answer.
     * @return true if switching was actually done.
     */
    public boolean trySwitchToSimpleFilter() {
        boolean success = filterBar.trySwitchToSimpleFilter();
        if (success)
            runFilter();
        return success;
    }

    /**
     * Switches the filter from "Basic" to "Advanced" mode. This is always successful (unlike the opposite way).
     */
    public void switchToAdvancedFilter() {
        filterBar.switchToAdvancedFilter();
        runFilter();
    }

    /**
     * Shows/hides the filter panel.
     */
    public void showFilterPanel(boolean show) {
        if (show != isFilterPanelVisible()) {
            filterBar.setVisible(show);
            GridData data = (GridData)filterBar.getLayoutData();
            data.exclude = !show;
            layout(true, true);
        }
    }

    /**
     * Returns {@code true} iff the filter panel is visible.
     */
    public boolean isFilterPanelVisible() {
        GridData data = (GridData)filterBar.getLayoutData();
        return !data.exclude;
    }

    @Override
    public boolean setFocus() {
        // try to restore focus where it was last time
        if (focusManager != null && focusManager.setFocus())
            return true;
        return super.setFocus();
    }

}
