# Search System Integration

## Overview

The FileFlow search system has been redesigned to leverage both traditional database queries and Elasticsearch for full-text search capabilities. The new architecture provides a seamless search experience while gracefully degrading when Elasticsearch is not available.

## Key Components

### 1. SearchService Interface
The central interface defining all search operations, including:
- Basic search across files and folders
- File-specific and folder-specific searches
- Type-based searches
- Content-based searches (Elasticsearch)
- Tag-based searches
- File indexing operations

### 2. SearchServiceImpl
The primary implementation that:
- Handles basic searches using database queries
- Delegates to Elasticsearch when available for advanced searches
- Maps between entity objects and DTOs
- Provides consistent response formatting

### 3. ElasticsearchSearchService Interface
A specialized interface for Elasticsearch-specific operations:
- Full-text search across file content
- Index management
- Advanced search capabilities

### 4. ElasticsearchSearchServiceImpl
Implementation that handles Elasticsearch queries:
- Text extraction and indexing
- Content-based search
- Tag search
- File type filtering
- Full-text search with relevance scoring

### 5. SearchController
RESTful API endpoints for all search capabilities:
- General search endpoints
- Specialized search endpoints for different content types
- Admin operations for indexing management

## Integration Approach

### Profile-Based Configuration
- Elasticsearch functionality is enabled via the "elasticsearch" Spring profile
- System gracefully degrades when Elasticsearch is not available

### Automatic Content Indexing
- Files are automatically indexed when uploaded or modified
- Text extraction is performed for supported file types
- Index is kept in sync with file operations (move, delete, restore)

### Search Priority Logic
- Metadata searches are handled by database queries for performance
- Content searches are routed to Elasticsearch
- Complex queries leverage Elasticsearch's full-text capabilities
- Simple queries can use either system based on availability

## Key Search Features

1. **Full-Text Search**
    - Search across file names, metadata, and content
    - Relevance-based results ordering
    - Support for phrase matching and fuzzy search

2. **Filtered Searches**
    - By file type (document, image, video, etc.)
    - By user tags
    - By folder location
    - By favorite status

3. **Advanced Search**
    - Content-based search within files
    - Combination of metadata and content criteria
    - Support for complex queries with multiple conditions

4. **Administrative Features**
    - Manual indexing triggers
    - Full re-indexing capabilities
    - Index health monitoring

## Implementation Considerations

### Performance Optimizations
- Elasticsearch queries are optimized for relevance
- Database queries use appropriate indexes
- Response pagination is implemented consistently
- Asynchronous indexing for large files

### Extension Points
- Support for additional file formats
- Advanced filtering options
- Search result highlighting
- Search suggestions and autocomplete

## Usage Examples

### Basic Search
```
GET /api/v1/search?query=project&page=0&size=20
```

### Advanced Content Search
```
GET /api/v1/search/content?query=financial report 2023&page=0&size=20
```

### File Type Search
```
GET /api/v1/search/by-type/document?query=budget&page=0&size=20
```

### Tag-Based Search
```
GET /api/v1/search/tags/important?page=0&size=20
```

## Conclusion

The integrated search system provides a comprehensive solution that leverages the best of both worlds: fast database queries for simple searches and powerful full-text capabilities via Elasticsearch when available. The architecture ensures that basic functionality remains available even when Elasticsearch is not configured, while providing advanced search capabilities when it is.