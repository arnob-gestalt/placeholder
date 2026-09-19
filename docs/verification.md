# Clinicalc verification notes

Current commit verification scope:

- Android project remains Kotlin/Compose with compileSdk 35 and targetSdk 35.
- Master catalog is still copied into `app/src/main/assets/catalog.md` during `preBuild`.
- Typed implementations included: anion gap, corrected calcium, serum osmolality, MAP, MELD clamps, QTc Bazett, CHA₂DS₂-VASc, and core unit conversions.
- Inputs are held only in Compose state and are not persisted.
- Flagged/reference presentation uses an amber solid surface and explicit verification copy.

Required next validation command: `./gradlew assembleDebug`.
