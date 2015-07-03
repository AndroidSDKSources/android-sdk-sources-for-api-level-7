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

import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Imposes a concurrency limit on each tag, queueing up at most one pending command to run.  If more
 * commands are executed while one is already pending, the pending command is dropped on the floor
 * and replaced by the most recent.
 *
 * Because some commands may be dropped, this is only appropriate for cases where only the most
 * recent command for a particular tag needs to be run.
 */
public class PerTagExecutor {

    static private final String TAG = "GlobalSearch";
    static private final boolean DBG = false;

    private final Executor mExecutor;
    private final int mLimit;
    private final HashMap<String, Limiter> mTagInfos = new HashMap<String, Limiter>();

    /**
     * @param executor Used to run the commands.
     * @param maxConcurrentPerTag The maximum concurrent commands that are allowed to run per tag.
     */
    public PerTagExecutor(Executor executor, int maxConcurrentPerTag) {
        mExecutor = executor;
        mLimit = maxConcurrentPerTag;
    }

    /**
     * Executes the command only if there is nothing already running associated with the tag.
     *
     * Otherwise, it adds itself as the pending command to run once the current running command
     * finishes.  This will drop any other pending command for tag on the floor.
     *
     * @param tag The tag.
     * @param command The command.
     * @return Whether the command was queued in the pending slot.
     */
    public boolean execute(String tag, final Runnable command) {
        final Limiter limiter = getLimiter(tag);
        return limiter.run(mExecutor, command);
    }

    private synchronized Limiter getLimiter(String tag) {
        Limiter ti = mTagInfos.get(tag);
        if (ti == null) {
            ti = new Limiter(tag, mLimit);
            mTagInfos.put(tag, ti);
        }
        return ti;
    }

    /**
     * Book keeping per tag.
     */
    private static class Limiter {
        private final String mTag;
        private final int mLimit;
        private int mRunning = 0;
        private Runnable mPending;

        Limiter(String tag, int limit) {
            mTag = tag;
            mLimit = limit;
        }

        public synchronized boolean run(final Executor executor, final Runnable command) {
            if (DBG) Log.d(TAG, mTag + ": " + "run()");
            if (mRunning == mLimit) {
                if (DBG) Log.d(TAG, mTag + ": " + "at limit " + mLimit + ", updating pending");
                mPending = command;
                return true;
            } else if (mRunning > mLimit) {
                Log.w(TAG, "somehow have a running count (" + mRunning + ") greater than " 
                        + "the limit (" + mLimit + ")");
                mRunning = mLimit;
                mPending = command;
                return true;
            }
            boolean accepted = execute(executor, command);
            // if the executor rejected the task, pretend that it was stored as pending.
            // it will just never get run.
            return !accepted;
        }

        /**
         * Posts the command to the wrapped executor.
         *
         * @return {@code true} if the executor accepted the command.
         */
        private synchronized boolean execute(final Executor executor, final Runnable command) {
            if (DBG) Log.d(TAG, mTag + ": " + "running");
            mRunning++;
            try {
                executor.execute(new Runnable() {
                    public void run() {
                        try {
                            command.run();
                        } finally {
                            doneRunning(executor);
                        }
                    }
                });
                return true;
            } catch (RejectedExecutionException ex) {
                Log.w(TAG, mTag + ": Rejected by the executor.");
                return false;
            }
        }

        // package private to be more efficiently callable from the inner class in execute()
        synchronized void doneRunning(Executor executor) {
            if (mRunning <= 0) {
                Log.w(TAG, "PerTagExecutor: how can i be done running if I'm not "
                        + "running already :-/");
                mRunning = 1;
            }

            if (DBG) Log.d(TAG, mTag + ": " + "doneRunning()");
            mRunning--;
            if (mPending != null) {
                if (DBG) Log.d(TAG, mTag + ": " + "running pending command");
                Runnable toRun = mPending;
                mPending = null;
                execute(executor, toRun);
            }
        }
    }
}
