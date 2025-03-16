package com.fileflow.service.user;

import com.fileflow.dto.request.user.PasswordChangeRequest;
import com.fileflow.dto.request.user.UserUpdateRequest;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;

public interface UserService {
    UserResponse getCurrentUser();

    UserResponse getUserById(Long id);

    UserResponse updateUser(UserUpdateRequest updateRequest);

    ApiResponse changePassword(PasswordChangeRequest passwordChangeRequest);

    ApiResponse deleteAccount();

    UserResponse getUserStorageInfo();
}