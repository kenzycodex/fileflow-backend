package com.fileflow.service.search;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FileTagRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.storage.StorageServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SearchServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileTagRepository fileTagRepository;

    @Mock
    private ElasticsearchSearchService elasticsearchSearchService;

    @Mock
    private StorageServiceFactory storageServiceFactory;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SearchServiceImpl searchService;

    private User testUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    public void setup() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Setup user principal
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // Setup security context
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Setup user repository
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    }

    @Test
    public void testSearchWithoutElasticsearch() {
        // Create test data
        File file1 = new File();
        file1.setId(1L);
        file1.setFilename("test document.txt");
        file1.setUser(testUser);

        Folder folder1 = new Folder();
        folder1.setId(1L);
        folder1.setFolderName("test folder");
        folder1.setUser(testUser);

        // Mock repository calls
        Page<File> filePage = new PageImpl<>(Collections.singletonList(file1));
        Page<Folder> folderPage = new PageImpl<>(Collections.singletonList(folder1));

        when(fileRepository.searchByFilename(eq(testUser), eq("test"), any(Pageable.class)))
                .thenReturn(filePage);
        when(folderRepository.searchByFolderName(eq(testUser), eq("test"), any(Pageable.class)))
                .thenReturn(folderPage);

        // Call the service method
        SearchResponse response = searchService.search("test", 0, 10);

        // Verify the results
        assertNotNull(response);
        assertEquals(1, response.getFiles().size());
        assertEquals(1, response.getFolders().size());
        assertEquals("test document.txt", response.getFiles().get(0).getFilename());
        assertEquals("test folder", response.getFolders().get(0).getFolderName());
        assertEquals("test", response.getQuery());
    }

    @Test
    public void testSearchWithElasticsearch() {
        // Create a SearchService with Elasticsearch
        SearchServiceImpl searchServiceWithElastic = new SearchServiceImpl(
                fileRepository,
                folderRepository,
                userRepository,
                fileTagRepository,
                storageServiceFactory
        );
        searchServiceWithElastic.setElasticsearchSearchService(elasticsearchSearchService);

        // Create test data
        SearchResponse mockElasticsearchResponse = SearchResponse.builder()
                .files(new ArrayList<>())
                .folders(new ArrayList<>())
                .query("test content")
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .hasMore(false)
                .build();

        // Mock Elasticsearch search
        when(elasticsearchSearchService.fullSearch(eq("test content"), eq(1L), eq(0), eq(10)))
                .thenReturn(mockElasticsearchResponse);

        // Call the service method with a more complex query that should trigger Elasticsearch
        SearchResponse response = searchServiceWithElastic.search("test content", 0, 10);

        // Verify Elasticsearch was used
        verify(elasticsearchSearchService).fullSearch(eq("test content"), eq(1L), eq(0), eq(10));
    }

    @Test
    public void testSearchFiles() {
        // Create test data
        File file1 = new File();
        file1.setId(1L);
        file1.setFilename("document.txt");
        file1.setUser(testUser);

        // Mock repository calls
        Page<File> filePage = new PageImpl<>(Collections.singletonList(file1));

        when(fileRepository.searchByFilename(eq(testUser), eq("document"), any(Pageable.class)))
                .thenReturn(filePage);

        // Call the service method
        SearchResponse response = searchService.searchFiles("document", 0, 10);

        // Verify the results
        assertNotNull(response);
        assertEquals(1, response.getFiles().size());
        assertEquals(0, response.getFolders().size());
        assertEquals("document.txt", response.getFiles().get(0).getFilename());
    }

    @Test
    public void testSearchFolders() {
        // Create test data
        Folder folder1 = new Folder();
        folder1.setId(1L);
        folder1.setFolderName("Documents");
        folder1.setUser(testUser);

        // Mock repository calls
        Page<Folder> folderPage = new PageImpl<>(Collections.singletonList(folder1));

        when(folderRepository.searchByFolderName(eq(testUser), eq("doc"), any(Pageable.class)))
                .thenReturn(folderPage);

        // Call the service method
        SearchResponse response = searchService.searchFolders("doc", 0, 10);

        // Verify the results
        assertNotNull(response);
        assertEquals(0, response.getFiles().size());
        assertEquals(1, response.getFolders().size());
        assertEquals("Documents", response.getFolders().get(0).getFolderName());
    }

    @Test
    public void testSearchByFileType() {
        // Create test data
        File file1 = new File();
        file1.setId(1L);
        file1.setFilename("document.txt");
        file1.setFileType("document");
        file1.setUser(testUser);

        // Mock repository calls
        Page<File> filePage = new PageImpl<>(Collections.singletonList(file1));

        when(fileRepository.findByUserAndFileTypeAndIsDeletedFalse(eq(testUser), eq("document"), any(Pageable.class)))
                .thenReturn(filePage);

        // Call the service method
        SearchResponse response = searchService.searchByFileType("document", null, 0, 10);

        // Verify the results
        assertNotNull(response);
        assertEquals(1, response.getFiles().size());
        assertEquals("document.txt", response.getFiles().get(0).getFilename());
    }

    @Test
    public void testSearchByFileTypeWithQuery() {
        // Create test data
        File file1 = new File();
        file1.setId(1L);
        file1.setFilename("document.txt");
        file1.setFileType("document");
        file1.setUser(testUser);

        // Mock repository calls
        Page<File> filePage = new PageImpl<>(Collections.singletonList(file1));

        when(fileRepository.searchByFileTypeAndFilename(eq(testUser), eq("document"), eq("txt"), any(Pageable.class)))
                .thenReturn(filePage);

        // Call the service method
        SearchResponse response = searchService.searchByFileType("document", "txt", 0, 10);

        // Verify the results
        assertNotNull(response);
        assertEquals(1, response.getFiles().size());
        assertEquals("document.txt", response.getFiles().get(0).getFilename());
    }

    @Test
    public void testSearchTrash() {
        // Create test data
        File file1 = new File();
        file1.setId(1L);
        file1.setFilename("deleted.txt");
        file1.setUser(testUser);
        file1.setDeleted(true);
        file1.setDeletedAt(LocalDateTime.now());

        // Mock repository calls
        Page<File> filePage = new PageImpl<>(Collections.singletonList(file1));
        Page<Folder> folderPage = new PageImpl<>(Collections.emptyList());

        when(fileRepository.findByUserAndIsDeletedTrue(eq(testUser), any(Pageable.class)))
                .thenReturn(filePage);
        when(folderRepository.findByUserAndIsDeletedTrue(eq(testUser), any(Pageable.class)))
                .thenReturn(folderPage);

        // Call the service method
        SearchResponse response = searchService.searchTrash(null, 0, 10);

        // Verify the results
        assertNotNull(response);
        assertEquals(1, response.getFiles().size());
        assertEquals("deleted.txt", response.getFiles().get(0).getFilename());
    }

    @Test
    public void testSearchFileContentsWithoutElasticsearch() {
        // Call the service method
        SearchResponse response = searchService.searchFileContents("test content", 0, 10);

        // Verify the results
        assertNotNull(response);
        assertEquals(0, response.getFiles().size());
        assertEquals(0, response.getFolders().size());
    }

    @Test
    public void testSearchFileContentsWithElasticsearch() {
        // Create a SearchService with Elasticsearch
        SearchServiceImpl searchServiceWithElastic = new SearchServiceImpl(
                fileRepository,
                folderRepository,
                userRepository,
                fileTagRepository,
                storageServiceFactory
        );
        searchServiceWithElastic.setElasticsearchSearchService(elasticsearchSearchService);

        // Create test data
        SearchResponse mockElasticsearchResponse = SearchResponse.builder()
                .files(new ArrayList<>())
                .folders(new ArrayList<>())
                .query("test content")
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .hasMore(false)
                .build();

        // Mock Elasticsearch search
        when(elasticsearchSearchService.searchByContent(eq("test content"), eq(1L), eq(0), eq(10)))
                .thenReturn(mockElasticsearchResponse);

        // Call the service method
        SearchResponse response = searchServiceWithElastic.searchFileContents("test content", 0, 10);

        // Verify Elasticsearch was used
        verify(elasticsearchSearchService).searchByContent(eq("test content"), eq(1L), eq(0), eq(10));
    }

    @Test
    public void testIndexFile() {
        // Make sure elasticsearch service is set
        searchService.setElasticsearchSearchService(elasticsearchSearchService);

        // Create test data
        File file = new File();
        file.setId(1L);
        file.setFilename("document.txt");

        // Set up the mock to do nothing when indexFile is called (since it's void)
        doNothing().when(elasticsearchSearchService).indexFile(any(File.class));

        // Call the service method
        searchService.indexFile(file);

        // Verify Elasticsearch was used
        verify(elasticsearchSearchService).indexFile(file);
    }

    @Test
    public void testRemoveFileFromIndex() {
        // Make sure elasticsearch service is set
        searchService.setElasticsearchSearchService(elasticsearchSearchService);

        // Set up the mock to do nothing when removeFileIndex is called (since it's void)
        doNothing().when(elasticsearchSearchService).removeFileIndex(anyLong());

        // Call the service method
        searchService.removeFileFromIndex(1L);

        // Verify Elasticsearch was used
        verify(elasticsearchSearchService).removeFileIndex(1L);
    }
}