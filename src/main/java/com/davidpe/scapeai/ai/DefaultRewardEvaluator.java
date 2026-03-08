package com.davidpe.scapeai.ai;

import org.springframework.stereotype.Component;

@Component
public class DefaultRewardEvaluator implements RewardEvaluator {

  @Override
  public RewardAssessment evaluate(RewardContext context) {
    if (context.stepResult().state().exitReached()) {
      return RewardAssessment.of(RewardSignal.POSITIVE);
    }
    if (context.stepResult().collision()) {
      return RewardAssessment.of(RewardSignal.VERY_NEGATIVE);
    }
    return RewardAssessment.of(RewardSignal.NEGATIVE);
  }
}
