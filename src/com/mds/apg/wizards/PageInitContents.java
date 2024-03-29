/*
 * Copyright (C) 2010-2011 Mobile Developer Solutions
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mds.apg.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import java.io.File;

public final class PageInitContents extends WizardSection{
    
    // Set up storage for persistent initializers

    private final static String SOURCE_DIR = com.mds.apg.Activator.PLUGIN_ID + ".source"; 
    private final static String CONTENT_SELECTION = com.mds.apg.Activator.PLUGIN_ID + ".contentselection";
    private final static String PURE_IMPORT = com.mds.apg.Activator.PLUGIN_ID + ".pureimport";

    /** Last user-browsed location */
    private String mLocationCache;  
    private String mContentSelection;  // example, minimal, or user
    private boolean mPureImport;
    
    // widgets

    private Button mBrowseButton;
    private Label mLocationLabel;
    private Text mLocationPathField;
    Label mWithLabel;

    PageInitContents(AndroidPgProjectCreationPage wizardPage, Composite parent) {
        super(wizardPage);
        mLocationCache = doGetPreferenceStore().getString(SOURCE_DIR);  
        mContentSelection = doGetPreferenceStore().getString(CONTENT_SELECTION); 
        if (mContentSelection.equals("")) mContentSelection = "example";
        createGroup(parent);
    }
    
    /**
     * Creates the group for the Project options:
     * [radio] Use example source from phonegap directory
     * [radio] Create project from existing sources
     * Location [text field] [browse button]
     *
     * @param parent the parent composite
     */
    protected final void createGroup(Composite parent) {
        Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
        group.setLayout(new GridLayout(2, /* num columns */ false /* columns of not equal size */));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setFont(parent.getFont());
        group.setText("Project Contents");
        mWizardPage.mContentsSection = group; // Visibility can be adjusted by other widgets

        final Button createFromExampleRadio = new Button(group, SWT.RADIO);
        createFromExampleRadio.setText("Use PhoneGap example source as template for project");
        createFromExampleRadio.setSelection(mContentSelection.equals("example"));
        createFromExampleRadio.setToolTipText("Populate your project with the example shipped with your phonegap installation");
        
        // Label for showing the example is with JQM or Sencha
        mWithLabel = new Label(group, SWT.NONE);
        
        final Button minimalProject = new Button(group, SWT.RADIO);
        minimalProject.setText("Create minimal PhoneGap project");
        minimalProject.setToolTipText("Creates a minimal PhoneGap hello world");
        minimalProject.setSelection(mContentSelection.equals("minimal"));
        
        new Label(group, SWT.NONE); // dummy to force new line
        
        final Button existing_project_radio = new Button(group, SWT.RADIO);
        existing_project_radio.setText("Create project from specified source directory");
        existing_project_radio.setToolTipText("Specify root directory containing your sources that you wish to populate into the Android project"); 
        boolean doSetLocation = mContentSelection.equals("user");
        existing_project_radio.setSelection(doSetLocation);
        
        // Check box to do a pure import (versus adding in phonegap.js,etc. and making changes to it)

        final Button pureImport = new Button(group, SWT.CHECK);
        pureImport.setText("Pure Import");
        boolean initPure = doGetPreferenceStore().getString(PURE_IMPORT) != "";
        pureImport.setSelection(initPure);
        pureImport.setToolTipText("Disable any modifications to imported files. Use this to import an already working PhoneGap directory");
        pureImport.setVisible(doSetLocation);
        mPureImport = doSetLocation && initPure;
        
        SelectionListener location_listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                mContentSelection = createFromExampleRadio.getSelection() ? "example" : minimalProject.getSelection() ? "minimal" : "user";
                boolean pureImportVal = pureImport.getSelection();
                pureImport.setVisible(mContentSelection.equals("user"));
                doGetPreferenceStore().setValue(CONTENT_SELECTION, mContentSelection);
                mPureImport = pureImportVal && mContentSelection.equals("user");
                doGetPreferenceStore().setValue(PURE_IMPORT, mPureImport ? "true" : "");
                mWizardPage.validatePageComplete();
            }
        };

        createFromExampleRadio.addSelectionListener(location_listener);
        minimalProject.addSelectionListener(location_listener);
        existing_project_radio.addSelectionListener(location_listener);
        pureImport.addSelectionListener(location_listener);
        
        Composite location_group = new Composite(parent, SWT.NONE);
        location_group.setLayout(new GridLayout(3, /* num columns */
                false /* columns of not equal size */));
        location_group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        location_group.setFont(parent.getFont());

        mLocationLabel = new Label(location_group, SWT.NONE);
        mLocationLabel.setText("Location:");
        mLocationPathField = new Text(location_group, SWT.BORDER);  
        mLocationPathField.setText(getLocationSave());
        mBrowseButton = setupDirectoryBrowse(mLocationPathField, parent, location_group);
        enableLocationWidgets(doSetLocation);  
    }

    // --- Internal getters & setters ------------------

    @Override
    final String getValue() {
        return mLocationPathField == null ? "" : mLocationPathField.getText().trim(); //$NON-NLS-1$
    }
    
    @Override
    final Text getRawValue() {
        return mLocationPathField; 
    }
    
    @Override
    final String getLocationSave() {
        return mLocationCache;
    }
    
    @Override
    final void setLocationSave(String s) {
        mLocationCache = s;
    }

    /** Returns the value of the "Create from Existing Sample" radio. */
    /* TODO - Simplify like senchaChecked */
    
    protected String getContentSelection() {
        return mContentSelection;
    }
    
    protected boolean isPureImport() {
        return mPureImport;
    }
    
    void locationVisibility(boolean v) {
        mLocationLabel.setVisible(v);
        mLocationPathField.setVisible(v);
    }

    // --- UI Callbacks ----
    
    /**
     * Enables or disable the location widgets depending on the user selection:
     * the location path is enabled when using the "existing source" mode (i.e. not new project)
     * or in new project mode with the "use default location" turned off.
     */
    void enableLocationWidgets(boolean locationEnabled) {
        mLocationLabel.setEnabled(locationEnabled);
        mLocationPathField.setEnabled(locationEnabled);
        mBrowseButton.setVisible(locationEnabled);
    }
    
    protected void enableLocationWidgets() {
        enableLocationWidgets(mContentSelection.equals("user"));
    }
        
    /**
     * Validates the location path field.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    protected int validate() {
        File locationDir = new File(getValue());
        if (!locationDir.exists() || !locationDir.isDirectory()) {
            return mWizardPage.setStatus("Location: must be a valid directory", 
                AndroidPgProjectCreationPage.MSG_ERROR);
        } else {
            String[] l = locationDir.list();
            if (l.length == 0) {
                return mWizardPage.setStatus("Location: is empty. It should include the source to populate the project", 
                        AndroidPgProjectCreationPage.MSG_ERROR);
            }
            if (!mPureImport) { // assume user knows what he's doing if pureImport
                boolean foundIndexHtml = false;
                boolean foundSencha = false;
                boolean foundJqm = false;
                boolean foundPhonegapJs = false;

                for (String s : l) {
                    if (s.equals("index.html")) {
                        foundIndexHtml = true;
                    } else if (s.equals("sencha")) {
                        foundSencha = true;
                    } else if (s.equals("jquery.mobile")) {
                        foundJqm = true;
                    } else if (s.equals("phonegap.js")) {
                        foundPhonegapJs = true;
                    }
                }
                if (!foundIndexHtml) {
                    return mWizardPage.setStatus("Location: must include an index.html file", 
                            AndroidPgProjectCreationPage.MSG_ERROR);
                }
                if (foundSencha && mWizardPage.mSenchaDialog.senchaChecked()) {
                    return mWizardPage.setStatus("Location: can not include a sencha directory." +
                            " Uncheck \"Include Sencha ...\" if the Location directory already includes Sencha Touch", 
                            AndroidPgProjectCreationPage.MSG_ERROR);
                }
                if (foundJqm && mWizardPage.mJqmDialog.jqmChecked()) {
                    return mWizardPage.setStatus("Location: can not include a jquery.mobile directory." +
                            " Uncheck \"Include JQuery Mobile ...\" if the Location directory already includes it", 
                            AndroidPgProjectCreationPage.MSG_ERROR);
                }
                if (foundPhonegapJs) {
                    return mWizardPage.setStatus("Location: " + getValue() +
                            " cannot include a phonegap.js file.  It " +
                            "will be supplied from the phonegap-android installation",
                            AndroidPgProjectCreationPage.ERROR);
                }
            }
            // TODO more validation
            
            // We now have a good directory, so set example path and save value
            doGetPreferenceStore().setValue(SOURCE_DIR, getValue()); 
            
            return AndroidPgProjectCreationPage.MSG_NONE;
        }
    }
}