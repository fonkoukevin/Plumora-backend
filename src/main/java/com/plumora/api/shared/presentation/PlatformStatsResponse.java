package com.plumora.api.shared.presentation;

// Field names are part of the public contract with the frontend (docs/api-contract.md,
// "Public stats") - the Flutter client's PlatformStatsModel reads exactly these three keys.
public record PlatformStatsResponse(long totalBooks, long totalAuthors, long totalReaders) {
}
