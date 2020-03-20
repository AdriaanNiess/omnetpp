/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.editors.datatable;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.omnetpp.common.Debug;
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

    private FilteringPanel filterPanel;
    private IDataControl data;
    private IDList idlist; // the unfiltered data list
    private ResultType type;
    private FocusManager focusManager;
    private int itemLimit = 100000; // max number of rows where a virtual Table does not yet have performance issues
    private boolean truncated = false; // whether table/tree content has been truncated (to itemLimit items)

    public FilteredDataPanel(Composite parent, int style, ResultType type) {
        super(parent, style);
        this.type = type;
        initialize(type);
        configureFilterPanel();
    }

    public FilteringPanel getFilterPanel() {
        return filterPanel;
    }

    public IDataControl getDataControl() {
        return data;
    }

    public void setIDList(IDList idlist) {
        this.idlist = idlist;
        updateFilterCombos();
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
        data.setResultFileManager(manager);
    }

    public ResultFileManager getResultFileManager() {
        return data.getResultFileManager();
    }

    public ResultType getType() {
        return type;
    }

    protected void initialize(ResultType type) {
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);
        filterPanel = new FilteringPanel(this, SWT.NONE);
        filterPanel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        if (type == null)
            data = new DataTree(this, SWT.MULTI);
        else
            data = new DataTable(this, SWT.MULTI, type);

        data.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        filterPanel.getToggleFilterTypeButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (filterPanel.isShowingAdvancedFilter())
                    trySwitchToSimpleFilter();
                else
                    switchToAdvancedFilter();
            }
        });

        focusManager = new FocusManager(this);
    }

    protected void configureFilterPanel() {
        SelectionListener selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // check the filter string
                if (!filterPanel.isFilterPatternValid()) {
                    MessageDialog.openWarning(getShell(), "Error in Filter Expression", "Syntax error in filter expression, panel contents unchanged.");
                    return;
                }
                runFilter();

                if (e.widget instanceof Combo)
                    updateFilterCombosExcept((Combo)e.widget);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);  //delegate
            }
        };

        // when the filter button gets pressed, update the content
        filterPanel.getAdvancedFilterText().addSelectionListener(selectionListener);
        filterPanel.getExperimentCombo().addSelectionListener(selectionListener);
        filterPanel.getMeasurementCombo().addSelectionListener(selectionListener);
        filterPanel.getReplicationCombo().addSelectionListener(selectionListener);
        filterPanel.getModuleNameCombo().addSelectionListener(selectionListener);
        filterPanel.getNameCombo().addSelectionListener(selectionListener);
    }

    protected void updateFilterCombos() {
        Debug.time("updateFilterCombos()", 10, () -> {
            if (!filterPanel.isDisposed())
                filterPanel.setFilterHints(getFilterHints());
        });
    }

    protected void updateFilterCombosExcept(Combo except) {
        if (data.getResultFileManager() != null && !filterPanel.isDisposed() && !filterPanel.isShowingAdvancedFilter()) {
            FilterHints hints = new FilterHints();

            for (FilterField field : filterPanel.getSimpleFilterFields()) {
                Combo combo = filterPanel.getFilterCombo(field);
                if (combo != except) {
                    String filter = filterPanel.getSimpleFilterExcluding(field);
                    IDList filteredIDList = computeFilteredIDList(filter, itemLimit);
                    hints.addHints(field, data.getResultFileManager(), filteredIDList);
                }
            }

            filterPanel.setFilterHintsOfCombos(hints);
        }
    }

    public FilterHints getFilterHints() {
        if (data.getResultFileManager() != null)
            return new FilterHints(data.getResultFileManager(), idlist);
        else
            return new FilterHints();
    }

    protected void runFilter() {
        Assert.isTrue(idlist!=null);

        Debug.time("runFilter() including setItemCount()", 10, () -> {
            IDList filteredIDList = computeFilteredIDList(filterPanel.getFilterIfValid(), itemLimit);
            data.setIDList(filteredIDList);

            if (getParent() instanceof FilteredDataTabFolder)
                ((FilteredDataTabFolder)getParent()).refreshPanelTitles();
        });
    }

    // Side effect: sets the 'truncated' flag
    protected IDList computeFilteredIDList(String filter, int itemLimit) {
        ResultFileManagerEx manager = data.getResultFileManager();
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
        return filterPanel.getFilter();
    }

    public void setFilterParams(String filter) {
        // an arbitrary pattern can only be shown in advanced view -- switch there
        if (!filterPanel.isShowingAdvancedFilter())
            filterPanel.showAdvancedFilter();
        filterPanel.getAdvancedFilterText().setText(filter);
        runFilter();
    }

    /**
     * Switches the filter from "Advanced" to "Basic" mode. If this cannot be done
     * (filter string invalid or too complex), the user is prompted with a dialog,
     * and switching may or may not actually take place depending on the answer.
     * @return true if switching was actually done.
     */
    public boolean trySwitchToSimpleFilter() {
        boolean success = filterPanel.trySwitchToSimpleFilter();
        if (success)
            runFilter();
        return success;
    }

    /**
     * Switches the filter from "Basic" to "Advanced" mode. This is always successful (unlike the opposite way).
     */
    public void switchToAdvancedFilter() {
        filterPanel.switchToAdvancedFilter();
        runFilter();
    }

    /**
     * Shows/hides the filter panel.
     */
    public void showFilterPanel(boolean show) {
        if (show != isFilterPanelVisible()) {
            filterPanel.setVisible(show);
            GridData data = (GridData)filterPanel.getLayoutData();
            data.exclude = !show;
            layout(true, true);
        }
    }

    /**
     * Returns {@code true} iff the filter panel is visible.
     */
    public boolean isFilterPanelVisible() {
        GridData data = (GridData)filterPanel.getLayoutData();
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
