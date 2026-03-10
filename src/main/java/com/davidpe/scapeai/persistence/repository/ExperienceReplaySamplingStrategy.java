package com.davidpe.scapeai.persistence.repository;

public enum ExperienceReplaySamplingStrategy {
  UNIFORM("uniform"),
  REWARD_AWARE("reward-aware"),
  NOVELTY_AWARE("novelty-aware");

  private final String id;

  ExperienceReplaySamplingStrategy(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
