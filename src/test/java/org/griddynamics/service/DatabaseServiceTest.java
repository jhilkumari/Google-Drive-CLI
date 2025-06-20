package org.griddynamics.service;

import org.griddynamics.domain.Directory;
import org.griddynamics.domain.File;
import org.griddynamics.domain.StorageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.griddynamics.db.SqlQueries.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {


    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private StorageService storageService;

    @Mock
    private InputStream inputStream;

    private DatabaseService databaseService;

    @BeforeEach
    void setUp() throws SQLException {
        // Initialize the service with the mock connection
        databaseService = new DatabaseService(connection);

        // Mock common behavior
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void getRootDirectory_WhenExists_ReturnsDirectory() throws SQLException {
        // Arrange
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("name")).thenReturn("root");
        when(resultSet.getInt("parent_id")).thenReturn(0);
        when(resultSet.getString("path")).thenReturn("root");

        Timestamp time = Timestamp.valueOf(LocalDateTime.now());
        when(resultSet.getTimestamp("created_at")).thenReturn(time);
        when(resultSet.getTimestamp("updated_at")).thenReturn(time);

        // Act
        Directory result = databaseService.getRootDirectory();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("root", result.getName());
        assertEquals("root", result.getPath());
        assertEquals(0, result.getParentId()); // because your mapping logic converts null to 0
    }


    @Test
    void getRootDirectory_WhenNotExists_CreatesAndReturnsDirectory() throws SQLException {
        // Arrange - Create separate mocks for SELECT and INSERT statements
        PreparedStatement selectStatement = mock(PreparedStatement.class);
        PreparedStatement insertStatement = mock(PreparedStatement.class);
        ResultSet selectResultSet = mock(ResultSet.class);
        ResultSet insertResultSet = mock(ResultSet.class);

        // SELECT: returns empty
        when(connection.prepareStatement(SELECT_ROOT_DIRECTORY)).thenReturn(selectStatement);
        when(selectStatement.executeQuery()).thenReturn(selectResultSet);
        when(selectResultSet.next()).thenReturn(false);

        // INSERT: returns a new result
        when(connection.prepareStatement(INSERT_DIRECTORY)).thenReturn(insertStatement);
        when(insertStatement.executeQuery()).thenReturn(insertResultSet);
        when(insertResultSet.next()).thenReturn(true);
        mockDirectoryResultSet(insertResultSet, 1, "root", 0, "root");

        // Act
        Directory result = databaseService.getRootDirectory();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("root", result.getName());
        assertEquals("root", result.getPath());
        assertEquals(0, result.getParentId());

        // Verify that the insert statement was executed
        verify(insertStatement).setString(1, "root");
        verify(insertStatement).setInt(2, 0);
        verify(insertStatement).setString(3, "root");
        verify(insertStatement).executeQuery();
    }


    @Test
    void getDirectory_WhenExists_ReturnsDirectory() throws SQLException {
        // Arrange
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        mockDirectoryResultSet(resultSet, 2, "test", 1, "/root/test");

        // Act
        Directory result = databaseService.getDirectory(2);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals("test", result.getName());
        assertEquals("/root/test", result.getPath());
        assertEquals(1, result.getParentId());
    }

    @Test
    void getDirectory_WhenNotExists_ReturnsNull() throws SQLException {
        // Arrange
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        Directory result = databaseService.getDirectory(999);

        // Assert
        assertNull(result);
    }

    @Test
    void saveDirectory_SuccessfullySavesAndSetsId() throws SQLException {
        // Arrange
        Directory directory = new Directory();
        directory.setName("new-dir");
        directory.setParentId(1);
        directory.setPath("/root/new-dir");

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(3);

        // Act
        databaseService.saveDirectory(directory);

        // Assert
        assertEquals(3, directory.getId());
        verify(preparedStatement).setString(1, "new-dir");
        verify(preparedStatement).setInt(2, 1);
        verify(preparedStatement).setString(3, "/root/new-dir");
    }

    @Test
    void saveFile_SuccessfullySavesFile() throws Exception {
        // Arrange
        File file = new File();
        file.setName("test.txt");
        file.setParentId(1);
        file.setFileType("txt");
        file.setFileSize(100L);

        // Mocking ResultSet to return expected values when queried
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(5);
        when(resultSet.getString("path")).thenReturn("/root");


        // Act
        databaseService.saveFile(file, inputStream, storageService);

        // Assert
        assertEquals(5, file.getId());
        assertEquals("/root/test.txt", file.getPath());
        verify(storageService).saveFileToDisk(5, inputStream, "txt");
        verify(preparedStatement).setString(5, "TEMP"); // Verify temp path is set
    }


    @Test
    void getFile_WhenExists_ReturnsFile() throws SQLException {
        // Arrange
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        mockFileResultSet(resultSet, 3, "file.txt", "/root/file.txt");

        // Act
        File result = databaseService.getFile(3);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getId());
        assertEquals("file.txt", result.getName());
        assertEquals(1, result.getParentId());
        assertEquals("txt", result.getFileType());
        assertEquals(100L, result.getFileSize());
        assertEquals("/root/file.txt", result.getPath());
    }

    @Test
    void deleteFile_SuccessfullyDeletesFile() throws Exception {
        // Arrange
        File file = new File();
        file.setId(4);
        file.setFileType("pdf");
        file.setPath("/root/doc.pdf");

        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        databaseService.deleteFile(file, storageService);

        // Assert
        verify(preparedStatement).setInt(1, 4);
        verify(preparedStatement).executeUpdate();
        verify(storageService).deleteFileFromDisk(4, "pdf");
    }

    @Test
    void deleteDirectory_SuccessfullyDeletesDirectory() throws SQLException {
        // Arrange
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        databaseService.deleteDirectory(2);

        // Assert
        verify(preparedStatement).setInt(1, 2);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void rename_UpdatesNameAndPathForItemAndDescendants() throws SQLException {
        int itemId = 2;
        String newName = "renamed";
        String currentPath = "/root/oldname";
        String parentPath = "/root";
        String newPath = "/root/renamed";

        // Mock #1: GET_ITEM
        PreparedStatement getItemStmt = mock(PreparedStatement.class);
        ResultSet getItemRs = mock(ResultSet.class);
        when(connection.prepareStatement(GET_ITEM))
                .thenReturn(getItemStmt);
        when(getItemStmt.executeQuery()).thenReturn(getItemRs);
        when(getItemRs.next()).thenReturn(true);
        when(getItemRs.getString("path")).thenReturn(currentPath);
        when(getItemRs.getInt("parent_id")).thenReturn(1);

        // Mock #2: getPathById
        PreparedStatement getPathStmt = mock(PreparedStatement.class);
        ResultSet getPathRs = mock(ResultSet.class);
        when(connection.prepareStatement(GET_PATH_BY_ID))
                .thenReturn(getPathStmt);
        when(getPathStmt.executeQuery()).thenReturn(getPathRs);
        when(getPathRs.next()).thenReturn(true);
        when(getPathRs.getString("path")).thenReturn(parentPath);

        // Mock #3: RENAME_ITEM
        PreparedStatement renameStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(RENAME_ITEM))
                .thenReturn(renameStmt);

        // Mock #4: updateDescendantPaths
        PreparedStatement descendantsQueryStmt = mock(PreparedStatement.class);
        ResultSet descendantsRs = mock(ResultSet.class);
        when(connection.prepareStatement(SELECT_DESCENDANTS))
                .thenReturn(descendantsQueryStmt);
        when(descendantsQueryStmt.executeQuery()).thenReturn(descendantsRs);
        when(descendantsRs.next()).thenReturn(true, true, false);
        when(descendantsRs.getInt("id")).thenReturn(3, 4);
        when(descendantsRs.getString("path")).thenReturn("/root/oldname/child1", "/root/oldname/child2");

        PreparedStatement updateDescendantsStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(UPDATE_PATH))
                .thenReturn(updateDescendantsStmt);

        // Act
        databaseService.rename(itemId, newName, true);

        // Assert
        // Verify main item update
        verify(renameStmt).setString(1, newName);
        verify(renameStmt).setString(2, newPath);
        verify(renameStmt).setInt(3, itemId);
        verify(renameStmt).executeUpdate();

        // Verify descendant updates (adjusted verification)
        verify(updateDescendantsStmt, times(2)).setString(eq(1), anyString());
        verify(updateDescendantsStmt, times(2)).setInt(eq(2), anyInt());
        verify(updateDescendantsStmt, times(2)).executeUpdate();
    }



    @Test
    void move_UpdatesParentAndPath() throws SQLException {
        // Arrange
        int itemId = 3;
        int newParentId = 2;
        String newPath = "/newparent/item";

        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        databaseService.move(itemId, newParentId, newPath);

        // Assert
        verify(preparedStatement).setInt(1, newParentId);
        verify(preparedStatement).setString(2, newPath);
        verify(preparedStatement).setInt(3, itemId);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void getDirectoryContents_ReturnsFilesAndDirectories() throws SQLException {
        // Arrange
        int directoryId = 1;

        when(connection.prepareStatement(GET_CONTENTS_BY_PARENT_ID)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simulate 2 rows, then end
        when(resultSet.next()).thenReturn(true, true, false);

        // Row 1: Directory
        when(resultSet.getBoolean("is_directory")).thenReturn(true, false); // for row 1 and 2
        when(resultSet.getInt("id")).thenReturn(2, 3);
        when(resultSet.getString("name")).thenReturn("subdir", "file.txt");
        when(resultSet.getInt("parent_id")).thenReturn(1, 1);
        when(resultSet.getString("path")).thenReturn("/root/subdir", "/root/file.txt");
        when(resultSet.getString("file_type")).thenReturn("txt");
        when(resultSet.getLong("file_size")).thenReturn(123L);
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()));

        // Act
        List<StorageEntity> contents = databaseService.getDirectoryContents(directoryId);

        // Assert
        assertEquals(2, contents.size());
        assertTrue(contents.get(0) instanceof Directory);
        assertTrue(contents.get(1) instanceof File);

        Directory dir = (Directory) contents.get(0);
        assertEquals(2, dir.getId());
        assertEquals("subdir", dir.getName());

        File file = (File) contents.get(1);
        assertEquals(3, file.getId());
        assertEquals("file.txt", file.getName());
    }


    @Test
    void searchFilesAndDirectories_ReturnsMatchingItems() throws SQLException {
        // Arrange
        String query = "test";
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simulate two rows in ResultSet
        when(resultSet.next()).thenReturn(true, true, false); // Two rows, then end

        // First item: File
        // Second item: Directory
        when(resultSet.getBoolean("is_directory")).thenReturn(false, true);

        // Common fields returned in order for two rows
        when(resultSet.getInt("id")).thenReturn(2, 1);
        when(resultSet.getString("name")).thenReturn("testfile.txt", "testdir");
        when(resultSet.getInt("parent_id")).thenReturn(1, 0);
        when(resultSet.getString("path")).thenReturn("/root/testfile.txt", "/root/testdir");

        // File-specific fields
        when(resultSet.getString("file_type")).thenReturn("txt", null);
        when(resultSet.getLong("file_size")).thenReturn(100L, 0L);

        // Timestamps
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        when(resultSet.getTimestamp("created_at")).thenReturn(now, now);
        when(resultSet.getTimestamp("updated_at")).thenReturn(now, now);

        // Act
        List<StorageEntity> results = databaseService.searchFilesAndDirectories(query);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof File);
        assertTrue(results.get(1) instanceof Directory);

        File file = (File) results.get(0);
        assertEquals("testfile.txt", file.getName());

        Directory dir = (Directory) results.get(1);
        assertEquals("testdir", dir.getName());
    }


    @Test
    void getChildren_ReturnsDirectChildren() throws SQLException {
        // Arrange
        int parentId = 1;
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false); // One child
        when(resultSet.getBoolean("is_directory")).thenReturn(true);

        mockDirectoryResultSet(resultSet, 2, "child", 1, "/root/child");

        // Act
        List<StorageEntity> children = databaseService.getChildren(parentId);

        // Assert
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof Directory);
        Directory dir = (Directory) children.get(0);
        assertEquals("child", dir.getName());
        assertEquals(1, dir.getParentId());
    }

    @Test
    void getAllDirectories_ReturnsAllDirectories() throws SQLException {
        // Arrange
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false); // Two directories

        // Mock directory 1 ("root") and directory 2 ("sub") in order
        when(resultSet.getInt("id")).thenReturn(1, 2);
        when(resultSet.getString("name")).thenReturn("root", "sub");
        when(resultSet.getInt("parent_id")).thenReturn(0, 1);
        when(resultSet.getString("path")).thenReturn("root", "/root/sub");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

        // Act
        List<Directory> directories = databaseService.getAllDirectories();

        // Assert
        assertEquals(2, directories.size());
        assertEquals("root", directories.get(0).getName());
        assertEquals("sub", directories.get(1).getName());
    }


    @Test
    void getAllDescendantDirectoryIds_ReturnsAllDescendants() throws SQLException {
        // Arrange
        int rootId = 1;

        // Root -> children: 2, 3
        PreparedStatement stmtRoot = mock(PreparedStatement.class);
        ResultSet rsRoot = mock(ResultSet.class);
        when(stmtRoot.executeQuery()).thenReturn(rsRoot);
        when(rsRoot.next()).thenReturn(true, true, false); // Two children
        when(rsRoot.getInt("id")).thenReturn(2, 3);

        // Dir 2 -> child: 4
        PreparedStatement stmt2 = mock(PreparedStatement.class);
        ResultSet rs2 = mock(ResultSet.class);
        when(stmt2.executeQuery()).thenReturn(rs2);
        when(rs2.next()).thenReturn(true, false); // One child
        when(rs2.getInt("id")).thenReturn(4);

        // Dir 3 -> no children
        PreparedStatement stmt3 = mock(PreparedStatement.class);
        ResultSet rs3 = mock(ResultSet.class);
        when(stmt3.executeQuery()).thenReturn(rs3);
        when(rs3.next()).thenReturn(false);

        // Dir 4 -> no children
        PreparedStatement stmt4 = mock(PreparedStatement.class);
        ResultSet rs4 = mock(ResultSet.class);
        when(stmt4.executeQuery()).thenReturn(rs4);
        when(rs4.next()).thenReturn(false);

        // Setup prepareStatement chaining
        when(connection.prepareStatement(GET_DESCENDANT_IDS))
                .thenReturn(stmtRoot) // for rootId
                .thenReturn(stmt2)    // for 2
                .thenReturn(stmt3)    // for 3
                .thenReturn(stmt4);   // for 4

        // Act
        Set<Integer> descendants = databaseService.getAllDescendantDirectoryIds(rootId);

        // Assert
        assertEquals(3, descendants.size());
        assertTrue(descendants.contains(2));
        assertTrue(descendants.contains(3));
        assertTrue(descendants.contains(4));
    }


    @Test
    void updateFilePaths_UpdatesPathsSuccessfully() throws SQLException {
        // Arrange
        int fileId = 5;
        String storagePath = "/storage/5.txt";

        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        databaseService.updateFilePaths(fileId, storagePath);

        // Assert
        verify(preparedStatement).setString(1, storagePath);
        verify(preparedStatement).setInt(2, fileId);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void getPathById_WhenExists_ReturnsPath() throws SQLException {
        // Arrange
        int itemId = 1;
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("path")).thenReturn("/root");

        // Act
        String path = databaseService.getPathById(itemId);

        // Assert
        assertEquals("/root", path);
    }

    @Test
    void getPathById_WhenNotExists_ReturnsNull() throws SQLException {
        // Arrange
        int itemId = 999;
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        String path = databaseService.getPathById(itemId);

        // Assert
        assertNull(path);
    }

    @Test
    void updateDescendantPaths_UpdatesAllDescendantPaths() throws SQLException {
        // Arrange
        String oldBasePath = "/root/old";
        String newBasePath = "/root/new";

        // Mock query result: two child items
        when(connection.prepareStatement(SELECT_DESCENDANTS)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false); // two rows
        when(resultSet.getInt("id")).thenReturn(2, 3);
        when(resultSet.getString("path")).thenReturn(
                "/root/old/child1",
                "/root/old/child2"
        );

        // Mock the update statement for each descendant
        PreparedStatement updateStmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(UPDATE_PATH)).thenReturn(updateStmt);

        // Act
        databaseService.updateDescendantPaths(oldBasePath, newBasePath);

        // Assert
        // Verify SELECT query
        verify(connection).prepareStatement(SELECT_DESCENDANTS);
        verify(preparedStatement).setString(eq(1), eq(oldBasePath + "/%"));
        verify(preparedStatement).executeQuery();

        // Verify UPDATE query was prepared twice
        verify(connection, times(2)).prepareStatement(UPDATE_PATH);

        // Capture and assert updated paths
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(updateStmt, times(2)).setString(eq(1), pathCaptor.capture());
        verify(updateStmt, times(2)).setInt(eq(2), idCaptor.capture());
        verify(updateStmt, times(2)).executeUpdate();

        List<String> updatedPaths = pathCaptor.getAllValues();
        List<Integer> updatedIds = idCaptor.getAllValues();

        assertEquals(List.of("/root/new/child1", "/root/new/child2"), updatedPaths);
        assertEquals(List.of(2, 3), updatedIds);
    }


    // Helper methods to mock ResultSet for directories and files
    private void mockDirectoryResultSet(ResultSet rs, int id, String name, int parentId, String path) throws SQLException {
        when(rs.getInt("id")).thenReturn(id);
        when(rs.getString("name")).thenReturn(name);
        when(rs.getInt("parent_id")).thenReturn(parentId);
        when(rs.getString("path")).thenReturn(path);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    }

    private void mockFileResultSet(ResultSet rs, int id, String name,
                                   String path) throws SQLException {
        when(rs.getInt("id")).thenReturn(id);
        when(rs.getString("name")).thenReturn(name);
        when(rs.getInt("parent_id")).thenReturn(1);
        when(rs.getString("file_type")).thenReturn("txt");
        when(rs.getLong("file_size")).thenReturn(100L);
        when(rs.getString("path")).thenReturn(path);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    }
}