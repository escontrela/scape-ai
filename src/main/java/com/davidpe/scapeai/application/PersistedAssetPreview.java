package com.davidpe.scapeai.application;

public record PersistedAssetPreview(
    PersistedAssetType type, String assetId, String content, boolean truncated) {}
