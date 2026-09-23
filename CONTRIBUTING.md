# Contributing to Entangl

First off, thank you for considering contributing to Entangl! It's people like you that make open-source privacy tools possible.

## How Can I Contribute?

### Reporting Bugs
This section guides you through submitting a bug report for Entangl. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

* Use the GitHub issue search — check if the issue has already been reported.
* Check if the issue has been fixed — try to reproduce it using the latest `main` branch.
* Provide a clear and descriptive title for the issue.
* Describe the exact steps to reproduce the problem in as many details as possible.

### Suggesting Enhancements
* Use a clear and descriptive title for the issue to identify the suggestion.
* Provide a step-by-step description of the suggested enhancement.
* Explain why this enhancement would be useful to most Entangl users.

### Pull Requests
* Fill in the required template.
* Do not include issue numbers in the PR title.
* Include screenshots and animated GIFs in your pull request whenever possible.
* Follow the Kotlin styleguide.
* End files with a newline.
* Add comprehensive unit tests (especially for anything touching the `libsignal-client` wrapper, UI state, or database operations).

## Development Setup
Please see the [README.md](README.md) for instructions on how to build the project locally.

## Coding Standards
* We use `ktlint` for Kotlin formatting. Please ensure your code passes linting before submitting a PR.
* Architecture follows a strict Clean Architecture pattern (Domain, Data, UI). Do not leak Android framework dependencies into the Domain layer.
* All cryptographic operations must ensure keys are wiped from memory (avoid `String` for keys, use `ByteArray` and `sodium_memzero`).
