#!/bin/bash

set -eux

function yml-dagger-version {
  # TODO(bcorso): Consider installing a proper yml parser like "yq"
  # We expect something like `daggerVersion: "2.32"` and convert it into `2.32`
  sed -rn 's/daggerVersion: "(.*)"/\1/p' $1
}

echo "Verifying Dagger version YML config matches latest:"

readonly YML_CONFIG=$1
readonly CWD=$(dirname $0)

readonly LATEST_DAGGER_VERSION=$($CWD/latest-dagger-version.sh)

readonly YML_DAGGER_VERSION=$(yml-dagger-version $YML_CONFIG)

# Fail if the latest dagger version is not correct
if [ "$LATEST_DAGGER_VERSION" != "$YML_DAGGER_VERSION" ]; then
  echo "The Dagger yml version does not match the latest Dagger version:"
  echo "    Latest Dagger version: $LATEST_DAGGER_VERSION"
  echo "       YML Dagger version: $YML_DAGGER_VERSION"
  exit 1
fi


echo "Done! The Dagger version up to date."
