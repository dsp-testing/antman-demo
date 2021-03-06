/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests FileSet using the Copy task.
 *
 */
public class CopyTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();


    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/copy.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void test1() {
        buildRule.executeTarget("test1");
        File f = new File(buildRule.getProject().getProperty("output"), "copytest1.tmp");
        if (!f.exists()) {
            fail("Copy failed");
        }
    }

    @Test
    public void test2() {
        buildRule.executeTarget("test2");
        File f = new File(buildRule.getProject().getProperty("output"), "copytest1dir/copy.xml");
        if (!f.exists()) {
            fail("Copy failed");
        }
    }

    @Test
    public void test3() {
        buildRule.executeTarget("test3");
        File file3  = new File(buildRule.getProject().getProperty("output"), "copytest3.tmp");
        //rollback file timestamp instead of delaying test
        FileUtilities.rollbackTimestamps(file3, 3);
        buildRule.executeTarget("test3Part2");
        assertTrue(file3.exists());

        File file3a = new File(buildRule.getProject().getProperty("output"), "copytest3a.tmp");
        assertTrue(file3a.exists());
        File file3b = new File(buildRule.getProject().getProperty("output"), "copytest3b.tmp");
        assertTrue(file3b.exists());
        File file3c = new File(buildRule.getProject().getProperty("output"), "copytest3c.tmp");
        assertTrue(file3c.exists());

        //file length checks rely on touch generating a zero byte file
        if (file3.length()==0) {
            fail("could not overwrite an existing, older file");
        }
        if (file3c.length()!=0) {
            fail("could not force overwrite an existing, newer file");
        }
        if (file3b.length()==0) {
            fail("unexpectedly overwrote an existing, newer file");
        }

        //file time checks for java1.2+
        assertTrue(file3a.lastModified() == file3.lastModified());
        assertTrue(file3c.lastModified() < file3a.lastModified());

    }

    @Test
    public void testFilterTest() {
        buildRule.executeTarget("filtertest");
        assertTrue(!buildRule.getLog().contains("loop in tokens"));
    }

    @Test
    public void testInfiniteFilter() {
        buildRule.executeTarget("infinitetest");
        assertContains("loop in tokens", buildRule.getOutput());
    }

    @Test
    public void testFilterSet() throws IOException {
        buildRule.executeTarget("testFilterSet");
        File tmp  = new File(buildRule.getProject().getProperty("output"), "copy.filterset.tmp");
        File check  = new File(buildRule.getProject().getBaseDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(tmp), FileUtilities.getFileContents(check));
    }

    @Test
    public void testFilterChain() throws IOException {
        buildRule.executeTarget("testFilterChain");
        File tmp  = new File(buildRule.getProject().getProperty("output"), "copy.filterchain.tmp");
        File check  = new File(buildRule.getProject().getBaseDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(tmp), FileUtilities.getFileContents(check));
    }

    @Test
    public void testSingleFileFileset() {
        buildRule.executeTarget("test_single_file_fileset");
        File file  = new File(buildRule.getProject().getProperty("output"),
                                        "copytest_single_file_fileset.tmp");
        assertTrue(file.exists());
    }

    @Test
    public void testSingleFilePath() {
        buildRule.executeTarget("test_single_file_path");
        File file  = new File(buildRule.getProject().getProperty("output"),
                                        "copytest_single_file_path.tmp");
        assertTrue(file.exists());
    }

    @Test
    public void testTranscoding() throws IOException {
        buildRule.executeTarget("testTranscoding");
        File f1 = buildRule.getProject().resolveFile("copy/expected/utf-8");
        File f2 = new File(buildRule.getProject().getProperty("output"), "copytest1.tmp");
        assertEquals(FileUtilities.getFileContents(f1), FileUtilities.getFileContents(f2));
    }

    @Test
    public void testMissingFileIgnore() {
        buildRule.executeTarget("testMissingFileIgnore");
        assertContains("Warning: Could not find file", buildRule.getLog());
    }

    @Test
    public void testMissingFileBail() {
        try {
            buildRule.executeTarget("testMissingFileBail");
            fail("not-there doesn't exist");
        } catch (BuildException ex) {
            assertTrue(ex.getMessage()
                    .startsWith("Warning: Could not find file "));
        }
    }

    @Test
    public void testMissingDirIgnore() {
        buildRule.executeTarget("testMissingDirIgnore");
        assertContains("Warning: ", buildRule.getLog());
    }

    @Test
    public void testMissingDirBail() {
        try {
            buildRule.executeTarget("testMissingDirBail");
            fail("not-there doesn't exist");
        } catch (BuildException ex) {
            assertTrue(ex.getMessage().endsWith(" does not exist."));
        }
    }

    @Test
    public void testFileResourcePlain() {
        buildRule.executeTarget("testFileResourcePlain");
        File file1 = new File(buildRule.getProject().getProperty("to.dir") + "/file1.txt");
        File file2 = new File(buildRule.getProject().getProperty("to.dir") + "/file2.txt");
        File file3 = new File(buildRule.getProject().getProperty("to.dir") + "/file3.txt");
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());
    }

    @Ignore("Previously ignored by naming convention")
    @Test
    public void testFileResourceWithMapper() {
        buildRule.executeTarget("testFileResourceWithMapper");
        File file1 = new File(buildRule.getProject().getProperty("to.dir") + "/file1.txt.bak");
        File file2 = new File(buildRule.getProject().getProperty("to.dir") + "/file2.txt.bak");
        File file3 = new File(buildRule.getProject().getProperty("to.dir") + "/file3.txt.bak");
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());
    }

    @Test
    public void testFileResourceWithFilter() {
        buildRule.executeTarget("testFileResourceWithFilter");
        File file1 = new File(buildRule.getProject().getProperty("to.dir") + "/fileNR.txt");
        assertTrue(file1.exists());
        FileReader f = null;
        try {
            f = new FileReader(file1);
            String file1Content = FileUtils.readFully(f);
            assertEquals("This is file 42", file1Content);
        } catch (IOException e) {
            // no-op: not a real business error
        } finally {
            FileUtils.close(f);
        }
    }

    @Test
    public void testPathAsResource() {
        buildRule.executeTarget("testPathAsResource");
        File file1 = new File(buildRule.getProject().getProperty("to.dir") + "/file1.txt");
        File file2 = new File(buildRule.getProject().getProperty("to.dir") + "/file2.txt");
        File file3 = new File(buildRule.getProject().getProperty("to.dir") + "/file3.txt");
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());
    }

    @Test
    public void testZipfileset() {
        buildRule.executeTarget("testZipfileset");
        File file1 = new File(buildRule.getProject().getProperty("to.dir") + "/file1.txt");
        File file2 = new File(buildRule.getProject().getProperty("to.dir") + "/file2.txt");
        File file3 = new File(buildRule.getProject().getProperty("to.dir") + "/file3.txt");
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());
    }

    @Test
    public void testDirset() {
        buildRule.executeTarget("testDirset");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testResourcePlain() {
        buildRule.executeTarget("testResourcePlain");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testResourcePlainWithMapper() {
        buildRule.executeTarget("testResourcePlainWithMapper");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testResourcePlainWithFilter() {
        buildRule.executeTarget("testResourcePlainWithFilter");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testOnlineResources() {
        buildRule.executeTarget("testOnlineResources");
    }

    /**
     * Tests that the {@code copy} task doesn't corrupt the source file, if the target of the copy operation is a symlink
     * to the source file being copied
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=60644">issue 60644</a>
     */
    @Test
    public void testCopyToSymlinkedSelf() throws Exception {
        // we are only going to test against systems that support symlinks
        Assume.assumeTrue("Symlinks not supported on this operating system", Os.isFamily(Os.FAMILY_UNIX));

        // setup the source files to run copying against
        buildRule.executeTarget("setupSelfCopyTesting");
        final File testDir = new File(buildRule.getProject().getProperty("self.copy.test.root.dir"));
        Assert.assertTrue(testDir + " was expected to be a directory", testDir.isDirectory());
        final File srcFile = new File(testDir, "file.txt");
        Assert.assertTrue("Source file " + srcFile + " was expected to be a file", srcFile.isFile());
        final long originalFileSize = srcFile.length();
        final String originalContent;
        final BufferedReader reader = new BufferedReader(new FileReader(srcFile));
        try {
            originalContent = FileUtils.readFully(reader);
        } finally {
            reader.close();
        }
        Assert.assertTrue("Content missing in file " + srcFile, originalContent != null && originalContent.length() > 0);

        // run the copy tests
        buildRule.executeTarget("testSelfCopy");
        // make sure the source file hasn't been impacted by the copy
        assertSizeAndContent(srcFile, originalFileSize, originalContent);
        final File symlinkedFile = new File(testDir, "sylmink-file.txt");
        Assert.assertTrue(symlinkedFile + " was expected to be a file", symlinkedFile.isFile());
        assertSizeAndContent(symlinkedFile, originalFileSize, originalContent);

        final File symlinkedTestDir = new File(buildRule.getProject().getProperty("self.copy.test.symlinked.dir"));
        Assert.assertTrue(symlinkedTestDir + " was expected to be a directory", symlinkedTestDir.isDirectory());
        assertSizeAndContent(new File(symlinkedTestDir, "file.txt"), originalFileSize, originalContent);
        assertSizeAndContent(new File(symlinkedTestDir, "sylmink-file.txt"), originalFileSize, originalContent);

    }

    private void assertSizeAndContent(final File file, final long expectedSize, final String expectedContent) throws IOException {
        Assert.assertTrue(file + " was expected to be a file", file.isFile());
        Assert.assertEquals("Unexpected size of file " + file, expectedSize, file.length());
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String content;
        try {
            content = FileUtils.readFully(reader);
        } finally {
            reader.close();
        }
        Assert.assertEquals("Unexpected content in file " + file, expectedContent, content);
    }
}
