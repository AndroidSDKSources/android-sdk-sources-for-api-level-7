/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.properties;

import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.project.ApkSettings;
import com.android.sdkuilib.internal.widgets.SdkTargetSelector;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page for "Android" project.
 * This is accessible from the Package Explorer when right clicking a project and choosing
 * "Properties".
 *
 */
public class AndroidPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

    private IProject mProject;
    private SdkTargetSelector mSelector;
    // APK-SPLIT: This is not yet supported, so we hide the UI
//    private Button mSplitByDensity;

    public AndroidPropertyPage() {
        // pass
    }

    @Override
    protected Control createContents(Composite parent) {
        // get the element (this is not yet valid in the constructor).
        mProject = (IProject)getElement();

        // get the targets from the sdk
        IAndroidTarget[] targets = null;
        if (Sdk.getCurrent() != null) {
            targets = Sdk.getCurrent().getTargets();
        }

        // build the UI.
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        top.setLayout(new GridLayout(1, false));

        Label l = new Label(top, SWT.NONE);
        l.setText("Project Build Target");

        mSelector = new SdkTargetSelector(top, targets);

        /*
         * APK-SPLIT: This is not yet supported, so we hide the UI
        Group g = new Group(top, SWT.NONE);
        g.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        g.setLayout(new GridLayout(1, false));
        g.setText("APK Generation");

        mSplitByDensity = new Button(g, SWT.CHECK);
        mSplitByDensity.setText("One APK per density");

*/
        // fill the ui
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null && mProject.isOpen()) {
            // get the target
            IAndroidTarget target = currentSdk.getTarget(mProject);
            if (target != null) {
                mSelector.setSelection(target);
            }

            /*
             * APK-SPLIT: This is not yet supported, so we hide the UI
            // get the project settings
            ApkSettings settings = currentSdk.getApkSettings(mProject);
            mSplitByDensity.setSelection(settings.isSplitByDpi());
            */
        }

        mSelector.setSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // look for the selection and validate the page if there is a selection
                IAndroidTarget target = mSelector.getSelected();
                setValid(target != null);
            }
        });

        if (mProject.isOpen() == false) {
            // disable the ui.
        }

        return top;
    }

    @Override
    public boolean performOk() {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            ApkSettings apkSettings = new ApkSettings();
            /*
             * APK-SPLIT: This is not yet supported, so we hide the UI
            apkSettings.setSplitByDensity(mSplitByDensity.getSelection());
             */

            currentSdk.setProject(mProject, mSelector.getSelected(), apkSettings);
        }

        return true;
    }
}
