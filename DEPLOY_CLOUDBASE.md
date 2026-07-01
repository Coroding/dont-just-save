# Dont Just Save CloudBase Deployment

This repository deploys the interactive demo as a CloudBase static website.

- Publish directory: `dont-just-save-interactive-demo`
- CloudBase target path: `/dont-just-save`
- Workflow: `.github/workflows/deploy-cloudbase.yml`

Required GitHub Actions secrets:

- `TCB_SECRET_ID`
- `TCB_SECRET_KEY`
- `TCB_ENV_ID`

Manual trigger:

```bash
gh workflow run "Deploy Dont Just Save to CloudBase" -R Coroding/dont-just-save --ref main
```

Expected CloudBase URL:

```text
https://<your-cloudbase-static-domain>/dont-just-save/
```
