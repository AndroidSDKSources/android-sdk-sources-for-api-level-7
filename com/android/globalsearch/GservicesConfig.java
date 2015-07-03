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
import android.content.Context;
import android.provider.Settings.Gservices;

/**
 * Server-side settable parameters for GlobalSearch.
 */
public class GservicesConfig extends Config {

    private ContentResolver mResolver;

    public GservicesConfig(Context context) {
        mResolver = context.getContentResolver();
    }

    private int getInt(String name, int defValue) {
        return Gservices.getInt(mResolver, name, defValue);
    }

    private long getLong(String name, long defValue) {
        return Gservices.getLong(mResolver, name, defValue);
    }

    @Override
    public int getNumPromotedSources() {
        return getInt(Gservices.SEARCH_NUM_PROMOTED_SOURCES,
                super.getNumPromotedSources());
    }

    @Override
    public int getMaxResultsToDisplay(){
        return getInt(Gservices.SEARCH_MAX_RESULTS_TO_DISPLAY,
                super.getMaxResultsToDisplay());
    }

    @Override
    public int getMaxResultsPerSource(){
        return getInt(Gservices.SEARCH_MAX_RESULTS_PER_SOURCE,
                super.getMaxResultsPerSource());
    }

    @Override
    public int getWebResultsOverrideLimit(){
        return getInt(Gservices.SEARCH_WEB_RESULTS_OVERRIDE_LIMIT,
                super.getWebResultsOverrideLimit());
    }

    @Override
    public long getPromotedSourceDeadlineMillis(){
        return getLong(Gservices.SEARCH_PROMOTED_SOURCE_DEADLINE_MILLIS,
                super.getPromotedSourceDeadlineMillis());
    }

    @Override
    public long getSourceTimeoutMillis(){
        return getLong(Gservices.SEARCH_SOURCE_TIMEOUT_MILLIS,
                super.getSourceTimeoutMillis());
    }

    @Override
    public long getPrefillMillis(){
        return getLong(Gservices.SEARCH_PREFILL_MILLIS,
                super.getPrefillMillis());
    }

    @Override
    public long getMaxStatAgeMillis(){
        return getLong(Gservices.SEARCH_MAX_STAT_AGE_MILLIS,
                super.getMaxStatAgeMillis());
    }

    @Override
    public long getMaxSourceEventAgeMillis(){
        return getLong(Gservices.SEARCH_MAX_SOURCE_EVENT_AGE_MILLIS,
                super.getMaxSourceEventAgeMillis());
    }

    @Override
    public int getMinImpressionsForSourceRanking(){
        return getInt(Gservices.SEARCH_MIN_IMPRESSIONS_FOR_SOURCE_RANKING,
                super.getMinImpressionsForSourceRanking());
    }

    @Override
    public int getMinClicksForSourceRanking(){
        return getInt(Gservices.SEARCH_MIN_CLICKS_FOR_SOURCE_RANKING,
                super.getMinClicksForSourceRanking());
    }

    @Override
    public int getMaxShortcutsReturned(){
        return getInt(Gservices.SEARCH_MAX_SHORTCUTS_RETURNED,
                super.getMaxShortcutsReturned());
    }

    @Override
    public int getQueryThreadCorePoolSize(){
        return getInt(Gservices.SEARCH_QUERY_THREAD_CORE_POOL_SIZE,
                super.getQueryThreadCorePoolSize());
    }

    @Override
    public int getQueryThreadMaxPoolSize(){
        return getInt(Gservices.SEARCH_QUERY_THREAD_MAX_POOL_SIZE,
                super.getQueryThreadMaxPoolSize());
    }

    @Override
    public int getShortcutRefreshCorePoolSize(){
        return getInt(Gservices.SEARCH_SHORTCUT_REFRESH_CORE_POOL_SIZE,
                super.getShortcutRefreshCorePoolSize());
    }

    @Override
    public int getShortcutRefreshMaxPoolSize(){
        return getInt(Gservices.SEARCH_SHORTCUT_REFRESH_MAX_POOL_SIZE,
                super.getShortcutRefreshMaxPoolSize());
    }

    @Override
    public int getThreadKeepaliveSeconds(){
        return getInt(Gservices.SEARCH_THREAD_KEEPALIVE_SECONDS,
                super.getThreadKeepaliveSeconds());
    }

    @Override
    public int getPerSourceConcurrentQueryLimit(){
        return getInt(Gservices.SEARCH_PER_SOURCE_CONCURRENT_QUERY_LIMIT,
                super.getPerSourceConcurrentQueryLimit());
    }

}
