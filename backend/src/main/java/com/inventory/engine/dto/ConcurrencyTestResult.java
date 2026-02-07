package com.inventory.engine.dto;

public class ConcurrencyTestResult {

    private int totalRequests;
    private int successCount;
    private int conflictCount;
    private Integer finalQuantity;
    private Long finalVersion;
    private long durationMs;

    public ConcurrencyTestResult() {}

    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getConflictCount() { return conflictCount; }
    public void setConflictCount(int conflictCount) { this.conflictCount = conflictCount; }

    public Integer getFinalQuantity() { return finalQuantity; }
    public void setFinalQuantity(Integer finalQuantity) { this.finalQuantity = finalQuantity; }

    public Long getFinalVersion() { return finalVersion; }
    public void setFinalVersion(Long finalVersion) { this.finalVersion = finalVersion; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
