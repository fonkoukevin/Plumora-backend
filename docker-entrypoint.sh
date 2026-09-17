#!/bin/sh
# Runs as root (the image's default user - see Dockerfile) so it can fix ownership of
# /app/uploads before dropping to the unprivileged "plumora" user to actually run the JVM.
#
# Why this is needed: /app/uploads is a named Docker volume (backend_uploads in
# deploy/compose.prod.yml), not part of the image. Docker only initializes a volume's
# ownership from the image on its very first creation - a volume created back when this
# container ran as root (before the non-root "plumora" user existed) keeps that root
# ownership forever after, even once the image switches to a non-root user. The app then
# silently fails every write under that path (book cover uploads - see
# LocalBookCoverStorage.store) with no visible error, because the frontend's
# _isMultipartUnsupported fallback (book_api_service.dart) retries without the cover
# instead of surfacing the failure.
#
# This fixes that drift on every container start, for any volume state, instead of relying
# on a one-off manual `docker exec chown` on the VPS.
set -eu

mkdir -p /app/uploads
chown -R plumora:plumora /app/uploads

exec su-exec plumora:plumora java -jar app.jar
