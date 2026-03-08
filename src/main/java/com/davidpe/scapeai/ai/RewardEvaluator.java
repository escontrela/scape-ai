package com.davidpe.scapeai.ai;

public interface RewardEvaluator {

  RewardAssessment evaluate(RewardContext context);
}
