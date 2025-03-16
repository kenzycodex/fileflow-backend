package com.fileflow.dto.response.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityStatistics {
    private Long userId;
    private String username;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalActivities;
    private Map<String, Long> activityCounts;
    private Map<LocalDate, Long> activityByDate;
}