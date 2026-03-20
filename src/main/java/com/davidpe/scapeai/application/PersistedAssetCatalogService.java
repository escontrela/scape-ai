package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.repository.PersistedAssetCatalogRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PersistedAssetCatalogService {

  private final PersistedAssetCatalogRepository repository;

  public PersistedAssetCatalogService(PersistedAssetCatalogRepository repository) {
    this.repository = repository;
  }

  public List<PersistedAssetCatalogItem> listAll() {
    return repository.findAll().stream()
        .sorted(
            Comparator.comparing(PersistedAssetCatalogItem::type)
                .thenComparing(
                    PersistedAssetCatalogItem::updatedAtEpochMillis,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PersistedAssetCatalogItem::assetId))
        .toList();
  }
}
