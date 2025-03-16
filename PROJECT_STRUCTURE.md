### FILEFLOW BACKEND STRUCTURE

```
fileflow-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── fileflow/
│   │   │           ├── FileFlowApplication.java
│   │   │           ├── config/
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   ├── JwtConfig.java
│   │   │           │   ├── WebConfig.java
│   │   │           │   ├── StorageConfig.java
│   │   │           │   ├── AsyncConfig.java
│   │   │           │   ├── CacheConfig.java
│   │   │           │   └── SwaggerConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── AuthController.java
│   │   │           │   ├── UserController.java
│   │   │           │   ├── FileController.java
│   │   │           │   ├── FolderController.java
│   │   │           │   ├── ShareController.java
│   │   │           │   ├── SearchController.java
│   │   │           │   ├── TrashController.java
│   │   │           │   ├── TagController.java
│   │   │           │   ├── CommentController.java
│   │   │           │   ├── NotificationController.java
│   │   │           │   ├── DeviceController.java
│   │   │           │   ├── StatisticsController.java
│   │   │           │   ├── AdminController.java
│   │   │           │   ├── SyncController.java
│   │   │           │   └── IntegrationController.java
│   │   │           ├── service/
│   │   │           │   ├── auth/
│   │   │           │   │   ├── AuthService.java
│   │   │           │   │   ├── JwtService.java
│   │   │           │   │   └── AuthServiceImpl.java
│   │   │           │   ├── user/
│   │   │           │   │   ├── UserService.java
│   │   │           │   │   └── UserServiceImpl.java
│   │   │           │   ├── file/
│   │   │           │   │   ├── FileService.java
│   │   │           │   │   └── FileServiceImpl.java
│   │   │           │   ├── folder/
│   │   │           │   │   ├── FolderService.java
│   │   │           │   │   └── FolderServiceImpl.java
│   │   │           │   ├── storage/
│   │   │           │   │   ├── StorageService.java
│   │   │           │   │   ├── LocalStorageService.java
│   │   │           │   │   └── MinioStorageService.java
│   │   │           │   ├── share/
│   │   │           │   │   ├── ShareService.java
│   │   │           │   │   └── ShareServiceImpl.java
│   │   │           │   ├── search/
│   │   │           │   │   ├── SearchService.java
│   │   │           │   │   └── SearchServiceImpl.java
│   │   │           │   ├── notification/
│   │   │           │   │   ├── NotificationService.java
│   │   │           │   │   └── NotificationServiceImpl.java
│   │   │           │   ├── activity/
│   │   │           │   │   ├── ActivityService.java
│   │   │           │   │   └── ActivityServiceImpl.java
│   │   │           │   ├── tag/
│   │   │           │   │   ├── TagService.java
│   │   │           │   │   └── TagServiceImpl.java
│   │   │           │   ├── comment/
│   │   │           │   │   ├── CommentService.java
│   │   │           │   │   └── CommentServiceImpl.java
│   │   │           │   ├── sync/
│   │   │           │   │   ├── SyncService.java
│   │   │           │   │   └── SyncServiceImpl.java
│   │   │           │   ├── quota/
│   │   │           │   │   ├── QuotaService.java
│   │   │           │   │   └── QuotaServiceImpl.java
│   │   │           │   ├── thumbnail/
│   │   │           │   │   ├── ThumbnailService.java
│   │   │           │   │   └── ThumbnailServiceImpl.java
│   │   │           │   ├── version/
│   │   │           │   │   ├── VersioningService.java
│   │   │           │   │   └── VersioningServiceImpl.java
│   │   │           │   ├── statistics/
│   │   │           │   │   ├── StatisticsService.java
│   │   │           │   │   └── StatisticsServiceImpl.java
│   │   │           │   ├── device/
│   │   │           │   │   ├── DeviceService.java
│   │   │           │   │   └── DeviceServiceImpl.java
│   │   │           │   └── health/
│   │   │           │       ├── HealthService.java
│   │   │           │       └── HealthServiceImpl.java
│   │   │           ├── model/
│   │   │           │   ├── User.java
│   │   │           │   ├── UserSettings.java
│   │   │           │   ├── File.java
│   │   │           │   ├── Folder.java
│   │   │           │   ├── FileVersion.java
│   │   │           │   ├── Share.java
│   │   │           │   ├── Activity.java
│   │   │           │   ├── Tag.java
│   │   │           │   ├── FileTag.java
│   │   │           │   ├── Comment.java
│   │   │           │   ├── Notification.java
│   │   │           │   ├── NotificationPreference.java
│   │   │           │   ├── Device.java
│   │   │           │   ├── SyncQueue.java
│   │   │           │   ├── StorageChunk.java
│   │   │           │   ├── FileAccessLog.java
│   │   │           │   └── QuotaExtension.java
│   │   │           ├── repository/
│   │   │           │   ├── UserRepository.java
│   │   │           │   ├── UserSettingsRepository.java
│   │   │           │   ├── FileRepository.java
│   │   │           │   ├── FolderRepository.java
│   │   │           │   ├── FileVersionRepository.java
│   │   │           │   ├── ShareRepository.java
│   │   │           │   ├── ActivityRepository.java
│   │   │           │   ├── TagRepository.java
│   │   │           │   ├── FileTagRepository.java
│   │   │           │   ├── CommentRepository.java
│   │   │           │   ├── NotificationRepository.java
│   │   │           │   ├── NotificationPreferenceRepository.java
│   │   │           │   ├── DeviceRepository.java
│   │   │           │   ├── SyncQueueRepository.java
│   │   │           │   ├── StorageChunkRepository.java
│   │   │           │   ├── FileAccessLogRepository.java
│   │   │           │   └── QuotaExtensionRepository.java
│   │   │           ├── dto/
│   │   │           │   ├── request/
│   │   │           │   │   ├── auth/
│   │   │           │   │   │   ├── SignUpRequest.java
│   │   │           │   │   │   ├── SignInRequest.java
│   │   │           │   │   │   ├── RefreshTokenRequest.java
│   │   │           │   │   │   └── PasswordResetRequest.java
│   │   │           │   │   ├── user/
│   │   │           │   │   │   ├── UserUpdateRequest.java
│   │   │           │   │   │   └── PasswordChangeRequest.java
│   │   │           │   │   ├── file/
│   │   │           │   │   │   ├── FileUploadRequest.java
│   │   │           │   │   │   ├── FileUpdateRequest.java
│   │   │           │   │   │   └── ChunkUploadRequest.java
│   │   │           │   │   ├── folder/
│   │   │           │   │   │   ├── FolderCreateRequest.java
│   │   │           │   │   │   └── FolderUpdateRequest.java
│   │   │           │   │   └── share/
│   │   │           │   │       ├── ShareCreateRequest.java
│   │   │           │   │       └── ShareUpdateRequest.java
│   │   │           │   ├── response/
│   │   │           │   │   ├── auth/
│   │   │           │   │   │   ├── JwtResponse.java
│   │   │           │   │   │   └── UserResponse.java
│   │   │           │   │   ├── file/
│   │   │           │   │   │   ├── FileResponse.java
│   │   │           │   │   │   ├── FileListResponse.java
│   │   │           │   │   │   └── FileUploadResponse.java
│   │   │           │   │   ├── folder/
│   │   │           │   │   │   ├── FolderResponse.java
│   │   │           │   │   │   └── FolderContentsResponse.java
│   │   │           │   │   ├── share/
│   │   │           │   │   │   └── ShareResponse.java
│   │   │           │   │   └── common/
│   │   │           │   │       ├── ApiResponse.java
│   │   │           │   │       └── PagedResponse.java
│   │   │           │   └── mapper/
│   │   │           │       ├── UserMapper.java
│   │   │           │       ├── FileMapper.java
│   │   │           │       ├── FolderMapper.java
│   │   │           │       └── ShareMapper.java
│   │   │           ├── exception/
│   │   │           │   ├── GlobalExceptionHandler.java
│   │   │           │   ├── ResourceNotFoundException.java
│   │   │           │   ├── BadRequestException.java
│   │   │           │   ├── UnauthorizedException.java
│   │   │           │   ├── ForbiddenException.java
│   │   │           │   ├── StorageException.java
│   │   │           │   ├── FileException.java
│   │   │           │   └── QuotaExceededException.java
│   │   │           ├── security/
│   │   │           │   ├── JwtAuthenticationFilter.java
│   │   │           │   ├── JwtTokenProvider.java
│   │   │           │   ├── UserPrincipal.java
│   │   │           │   ├── CustomUserDetailsService.java
│   │   │           │   └── JwtAuthenticationEntryPoint.java
│   │   │           ├── bridge/
│   │   │           │   ├── FileSystemBridge.java
│   │   │           │   ├── NotificationBridge.java
│   │   │           │   ├── DeviceInfoBridge.java
│   │   │           │   ├── OfflineBridge.java
│   │   │           │   └── MediaBridge.java
│   │   │           ├── util/
│   │   │           │   ├── FileUtils.java
│   │   │           │   ├── SecurityUtils.java
│   │   │           │   ├── DateUtils.java
│   │   │           │   ├── ValidationUtils.java
│   │   │           │   ├── StringUtils.java
│   │   │           │   ├── Constants.java
│   │   │           │   └── ResponseBuilder.java
│   │   │           ├── event/
│   │   │           │   ├── listener/
│   │   │           │   │   ├── FileEventListener.java
│   │   │           │   │   ├── UserEventListener.java
│   │   │           │   │   └── ShareEventListener.java
│   │   │           │   └── publisher/
│   │   │           │       ├── FileEventPublisher.java
│   │   │           │       ├── UserEventPublisher.java
│   │   │           │       └── ShareEventPublisher.java
│   │   │           ├── task/
│   │   │           │   ├── CleanupTask.java
│   │   │           │   ├── QuotaCheckTask.java
│   │   │           │   ├── ThumbnailGenerationTask.java
│   │   │           │   └── NotificationCleanupTask.java
│   │   │           └── validation/
│   │   │               ├── annotation/
│   │   │               │   └── ValidEmail.java
│   │   │               └── validator/
│   │   │                   └── EmailValidator.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── db/
│   │       │   └── migration/
│   │       │       ├── V1__init.sql
│   │       │       ├── V2__add_tags.sql
│   │       │       └── V3__add_notifications.sql
│   │       ├── static/
│   │       └── templates/
│   │           └── email/
│   │               ├── welcome.html
│   │               ├── reset-password.html
│   │               └── share-notification.html
│   └── test/
│       └── java/
│           └── com/
│               └── fileflow/
│                   ├── controller/
│                   ├── service/
│                   └── repository/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .gitignore
├── README.md
└── scripts/
    ├── setup-db.sh
    └── setup-minio.sh
```