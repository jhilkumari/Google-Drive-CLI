package org.griddynamics.domain;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

class DirectoryTest {

    private Directory rootDirectory;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        rootDirectory = new Directory();
        rootDirectory.setName("root");
        rootDirectory.setPath("/root");
        rootDirectory.setCreatedAt(testTime);
        rootDirectory.setUpdatedAt(testTime);
    }

    @Test
    void testGetDetails_EmptyDirectory() {
        String expected = String.format("""
                Directory Information:
                    Name: root
                    Path: /root
                    Created: %s
                    Modified: %s
                    Contents: 0 item
                """, testTime, testTime);

        assertEquals(expected, rootDirectory.getDetails(0));
    }

    @Test
    void testGetDetails_FilesOnly() {
        // Add some files
        File file1 = new File();
        file1.setName("file1.txt");
        rootDirectory.files.add(file1);

        File file2 = new File();
        file2.setName("file2.txt");
        rootDirectory.files.add(file2);

        String expected = String.format("""
                Directory Information:
                    Name: root
                    Path: /root
                    Created: %s
                    Modified: %s
                    Contents: 2 items
                """, testTime, testTime);

        assertEquals(expected, rootDirectory.getDetails(2));
    }

    @Test
    void testGetDetails_SubdirectoriesOnly() {
        // Add some subdirectories
        Directory subDir1 = new Directory();
        subDir1.setName("sub1");
        rootDirectory.subDirectories.add(subDir1);

        Directory subDir2 = new Directory();
        subDir2.setName("sub2");
        rootDirectory.subDirectories.add(subDir2);

        String expected = String.format("""
                Directory Information:
                    Name: root
                    Path: /root
                    Created: %s
                    Modified: %s
                    Contents: 2 items
                """, testTime, testTime);

        assertEquals(expected, rootDirectory.getDetails(2));
    }

    @Test
    void testGetDetails_MixedContents() {
        // Add files and subdirectories
        File file1 = new File();
        file1.setName("file1.txt");
        rootDirectory.files.add(file1);

        Directory subDir1 = new Directory();
        subDir1.setName("sub1");
        rootDirectory.subDirectories.add(subDir1);

        String expected = String.format("""
                Directory Information:
                    Name: root
                    Path: /root
                    Created: %s
                    Modified: %s
                    Contents: 2 items
                """, testTime, testTime);

        assertEquals(expected, rootDirectory.getDetails(2));
    }

    @Test
    void testGetDetails_SingleItem() {
        // Add just one file
        File file1 = new File();
        file1.setName("file1.txt");
        rootDirectory.files.add(file1);

        String expected = String.format("""
                Directory Information:
                    Name: root
                    Path: /root
                    Created: %s
                    Modified: %s
                    Contents: 1 item
                """, testTime, testTime);

        assertEquals(expected, rootDirectory.getDetails(1));
    }

}