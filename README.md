# BDIX Bangladesh CloudStream Repo

A public CloudStream 3 repository focused only on Bangladeshi and BDIX-reachable sources. This project intentionally excludes international or non-BDIX providers so that the repo stays aligned with Bangladeshi ISP accessibility and local sharing use cases.

## What this repo is

This repo is a lightweight CloudStream 3 plugin repository for Bangladeshi-friendly sources. It is meant for people who want to install a clean, focused catalog of providers that are reachable from Bangladesh and typically available over BDIX or similar local-access paths.

## Install in CloudStream

1. Open CloudStream 3.
2. Go to Settings -> Extensions / Repository.
3. Add a new repository with this URL:

   https://raw.githubusercontent.com/pabel172/ShahriyaahCs/main/repo.json

4. Refresh repositories and install the available plugin(s).

## Repository structure

- `repo.json` — the repository manifest CloudStream reads.
- `plugins.json` — plugin list for the repository.
- `CircleFTP/` — existing CloudStream provider module.
- `BdixBangla/` — BDIX-focused provider module template and source catalog entry.

## Supported scope

The repo is intentionally limited to:

- Bangladeshi-accessible providers
- BDIX-friendly or locally reachable sources
- Sources compatible with CloudStream 3 in the Bangladesh ecosystem

It does not include unrelated international providers or non-BDIX-only catalogs.

## Local build

From the project root:

```bash
./gradlew make makePluginsJson ensureJarCompatibility
```

If you are using the CloudStream extension setup directly, the Gradle project is already structured for the standard CloudStream plugin build flow.

## Notes

- Keep the repo focused on legally reachable and region-appropriate sources only.
- Do not mix in global services that are not accessible from Bangladeshi networks.
- This repo is intended as a clean public catalog for BDIX-oriented Bangladeshi sources.
