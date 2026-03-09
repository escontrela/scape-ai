package com.davidpe.scapeai.application;

public record PolicyInferenceTrace(
    String policyId,
    double confidence,
    long latencyMillis,
    boolean fallbackApplied,
    String fallbackReason) {}
