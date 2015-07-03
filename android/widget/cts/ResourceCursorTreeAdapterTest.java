/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.widget.cts;

import com.android.cts.stub.R;
import com.android.internal.database.ArrayListCursor;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import android.content.Context;
import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorTreeAdapter;

import java.util.ArrayList;

/**
 * Test {@link ResourceCursorTreeAdapter}.
 */
@TestTargetClass(ResourceCursorTreeAdapter.class)
public class ResourceCursorTreeAdapterTest extends InstrumentationTestCase {
    private ResourceCursorTreeAdapter mResourceCursorTreeAdapter;

    private Context mContext;

    private ViewGroup mParent;

    private int mCollapsedGroupLayout = R.layout.cursoradapter_group0;

    private int mGroupLayout = mCollapsedGroupLayout;

    private int mExpandedGroupLayout = R.layout.cursoradapter_group1;

    private int mNormalChildLayout = R.layout.cursoradapter_item0;

    private int mChildLayout = mNormalChildLayout;

    private int mLastChildLayout = R.layout.cursoradapter_item1;

    private int mCollapsedGroupId = R.id.cursorAdapter_group0;

    private int mGroupId = mCollapsedGroupId;

    private int mExpandedGroupId = R.id.cursorAdapter_group1;

    private int mNormalChildId = R.id.cursorAdapter_item0;

    private int mChildId = mNormalChildId;

    private int mLastChildId = R.id.cursorAdapter_item1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mParent = (ViewGroup) layoutInflater.inflate(R.layout.cursoradapter_host, null);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors",
            method = "ResourceCursorTreeAdapter",
            args = {android.content.Context.class, android.database.Cursor.class, int.class, 
                    int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors",
            method = "ResourceCursorTreeAdapter",
            args = {android.content.Context.class, android.database.Cursor.class, int.class, 
                    int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "ResourceCursorTreeAdapter",
            args = {android.content.Context.class, android.database.Cursor.class, int.class, 
                    int.class, int.class, int.class}
        )
    })
    public void testConstructor() {
        mResourceCursorTreeAdapter = new MockResourceCursorTreeAdapter(mContext, null,
                mGroupLayout, mChildLayout);
        assertNull(mResourceCursorTreeAdapter.getCursor());

        Cursor cursor = createTestCursor(3, 3);
        mResourceCursorTreeAdapter = new MockResourceCursorTreeAdapter(mContext, cursor,
                mGroupLayout, mChildLayout);
        assertEquals(cursor, mResourceCursorTreeAdapter.getCursor());

        new MockResourceCursorTreeAdapter(mContext, null, mCollapsedGroupLayout,
                mExpandedGroupLayout, mChildLayout);

        new MockResourceCursorTreeAdapter(mContext, null, mCollapsedGroupLayout,
                mExpandedGroupLayout, mNormalChildLayout, mLastChildLayout);

        new MockResourceCursorTreeAdapter(mContext, null, -1, -1);

        new MockResourceCursorTreeAdapter(mContext, null, -1, -1, -1);

        new MockResourceCursorTreeAdapter(mContext, null, -1, -1, -1, -1);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "newChildView",
        args = {android.content.Context.class, android.database.Cursor.class, boolean.class, 
                android.view.ViewGroup.class}
    )
    // The parameters Context and Cursor are never readin the method
    public void testNewChildView() {
        mResourceCursorTreeAdapter = new MockResourceCursorTreeAdapter(mContext, null,
                mGroupLayout, mChildLayout);

        View result = mResourceCursorTreeAdapter.newChildView(null, null, true, mParent);
        assertEquals(mChildId, result.getId());

        result = mResourceCursorTreeAdapter.newChildView(null, null, false, mParent);
        assertEquals(mChildId, result.getId());

        mResourceCursorTreeAdapter = new MockResourceCursorTreeAdapter(mContext, null,
                mGroupLayout, mGroupLayout, mNormalChildLayout, mLastChildLayout);

        result = mResourceCursorTreeAdapter.newChildView(null, null, true, mParent);
        assertEquals(mLastChildId, result.getId());

        result = mResourceCursorTreeAdapter.newChildView(null, null, false, mParent);
        assertEquals(mNormalChildId, result.getId());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "newGroupView",
        args = {android.content.Context.class, android.database.Cursor.class, boolean.class, 
                android.view.ViewGroup.class}
    )
    // The parameters Context and Cursor are never readin the method
    public void testNewGroupView() {
        mResourceCursorTreeAdapter = new MockResourceCursorTreeAdapter(mContext, null,
                mGroupLayout, mChildLayout);

        View result = mResourceCursorTreeAdapter.newGroupView(null, null, true, mParent);
        assertEquals(mGroupId, result.getId());

        result = mResourceCursorTreeAdapter.newGroupView(null, null, false, mParent);
        assertEquals(mGroupId, result.getId());

        mResourceCursorTreeAdapter = new MockResourceCursorTreeAdapter(mContext, null,
                mCollapsedGroupLayout, mExpandedGroupLayout, mChildLayout);

        result = mResourceCursorTreeAdapter.newGroupView(null, null, true, mParent);
        assertEquals(mExpandedGroupId, result.getId());

        result = mResourceCursorTreeAdapter.newGroupView(null, null, false, mParent);
        assertEquals(mCollapsedGroupId, result.getId());
    }

    /**
     * Creates the test cursor.
     *
     * @param colCount the column count
     * @param rowCount the row count
     * @return the cursor
     */
    @SuppressWarnings("unchecked")
    private Cursor createTestCursor(int colCount, int rowCount) {
        ArrayList<ArrayList> list = new ArrayList<ArrayList>();
        String[] columns = new String[colCount];
        for (int i = 0; i < colCount; i++) {
            columns[i] = "column" + i;
        }

        for (int i = 0; i < rowCount; i++) {
            ArrayList<String> row = new ArrayList<String>();
            for (int j = 0; j < colCount; j++) {
                row.add("" + rowCount + "" + colCount);
            }
            list.add(row);
        }

        return new ArrayListCursor(columns, list);
    }

    private class MockResourceCursorTreeAdapter extends ResourceCursorTreeAdapter {
        public MockResourceCursorTreeAdapter(Context context, Cursor cursor,
                int collapsedGroupLayout, int expandedGroupLayout, int childLayout,
                int lastChildLayout) {
            super(context, cursor, collapsedGroupLayout, expandedGroupLayout, childLayout,
                    lastChildLayout);
        }

        public MockResourceCursorTreeAdapter(Context context, Cursor cursor,
                int collapsedGroupLayout, int expandedGroupLayout, int childLayout) {
            super(context, cursor, collapsedGroupLayout, expandedGroupLayout, childLayout);
        }

        public MockResourceCursorTreeAdapter(Context context, Cursor cursor, int groupLayout,
                int childLayout) {
            super(context, cursor, groupLayout, childLayout);
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor,
                boolean isLastChild) {
        }

        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor,
                boolean isExpanded) {
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            return null;
        }
    }
}