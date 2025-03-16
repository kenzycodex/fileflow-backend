package com.fileflow.task;

import com.fileflow.service.file.FileService;
import com.fileflow.service.quota.QuotaService;
import com.fileflow.service.share.ShareService;
import com.fileflow.service.trash.TrashService;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final FileService fileService;
    private final ShareService shareService;
    private final TrashService trashService;
    private final QuotaService quotaService;

    /**
     * Clean up files that have been in trash for more than 30 days
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupTrash() {
        log.info("Starting scheduled trash cleanup");
        int count = fileService.cleanupDeletedFiles(Constants.TRASH_RETENTION_DAYS);
        log.info("Completed trash cleanup. Deleted {} files.", count);
    }

    /**
     * Delete expired shares
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredShares() {
        log.info("Starting scheduled cleanup of expired shares");
        int count = shareService.deleteExpiredShares();
        log.info("Completed expired shares cleanup. Deleted {} shares.", count);
    }

    /**
     * Check and update user quota usage
     * Runs weekly on Sunday at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * 0")
    public void updateQuotaUsage() {
        log.info("Starting scheduled quota usage update");
        // Implementation would go here, checking actual storage against reported usage
        log.info("Completed quota usage update");
    }
}