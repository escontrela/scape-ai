package com.davidpe.scapeai.application;

public enum RecentRunsSortOption {
  BY_DATE("Date"),
  BY_REWARD("Reward");

  private final String label;

  RecentRunsSortOption(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
