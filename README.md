# ShahriyaahCs — CircleFTP CloudStream Repository

CloudStream 3 extension repository containing the CircleFTP provider.

## Repository manifest

After the first successful GitHub Actions build:

`https://raw.githubusercontent.com/ShahriyaahCs/ShahriyaahCs/builds/repo.json`

CloudStream's documentation specifies that a repository manifest contains `name`, `description`, `manifestVersion`, and direct `pluginLists` URLs. citeturn0search0

## Build

GitHub Actions builds the extension automatically on pushes to `main`.

For a local build:

```bash
gradle make makePluginsJson ensureJarCompatibility
```

The official CloudStream extension repository uses JDK 17, Android SDK setup, the CloudStream Gradle plugin, and the `make makePluginsJson` build flow. citeturn0search3turn0search2

## Features

- CircleFTP movie search
- CircleFTP TV-series search
- Movie playback
- Episode playback
- Basic season/episode parsing
- Quality detection
- Generated `plugins.json`
- Automatic `builds` branch publishing

## Notes

CircleFTP's API can change or be reachable only from particular networks. If the provider stops returning results, the API implementation in `CircleFTPProvider.kt` may need updating.

Use the extension only where access and playback of the underlying content are lawful.
