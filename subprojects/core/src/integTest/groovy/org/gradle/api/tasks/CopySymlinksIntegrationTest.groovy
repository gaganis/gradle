/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.tasks

import org.gradle.integtests.fixtures.AbstractIntegrationTest
import org.gradle.test.fixtures.file.TestFile
import org.gradle.testfixtures.internal.NativeServicesTestFixture
import org.gradle.util.PreconditionVerifier
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class CopySymlinksIntegrationTest extends AbstractIntegrationTest {
    @Rule public PreconditionVerifier verifier = new PreconditionVerifier()

    private TestFile external;
    private TestFile orig;
    private TestFile origContentFile;
    private TestFile subject;
    private TestFile linkTarget;
    private TestFile link;
    private TestFile subjectContentFile;

    @Before
    public void setup() {
        NativeServicesTestFixture.initialize();
    }

    @Test
    @Requires(TestPrecondition.SYMLINKS)
    public void willNotCrashOnDanglingSymlink() {
        setupSymlinks()

        testFile('build.gradle') << '''
            task testCopy(type: Copy) {
                from "test/orig"
                into "test/subject"
            }
'''

        inTestDirectory().withTasks('testCopy').run()
        assertTrue(orig.isDirectory())
        assertTrue(orig.exists())
        assertTrue(subject.isDirectory())
        assertTrue(subject.exists())
        assertTrue(origContentFile.exists())
        assertTrue(link.exists())
        assertTrue(subjectContentFile.exists())
        assertTrue(linkTarget.exists())
    }


    def void setupSymlinks() {
        orig = testDirectory.createDir('test/orig')
        origContentFile = orig.createFile('gaganis.txt')

        external = testDirectory.createDir('test/external')
        linkTarget = external.createFile('foo')

        subject = testDirectory.createDir('test/subject')
        link = subject.file('foo')
        link.createLink(linkTarget)
        subjectContentFile = subject.file('gaganis.txt')

        linkTarget.delete()

        assertTrue(orig.isDirectory())
        assertTrue(orig.exists())
        assertTrue(subject.isDirectory())
        assertTrue(subject.exists())
        assertTrue(origContentFile.exists())
        assertFalse(subjectContentFile.exists())
    }
}
