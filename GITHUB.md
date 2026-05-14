# GitHub Workflow

This repository uses GitHub in a very small number of ways:

## Branches

- `master` is the main application branch.
- `gh-pages` is used only for GitHub Pages content.
- `api-documentation` is a clean branch for documentation work.

## Remote

- `origin` points to `https://github.com/dmitthedazed/SvitloYE.git`

## Tags

- `v1.1`
- `v1.2`
- `v1.3`

Tags are kept locally unless explicitly pushed.

## Pages

- GitHub Pages is configured from `gh-pages`
- Public URL: `https://dmitthedazed.github.io/SvitloYE/`
- The Pages branch contains only static content:
  - `index.html`
  - `.well-known/assetlinks.json`
  - `.nojekyll`

## GitHub CLI

Use `gh` for repository metadata and GitHub-side changes.

Useful commands:

```bash
gh auth status
gh repo view dmitthedazed/SvitloYE
gh repo edit dmitthedazed/SvitloYE --description "..." --homepage "..."
gh api repos/dmitthedazed/SvitloYE/pages
gh api repos/dmitthedazed/SvitloYE --jq '.default_branch'
```

## Current Repository State

- GitHub Actions workflows were removed from the repo.
- The `main` branch was deleted from the remote.
- The repository currently relies on manual Git operations and GitHub CLI for metadata updates.
