package com.fileflow.util;

public final class Constants {

    // JWT Related Constants
    public static long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    public static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days

    // File Related Constants
    public static final long DEFAULT_STORAGE_QUOTA = 10L * 1024L * 1024L * 1024L; // 10GB
    public static final long MIN_STORAGE_QUOTA = 1L * 1024L * 1024L * 1024L; // 1GB
    public static final int MAX_FILENAME_LENGTH = 255;

    // Trash Settings
    public static final int TRASH_RETENTION_DAYS = 30;

    // Version-related Activity Constants
    public static final String ACTIVITY_CREATE_VERSION = "CREATE_VERSION";
    public static final String ACTIVITY_RESTORE_VERSION = "RESTORE_VERSION";
    public static final String ACTIVITY_DELETE_VERSION = "DELETE_VERSION";

    // Pagination Defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // User Activity Types
    public static final String ACTIVITY_LOGIN = "LOGIN";
    public static final String ACTIVITY_LOGOUT = "LOGOUT";
    public static final String ACTIVITY_UPLOAD = "UPLOAD";
    public static final String ACTIVITY_DOWNLOAD = "DOWNLOAD";
    public static final String ACTIVITY_DELETE = "DELETE";
    public static final String ACTIVITY_RESTORE = "RESTORE";
    public static final String ACTIVITY_PERMANENT_DELETE = "PERMANENT_DELETE";
    public static final String ACTIVITY_CREATE_FOLDER = "CREATE_FOLDER";
    public static final String ACTIVITY_UPDATE_FOLDER = "UPDATE_FOLDER";
    public static final String ACTIVITY_UPDATE_FILE = "UPDATE_FILE";
    public static final String ACTIVITY_MOVE = "MOVE";
    public static final String ACTIVITY_COPY = "COPY";
    public static final String ACTIVITY_SHARE = "SHARE";
    public static final String ACTIVITY_UPDATE_SHARE = "UPDATE_SHARE";
    public static final String ACTIVITY_DELETE_SHARE = "DELETE_SHARE";
    public static final String ACTIVITY_EMPTY_TRASH = "EMPTY_TRASH";
    public static final String ACTIVITY_RESTORE_ALL = "RESTORE_ALL";
    public static final String ACTIVITY_CREATE_TAG = "CREATE_TAG";
    public static final String ACTIVITY_UPDATE_TAG = "UPDATE_TAG";
    public static final String ACTIVITY_DELETE_TAG = "DELETE_TAG";
    public static final String ACTIVITY_TAG_FILE = "TAG_FILE";
    public static final String ACTIVITY_UNTAG_FILE = "UNTAG_FILE";
    public static final String ACTIVITY_COMMENT = "COMMENT";
    public static final String ACTIVITY_UPDATE_COMMENT = "UPDATE_COMMENT";
    public static final String ACTIVITY_DELETE_COMMENT = "DELETE_COMMENT";
    public static final String ACTIVITY_DEVICE_REGISTER = "DEVICE_REGISTER";
    public static final String ACTIVITY_DEVICE_UPDATE = "DEVICE_UPDATE";
    public static final String ACTIVITY_DEVICE_DELETE = "DEVICE_DELETE";
    public static final String ACTIVITY_DEVICE_SYNC = "DEVICE_SYNC";

    // Folder Item Types
    public static final String ITEM_TYPE_FILE = "FILE";
    public static final String ITEM_TYPE_FOLDER = "FOLDER";
    public static final String ITEM_TYPE_SHARE = "SHARE";
    public static final String ITEM_TYPE_TAG = "TAG";
    public static final String ITEM_TYPE_COMMENT = "COMMENT";
    public static final String ITEM_TYPE_DEVICE = "DEVICE";

    // Roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // Storage constants
    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    // Share Permission Types
    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";
    public static final String PERMISSION_COMMENT = "COMMENT";

    // Security
    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 30 * 24 * 60 * 60; // 30 days
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    private Constants() {
        // Private constructor to prevent instantiation
    }
}