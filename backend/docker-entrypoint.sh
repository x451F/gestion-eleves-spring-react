#!/bin/sh
set -eu

# This is a development-only Compose convenience. The key is generated into a
# named Docker volume, never copied into the image or source tree, and survives
# ordinary container restarts so active sessions remain valid during a demo.
secret_file="${APP_SECRET_CONFIGTREE_PATH:-/run/secrets/}app.security.jwt.secret-base64"
if [ ! -s "$secret_file" ]; then
  umask 077
  tmp_file="${secret_file}.$$"
  head -c 32 /dev/urandom | base64 | tr -d '\n' > "$tmp_file"
  mv "$tmp_file" "$secret_file"
fi

# Preserve development photos that were created before Compose used its named
# storage volume. Existing volume files win, so repeated starts are harmless.
if [ -d /seed-uploads ]; then
  cp -an /seed-uploads/. /data/uploads/eleves/ 2>/dev/null || true
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar "$@"
