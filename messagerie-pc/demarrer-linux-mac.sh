#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"
if [ ! -f target/messagerie-pc-1.0.0.jar ]; then
  echo "Compile d'abord avec : mvn clean package"
  exit 1
fi
exec java -jar target/messagerie-pc-1.0.0.jar
