package com.davidpe.scapeai.application;

import java.util.List;

public record PersistedAssetCleanupResult(
    List<PersistedAssetRef> deleted,
    List<PersistedAssetDeletionCandidate> blocked,
    List<PersistedAssetDeletionCandidate> failed) {}
