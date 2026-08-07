# Contributing to UniBank

UniBank is a personal portfolio banking system and contributions are welcome in the form of bug reports, feature suggestions, and small fixes. This document explains how to contribute effectively.

## Ways to Contribute

- Reporting a bug
- Discussing the current state of the code
- Submitting a fix
- Proposing a new feature

## Reporting Bugs

Good bug reports are specific and reproducible. Before opening one, check that the issue hasn't already been reported.

A good bug report includes:

- A clear summary of the problem
- Steps to reproduce it (be specific)
- What you expected to happen
- What actually happened, including any error messages or stack traces
- Your environment: Java version, MySQL version, OS

[Open a bug report](https://github.com/mansikananidev-creator/MapleBank/issues/new?template=bug_report.md)

## Suggesting Features

Feature requests are welcome. Please describe the problem you want to solve, not just the solution, so we can discuss the best approach.

[Open a feature request](https://github.com/mansikananidev-creator/MapleBank/issues/new?template=feature_request.md)

## Submitting a Pull Request

All code changes happen through pull requests. To submit one:

1. Fork the repo and create a branch from `main`:
   ```bash
   git checkout -b fix/your-fix-name
   # or
   git checkout -b feature/your-feature-name
   ```

2. Make your changes. Keep them focused — one fix or feature per PR.

3. If you've changed or added API endpoints, update the README accordingly.

4. Make sure the project builds and all tests pass:
   ```bash
   ./mvnw clean verify
   ```

5. Open a pull request against `main` with a clear description of what changed and why.

Any contributions you make will be understood to be under the same [MIT License](LICENSE) that covers the project.

## Code Style

- Follow standard Java naming conventions
- Keep controllers thin; business logic belongs in the service layer
- Use Lombok annotations to reduce boilerplate where appropriate
- Use specific, meaningful commit messages

## Questions

If you have a question about the codebase, feel free to open an issue with the label `question`.
