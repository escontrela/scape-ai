package com.davidpe.scapeai.application;

public record PersistedAssetCatalogItem(
    PersistedAssetType type,
    String assetId,
    Long updatedAtEpochMillis,
    long estimatedSizeBytes,
    String metadataSummary) {}
