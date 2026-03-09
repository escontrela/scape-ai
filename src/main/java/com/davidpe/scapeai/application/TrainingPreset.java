package com.davidpe.scapeai.application;

import java.time.Duration;

public record TrainingPreset(Long id, int episodes, Duration timeout, String policy, Long seed) {}
