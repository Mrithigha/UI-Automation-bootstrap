package com.hr.ui.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/*
 * DownloadUtils — helpers for testing file downloads.
 *
 * The browser is already configured to save files to a known folder (set via download.dir
 * in config.properties, default: downloads/). These methods let you wait for a file to
 * appear there and verify it looks right.
 *
 * Typical use after clicking an "Export CSV" button:
 *   File csv = DownloadUtils.waitForDownload(
 *       ConfigManager.get("download.dir", "downloads"),
 *       ".*\\.csv",  // regex for the expected file name
 *       30           // seconds to wait before giving up
 *   );
 *   assertTrue(csv.length() > 0, "Downloaded CSV should not be empty");
 */
public final class DownloadUtils {

    private static final Logger log = LogManager.getLogger(DownloadUtils.class);
    private static final int POLL_INTERVAL_MS = 500;

    private DownloadUtils() {}

    // polls until a file matching the regex lands in downloadDir — skips .crdownload/.part temp files
    public static File waitForDownload(String downloadDir, String fileNameRegex, int timeoutSeconds) {
        log.info("Waiting up to {}s for file matching '{}' in: {}", timeoutSeconds, fileNameRegex, downloadDir);
        File dir = new File(downloadDir);
        if (!dir.exists()) dir.mkdirs();

        long deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1000;

        while (System.currentTimeMillis() < deadline) {
            File[] matches = dir.listFiles((d, name) ->
                    name.matches(fileNameRegex)
                    && !name.endsWith(".crdownload")
                    && !name.endsWith(".part")
                    && !name.endsWith(".tmp"));

            if (matches != null && matches.length > 0) {
                File latest = Arrays.stream(matches)
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElseThrow();
                log.info("Download complete: {} ({} bytes)", latest.getAbsolutePath(), latest.length());
                return latest;
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for download", e);
            }
        }

        throw new RuntimeException(
            "Download did not complete within " + timeoutSeconds + "s for pattern: " + fileNameRegex
            + " in directory: " + downloadDir);
    }

    // quick non-waiting check — useful when you know the download should already be done
    public static boolean isFileDownloaded(String downloadDir, String fileName) {
        File file = new File(downloadDir, fileName);
        boolean exists = file.exists() && file.length() > 0;
        log.debug("isFileDownloaded('{}') = {}", file.getAbsolutePath(), exists);
        return exists;
    }

    // wipes the download folder — call in @Before so leftover files from previous runs don't cause false passes
    public static void clearDownloadDir(String downloadDir) {
        File dir = new File(downloadDir);
        if (!dir.exists()) {
            log.debug("Download directory does not exist yet, nothing to clear: {}", downloadDir);
            return;
        }
        File[] files = dir.listFiles();
        int deleted = 0;
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.delete()) deleted++;
            }
        }
        log.info("Cleared {} file(s) from download directory: {}", deleted, downloadDir);
    }

    // returns -1 if the file doesn't exist
    public static long fileSizeBytes(File file) {
        return file != null && file.exists() ? file.length() : -1L;
    }
}
