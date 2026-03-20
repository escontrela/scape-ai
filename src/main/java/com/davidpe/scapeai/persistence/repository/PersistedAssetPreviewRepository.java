package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.PersistedAssetType;
import java.util.Map;
import java.util.Optional;

public interface PersistedAssetPreviewRepository {

  Optional<Map<String, Object>> findByTypeAndId(PersistedAssetType type, String assetId);
}
