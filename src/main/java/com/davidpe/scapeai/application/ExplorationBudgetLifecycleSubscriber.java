package com.davidpe.scapeai.application;

import java.util.EnumSet;
import org.springframework.stereotype.Component;

@Component
public class ExplorationBudgetLifecycleSubscriber {

  public ExplorationBudgetLifecycleSubscriber(
      TrainingLifecycleSubscriberRouter router, ExplorationBudgetService explorationBudgetService) {
    router.register(
        "exploration-budget-subscriber",
        EnumSet.of(TrainingLifecycleEventType.TIMED_OUT, TrainingLifecycleEventType.FINISHED),
        event -> {
          if (event.type() == TrainingLifecycleEventType.FINISHED
              && event.detail() != null
              && event.detail().contains("RESET")) {
            return;
          }
          explorationBudgetService.consumeEpisodeBudget();
        });
  }
}
