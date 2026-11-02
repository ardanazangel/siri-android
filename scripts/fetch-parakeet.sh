#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/libs app/src/main/assets/models/parakeet
version="1.13.3"
aar="app/libs/sherpa-onnx-$version.aar"
if [ ! -s "$aar" ]; then
  curl --fail --location --retry 3 --output "$aar.part" "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$version/sherpa-onnx-$version.aar"
  mv "$aar.part" "$aar"
fi
base="https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8/resolve/main"
for name in encoder.int8.onnx decoder.int8.onnx joiner.int8.onnx tokens.txt; do
  dest="app/src/main/assets/models/parakeet/$name"
  if [ ! -s "$dest" ]; then
    curl --fail --location --retry 3 --output "$dest.part" "$base/$name"
    mv "$dest.part" "$dest"
  fi
done
echo "Offline Parakeet assets ready."
