package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActiveMovementPolicyServiceTest {

  @Test
  void shouldSelectPolicyAndExposeCurrentOption() {
    MovementPolicy heuristic = context -> MoveDirection.RIGHT;
    MovementPolicy random = context -> MoveDirection.UP;
    ActiveMovementPolicyService service =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", heuristic, "random-controlled", random), "heuristic-baseline");

    service.selectPolicy("random-controlled");

    assertEquals("random-controlled", service.activePolicyId());
    assertEquals(random, service.activePolicy());
  }

  @Test
  void shouldRejectUnknownPolicy() {
    MovementPolicy policy = context -> MoveDirection.RIGHT;
    ActiveMovementPolicyService service =
        new ActiveMovementPolicyService(
            Map.of("heuristic-baseline", policy, "random-controlled", policy), "heuristic-baseline");

    assertThrows(IllegalArgumentException.class, () -> service.selectPolicy("unknown-policy"));
  }
}
