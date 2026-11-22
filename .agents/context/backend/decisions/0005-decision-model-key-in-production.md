# 0005. Decision model key in the production deploy

**Status:** Accepted (owner: Maksym U., 2026-11-22) · Completes the production wiring of jev-1 (DJ1-001)

## Context

The decision model (Premise assessment, DJ1-001) is on by default in `application.yaml` (`ASSESSMENT_MODEL_ENABLED:true`), but `AssessmentConfig` falls back to rules only when `ASSESSMENT_MODEL_API_KEY` is blank. The production deploy (`.github/workflows/ci.yml`) never passed the key to Cloud Run, although the GitHub Actions secret `ASSESSMENT_MODEL_API_KEY` existed. Production logged `Decision model is enabled but ASSESSMENT_MODEL_API_KEY is not set` at every start, ran with `mode=RULES_ONLY` and scored every Analysis with `confidence-2` instead of the model version (`confidence-4` on master). `gcloud run deploy --set-env-vars/--set-secrets` replaces all variables and secrets, so a key added by hand in the Cloud Run console is removed by the next deploy.

## Decision

- The deploy job copies the Actions secret `ASSESSMENT_MODEL_API_KEY` (org or repository level) to the Secret Manager secret `<repo>-assessment-model-api-key` (a new version only when the value changed) and mounts it into Cloud Run as `ASSESSMENT_MODEL_API_KEY` (`--set-secrets ...:latest`), the same way as `SOURCES_GITHUB_TOKEN`.
- The deploy also sets `ASSESSMENT_MODEL_ENABLED` explicitly: `true` with the key, `false` without it. A missing Actions secret gives a workflow warning, not a failed deploy: rules only is a supported mode (DJ1-001 "off by default"), and the demo must not depend on an external service.
- No IAM change: the deployer service account holds `roles/secretmanager.admin` and the runtime one `roles/secretmanager.secretAccessor` (`scripts/gcp-bootstrap.sh`).

## Consequences

- With the Actions secret set, production assesses Premises with the model and stores Analyses as `confidence-4`; without it, as `confidence-2`, with no warning in the app log.
- Earlier production Analyses keep `confidence-2`. A new Analysis of the same Case is compared against them with `ALGORITHM_VERSION_DIFFERS`.
- Rotation: update the Actions secret and re-run the deploy; the new secret version is mounted as `latest` on the new revision.
- Sending Premise text windows to the external service still needs the mentors' approval (DJ1-001, challenge 5.7). Removing the Actions secret switches the model off on the next deploy.

## Verification

After the deploy, the Cloud Run startup log shows `Premise assessment: mode=WITH_MODEL`, the service lists `ASSESSMENT_MODEL_API_KEY` as a secret reference, and a new Analysis returns `algorithmVersion` `confidence-4`.
