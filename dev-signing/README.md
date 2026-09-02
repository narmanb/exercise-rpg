# Development APK signing

`path-of-the-wild-development.p12` is intentionally a **public development-only** signing key used by debug builds and GitHub Actions artifacts.

Its purpose is to keep development APK identity stable across CI runs so testers can install later debug builds over earlier debug builds without losing app data.

Do **not** use this key for a production or Play Store release. A release build must use a separate private signing key that is never committed to this repository.

Current debug credentials are intentionally non-secret because this key is not a trust boundary:

- store password: `potwdev`
- alias: `pathofthewild-dev`
- key password: `potwdev`

Because older CI builds used ephemeral Android debug keys, switching to this development key may require one uninstall/reinstall. Before doing that, export the current `.potw` save from the app and import it after installation. Once migrated to this development key, future debug APKs built from this configuration can update in place.
