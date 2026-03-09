package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.MazeCoverageSummaryEntity;
import java.util.List;

public interface MazeCoverageRepository {

  void upsertCoverage(long mazeId, String policyId, boolean solved, long updatedAtEpochMillis);

  List<MazeCoverageSummaryEntity> findPendingCoverageSummary();
}
