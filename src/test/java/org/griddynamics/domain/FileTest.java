package org.griddynamics.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FileTest {

    private File testFile;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        testFile = new File();
        testFile.setName("document.pdf");
        testFile.setPath("/documents/document.pdf");
        testFile.setCreatedAt(testTime);
        testFile.setUpdatedAt(testTime);
        testFile.setFileSize(1024L);
        testFile.setFileType("pdf");
    }

    @Test
    void testGetDetails() {
        String expected = String.format("""
            File Information:
                Name: document.pdf
                Path: /documents/document.pdf
                Created: %s
                Modified: %s
                Size: 1024 bytes
                Extension: pdf
            """, testTime, testTime);

        assertEquals(expected, testFile.getDetails());
    }

    @Test
    void testGetDetails_EmptyFileType() {
        testFile.setFileType("");
        String expected = String.format("""
            File Information:
                Name: document.pdf
                Path: /documents/document.pdf
                Created: %s
                Modified: %s
                Size: 1024 bytes
                Extension: %s
            """, testTime, testTime,"");

        assertEquals(expected, testFile.getDetails());
    }

    @Test
    void testGetDetailsWithNullFileType() {
        testFile.setFileType(null);
        String expected = String.format("""
            File Information:
                Name: document.pdf
                Path: /documents/document.pdf
                Created: %s
                Modified: %s
                Size: 1024 bytes
                Extension: null
            """, testTime, testTime);

        assertEquals(expected, testFile.getDetails());
    }

    @Test
    void testGetDetailsWithZeroFileSize() {
        testFile.setFileSize(0L);
        String expected = String.format("""
            File Information:
                Name: document.pdf
                Path: /documents/document.pdf
                Created: %s
                Modified: %s
                Size: 0 bytes
                Extension: pdf
            """, testTime, testTime);

        assertEquals(expected, testFile.getDetails());
    }

    @Test
    void testGetDetailsWithLargeFileSize() {
        testFile.setFileSize(1073741824L); // 1GB
        String expected = String.format("""
            File Information:
                Name: document.pdf
                Path: /documents/document.pdf
                Created: %s
                Modified: %s
                Size: 1073741824 bytes
                Extension: pdf
            """, testTime, testTime);

        assertEquals(expected, testFile.getDetails());
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("document.pdf", testFile.getName());
        assertEquals("/documents/document.pdf", testFile.getPath());
        assertEquals(testTime, testFile.getCreatedAt());
        assertEquals(testTime, testFile.getUpdatedAt());
        assertEquals(1024L, testFile.getFileSize());
        assertEquals("pdf", testFile.getFileType());

        // Test setters
        LocalDateTime newTime = LocalDateTime.now().plusDays(1);
        testFile.setUpdatedAt(newTime);
        assertEquals(newTime, testFile.getUpdatedAt());

        testFile.setFileSize(2048L);
        assertEquals(2048L, testFile.getFileSize());

        testFile.setFileType("docx");
        assertEquals("docx", testFile.getFileType());
    }

    @Test
    void testFileTypeWithMultipleDots() {
        testFile.setName("archive.tar.gz");
        testFile.setFileType("gz");
        String expected = String.format("""
            File Information:
                Name: archive.tar.gz
                Path: /documents/document.pdf
                Created: %s
                Modified: %s
                Size: 1024 bytes
                Extension: gz
            """, testTime, testTime);

        assertEquals(expected, testFile.getDetails());
    }
}