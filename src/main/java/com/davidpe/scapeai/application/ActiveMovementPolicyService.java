package com.davidpe.scapeai.application;

import com.davidpe.scapeai.ai.MovementPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActiveMovementPolicyService {

  private static final String HEURISTIC_BASELINE = "heuristic-baseline";
  private static final String RANDOM_CONTROLLED = "random-controlled";

  private final Map<String, MovementPolicy> policiesById;
  private final List<MovementPolicyOption> options;
  private final AtomicReference<String> activePolicyId;

  public ActiveMovementPolicyService(
      @Qualifier("heuristicBaselineMovementPolicy") MovementPolicy heuristicBaseline,
      @Qualifier("randomControlledMovementPolicy") MovementPolicy randomControlled,
      @Value("${scape.ai.policy:heuristic-baseline}") String defaultPolicy) {
    this(
        Map.of(HEURISTIC_BASELINE, heuristicBaseline, RANDOM_CONTROLLED, randomControlled),
        defaultPolicy);
  }

  ActiveMovementPolicyService(Map<String, MovementPolicy> policiesById, String defaultPolicy) {
    if (policiesById.isEmpty()) {
      throw new IllegalArgumentException("At least one movement policy must be registered");
    }

    LinkedHashMap<String, MovementPolicy> ordered = new LinkedHashMap<>();
    policiesById.forEach(ordered::put);
    this.policiesById = Map.copyOf(ordered);
    this.options =
        List.of(
            new MovementPolicyOption(HEURISTIC_BASELINE, "Heuristic baseline"),
            new MovementPolicyOption(RANDOM_CONTROLLED, "Random controlled"));

    String firstAvailable = this.policiesById.keySet().iterator().next();
    String effectiveDefault = this.policiesById.containsKey(defaultPolicy) ? defaultPolicy : firstAvailable;
    this.activePolicyId = new AtomicReference<>(effectiveDefault);
  }

  public List<MovementPolicyOption> availablePolicies() {
    return options;
  }

  public String activePolicyId() {
    return activePolicyId.get();
  }

  public void selectPolicy(String policyId) {
    if (!policiesById.containsKey(policyId)) {
      throw new IllegalArgumentException("Unsupported movement policy: " + policyId);
    }
    activePolicyId.set(policyId);
  }

  public MovementPolicy activePolicy() {
    String selected = activePolicyId.get();
    MovementPolicy policy = policiesById.get(selected);
    if (policy == null) {
      throw new IllegalStateException("No policy registered for id: " + selected);
    }
    return policy;
  }
}
