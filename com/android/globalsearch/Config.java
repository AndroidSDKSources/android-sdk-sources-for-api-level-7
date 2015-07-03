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

import android.content.Context;

/**
 * This class provides values for configurable parameters in all of GlobalSearch.
 *
 * All the methods in this class return fixed default values. The subclass
 * {@link GservicesConfig} user server-side settable parameters.
 */
public class Config {

    private static final long DAY_MILLIS = 86400000L;

    // Default values
    private static final int NUM_PROMOTED_SOURCES = 4;
    private static final int MAX_RESULTS_TO_DISPLAY = 7;
    private static final int MAX_RESULTS_PER_SOURCE = 51 + MAX_RESULTS_TO_DISPLAY;
    private static final int WEB_RESULTS_OVERRIDE_LIMIT = 20;
    private static final long PROMOTED_SOURCE_DEADLINE_MILLIS = 6000;
    private static final long SOURCE_TIMEOUT_MILLIS = 10000;
    private static final long PREFILL_MILLIS = 400;

    private static final long MAX_STAT_AGE_MILLIS = 7 * DAY_MILLIS;
    private static final long MAX_SOURCE_EVENT_AGE_MILLIS = 30 * DAY_MILLIS;
    private static final int MIN_IMPRESSIONS_FOR_SOURCE_RANKING = 5;
    private static final int MIN_CLICKS_FOR_SOURCE_RANKING = 3;
    private static final int MAX_SHORTCUTS_RETURNED = 12;

    private static final int QUERY_THREAD_CORE_POOL_SIZE = NUM_PROMOTED_SOURCES;
    private static final int QUERY_THREAD_MAX_POOL_SIZE = QUERY_THREAD_CORE_POOL_SIZE + 2;
    private static final int SHORTCUT_REFRESH_CORE_POOL_SIZE = 3;
    private static final int SHORTCUT_REFRESH_MAX_POOL_SIZE = SHORTCUT_REFRESH_CORE_POOL_SIZE;
    private static final int THREAD_KEEPALIVE_SECONDS = 5;
    private static final int PER_SOURCE_CONCURRENT_QUERY_LIMIT = 3;

    protected Config() {
    }

    /**
     * Gets a configuration that can be updated remotely.
     */
    public static Config getConfig(Context context) {
        return new GservicesConfig(context);
    }

    /**
     * Gets the default configuration, which does not include remotely updated settings.
     */
    public static Config getDefaultConfig() {
        return new Config();
    }

    public int getNumPromotedSources() {
        return NUM_PROMOTED_SOURCES;
    }

    public int getMaxResultsToDisplay(){
        return MAX_RESULTS_TO_DISPLAY;
    }

    public int getMaxResultsPerSource(){
        return MAX_RESULTS_PER_SOURCE;
    }

    public int getWebResultsOverrideLimit(){
        return WEB_RESULTS_OVERRIDE_LIMIT;
    }

    public long getPromotedSourceDeadlineMillis(){
        return PROMOTED_SOURCE_DEADLINE_MILLIS;
    }

    public long getSourceTimeoutMillis(){
        return SOURCE_TIMEOUT_MILLIS;
    }

    public long getPrefillMillis(){
        return PREFILL_MILLIS;
    }

    public long getMaxStatAgeMillis(){
        return MAX_STAT_AGE_MILLIS;
    }

    public long getMaxSourceEventAgeMillis(){
        return MAX_SOURCE_EVENT_AGE_MILLIS;
    }

    public int getMinImpressionsForSourceRanking(){
        return MIN_IMPRESSIONS_FOR_SOURCE_RANKING;
    }

    public int getMinClicksForSourceRanking(){
        return MIN_CLICKS_FOR_SOURCE_RANKING;
    }

    public int getMaxShortcutsReturned(){
        return MAX_SHORTCUTS_RETURNED;
    }

    public int getQueryThreadCorePoolSize(){
        return QUERY_THREAD_CORE_POOL_SIZE;
    }

    public int getQueryThreadMaxPoolSize(){
        return QUERY_THREAD_MAX_POOL_SIZE;
    }

    public int getShortcutRefreshCorePoolSize(){
        return SHORTCUT_REFRESH_CORE_POOL_SIZE;
    }

    public int getShortcutRefreshMaxPoolSize(){
        return SHORTCUT_REFRESH_MAX_POOL_SIZE;
    }

    public int getThreadKeepaliveSeconds(){
        return THREAD_KEEPALIVE_SECONDS;
    }

    public int getPerSourceConcurrentQueryLimit(){
        return PER_SOURCE_CONCURRENT_QUERY_LIMIT;
    }

}
