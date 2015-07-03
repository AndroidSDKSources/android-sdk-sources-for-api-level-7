/*
 * Copyright (C) The Android Open Source Project
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

import android.test.suitebuilder.annotation.SmallTest;

import java.util.LinkedList;
import java.util.concurrent.Executor;

import junit.framework.TestCase;

/**
 * Tests for {@link PerTagExecutor}.
 */
@SmallTest
public class PerTagExecutorTest extends TestCase {

    private MockExecutor mExecutor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mExecutor = new MockExecutor();
    }

    public void testLimit() throws Exception {
        PerTagExecutor tagExecutor = new PerTagExecutor(mExecutor, 2);

        TRunnable a1 = new TRunnable();
        TRunnable a2 = new TRunnable();
        TRunnable a3 = new TRunnable();

        TRunnable b1 = new TRunnable();

        assertFalse(tagExecutor.execute("a", a1));
        assertFalse(tagExecutor.execute("a", a2));
        assertTrue(tagExecutor.execute("a", a3));
        assertFalse(tagExecutor.execute("b", b1));

        mExecutor.runNext(); // run a1, will trigger a3 to be runnable
        assertTrue("a1 should have been run", a1.hasRun());
        assertFalse("a2 should not have been run yet", a2.hasRun());
        assertFalse("a3 should not have been run yet", a3.hasRun());
        assertFalse("b1 should not have been run yet", b1.hasRun());
        mExecutor.runNext(); // run a2
        assertTrue("a2 should have been run", a2.hasRun());
        assertFalse("a3 should not have been run yet", a3.hasRun());
        assertFalse("b1 should not have been run yet", b1.hasRun());
        mExecutor.runNext(); // run b1
        assertTrue("b1 should have been run", b1.hasRun());
        assertFalse("a3 should not have been run yet", a3.hasRun());
        mExecutor.runNext(); // run a3
        assertTrue("a3 should have been run", a3.hasRun());
    }

    public void testPendingRuns() throws Exception {
        PerTagExecutor tagExecutor = new PerTagExecutor(mExecutor, 2);

        TRunnable a1 = new TRunnable();
        TRunnable a2 = new TRunnable();
        TRunnable a3 = new TRunnable();

        tagExecutor.execute("a", a1);
        tagExecutor.execute("a", a2);
        tagExecutor.execute("a", a3);

        mExecutor.runNext(); // run a1, will trigger a3 to be runnable
        assertTrue("a1 should have been run", a1.hasRun());
        assertFalse("a2 should not have been run yet", a2.hasRun());
        assertFalse("a3 should not have been run yet", a3.hasRun());
        mExecutor.runNext(); // run a2
        assertTrue("a2 should have been run", a2.hasRun());
        assertFalse("a3 should not have been run yet", a3.hasRun());
        mExecutor.runNext(); // run a3
        assertTrue("a3 should have been run", a3.hasRun());
    }

    public void testPendingRuns_intermediateDropped() throws Exception {
        PerTagExecutor tagExecutor = new PerTagExecutor(mExecutor, 2);

        TRunnable a1 = new TRunnable();
        TRunnable a2 = new TRunnable();
        TRunnable a3 = new TRunnable();
        TRunnable a4 = new TRunnable();

        tagExecutor.execute("a", a1);
        tagExecutor.execute("a", a2);
        tagExecutor.execute("a", a3);
        tagExecutor.execute("a", a4);

        mExecutor.runNext(); // run a1, will trigger a3 to be runnable
        assertTrue("a1 should have been run", a1.hasRun());
        assertFalse("a2 should not have been run yet", a2.hasRun());
        assertFalse("a3 should be dropped, not run", a3.hasRun());
        assertFalse("a4 should not have been run yet", a4.hasRun());
        mExecutor.runNext(); // run a2
        assertTrue("a2 should have been run", a2.hasRun());
        assertFalse("a3 should be dropped, not run", a3.hasRun());
        assertFalse("a4 should not have been run yet", a4.hasRun());
        mExecutor.runNext(); // run a4
        assertFalse("a3 should be dropped, not run", a3.hasRun());
        assertTrue("a4 should have been run", a4.hasRun());
    }

    /**
     * A simple executor that maintains a queue and executes one task synchronously every
     * time {@link #runNext()} is called. This gives us predictable scheduling for the tests to
     * avoid timeouts waiting for threads to finish.
     */
    private static class MockExecutor implements Executor {
        private final LinkedList<Runnable> mQueue = new LinkedList<Runnable>();
        public synchronized void execute(Runnable command) {
            mQueue.addLast(command);
        }
        public synchronized boolean runNext() {
            if (mQueue.isEmpty()) {
                return false;
            }
            Runnable command = mQueue.removeFirst();
            command.run();
            return true;
        }
    }

    /**
     * A runnable that knows when it has been run.
     */
    private static class TRunnable implements Runnable {
        boolean mRun = false;

        public synchronized void run() {
            mRun = true;
        }

        public synchronized boolean hasRun() {
            return mRun;
        }
    }
}
