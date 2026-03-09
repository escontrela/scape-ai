package com.davidpe.scapeai.ai;

import org.springframework.stereotype.Component;

@Component
public class DefaultRewardEvaluator implements RewardEvaluator {

  @Override
  public RewardAssessment evaluate(RewardContext context) {
    RewardSignal signal;
    if (context.stepResult().state().exitReached()) {
      signal = RewardSignal.POSITIVE;
    } else if (context.stepResult().collision()) {
      signal = RewardSignal.VERY_NEGATIVE;
    } else if (context.loopDetected()) {
      signal = RewardSignal.VERY_NEGATIVE;
    } else {
      signal = RewardSignal.NEGATIVE;
    }
    double value = signal.score();
    if (context.discoveredNewCell()) {
      value += 0.25;
      if (context.movedToUnderExploredSide()) {
        value += 0.35;
      }
    }
    if (context.loopDetected() && context.transitionRepeatCount() > 0) {
      value -= 0.2 * context.transitionRepeatCount();
    }
    return new RewardAssessment(signal, value);
  }
}
