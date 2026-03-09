package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.repository.MazeCoverageRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MazeCoverageSummaryService {

  private final MazeCoverageRepository mazeCoverageRepository;

  public MazeCoverageSummaryService(MazeCoverageRepository mazeCoverageRepository) {
    this.mazeCoverageRepository = mazeCoverageRepository;
  }

  public List<MazeCoverageSummaryRow> pendingCoverage() {
    return mazeCoverageRepository.findPendingCoverageSummary().stream()
        .map(entity -> new MazeCoverageSummaryRow(entity.mazeName(), entity.pendingPolicies()))
        .toList();
  }
}
