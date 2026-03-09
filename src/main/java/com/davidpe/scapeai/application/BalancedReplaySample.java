package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;

public record BalancedReplaySample(
    ExperienceTransitionEntity transition, ExperienceReplayOutcome outcome) {}
