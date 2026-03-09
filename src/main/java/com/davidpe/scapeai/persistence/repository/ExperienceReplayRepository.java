package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;
import java.util.List;

public interface ExperienceReplayRepository {

  ExperienceTransitionEntity save(ExperienceTransitionEntity transition);

  List<ExperienceTransitionEntity> findRecent(int page, int pageSize);
}
