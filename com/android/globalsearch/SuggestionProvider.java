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

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fetches query results from contacts, applications and network-based Google Suggests to provide
 * search suggestions.
 */
public class SuggestionProvider extends ContentProvider {

    // set to true to enable the more verbose debug logging for this file
    private static final boolean DBG = false;
    private static final String TAG = "GlobalSearch";

    private static final String AUTHORITY = "com.android.globalsearch.SuggestionProvider";

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    // UriMatcher constants
    private static final int SEARCH_SUGGEST = 0;

    private Config mConfig;
    private SuggestionSources mSources;

    // Executes notifications from the SuggestionCursor on
    // the main event handling thread.
    private Handler mNotifyHandler;
    private ExecutorService mQueryExecutor;
    private ExecutorService mRefreshExecutor;

    private static ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            final Thread thread = new SuggestionThread(
                    r, "GlobalSearch #" + mCount.getAndIncrement());
            return thread;
        }
    };

    /**
     * Sets the thread priority to {@link Process#THREAD_PRIORITY_BACKGROUND}.
     */
    private static class SuggestionThread extends Thread {

        private SuggestionThread(Runnable runnable, String threadName) {
            super(runnable, threadName);
        }

        @Override
        public void run() {
            // take it easy on the UI thread
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            super.run();
        }
    }

    private SessionManager mSessionManager;

    public SuggestionProvider() {
    }

    @Override
    public boolean onCreate() {
        if (DBG) Log.d("SESSION", "SuggestionProvider.onCreate");
        mConfig = Config.getConfig(getContext());
        mSources = new SuggestionSources(getContext());
        mSources.load();

        mNotifyHandler = new Handler(Looper.getMainLooper());

        mQueryExecutor = new ThreadPoolExecutor(
                mConfig.getQueryThreadCorePoolSize(),
                mConfig.getQueryThreadMaxPoolSize(),
                mConfig.getThreadKeepaliveSeconds(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                sThreadFactory);
        
        mRefreshExecutor = new ThreadPoolExecutor(
                mConfig.getShortcutRefreshCorePoolSize(),
                mConfig.getShortcutRefreshMaxPoolSize(),
                mConfig.getThreadKeepaliveSeconds(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                sThreadFactory);

        mSessionManager = SessionManager.refreshSessionmanager(
                getContext(),
                mConfig,
                mSources,
                ClickLogger.getClickLogger(getContext()),
                ShortcutRepositoryImplLog.create(getContext(), mConfig),
                new PerTagExecutor(mQueryExecutor, mConfig.getPerSourceConcurrentQueryLimit()),
                mRefreshExecutor,
                mNotifyHandler);

        return true;
    }

    /**
     * This will always return {@link SearchManager#SUGGEST_MIME_TYPE} as this
     * provider is purely to provide suggestions.
     */
    @Override
    public String getType(Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    /**
     * Queries for a given search term and returns a cursor containing
     * suggestions ordered by best match.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        if (DBG) Log.d(TAG, "query(" + uri + ")");

        // Get the search text
        String query;
        if (uri.getPathSegments().size() > 1) {
            query = uri.getLastPathSegment().toLowerCase();
        } else {
            query = "";
        }

        switch (sUriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return mSessionManager.query(getContext(), query);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
                SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH_SUGGEST);
        return matcher;
    }
}
