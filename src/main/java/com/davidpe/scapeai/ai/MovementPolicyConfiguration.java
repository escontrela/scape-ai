package com.davidpe.scapeai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MovementPolicyConfiguration {

  @Bean("heuristicBaselineMovementPolicy")
  public MovementPolicy heuristicBaselineMovementPolicy() {
    return new SimpleMovementPolicy();
  }

  @Bean("randomControlledMovementPolicy")
  public MovementPolicy randomControlledMovementPolicy(
      @Value("${scape.ai.random-seed:20260309}") long seed) {
    return new RandomControlledMovementPolicy(seed);
  }
}
