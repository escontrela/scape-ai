package com.davidpe.scapeai.application;

import org.springframework.stereotype.Component;

@Component
public class TrainingSessionContextHolder {

  private volatile String mazeName;
  private volatile String policyId;

  public void activate(String mazeName, String policyId) {
    this.mazeName = mazeName;
    this.policyId = policyId;
  }

  public String mazeName() {
    return mazeName;
  }

  public String policyId() {
    return policyId;
  }
}
