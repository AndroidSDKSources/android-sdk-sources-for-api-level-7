/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package com.android.packageinstaller;

import com.android.packageinstaller.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This activity corresponds to a download progress screen that is displayed 
 * when the user tries
 * to install an application bundled as an apk file. The result of the application install
 * is indicated in the result code that gets set to the corresponding installation status
 * codes defined in PackageManager. If the package being installed already exists,
 * the existing package is replaced with the new one.
 */
public class InstallAppProgress extends Activity implements View.OnClickListener {
    private final String TAG="InstallAppProgress";
    private boolean localLOGV = false;
    private ApplicationInfo mAppInfo;
    private Uri mPackageURI;
    private ProgressBar mProgressBar;
    private View mOkPanel;
    private TextView mStatusTextView;
    private Button mDoneButton;
    private Button mLaunchButton;
    final static int SUCCEEDED = 1;
    final static int FAILED = 0;
    private final int INSTALL_COMPLETE = 1;
    private Intent mLaunchIntent;
    private File mTmpFile;
    private final String TMP_FILE_NAME="tmpCopy.apk";

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INSTALL_COMPLETE:
                    // Update the status text
                    mProgressBar.setVisibility(View.INVISIBLE);
                    // Show the ok button
                    int centerTextLabel;
                    Drawable centerTextDrawable = null;
                    if(msg.arg1 == SUCCEEDED) {
                        mLaunchButton.setVisibility(View.VISIBLE);
                        centerTextDrawable = getResources().getDrawable(R.drawable.button_indicator_finish);
                        centerTextLabel = R.string.install_done;
                        // Enable or disable launch button
                        mLaunchIntent = getPackageManager().getLaunchIntentForPackage(
                                mAppInfo.packageName);
                        if(mLaunchIntent != null) {
                            mLaunchButton.setOnClickListener(InstallAppProgress.this);
                        } else {
                            mLaunchButton.setEnabled(false);
                        }
                    } else {
                        centerTextDrawable = Resources.getSystem().getDrawable(
                                com.android.internal.R.drawable.ic_bullet_key_permission);
                        centerTextLabel = R.string.install_failed;
                        mLaunchButton.setVisibility(View.INVISIBLE);
                    }
                    if (centerTextDrawable != null) {
                    centerTextDrawable.setBounds(0, 0,
                            centerTextDrawable.getIntrinsicWidth(),
                            centerTextDrawable.getIntrinsicHeight());
                        mStatusTextView.setCompoundDrawables(centerTextDrawable, null, null, null);
                    }
                    mStatusTextView.setText(centerTextLabel);
                    mDoneButton.setOnClickListener(InstallAppProgress.this);
                    mOkPanel.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    };
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        mAppInfo = intent.getParcelableExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO);
        mPackageURI = intent.getData();
        initView();
    }
    
    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) {
            Message msg = mHandler.obtainMessage(INSTALL_COMPLETE);
            msg.arg1 = returnCode;
            mHandler.sendMessage(msg);
        }
    }

    public void initView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.op_progress);
        int installFlags = 0;
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(mAppInfo.packageName, 
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            if(pi != null) {
                installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
            }
        } catch (NameNotFoundException e) {
        }
        if((installFlags & PackageManager.INSTALL_REPLACE_EXISTING )!= 0) {
            Log.w(TAG, "Replacing package:" + mAppInfo.packageName);
        }
        PackageUtil.initSnippetForNewApp(this, mAppInfo,
                R.id.app_snippet, mPackageURI);
        mStatusTextView = (TextView)findViewById(R.id.center_text);
        mStatusTextView.setText(R.string.installing);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminate(true);
        // Hide button till progress is being displayed
        mOkPanel = (View)findViewById(R.id.buttons_panel);
        mDoneButton = (Button)findViewById(R.id.done_button);
        mLaunchButton = (Button)findViewById(R.id.launch_button);
        mOkPanel.setVisibility(View.INVISIBLE);

        // Create temp file before invoking install api
        mTmpFile = createTempPackageFile(mPackageURI.getPath());
        if (mTmpFile == null) {
            Message msg = mHandler.obtainMessage(INSTALL_COMPLETE);
            msg.arg1 = PackageManager.INSTALL_FAILED_INSUFFICIENT_STORAGE;
            mHandler.sendMessage(msg);
            return;
        }
        mPackageURI = Uri.parse("file://" + mTmpFile.getPath());
        String installerPackageName = getIntent().getStringExtra(
                Intent.EXTRA_INSTALLER_PACKAGE_NAME);
        
        PackageInstallObserver observer = new PackageInstallObserver();
        pm.installPackage(mPackageURI, observer, installFlags, installerPackageName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTmpFile != null && mTmpFile.exists()) {
            mTmpFile.delete();
        }
    }

    public void onClick(View v) {
        if(v == mDoneButton) {
            if (mAppInfo.packageName != null) {
                Log.i(TAG, "Finished installing "+mAppInfo.packageName);
            }
            finish();
        } else if(v == mLaunchButton) {
            startActivity(mLaunchIntent);
            finish();
        }
    }

    private File createTempPackageFile(String filePath) {
        File tmpPackageFile  = getFileStreamPath(TMP_FILE_NAME);
        if (tmpPackageFile == null) {
            Log.w(TAG, "Failed to create temp file");
            return null;
        }
        if (tmpPackageFile.exists()) {
            tmpPackageFile.delete();
        }
        // Open file to make it world readable
        FileOutputStream fos;
        try {
            fos = openFileOutput(TMP_FILE_NAME, MODE_WORLD_READABLE);
        } catch (FileNotFoundException e1) {
            Log.e(TAG, "Error opening file " + TMP_FILE_NAME);
            return null;
        }
        try {
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error opening file " + TMP_FILE_NAME);
            return null;
        }

        File srcPackageFile = new File(filePath);
        if (!FileUtils.copyFile(srcPackageFile, tmpPackageFile)) {
            Log.w(TAG, "Failed to make copy of file: " + srcPackageFile);
            return null;
        }
        return tmpPackageFile;
    }
}
