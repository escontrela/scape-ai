package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.PersistedAssetCatalogItem;
import java.util.List;

public interface PersistedAssetCatalogRepository {

  List<PersistedAssetCatalogItem> findAll();
}
