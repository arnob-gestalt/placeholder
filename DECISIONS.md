# Clinicalc decisions

## D-001 — Native Android platform

**Gate:** architecture/platform
**Choice:** Kotlin + Jetpack Compose native Android app; bundled offline catalog; no web/PWA runtime.
**Rationale:** Explicitly required by the Clinicalc Android build prompt and preserves offline operation.
**Authority:** Android build prompt §0.4 and §2.1 L1–L4.

## D-002 — Conservative clinical fallback

Entries without a complete, machine-checkable formula remain reference/verification cards. The app does not invent coefficients or silently promote uncertain data.

## D-003 — Prototype-to-production boundary

This commit establishes the native offline shell, registry adapter, typed bedside engine, converter foundation, disclaimer surfaces, and verification styling. Full registry ingestion, Room/DataStore persistence, AGSL Glass Lab, complete golden-vector coverage, and device evidence remain tracked as implementation work rather than being represented as complete.

## D-004 — JS engine is the compute source of truth for the web app

**Gate:** registry-vs-hand-spec split
**Choice:** `packages/engine/spec-*.js` + `compute.js` drive all web calculations; `data/registry.json` is the browse/reference catalog only.
**Rationale:** Encyclopedia free-text coefficients are too heterogeneous to auto-compile safely; the CHA2DS2-VASc age-band double-count confirmed generic parsing over-counts. Hand specs use grouped max-of-group summation.

## D-005 — Golden vectors pin source arithmetic, including inconsistencies

Four Appendix B printed maxima disagree with their own tables (Geneva 10 vs 9, ARISCAT 123 vs 116, RIPASA 16 vs 15.0, LODS 22 vs 60 constructed). Tests pin actual behavior and flag the mismatch rather than inventing points. MELD-clamp vector correctly yields 20 (floor bili/INR to 1.0, cap Cr at 4.0).

## D-006 — Web preview ships alongside native Android shell

`web/` is now a module app importing the same engine specs (30 calculators + 10 external-only cards). D-001 native shell is unchanged and still the Android path; the web preview exists because no Android SDK/emulator is available in this environment to run it.
