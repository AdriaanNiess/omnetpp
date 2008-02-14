package org.omnetpp.launch.tabs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.omnetpp.common.project.ProjectUtils;
import org.omnetpp.common.ui.HoverSupport;
import org.omnetpp.common.ui.IHoverTextProvider;
import org.omnetpp.common.ui.SizeConstraint;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.inifile.editor.model.InifileParser;
import org.omnetpp.inifile.editor.model.ParseException;
import org.omnetpp.launch.IOmnetppLaunchConstants;
import org.omnetpp.launch.LaunchPlugin;


/**
 * A launch configuration tab that displays and edits omnetpp project
 *
 * @author rhornig
 */
public class OmnetppMainTab extends AbstractLaunchConfigurationTab 
    implements ModifyListener {

	private ILaunchConfiguration config;

	// UI widgets
	protected Text fProgText;
	protected Button fShowDebugViewButton;

    // working dir
    private Button fWorkspaceButton;
    private Button fVariablesButton;
    private Text workingDirText = null;

    // simulation parameters group
    protected final String DEFAULT_RUNTOOLTIP= "The run number(s) that should be executed (eg.: 0,2,7,9..11 or * for ALL runs) (default: 0)";
	// UI widgets
	protected Text fInifileText;
	protected Combo fConfigCombo;
    protected Text fRunText;
    protected Text fNedPathText;
    protected Spinner fParallelismSpinner;
    protected Button fDefaultEnvButton;
    protected Button fCmdEnvButton;
    protected Button fTkEnvButton;
    protected Button fOtherEnvButton;
    protected Text fOtherEnvText;
    protected Text fLibraryText;
    protected Text fAdditionalText;
    private boolean debugLaunchMode = false;
    private String infoText = null;
    
    public OmnetppMainTab() {
        super();
    }

    public void createControl(Composite parent) {
        debugLaunchMode = ILaunchManager.DEBUG_MODE.equals(getLaunchConfigurationDialog().getMode());
        Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
        createWorkingDirGroup(comp, 1);
		createSimulationGroup(comp, 1);
        createConfigurationGroup(comp, 1);
        createUIGroup(comp, 1);
        createAdditionalGroup(comp, 1);
        createOptionsGroup(comp, 1);
        setControl(comp);
    }

    // dynamic behavior
    private enum ArgType {INI, CONFIG, RUN, UI, LIB, NEDPATH, UNKNOWN};

    public void initializeFrom(ILaunchConfiguration config) {
    	this.config = config;
        
        try {
        	// working directory init
        	String wd = config.getAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, "");
        	setWorkingDirectoryText(wd);
          	// append the project path and the program name. Both of them are
            IPath projPath = new Path(config.getAttribute(IOmnetppLaunchConstants.ATTR_PROJECT_NAME, ""));
            String progName = config.getAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_NAME, "");
			fProgText.setText(projPath.append(progName).toString());
            fShowDebugViewButton.setSelection(config.getAttribute(IOmnetppLaunchConstants.ATTR_SHOWDEBUGVIEW, false));
            
            // simulation parameters block init
            ArgType nextType = ArgType.UNKNOWN;
            String args[] = StringUtils.split(config.getAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_ARGUMENTS, ""));
            String restArgs = "";        // the rest of the arguments we cannot recognize
            String iniArgs = "", libArgs = "", configArg="", runArg="", uiArg ="", nedPathArg ="";
            for (int i=0; i<args.length; ++i) {
                switch (nextType) {
                    case INI:
                        iniArgs += args[i]+" ";
                        nextType = ArgType.UNKNOWN;
                        continue;
                    case LIB:
                        libArgs += args[i]+" ";
                        nextType = ArgType.UNKNOWN;
                        continue;
                    case CONFIG:
                        configArg = args[i];
                        nextType = ArgType.UNKNOWN;
                        continue;
                    case RUN:
                        runArg = args[i];
                        nextType = ArgType.UNKNOWN;
                        continue;
                    case UI:
                        uiArg = args[i];
                        nextType = ArgType.UNKNOWN;
                        continue;
                    case NEDPATH:
                        nedPathArg = args[i];
                        nextType = ArgType.UNKNOWN;
                        continue;
                }

                if ("-f".equals(args[i]))
                    nextType = ArgType.INI;
                else if ("-c".equals(args[i]))
                    nextType = ArgType.CONFIG;
                else if ("-r".equals(args[i]))
                    nextType = ArgType.RUN;
                else if ("-u".equals(args[i]))
                    nextType = ArgType.UI;
                else if ("-l".equals(args[i]))
                    nextType = ArgType.LIB;
                else if ("-n".equals(args[i]))
                    nextType = ArgType.NEDPATH;
                else {
                    nextType = ArgType.UNKNOWN;
                    restArgs += args[i]+" ";
                }
            }

            // set the controls
            fInifileText.setText(iniArgs.trim());
            fLibraryText.setText(libArgs.trim());
            fAdditionalText.setText(restArgs.trim());
            // if the ned source path is the default, erase it (meaning it is default)
            if (nedPathArg.trim().equals(getDefaultNedSourcePath()))
            	nedPathArg = "";
            fNedPathText.setText(nedPathArg.trim());
            fOtherEnvText.setText("");
            fDefaultEnvButton.setSelection(false);
            fCmdEnvButton.setSelection(false);
            fTkEnvButton.setSelection(false);
            fOtherEnvButton.setSelection(false);
            if ("".equals(uiArg)) {
                fDefaultEnvButton.setSelection(true);
            }
            else {
                fDefaultEnvButton.setSelection(false);
                if ("Cmdenv".equals(uiArg))
                    fCmdEnvButton.setSelection(true);
                else if ("Tkenv".equals(uiArg))
                    fTkEnvButton.setSelection(true);
                else {
                    fOtherEnvButton.setSelection(true);
                    fOtherEnvText.setText(uiArg.trim());
                }
            }
            fOtherEnvText.setEnabled(fOtherEnvButton.getSelection());
            updateUIGroup();
            updateConfigCombo();
            setConfigName(configArg.trim());

            if (debugLaunchMode) {
				// if this is a debug launch the value from the command line
                fRunText.setText(runArg.trim());
			} else
                // otherwise we get it from a separate attribute
                fRunText.setText(config.getAttribute(IOmnetppLaunchConstants.ATTR_RUN, ""));

            if (fParallelismSpinner != null)
                fParallelismSpinner.setSelection(config.getAttribute(IOmnetppLaunchConstants.ATTR_NUM_CONCURRENT_PROCESSES, 1));

        } catch (CoreException ce) {
            LaunchPlugin.logError(ce);
        }
	}


    /**
     * Fills the config combo with the config section values from the inifiles 
     */
    protected void updateConfigCombo() {
        IFile[] inifiles = getIniFiles();
        if (config == null || inifiles == null)
            fConfigCombo.setItems(new String[] {});
        else {
            String currentSelection = getConfigName();
            String newConfigNames[] = getConfigNames(inifiles);
            if (!ObjectUtils.equals(StringUtils.join(fConfigCombo.getItems()," - "),
                                   StringUtils.join(newConfigNames," - "))) {
                fConfigCombo.setItems(newConfigNames);
                setConfigName(currentSelection);
            }
        }
    }

    /**
     * updates the control states in the UI group
     */
    private void updateUIGroup() {
        if (!fOtherEnvButton.getSelection())
            fOtherEnvText.setText("");
        fOtherEnvText.setEnabled(fOtherEnvButton.getSelection());

        if (fParallelismSpinner != null) {
            fParallelismSpinner.setEnabled(fCmdEnvButton.getSelection());
            if (!fCmdEnvButton.getSelection())
                fParallelismSpinner.setSelection(1);
        }
    }

    /**
     * Expands and returns the working directory attribute of the given launch
     * configuration.
     *
     * @return an absolute path to a directory in the local file system, or
     * the location of the workspace if unspecified
     */
    private IPath getWorkingDirectoryPath(){
    	String expandedLocation;
    	try {
    		expandedLocation = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(getWorkingDirectoryText());
    		if (expandedLocation.length() > 0) {
    			IPath newPath = new Path(expandedLocation);
    			return newPath.makeAbsolute();
    		}
    	} catch (CoreException e) {
    		LaunchPlugin.logError("Error getting working directory from the dilalog", e);
    	}
    	return null;
    	
    }

    protected String getWorkingDirectoryText() {
        return workingDirText.getText().trim();
    }

    protected void setWorkingDirectoryText(String dir) {
        if (dir != null) {
            workingDirText.setText(dir);
        }
    }

    /**
     * true if project is specified (directly or indirectly in the exe file name) and it has omnetpp nature 
     */
    protected boolean isOmnetppProject() {
        IProject project = getProject();
        return project != null && ProjectUtils.hasOmnetppNature(project);
    }

    /**
     * Returns the project in which the currently selected EXE file is located or null if 
     * the project is not yet specified (or closed)
     */
    protected IProject getProject() {
    	String projectName = (new Path(fProgText.getText())).segment(0);
    	if (StringUtils.isBlank(projectName))
    		return null;
    	IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    	return project != null && project.exists() ? project : null;
    }
    
    /**
     * Returns the selected library files. Returns null on error
     */
    private IFile[] getLibFiles() {
        List<IFile> result = new ArrayList<IFile>();
        String names[] =  StringUtils.split(fLibraryText.getText().trim());

        for (String name : names) {
            IPath iniPath = getWorkingDirectoryPath().append(name).makeAbsolute();
            IFile[] ifiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(iniPath);
            if (ifiles.length == 1)
                result.add(ifiles[0]);
            else
                return null;
        }
        return result.toArray(new IFile[result.size()]);
    }

    /**
     * Returns the selected ini files or (omnetpp.ini) if the inifile text line
     * is empty. Returns null on error.
     */
    private IFile[] getIniFiles() {
        List<IFile> result = new ArrayList<IFile>();
        IPath workingDirectoryPath = getWorkingDirectoryPath();
        if (workingDirectoryPath == null)
            return null;
        String names[] =  StringUtils.split(fInifileText.getText().trim());
        // default ini file is omnetpp ini
        if (names.length == 0)
            names = new String[] {"omnetpp.ini"};

        for (String name : names) {
            IPath iniPath = workingDirectoryPath.append(name).makeAbsolute();
            IFile[] ifiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(iniPath);
            if (ifiles.length == 1)
                result.add(ifiles[0]);
            else
                return null;
        }
        return result.toArray(new IFile[result.size()]);
    }

    /**
     * Returns all the configuration names found in the supplied inifiles
     */
    private String [] getConfigNames(IFile[] inifiles) {
        Map<String,OmnetppLaunchUtils.ConfigEnumeratorCallback.Section> sections 
            = new LinkedHashMap<String, OmnetppLaunchUtils.ConfigEnumeratorCallback.Section>();
        if (inifiles != null)
            for (IFile inifile : inifiles) {
                InifileParser iparser = new InifileParser();
                try {
                    iparser.parse(inifile, new OmnetppLaunchUtils.ConfigEnumeratorCallback(inifile, sections));
                } catch (ParseException e) {
                    setErrorMessage("Error reading inifile: "+e.getMessage());
                } catch (CoreException e) {
                    setErrorMessage("Error reading inifile: "+e.getMessage());
                } catch (IOException e) {
                    setErrorMessage("Error reading inifile: "+e.getMessage());
                }
            }
        List<String> result = new ArrayList<String>();
        result.add("");
        for (OmnetppLaunchUtils.ConfigEnumeratorCallback.Section sec : sections.values())
            result.add(sec.toString());
        return result.toArray(new String[] {});
    }

    /**
     * Returns whether the currently selected line in the config combo is a scenario
     */
    private boolean isScenario() {
    	//FIXME scenario sections are now also called [Config ...], so more sophisticated parsing is needed to figure out of which sections are in fact scenarios --Andras
        return fConfigCombo.getText().contains("(scenario)"); //FIXME by checking the label??? --Andras
    }

    /**
     * Returns the currently selected config name (after stripping the comments an other stuff)
     */
    private String getConfigName() {
        return StringUtils.substringBefore(fConfigCombo.getText(), "--").trim();
    }

    /**
     * @param name The config name that should be selected from the drop down. If no match found
     * the first empty line will be selected
     */
    private void setConfigName(String name) {
        fConfigCombo.setText("");
        for (String line : fConfigCombo.getItems())
            if (line.startsWith(name)) {
                fConfigCombo.setText(line);
                return;
            }
    }
    
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        Path progPath = new Path(fProgText.getText());
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, getWorkingDirectoryText());
		configuration.setAttribute(IOmnetppLaunchConstants.ATTR_PROJECT_NAME, progPath.segment(0));
		configuration.setAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_NAME, progPath.removeFirstSegments(1).toString());
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_SHOWDEBUGVIEW, fShowDebugViewButton.getSelection());

        // simulation block parameters
        String arg = "";
        if (StringUtils.isNotBlank(fInifileText.getText()))
            arg += "-f "+ StringUtils.join(StringUtils.split(fInifileText.getText())," -f ")+" ";
        if (StringUtils.isNotBlank(fLibraryText.getText()))
            arg += "-l "+ StringUtils.join(StringUtils.split(fLibraryText.getText())," -l ")+" ";
        if (StringUtils.isNotBlank(getConfigName()))
            arg += "-c "+getConfigName()+" ";
        
        // handle the ned path (if empty store and use the default ${ned_path:/ini_files_projectname}
        String nedPath = fNedPathText.getText();
		if (StringUtils.isNotBlank(nedPath))
            arg += "-n "+StringUtils.trimToEmpty(nedPath)+" ";
		else
			arg += "-n "+getDefaultNedSourcePath() +" ";
		
        // if we are in debug mode, we should store the run parameter into the command line
        String strippedRun = StringUtils.deleteWhitespace(fRunText.getText());
        if (debugLaunchMode) {
            if (StringUtils.isNotBlank(fRunText.getText()))
                arg += "-r "+strippedRun+" ";
        }
        else {
            // otherwise (stand-alone starter) we store it into a separate attribute
            configuration.setAttribute(IOmnetppLaunchConstants.ATTR_RUN, strippedRun);
        }

        if (fParallelismSpinner != null)
            configuration.setAttribute(IOmnetppLaunchConstants.ATTR_NUM_CONCURRENT_PROCESSES, fParallelismSpinner.getSelection());

        if (fCmdEnvButton.getSelection())
            arg += "-u Cmdenv ";
        if (fTkEnvButton.getSelection())
            arg += "-u Tkenv ";
        if (fOtherEnvButton.getSelection())
            arg += "-u "+fOtherEnvText.getText()+" ";
        arg += fAdditionalText.getText();
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_ARGUMENTS, arg);
        // clear the run info text, so next time it will be re-requested
        infoText = null;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    	// check the current selection and figure out the needed values if possible
    	// TODO detect selection and adjust the parameters accordingly
    	IResource selected = DebugUITools.getSelectedResource();
    	String defProject = selected == null || selected.getProject() == null 
    							? null : "/"+selected.getProject().getName();
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_PROJECT_NAME, defProject);
        
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_NAME, "");
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_SHOWDEBUGVIEW, false);
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, "");
        configuration.setAttribute(IOmnetppLaunchConstants.ATTR_PROGRAM_ARGUMENTS, "");
    }

    /**
     * The default value for the ned source path (all ned source folders in the same project where the first
     * ini file is present plus all dependent projects ned source folders 
     */
    private String getDefaultNedSourcePath() {
		IFile[] inifiles = getIniFiles();
		if (inifiles == null || inifiles.length == 0) {
			if (getProject() == null)
				return "";
			else
				return "${ned_path:" + getProject().getFullPath().toString() + "}";
		}
		return "${ned_path:" + inifiles[0].getProject().getFullPath().toString() +"}";
    }
    
   	/*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
	@Override
    public boolean isValid(ILaunchConfiguration configuration) {
	    setErrorMessage(null);

	    // working directory
        // if variables are present, we cannot resolve the directory
        String workingDirPath = getWorkingDirectoryText();
        if (workingDirPath.indexOf("${") >= 0) {
            IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
            try {
                manager.validateStringVariables(workingDirPath);
            }
            catch (CoreException e) {
                setErrorMessage(e.getMessage());
                return false;
            }
        }
        else if (workingDirPath.length() > 0) {
            IContainer container = getContainer();
            if (container == null) {
                File dir = new File(workingDirPath);
                if (dir.isDirectory())
                    return true;
                setErrorMessage("Not a directory");
                return false;
            }
        }
        else if (workingDirPath.length() == 0) {
            setErrorMessage("Working directory must be set");
            return false;
        }
	    
	    // exe and project testing
	    String name = fProgText.getText().trim();
	    if (name.length() == 0) {
	        setErrorMessage("Simulation program not specified");
	        return false;
	    }
	    Path exePath = new Path(name);
		IFile exefile = exePath.segmentCount() >1 ? ResourcesPlugin.getWorkspace().getRoot().getFile(exePath) : null;
	    if (exefile == null || !exefile.isAccessible()) {
	        setErrorMessage("Simulation program does not exist or not accessible in workspace");
	        return false;
	    }

	    if (!isOmnetppProject()) {
	        setErrorMessage("The selected project must be an OMNEST/OMNeT++ simulation");
	        return false;
	    }
	    
	    // simulation block parameters
		IFile ifiles[] = getIniFiles();
        if (ifiles == null) {
            setErrorMessage("Initialization file does not exist, or not accessible in workspace");
            return false;
        }
        for (IFile ifile : ifiles)
            if (!ifile.isAccessible()) {
                setErrorMessage("Initialization file "+ifile.getName()+" does not exist, or not accessible in workspace");
                return false;
            }

        IFile libfiles[] = getLibFiles();
        if ( libfiles == null) {
            setErrorMessage("Library file does not exist, or not accessible in workspace");
            return false;
        }
        for (IFile ifile : libfiles)
            if (!ifile.isAccessible()) {
                setErrorMessage("Library file "+ifile.getName()+" does not exist, or not accessible in workspace");
                return false;
            }

        if ("".equals(StringUtils.deleteWhitespace(fRunText.getText())) && isScenario() ) {
            setErrorMessage("Run number(s) must be specified if a scenario is selected");
            return false;
        }

        Integer runs[] = LaunchPlugin.parseRuns(StringUtils.deleteWhitespace(fRunText.getText()), 2);
        if (runs == null) {
            setErrorMessage("The run number(s) should be in a format like: 0,2,7,9..11 or use * for ALL runs");
            return false;
        }

        if (fOtherEnvButton.getSelection() && StringUtils.isEmpty(fOtherEnvText.getText())) {
            setErrorMessage("Environment type must be specified");
            return false;
        }
        if (!fCmdEnvButton.getSelection() && !fOtherEnvButton.getSelection()
                && runs!=null && runs.length>1) {
            setErrorMessage("Multiple runs are only supported for the Command line environment");
            return false;
        }
	    
        return super.isValid(configuration);
	}

    @Override
    public Image getImage() {
        return LaunchPlugin.getImage("/icons/full/ctool16/omnetsim.gif");
    }

    public String getName() {
        return "Main";
    }

    
	// ********************************************************************
    // event handlers
    
    /**
     * Show a dialog that lets the user select a project. This in turn provides context for the main
     * type, allowing the user to key a main type name, or constraining the search for main types to
     * the specified project.
     */
    protected void handleBinaryBrowseButtonSelected() {
        ElementTreeSelectionDialog dialog
            = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
                                                         new OmnetppLaunchUtils.ExecutableWorkbenchContentProvider());
        dialog.setAllowMultiple(false);
        dialog.setTitle("Select Executable File");
        dialog.setMessage("Select the executable file that should be started.\n");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
        if (dialog.open() == IDialogConstants.OK_ID && dialog.getFirstResult() instanceof IFile) {
            String exefile = ((IFile)dialog.getFirstResult()).getFullPath().toString();
            fProgText.setText(exefile);
        }
    }

	protected void handleBrowseLibrariesButtonSelected() {
	    String extensionRegexp = ".*\\.";
	    if (SWT.getPlatform().equals("win32"))
	        extensionRegexp += "dll";
	    else if (SWT.getPlatform().equals("carbon"))
            extensionRegexp += "dylib";
	    else
            extensionRegexp += "so";
	    ElementTreeSelectionDialog dialog
	        = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
	            new OmnetppLaunchUtils.FilteredWorkbenchContentProvider(extensionRegexp));
	    dialog.setTitle("Select Shared Libraries");
	    dialog.setMessage("Select the library file(s) you want to load at the beginning of the simulation.\n" +
	    		          "Multiple files can be selected.");
	    dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
	    dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
	    if (dialog.open() == IDialogConstants.OK_ID) {
	        String libfiles = "";
	        for (Object resource : dialog.getResult()) {
	            if (resource instanceof IFile)
                    libfiles += OmnetppLaunchUtils.makeRelativePathTo(((IFile)resource).getRawLocation(),
                            getWorkingDirectoryPath()).toString()+" ";
	        }
	        fLibraryText.setText(libfiles.trim());
	    }
    }
	
	protected void handleBrowseInifileButtonSelected() {
        ElementTreeSelectionDialog dialog
            = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
                                                         new OmnetppLaunchUtils.FilteredWorkbenchContentProvider(".*\\.ini"));
        dialog.setTitle("Select INI Files");
        dialog.setMessage("Select the initialization file(s) for the simulation.\n" +
        		          "Multiple files can be selected.");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
        if (dialog.open() == IDialogConstants.OK_ID) {
            String inifiles = "";
            for (Object resource : dialog.getResult()) {
                if (resource instanceof IFile)
                        inifiles += OmnetppLaunchUtils.makeRelativePathTo(((IFile)resource).getRawLocation(),
                                                       getWorkingDirectoryPath()).toString()+" ";
            }
            fInifileText.setText(inifiles.trim());
            updateConfigCombo();
        }
	}

    // working directory group handlers
    /**
     * Show a dialog that lets the user select a working directory from
     * the workspace
     */
    private void handleWorkspaceDirBrowseButtonSelected() {
        IContainer currentContainer= getContainer();
        if (currentContainer == null) {
            currentContainer = ResourcesPlugin.getWorkspace().getRoot();
        }
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), currentContainer, false, "Select a workspace relative working directory:");
        dialog.showClosedProjects(false);
        dialog.open();
        Object[] results = dialog.getResult();
        if (results != null && results.length > 0 && results[0] instanceof IPath) {
            IPath path = (IPath)results[0];
            String containerName = path.makeRelative().toString();
            setWorkingDirectoryText("${workspace_loc:" + containerName + "}");
        }
    }

    /**
     * Returns the selected workspace container,or <code>null</code>
     */
    protected IContainer getContainer() {
        String path = getWorkingDirectoryText();
        if (path.length() > 0) {
            IResource res = null;
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            if (path.startsWith("${workspace_loc:")) {
                IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
                try {
                    path = manager.performStringSubstitution(path, false);
                    IContainer[] containers = root.findContainersForLocation(new Path(path));
                    if (containers.length > 0) {
                        res = containers[0];
                    }
                }
                catch (CoreException e) {}
            }
            else {
                res = root.findMember(path);
            }
            if (res instanceof IContainer)
                return (IContainer)res;
        }
        return null;
    }

    /**
     * Runs when the working dir variables button has been selected
     */
    private void handleWorkingDirVariablesButtonSelected() {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variableText = dialog.getVariableExpression();
        if (variableText != null) {
            workingDirText.insert(variableText);
        }
    }

	// ********************************************************************
	// event listeners
	
    public void modifyText(ModifyEvent e) {
        if (e.getSource() == fProgText || e.getSource() == workingDirText) {
            updateConfigCombo();
        }
        updateLaunchConfigurationDialog();
    }
    
    // ********************************************************************
    // dialog UI control creation and layout
    public void createWorkingDirGroup(Composite parent, int colSpan) {
        Group group = SWTFactory.createGroup(parent, "Working directory", 3, colSpan, GridData.FILL_HORIZONTAL);
        setControl(group);
        workingDirText = SWTFactory.createSingleText(group, 1);
        workingDirText.addModifyListener(this);
        fWorkspaceButton = createPushButton(group, "Browse...", null);
        fWorkspaceButton.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
                handleWorkspaceDirBrowseButtonSelected();
        	}
        });
        fVariablesButton = createPushButton(group, "Variables...", null);
        fVariablesButton.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
                handleWorkingDirVariablesButtonSelected();
        	}
        });
    }

    protected void createSimulationGroup(Composite parent, int colSpan) {
        Composite comp = SWTFactory.createGroup(parent, "Simulation Program", 3, colSpan, GridData.FILL_HORIZONTAL);
        GridLayout ld = (GridLayout)comp.getLayout();
        ld.marginHeight = 1;

		SWTFactory.createLabel(comp, "Executable:",1);

		fProgText = SWTFactory.createSingleText(comp, 1);
		fProgText.addModifyListener(this);

		Button fBrowseForBinaryButton = SWTFactory.createPushButton(comp, "Browse...", null);
		fBrowseForBinaryButton.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent evt) {
				handleBinaryBrowseButtonSelected();
			}
		});

		SWTFactory.createLabel(comp, "Dynamic libraries:", 1);

        fLibraryText = SWTFactory.createSingleText(comp, 1);
        fLibraryText.setToolTipText("DLLs or shared libraries to load (without extension, relative to the working directory)");
        fLibraryText.addModifyListener(this);

        Button browseLibrariesButton = SWTFactory.createPushButton(comp, "Browse...", null);
        browseLibrariesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleBrowseLibrariesButtonSelected();
            }
        });

        SWTFactory.createLabel(comp, "NED Source Path:", 1);
        fNedPathText = SWTFactory.createSingleText(comp, 2);
        fNedPathText.setToolTipText("Specify the directories where NED files are read from (relative to the first selected INI file). " +
        		"If left empty, calculates the path automatically from the project settings.");
        fNedPathText.addModifyListener(this);
    }

    protected void createConfigurationGroup(Composite parent, int colSpan) {
		Composite comp = SWTFactory.createGroup(parent, "Configuration", 4, colSpan, GridData.FILL_HORIZONTAL);

        SWTFactory.createLabel(comp, "Initialization file(s):", 1);

        fInifileText = SWTFactory.createSingleText(comp, 2);
        fInifileText.setToolTipText("The INI file(s) defining parameters and configuration blocks (default: omnetpp.ini, relative to the working directory)");
        fInifileText.addModifyListener(this);

        Button browseInifileButton = SWTFactory.createPushButton(comp, "Browse...", null);
        browseInifileButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleBrowseInifileButtonSelected();
            }
        });

        SWTFactory.createLabel(comp, "Configuration name:",1);

        fConfigCombo = SWTFactory.createCombo(comp, SWT.BORDER | SWT.READ_ONLY, 3, new String[] {});
		fConfigCombo.setToolTipText("The configuration from the INI file that should be executed");
		fConfigCombo.setVisibleItemCount(10);
		fConfigCombo.addModifyListener(this);

		SWTFactory.createLabel(comp, "Run number:",1);

		int runSPan = debugLaunchMode ? 3 : 1;
        fRunText = SWTFactory.createSingleText(comp, runSPan);
        fRunText.addModifyListener(this);
        HoverSupport hover = new HoverSupport();
        hover.adapt(fRunText, new IHoverTextProvider() {
            public String getHoverTextFor(Control control, int x, int y, SizeConstraint outPreferredSize) {
                if (infoText == null)
                    infoText = LaunchPlugin.getSimulationRunInfo(config);
                outPreferredSize.preferredWidth = 350;
                return HoverSupport.addHTMLStyleSheet(DEFAULT_RUNTOOLTIP+"<pre>"+infoText+"</pre>");
            }
        });

        // parallel execution is not possible under CDT
        if (!debugLaunchMode) {
            SWTFactory.createLabel(comp, "Processes to run in parallel:", 1);
            fParallelismSpinner = new Spinner(comp, SWT.BORDER);
            fParallelismSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
            fParallelismSpinner.setMinimum(1);
            fParallelismSpinner.addModifyListener(this);
        }
    }

    protected void createUIGroup(Composite parent, int colSpan) {
    	SelectionAdapter selectionAdapter = new SelectionAdapter() {
    		@Override
    		public void widgetSelected(SelectionEvent e) {
    			updateUIGroup();
    	        updateLaunchConfigurationDialog();
    		}
    	};
    	
        Composite comp = SWTFactory.createComposite(parent, 6, colSpan, GridData.FILL_HORIZONTAL);

        SWTFactory.createLabel(comp, "User interface:", 1);

        fDefaultEnvButton = SWTFactory.createRadioButton(comp, "Default");
        fDefaultEnvButton.setLayoutData(new GridData());
        fDefaultEnvButton.setSelection(true);
        fDefaultEnvButton.addSelectionListener(selectionAdapter);

        fCmdEnvButton = SWTFactory.createRadioButton(comp, "Command line");
        fCmdEnvButton.setLayoutData(new GridData());
        fCmdEnvButton.addSelectionListener(selectionAdapter);

        fTkEnvButton = SWTFactory.createRadioButton(comp, "Tcl/Tk");
        fTkEnvButton.setLayoutData(new GridData());
        fTkEnvButton.addSelectionListener(selectionAdapter);

        fOtherEnvButton = SWTFactory.createRadioButton(comp, "Other:");
        fOtherEnvButton.setLayoutData(new GridData());
        fOtherEnvButton.addSelectionListener(selectionAdapter);

        fOtherEnvText = SWTFactory.createSingleText(comp, 1);
        fOtherEnvText.setToolTipText("Specify the custom environment name");
        fOtherEnvText.addModifyListener(this);
    }

    protected void createAdditionalGroup(Composite parent, int colSpan) {
        Composite comp = SWTFactory.createComposite(parent, 2, colSpan, GridData.FILL_HORIZONTAL);
        GridLayout ld = (GridLayout)comp.getLayout();
        ld.marginHeight = 1;

        SWTFactory.createLabel(comp, "Additional arguments:", 1);
        fAdditionalText = SWTFactory.createSingleText(comp, 1);
        fAdditionalText.setToolTipText("Specify additional command line arguments");
        fAdditionalText.addModifyListener(this);
    }

    protected void createOptionsGroup(Composite parent, int colSpan) {
        Composite mainComp =  SWTFactory.createComposite(parent, 3,colSpan,GridData.FILL_HORIZONTAL);
        GridLayout ld = (GridLayout)mainComp.getLayout();
        ld.marginHeight = 1;

        fShowDebugViewButton = SWTFactory.createCheckButton(mainComp, "Show Debug View on Launch", null, false, 3);
        // FIXME do we need it ????
        fShowDebugViewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
    }
}
