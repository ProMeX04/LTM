#!/usr/bin/env zsh
set -e

# Build once to avoid racing maven processes
(cd client && mvn -q -DskipTests package)

# Run two clients with a short delay to prevent .m2/target locking issues
(cd client && mvn -q javafx:run) &
sleep 5
(cd client && mvn -q javafx:run) &

echo "Started 2 client instances. Use 'jobs' to see them."
