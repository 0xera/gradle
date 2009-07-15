/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.integtests;

import org.apache.commons.io.FileUtils;
import org.gradle.api.UncheckedIOException;
import org.gradle.util.GFileUtils;
import org.gradle.util.CompressUtil;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestFile {
    private final File file;

    public TestFile(File file, Object... path) {
        File current = GFileUtils.canonicalise(file);
        for (Object p : path) {
            current = GFileUtils.canonicalise(new File(current, p.toString()));
        }
        this.file = current;
    }

    public TestFile file(Object... path) {
        return new TestFile(file, path);
    }

    public TestFile writelns(String... lines) {
        return writelns(Arrays.asList(lines));
    }

    public TestFile write(Object content) {
        try {
            FileUtils.writeStringToFile(file, content.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public String getText() {
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void unzipTo(TestFile file) {
        CompressUtil.unzip(this.file, file.file);
    }

    public void touch() {
        try {
            FileUtils.touch(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public File asFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.getPath();
    }

    public TestFile writelns(List<String> lines) {
        Formatter formatter = new Formatter();
        for (String line : lines) {
            formatter.format("%s%n", line);
        }
        return write(formatter);
    }

    public void assertExists() {
        assertTrue(String.format("%s does not exist", file), file.exists());
    }

    public void assertDoesNotExist() {
        assertFalse(String.format("%s should not exist", file), file.exists());
    }

    /**
     * Asserts that this file contains exactly the given set of descendents
     */
    public void assertHasDescendents(String... descendents) {
        Set<String> actual = new TreeSet<String>();
        visit(actual, "", file);
        Set<String> expected = new TreeSet<String>(Arrays.asList(descendents));
        assertEquals(expected, actual);
    }

    private void visit(Set<String> names, String prefix, File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (child.isFile()) {
                    names.add(prefix + child.getName());
                } else if (child.isDirectory()) {
                    visit(names, prefix + child.getName() + "/", child);
                }
            }
        }
    }
}
