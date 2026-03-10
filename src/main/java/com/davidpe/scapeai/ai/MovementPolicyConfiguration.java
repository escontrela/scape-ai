package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.application.SessionRandomSource;
import com.davidpe.scapeai.ai.infrastructure.djl.DjlDirectionPredictor;
import com.davidpe.scapeai.ai.infrastructure.djl.DjlMovementPolicyAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
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
  public MovementPolicy randomControlledMovementPolicy(SessionRandomSource sessionRandomSource) {
    return new RandomControlledMovementPolicy(sessionRandomSource::random);
  }

  @Bean
  public DjlDirectionPredictor djlDirectionPredictor() {
    return context -> null;
  }

  @Bean("djlMovementPolicy")
  public MovementPolicy djlMovementPolicy(
      DjlDirectionPredictor predictor,
      @Qualifier("heuristicBaselineMovementPolicy") MovementPolicy fallbackPolicy,
      @Value("${scape.ai.djl-fallback-confidence-threshold:0.45}") double confidenceThreshold) {
    MovementPolicy djlPrimaryWithTrace = new DjlMovementPolicyAdapter(predictor, context -> null);
    return new CompositeMovementPolicy(
        "djl-composite", djlPrimaryWithTrace, fallbackPolicy, confidenceThreshold);
  }
}
