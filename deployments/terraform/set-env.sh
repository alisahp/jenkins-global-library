#!/usr/bin/env bash

wget --quiet --no-check-certificate "https://raw.githubusercontent.com/alisahp/common_scripts/master/set-environments/terraform/google-set-env.sh" \
  -O set-env >/dev/null
source 'set-env' "$@"
rm -rf "set-env"
