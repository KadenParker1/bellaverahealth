package com.pm.bellavera.admin.api;

/** How many accounts a broadcast actually went to, and how many sends failed. */
public record BroadcastEmailResult(int sentCount, int failedCount) {
}
