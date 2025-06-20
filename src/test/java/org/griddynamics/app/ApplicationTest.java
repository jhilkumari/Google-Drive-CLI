package org.griddynamics.app;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.griddynamics.service.DatabaseService;
import org.griddynamics.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ApplicationTest {

    private DatabaseService dbService;


    private StorageService storageService;

    @Autowired
    private Application application;

    private Directory rootDir;

    @BeforeEach
    public void setUp() {
        rootDir = new Directory();
        rootDir.setId(1);
        rootDir.setName("root");
        rootDir.setPath("/");

        when(dbService.getRootDirectory()).thenReturn(rootDir);
        application.currentDirectory = rootDir;
    }

    @Test
    public void testCreateDirectory() {
        // Simulate input
        String simulatedInput = "TestDir\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

        application.createDirectory(rootDir);

        verify(dbService, times(1)).saveDirectory(any(Directory.class));
    }

    @Test
    public void testUploadFile_FileDoesNotExist() {
        // Simulate non-existent file path
        String simulatedInput = "nonexistent_file.txt\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

        application.uploadFile(rootDir);
        verify(dbService, never()).saveFile(any(File.class), any(FileInputStream.class), any(StorageService.class));
    }

    @Test
    public void testListContents_EmptyDirectory() {
        when(dbService.getDirectoryContents(rootDir.getId())).thenReturn(List.of());

        application.listContents(rootDir);

        verify(dbService, times(1)).getDirectoryContents(rootDir.getId());
    }

    @Test
    public void testSearchItems_NoResults() {
        when(dbService.searchFilesAndDirectories("nosuchfile")).thenReturn(List.of());

        System.setIn(new ByteArrayInputStream("nosuchfile\n".getBytes()));
        application.searchItems();

        verify(dbService, times(1)).searchFilesAndDirectories("nosuchfile");
    }

    @Test
    public void testRenameFile() {
        File file = new File();
        file.setId(2);
        file.setName("oldname.txt");
        file.setFileType("txt");

        when(dbService.getFile(2)).thenReturn(file);
        when(dbService.getDirectory(2)).thenReturn(null);

        String input = "2\nnewname.txt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        application.renameItem();

        verify(dbService).rename(2, "newname.txt", false);
    }
}
