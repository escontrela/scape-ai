package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.application.PolicyInferenceTrace;
import java.util.Optional;

public interface InferenceTraceProvider {

  Optional<PolicyInferenceTrace> latestInferenceTrace();
}
