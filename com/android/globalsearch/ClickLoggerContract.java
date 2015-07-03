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

import android.content.ContentResolver;
import android.net.Uri;

/**
 * URIs, columns, permissions and other constants for talking to the click log provider.
 */
public class ClickLoggerContract {

    /**
     * The content provider authority for the click log provider.
     */
    public static final String LOG_AUHTORITY = "com.android.globalsearch.log";

    /**
     * Content URI path for logging clicks.
     */
    public static final String CLICKS_PATH = "clicks";

    /**
     * Content URI that click log requests should be inserted at.
     */
    public static final Uri CLICK_LOG_URI =
            Uri.parse("content://" + LOG_AUHTORITY + "/" + CLICKS_PATH);

    /**
     * Media type of {@link #CLICK_LOG_URI}.
     */
    public static final String CLICKS_MIME_TYPE =
        ContentResolver.CURSOR_DIR_BASE_TYPE + "/com.android.globalsearch.log.click";

    /**
     * Permission that the application that hosts the click log provider must have in order
     * to receive click log requests.
     */
    public static final String PERMISSION_RECEIVE_GLOBALSEARCH_LOG =
            "com.android.globalsearch.permission.RECEIVE_GLOBALSEARCH_LOG";

    /**
     * The query as typed by the user.
     * Column for inserts on {@link #CLICK_LOG_URI}.
     */
    public static final String COL_QUERY = "q";

    /**
     * The position of the clicked suggestion among all displayed suggestions.
     * Column for inserts on {@link #CLICK_LOG_URI}.
     */
    public static final String COL_POS = "pos";

    /**
     * The value (if any) of {@link android.app.SearchManager#SUGGEST_COLUMN_INTENT_EXTRA_DATA}
     * for the suggestion that was clicked.
     * Column for inserts on {@link #CLICK_LOG_URI}.
     */
    public static final String COL_EXTRA_DATA = "extra";

    /**
     * The action key (if any) for the action key that was used to launch the suggestion.
     * Column for inserts on {@link #CLICK_LOG_URI}.
     */
    public static final String COL_ACTION_KEY = "actionKey";

    /**
     * The action key message (if any) for the action key that was used to launch the suggestion.
     * Column for inserts on {@link #CLICK_LOG_URI}.
     */
    public static final String COL_ACTION_MSG = "actionMsg";

    /**
     * A comma-separated list of suggestion types, one for suggestion that was displayed
     * when the suggestion was launched.
     * Column for inserts on {@link #CLICK_LOG_URI}.
     */
    public static final String COL_SLOTS = "slots";

    /**
     * Suggestion from the web search provider.
     * This is a suggestion type for {@link #COL_SLOTS}.
     */
    public static final String TYPE_WEB = "0";

    /**
     * Built-in GlobalSearch suggestion (e.g. "Go to web site").
     * This is a suggestion type for {@link #COL_SLOTS}.
     */
    public static final String TYPE_BUILTIN = "1";

    /**
     * All other suggestions (e.g. Contacts, Apps, Music, third party suggestions).
     * This is a suggestion type for {@link #COL_SLOTS}.
     */
    public static final String TYPE_OTHER = "2";

}
