package com.davidpe.scapeai.application;

import java.time.Duration;

public record TrainingPresetDraft(int episodes, Duration timeout, String policy, Long seed) {}
