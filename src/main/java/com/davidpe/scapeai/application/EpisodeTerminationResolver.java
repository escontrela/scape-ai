package com.davidpe.scapeai.application;

final class EpisodeTerminationResolver {

  private EpisodeTerminationResolver() {}

  static EpisodeEndReason resolve(boolean exitReached, boolean timeoutReached, boolean aborted) {
    int terminals = (exitReached ? 1 : 0) + (timeoutReached ? 1 : 0) + (aborted ? 1 : 0);
    if (terminals != 1) {
      throw new IllegalStateException(
          "Invalid terminal state combination: exitReached="
              + exitReached
              + ", timeoutReached="
              + timeoutReached
              + ", aborted="
              + aborted);
    }
    if (exitReached) {
      return EpisodeEndReason.EXIT_REACHED;
    }
    if (timeoutReached) {
      return EpisodeEndReason.TIMEOUT;
    }
    return EpisodeEndReason.ABORTED;
  }
}
