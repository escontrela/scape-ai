package com.davidpe.scapeai.application;

public record PersistedAssetDeletionCandidate(
    PersistedAssetRef ref, boolean eligible, String validationMessage) {}
