/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.security.tests.java.security;

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.security.DigestException;

@TestTargetClass(DigestException.class)
public class DigestException2Test extends junit.framework.TestCase {

    /**
     * @tests java.security.DigestException#DigestException()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "DigestException",
        args = {}
    )
    public void test_Constructor() {
        // Test for method java.security.DigestException()
        DigestException de = new DigestException();
        assertNull("Exception with no message should yield null message.", de
                .getMessage());
    }

    /**
     * @tests java.security.DigestException#DigestException(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Different variants of string parameter (empty, null, etc.) weren't checked",
        method = "DigestException",
        args = {java.lang.String.class}
    )
    public void test_ConstructorLjava_lang_String() {
        // Test for method java.security.DigestException(java.lang.String)
        DigestException de = new DigestException("Test message");
        assertEquals("Exception message is incorrect", "Test message", de
                .getMessage());
    }
}