/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.winrun4j.eclipse;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class WExportWizardPage extends WizardPage
{
    private static final String PAGE_NAME = "WinRun4JExportWizardPage";
    private static final String SETTING_LAUNCH_CONFIG = "WinRun4J.export.launchConfig";
    private static final String SETTING_LAUNCHER_NAME = "WinRun4J.export.launcherName";
    private static final String SETTING_OUTPUTDIR = "WinRun4J.export.outputDir";
    private static final String SETTING_OUTPUTDIR_LIST = "WinRun4J.export.outputDirList";
    private static final String SETTING_ICON_LIST = "WinRun4J.export.iconList";
    private static final String SETTING_ICON = "WinRun4J.export.icon";
    private static final String SETTING_LAUNCHER_TYPE = "WinRun4J.export.launcherType";
    private Map launchConfigs = new TreeMap();

    // UI elements
    private Combo outputDirectoryCombo;
    private Combo launchConfigurationCombo;
    private Combo launcherIconCombo;
    private Button launcherTypeRadioStandard;
    private Button launcherTypeRadioSingle;
    private Text launcherNameText;

    // Model data
    private ILaunchConfiguration launchConfig;
    private File launcherFile;
    private File launcherIcon;

    protected WExportWizardPage() {
        super(PAGE_NAME);
        setTitle(WMessages.exportWizardPage_title);
        setDescription(WMessages.exportWizardPage_description);
    }

    public ILaunchConfiguration getLaunchConfig() {
        return launchConfig;
    }

    public File getLauncherFile() {
        return launcherFile;
    }

    public File getLauncherIcon() {
        return launcherIcon;
    }

    public boolean isStandardLauncher() {
        return launcherTypeRadioStandard.getSelection();
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        // Launch config
        Label lcl = new Label(composite, SWT.NULL);
        lcl.setText("Launch Configuration:");
        GridHelper.setHorizontalSpan(lcl, 2);
        launchConfigurationCombo = new Combo(composite, SWT.NULL);
        launchConfigurationCombo.setItems(findLaunchConfigurations());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        launchConfigurationCombo.setLayoutData(gd);
        launchConfigurationCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateSelectedLaunchConfiguration();
                setPageComplete(isPageComplete());
            }
        });

        // Launcher name
        Label lnl = new Label(composite, SWT.NULL);
        lnl.setText("Launcher Name:");
        GridHelper.setHorizontalSpan(lcl, 2);
        launcherNameText = new Text(composite, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        launcherNameText.setLayoutData(gd);
        launcherNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateLauncherFile();
                setPageComplete(isPageComplete());
            }
        });

        // Output dir
        Label odl = new Label(composite, SWT.NULL);
        odl.setText("Output Directory:");
        GridHelper.setHorizontalSpan(odl, 2);
        outputDirectoryCombo = new Combo(composite, SWT.NULL);
        outputDirectoryCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateLauncherFile();
                setPageComplete(isPageComplete());
            }
        });
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        outputDirectoryCombo.setLayoutData(gd);
        Button odcb = new Button(composite, SWT.PUSH);
        odcb.setText("Browse...");
        odcb.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                browseForOutputDirectory();
            }
        });

        // Icon
        Label il = new Label(composite, SWT.NULL);
        il.setText("Launcher Icon:");
        GridHelper.setHorizontalSpan(il, 2);
        launcherIconCombo = new Combo(composite, SWT.NULL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        launcherIconCombo.setLayoutData(gd);
        launcherIconCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateLauncherIconFile();
                setPageComplete(isPageComplete());
            }
        });
        Button ib = new Button(composite, SWT.PUSH);
        ib.setText("Browse...");
        ib.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                browseForLauncherIcon();
            }
        });

        // Launcher type
        Label ltl = new Label(composite, SWT.NULL);
        ltl.setText("Launcher Type:");
        GridHelper.setHorizontalSpan(ltl, 2);
        launcherTypeRadioStandard = new Button(composite, SWT.RADIO);
        launcherTypeRadioStandard.setText("Standard");
        launcherTypeRadioStandard.setSelection(true);
        GridHelper.setHorizontalSpan(launcherTypeRadioStandard, 2);
        launcherTypeRadioSingle = new Button(composite, SWT.RADIO);
        launcherTypeRadioSingle.setText("Fat Executable (with jars embedded)");
        GridHelper.setHorizontalSpan(launcherTypeRadioSingle, 2);

        Dialog.applyDialogFont(composite);
        setControl(composite);

        // Use IDialogSettings to remember selections/dir/file history
        loadSettings();
    }

    protected void updateLauncherIconFile() {
        String ln = launcherIconCombo.getText();
        if (ln == null || "".equals(ln)) {
            launcherIcon = null;
            return;
        }

        launcherIcon = new File(ln);
    }

    protected void updateLauncherFile() {
        String ln = launcherNameText.getText();
        String od = outputDirectoryCombo.getText();
        if (ln == null || "".equals(ln) || od == null || "".equals(od)) {
            this.launcherFile = null;
            return;
        }
        File odf = new File(od);
        this.launcherFile = new File(odf, ln);
    }

    protected void updateSelectedLaunchConfiguration() {
        this.launchConfig = (ILaunchConfiguration) launchConfigs.get(UIHelper
                .getSelection(launchConfigurationCombo));
        if (launchConfig == null) {
            this.launcherNameText.setText("");
            this.launchConfig = null;
            return;
        }
        String ln = launchConfig.getName();
        int idx = ln.indexOf('(');
        if (idx != -1) {
            ln = ln.substring(0, idx - 1);
        }
        ln += ".exe";
        this.launcherNameText.setText(ln);
    }

    protected void browseForLauncherIcon() {
        FileDialog fd = new FileDialog(getShell());
        fd.setFilterExtensions(new String[] { "*.ico" });
        fd.setText("Launcher Icon File");
        String fn = fd.open();
        if (fn == null)
            fn = "";
        this.launcherIconCombo.setText(fn);
    }

    protected void browseForOutputDirectory() {
        DirectoryDialog dd = new DirectoryDialog(getShell());
        dd.setText("Launcher Output Directory");
        dd.setMessage("Select a directory to generate the launcher into");
        String ds = dd.open();
        if (ds == null)
            ds = "";
        this.outputDirectoryCombo.setText(ds);
    }

    private String[] findLaunchConfigurations() {
        try {
            launchConfigs.clear();
            ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
            ILaunchConfigurationType type = manager
                    .getLaunchConfigurationType(IWLaunchConfigurationConstants.TYPE);
            ILaunchConfiguration[] launchConfigs = manager.getLaunchConfigurations(type);

            for (int i = 0; i < launchConfigs.length; i++) {
                ILaunchConfiguration launchConfig = launchConfigs[i];
                if (!launchConfig.getAttribute(IDebugUIConstants.ATTR_PRIVATE, false)) {
                    String projectName = launchConfig.getAttribute(
                            IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$                    
                    this.launchConfigs.put(
                            launchConfig.getName() + " - " + projectName, launchConfig); //$NON-NLS-1$
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
            // FIXME
        }

        return (String[]) launchConfigs.keySet().toArray(new String[0]);
    }

    public boolean isPageComplete() {
        setMessage(null);
        if (launchConfigurationCombo.getText().equals("")) {
            setMessage("Select a 'WinRun4J Application' launch configuration", WARNING);
            return false;
        }
        if (launcherNameText.getText().equals("")) {
            setMessage(
                    "Specify a launcher name. This is the name of the executable that will be generated.",
                    WARNING);
            return false;
        }
        if (outputDirectoryCombo.getText().equals("")) {
            setMessage(
                    "Specify an output directory. This is where the launcher will be generated.",
                    WARNING);
            return false;
        }
        return this.launchConfig != null && this.launcherFile != null;
    }

    public void saveSettings() {
        IDialogSettings id = getDialogSettings();
        if (id == null)
            return;
        id.put(SETTING_LAUNCH_CONFIG, launchConfigurationCombo.getText());
        id.put(SETTING_LAUNCHER_NAME, launcherNameText.getText());
        id.put(SETTING_OUTPUTDIR, outputDirectoryCombo.getText());
        updateSettingList(id, SETTING_OUTPUTDIR_LIST, outputDirectoryCombo.getText());
        id.put(SETTING_ICON, launcherIconCombo.getText());
        updateSettingList(id, SETTING_ICON_LIST, launcherIconCombo.getText());
        id.put(SETTING_LAUNCHER_TYPE, launcherTypeRadioSingle.getSelection());
    }

    public void updateSettingList(IDialogSettings id, String setting, String value) {
        String[] a = id.getArray(setting);
        if (a == null)
            a = new String[0];
        TreeSet hs = new TreeSet(Arrays.asList(a));
        hs.add(value);
        id.put(setting, (String[]) hs.toArray(new String[hs.size()]));
    }

    public void loadSettings() {
        IDialogSettings id = getDialogSettings();
        if (id == null)
            return;
        String slc = id.get(SETTING_LAUNCH_CONFIG);
        if (slc != null)
            UIHelper.select(launchConfigurationCombo, slc);
        String sln = id.get(SETTING_LAUNCHER_NAME);
        if (sln != null)
            launcherNameText.setText(sln);
        String[] sol = id.getArray(SETTING_OUTPUTDIR_LIST);
        if (sol != null)
            outputDirectoryCombo.setItems(sol);
        UIHelper.select(outputDirectoryCombo, id.get(SETTING_OUTPUTDIR));
        String[] sil = id.getArray(SETTING_ICON_LIST);
        if (sil != null)
            launcherIconCombo.setItems(sil);
        UIHelper.select(launcherIconCombo, id.get(SETTING_ICON));
        if (id.getBoolean(SETTING_LAUNCHER_TYPE)) {
            launcherTypeRadioSingle.setSelection(true);
            launcherTypeRadioStandard.setSelection(false);
        }
    }

}