package com.davidpe.scapeai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MovementPolicyConfiguration {

  private static final String BASELINE_POLICY = "heuristic-baseline";

  @Bean
  public MovementPolicy movementPolicy(
      @Value("${scape.ai.policy:heuristic-baseline}") String policyName) {
    if (BASELINE_POLICY.equals(policyName)) {
      return new SimpleMovementPolicy();
    }
    throw new IllegalArgumentException("Unsupported movement policy: " + policyName);
  }
}
