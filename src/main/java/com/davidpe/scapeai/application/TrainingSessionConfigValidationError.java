package com.davidpe.scapeai.application;

public record TrainingSessionConfigValidationError(
    TrainingSessionConfigErrorCode code, String message) {}
