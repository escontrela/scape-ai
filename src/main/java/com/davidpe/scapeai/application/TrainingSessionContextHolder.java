package com.davidpe.scapeai.application;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TrainingSessionContextHolder {

  private volatile String sessionId;
  private volatile String mazeName;
  private volatile String policyId;
  private volatile Long presetId;
  private volatile Long effectiveSeed;
  private volatile Long startedAtEpochMillis;

  public void activate(String mazeName, String policyId) {
    activate(mazeName, policyId, null, null, System.currentTimeMillis());
  }

  public void activate(
      String mazeName,
      String policyId,
      Long presetId,
      Long effectiveSeed,
      Long startedAtEpochMillis) {
    this.sessionId = UUID.randomUUID().toString();
    this.mazeName = mazeName;
    this.policyId = policyId;
    this.presetId = presetId;
    this.effectiveSeed = effectiveSeed;
    this.startedAtEpochMillis =
        startedAtEpochMillis == null ? System.currentTimeMillis() : startedAtEpochMillis;
  }

  public String sessionId() {
    return sessionId;
  }

  public String mazeName() {
    return mazeName;
  }

  public String policyId() {
    return policyId;
  }

  public Long presetId() {
    return presetId;
  }

  public Long effectiveSeed() {
    return effectiveSeed;
  }

  public Long startedAtEpochMillis() {
    return startedAtEpochMillis;
  }
}
