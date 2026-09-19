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
