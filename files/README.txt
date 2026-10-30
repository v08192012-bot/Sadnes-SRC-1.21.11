Files published by CI.
Jars larger than 68 MB were split into *.part.aa, *.part.ab, ... parts.
Reassemble a split jar with:
  cat <jar>.part.aa <jar>.part.ab ... > <jar>
  (parts sort lexically; see SPLIT_MANIFEST.txt)
Verify integrity: sha256sum -c SHA256SUMS.txt
