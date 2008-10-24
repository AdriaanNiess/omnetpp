package org.omnetpp.cdt.ui;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.omnetpp.cdt.Activator;
import org.omnetpp.cdt.makefile.MakemakeOptions;

/**
 * @author Andras
 */
//FIXME displa panel status in dialog!
public class MakemakeOptionsDialog extends TitleAreaDialog {
    protected IContainer folder;
    protected MakemakeOptions options; // initial/result
    protected List<IContainer> makeFolders;  // needed by the panel
    
    // controls
    protected MakemakeOptionsPanel optionsPanel;
    
    public MakemakeOptionsDialog(Shell parentShell, IContainer folder, MakemakeOptions options, List<IContainer> makeFolders) {
        super(parentShell);
        this.folder = folder;
        this.options = options;
        this.makeFolders = makeFolders;
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Makemake Options");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Makemake Options");
        setMessage("Makefile generation (Makemake) options for " + folder.getFullPath().toString() + ".");
        
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout(1,false));
        
        optionsPanel = new MakemakeOptionsPanel(composite, SWT.NONE) {
            @Override
            protected void setErrorMessage(String text) {
                setMakemakePanelErrorMessage(text);
            }            
        };
        optionsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        optionsPanel.populate(folder, options, makeFolders);
        return optionsPanel;
    }

    protected void setMakemakePanelErrorMessage(String text) {
        setErrorMessage(text);
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton!=null)
            okButton.setEnabled(text==null);
    }            

    @Override
    protected void okPressed() {
        // save result before dialog gets disposed
        options = optionsPanel.getResult();

        try {
            optionsPanel.saveMakefragFiles();
        }
        catch (CoreException e) {
            errorDialog(e); // sorry, that's all we can do here (we cannot veto the OK processing)
        }
        super.okPressed();
    }


    public MakemakeOptions getResult() {
        return options;
    }

    protected void errorDialog(CoreException e) {
        Activator.logError(e);
        ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Error", e.getMessage(), e.getStatus());
    }
}
