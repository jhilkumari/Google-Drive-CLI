package org.griddynamics.app;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.griddynamics.service.DatabaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuBarTest {

    @Mock
    private DatabaseService dbService;

    private Directory dir;
    private File file;
    private Directory subDir;

    @BeforeEach
    void setUp() {
        dir = new Directory();
        dir.setId(1);
        dir.setName("root");
        dir.setParentId(0);

        file = new File();
        file.setId(2);
        file.setName("file1.txt");

        subDir = new Directory();
        subDir.setId(3);
        subDir.setName("sub");
        subDir.setParentId(1);
    }

    @Test
    void testGetMainMenu_ShouldReturnCorrectMenuString() {
        String expectedMenu = """
                Choose an option:
                    1. List contents
                    2. Create directory
                    3. Upload file
                    4. Download file
                    5. Rename item
                    6. Move item
                    7. Delete item
                    8. View item details
                    9. Search
                    10.Change directory
                    0. Exit
                """;
        assertEquals(expectedMenu, MenuBar.getMainMenu());
    }

    @Test
    void testListFilesInDirectory_WithFiles_ShouldPrintFileNames() {
        when(dbService.getChildren(dir.getId())).thenReturn(List.of(file));

        MenuBar.listFilesInDirectory(dbService, dir);

        verify(dbService).getChildren(dir.getId());
    }

    @Test
    void testListFilesInDirectory_NoFiles_ShouldPrintNoFilesMessage() {
        when(dbService.getChildren(dir.getId())).thenReturn(List.of());

        MenuBar.listFilesInDirectory(dbService, dir);

        verify(dbService).getChildren(dir.getId());
    }

    @Test
    void testListFilesInDirectory_DbServiceReturnsNull_ShouldHandleGracefully() {
        when(dbService.getChildren(dir.getId())).thenReturn(null);

        MenuBar.listFilesInDirectory(dbService, dir);

        verify(dbService).getChildren(dir.getId());
    }

    @Test
    void testListItemsInDirectory_includeSelfAndRoot() {
        when(dbService.getChildren(dir.getId())).thenReturn(List.of(file, subDir));

        MenuBar.listItemsInDirectory(dbService, dir, true, true);

        verify(dbService).getChildren(dir.getId());
    }

    @Test
    void testListItemsInDirectory_excludeSelf() {
        when(dbService.getChildren(dir.getId())).thenReturn(List.of(file));

        MenuBar.listItemsInDirectory(dbService, dir, false, false);

        verify(dbService).getChildren(dir.getId());
    }

    @Test
    void testListAllDirectories_excludesCurrentDirectory() {
        Directory d1 = new Directory();
        d1.setId(10);
        d1.setName("Another");

        when(dbService.getAllDirectories()).thenReturn(List.of(dir, d1));

        MenuBar.listAllDirectories(dbService, dir);

        verify(dbService).getAllDirectories();
    }

    @Test
    void testListValidDestinationDirectories_excludesSelfDescendantsAndParent() {
        Directory folderToMove = new Directory();
        folderToMove.setId(100);
        folderToMove.setName("ToMove");
        folderToMove.setParentId(1);  // dir is parent

        Set<Integer> descendants = new HashSet<>(List.of(3));
        when(dbService.getAllDescendantDirectoryIds(folderToMove.getId())).thenReturn(descendants);
        when(dbService.getAllDirectories()).thenReturn(List.of(folderToMove, dir, subDir));

        MenuBar.listValidDestinationDirectories(dbService, folderToMove);

        verify(dbService).getAllDirectories();
        verify(dbService).getAllDescendantDirectoryIds(folderToMove.getId());
    }

    @Test
    void testListValidDestinationDirectories_printsExpectedOutput() {
        Directory folderToMove = new Directory();
        folderToMove.setId(100);
        folderToMove.setName("ToMove");
        folderToMove.setParentId(1);

        Directory validTarget = new Directory();
        validTarget.setId(4);
        validTarget.setName("ValidTarget");
        validTarget.setParentId(0);

        Set<Integer> descendants = new HashSet<>(List.of(3));
        when(dbService.getAllDescendantDirectoryIds(folderToMove.getId())).thenReturn(descendants);
        when(dbService.getAllDirectories()).thenReturn(List.of(folderToMove, dir, subDir, validTarget));

        // Capture console output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        MenuBar.listValidDestinationDirectories(dbService, folderToMove);

        System.setOut(originalOut); // Restore original System.out

        String output = outContent.toString();
        assert(output.contains("Id: 4 [Directory] ValidTarget"));

        verify(dbService).getAllDirectories();
        verify(dbService).getAllDescendantDirectoryIds(folderToMove.getId());
    }

}
