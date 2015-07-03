/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.globalsearch;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

/**
 * Logs clicks to an external content provider.
 * The click log provider is protected by a permission to avoid log stuffing,
 * and the click log provider must have a special permission to avoid log stealing.
 */
public class ClickLogger {

    private static final boolean DBG = false;
    private static final String TAG = "GlobalSearch.ClickLogger";

    private final Context mContext;

    private final ProviderInfo mLogReceiverInfo;

    private ClickLogger(Context context, ProviderInfo logReceiverInfo) {
        mContext = context;
        mLogReceiverInfo = logReceiverInfo;
    }

    /**
     * Logs a click.
     *
     * @param query Query as typed by the user.
     * @param clickPos The index of the clicked suggestion.
     * @param displayedSuggestions The suggestions that were displayed.
     * @param actionKey action key used to click the suggestion, or KeyEvent.KEYCODE_UNKNOWN.
     * @param actionMsg action message for the action key used, or {@code null}.
     */
    public void logClick(String query, int clickPos, List<SuggestionData> displayedSuggestions,
            int actionKey, String actionMsg) {
        int suggestionCount = displayedSuggestions.size();
        if (clickPos < 0 || clickPos >= suggestionCount) {
            Log.w(TAG, "Click out of range: " + clickPos + ", count: " + suggestionCount);
            return;
        }
        SuggestionData clicked = displayedSuggestions.get(clickPos);
        // Only log clicks on suggestions that come from the same package as the log receiver.
        if (!isSuggestionFromLogReceiver(clicked)) {
            return;
        }
        ContentValues row = new ContentValues();
        row.put(ClickLoggerContract.COL_QUERY, query);
        row.put(ClickLoggerContract.COL_POS, clickPos);
        String extraData = clicked.getIntentExtraData();
        if (extraData != null) {
            row.put(ClickLoggerContract.COL_EXTRA_DATA, extraData);
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            row.put(ClickLoggerContract.COL_ACTION_KEY, actionKey);
        }
        if (actionMsg != null) {
            row.put(ClickLoggerContract.COL_ACTION_MSG, actionMsg);
        }
        StringBuilder slots = new StringBuilder();
        for (int i = 0; i < suggestionCount; i++) {
            SuggestionData suggestion = displayedSuggestions.get(i);
            slots.append(getSlotInfo(suggestion));
            if (i < suggestionCount - 1) {
                slots.append(",");
            }
        }
        row.put(ClickLoggerContract.COL_SLOTS, slots.toString());
        try {
            if (DBG) Log.d(TAG, "insert(" + ClickLoggerContract.CLICK_LOG_URI + "," + row + ")");
            mContext.getContentResolver().insert(ClickLoggerContract.CLICK_LOG_URI, row);
        } catch (RuntimeException ex) {
            // Guard against buggy logger implementations
            Log.e(TAG, "Failed to log click: " + ex);
        }
    }

    /**
     * Checks whether the given suggestion comes from the same application as the one
     * hosting the click log receiver.
     */
    private boolean isSuggestionFromLogReceiver(SuggestionData clicked) {
        String packageName = clicked.getSource().getPackageName();
        String receiverPackage = mLogReceiverInfo.applicationInfo.packageName;
        return packageName != null && packageName.equals(receiverPackage);
    }

    /**
     * Gets the information that we will log for each displayed suggestion.
     */
    private String getSlotInfo(SuggestionData suggestion) {
        if (SuggestionFactoryImpl.BUILTIN_SOURCE_COMPONENT.equals(suggestion.getSource())) {
            return ClickLoggerContract.TYPE_BUILTIN;
        } else if (isSuggestionFromLogReceiver(suggestion)) {
            // This is a bit of a hack, we assume that the app that handles log requests is
            // the web suggestion source.
            return ClickLoggerContract.TYPE_WEB;
        } else {
            return ClickLoggerContract.TYPE_OTHER;
        }
    }

    /**
     * Gets a click logger, if clicks should be logged.
     *
     * @param context
     * @return A click logger, or {@code null} if clicks should not be logged.
     */
    public static ClickLogger getClickLogger(Context context) {
        PackageManager pm = context.getPackageManager();
        ProviderInfo providerInfo =
                pm.resolveContentProvider(ClickLoggerContract.LOG_AUHTORITY, 0);
        if (providerInfo == null) {
            // This is not an error, since the platform may not include a click logger
            if (DBG) Log.d(TAG, "No provider found for " + ClickLoggerContract.LOG_AUHTORITY);
            return null;
        }
        String providerPackage = providerInfo.applicationInfo.packageName;
        // Check if the content provider has permission to receive click log data
        if (pm.checkPermission(ClickLoggerContract.PERMISSION_RECEIVE_GLOBALSEARCH_LOG,
                providerPackage) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Package " + providerPackage + " does not have permission "
                    + ClickLoggerContract.PERMISSION_RECEIVE_GLOBALSEARCH_LOG);
            return null;
        }
        return new ClickLogger(context, providerInfo);
    }

}
