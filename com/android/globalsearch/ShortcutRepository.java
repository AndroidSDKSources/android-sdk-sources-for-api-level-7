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

import android.content.ComponentName;

import java.util.ArrayList;

/**
 * Holds information about shortcuts (results the user has clicked on before), and returns
 * appropriate shortcuts for a given query.
 */
public interface ShortcutRepository {

    /**
     * Checks whether there is any stored history.
     */
    boolean hasHistory();

    /**
     * Clears all shortcut history.
     */
    void clearHistory();

    /**
     * Deletes any database files and other resources used by the repository.
     * This is not necessary to clear the history, and is mostly useful
     * for unit tests.
     */
    void deleteRepository();

    /**
     * Closes any database connections etc held by this object.
     */    
    void close();

    /**
     * Used to Report the stats about a completed {@link SuggestionSession}.
     *
     * @param stats The stats.
     */
    void reportStats(SessionStats stats);

    /**
     * @param query The query.
     * @return A list short-cutted results for the query.
     */
    ArrayList<SuggestionData> getShortcutsForQuery(String query);

    /**
     * @return A ranking of suggestion sources based on clicks and impressions.
     */
    ArrayList<ComponentName> getSourceRanking();

    /**
     * Refreshes a shortcut.
     *
     * @param source Identifies the source of the shortcut.
     * @param shortcutId Identifies the shortcut.
     * @param refreshed An up to date shortcut, or <code>null</code> if the shortcut should be
     *   removed.
     */
    void refreshShortcut(ComponentName source, String shortcutId, SuggestionData refreshed);

}
