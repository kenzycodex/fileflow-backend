package com.fileflow.dto.request.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncBatchRequest {
    private List<Long> successfulItems;
    private List<Long> failedItems;
}