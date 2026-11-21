#!/bin/bash

set -euo pipefail

if [ $# -lt 3 ]; then
  echo "Usage: $0 <gcp-project-id> <gcp-region> <github-org>"
  echo "Example: $0 my-hackathon europe-west1 NononotaaSquad"
  exit 1
fi

PROJECT_ID="$1"
REGION="$2"
GITHUB_ORG="$3"
POOL="github"
REGISTRY="hackathon"
DEPLOYER="github-deployer"
RUNTIME="cloud-run-runtime"

gcloud config set project "$PROJECT_ID" >/dev/null
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
DEPLOYER_EMAIL="$DEPLOYER@$PROJECT_ID.iam.gserviceaccount.com"
RUNTIME_EMAIL="$RUNTIME@$PROJECT_ID.iam.gserviceaccount.com"
PROVIDER="projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL/providers/$POOL"

echo "Enabling APIs..."
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com \
  iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com

echo "Creating Artifact Registry repository..."
gcloud artifacts repositories describe "$REGISTRY" --location="$REGION" >/dev/null 2>&1 ||
  gcloud artifacts repositories create "$REGISTRY" --location="$REGION" --repository-format=docker

echo "Creating service accounts..."
gcloud iam service-accounts describe "$DEPLOYER_EMAIL" >/dev/null 2>&1 ||
  gcloud iam service-accounts create "$DEPLOYER" --display-name="GitHub Actions deployer"
gcloud iam service-accounts describe "$RUNTIME_EMAIL" >/dev/null 2>&1 ||
  gcloud iam service-accounts create "$RUNTIME" --display-name="Cloud Run runtime"

echo "Granting roles..."
for role in roles/run.admin roles/artifactregistry.writer roles/secretmanager.admin; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" --member="serviceAccount:$DEPLOYER_EMAIL" --role="$role" --condition=None >/dev/null
done
gcloud projects add-iam-policy-binding "$PROJECT_ID" --member="serviceAccount:$RUNTIME_EMAIL" \
  --role=roles/secretmanager.secretAccessor --condition=None >/dev/null
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_EMAIL" --member="serviceAccount:$DEPLOYER_EMAIL" \
  --role=roles/iam.serviceAccountUser >/dev/null

echo "Configuring Workload Identity Federation for GitHub org $GITHUB_ORG..."
gcloud iam workload-identity-pools describe "$POOL" --location=global >/dev/null 2>&1 ||
  gcloud iam workload-identity-pools create "$POOL" --location=global --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers describe "$POOL" --location=global --workload-identity-pool="$POOL" >/dev/null 2>&1 ||
  gcloud iam workload-identity-pools providers create-oidc "$POOL" --location=global --workload-identity-pool="$POOL" \
    --display-name="GitHub" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
    --attribute-condition="assertion.repository_owner == '$GITHUB_ORG'"
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER_EMAIL" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL/attribute.repository_owner/$GITHUB_ORG" >/dev/null

declare -A VARS=(
  [GCP_PROJECT_ID]="$PROJECT_ID"
  [GCP_PROJECT_NUMBER]="$PROJECT_NUMBER"
  [GCP_REGION]="$REGION"
  [GCP_ARTIFACT_REGISTRY]="$REGION-docker.pkg.dev/$PROJECT_ID/$REGISTRY"
  [GCP_WORKLOAD_IDENTITY_PROVIDER]="$PROVIDER"
  [GCP_SERVICE_ACCOUNT]="$DEPLOYER_EMAIL"
  [GCP_RUNTIME_SERVICE_ACCOUNT]="$RUNTIME_EMAIL"
)

if command -v gh >/dev/null 2>&1; then
  echo "Setting GitHub organization variables..."
  for name in "${!VARS[@]}"; do
    gh variable set "$name" --org "$GITHUB_ORG" --visibility all --body "${VARS[$name]}"
  done
  echo "Enter the MongoDB Atlas connection string for the MONGODB_URI organization secret:"
  gh secret set MONGODB_URI --org "$GITHUB_ORG" --visibility all
else
  echo "GitHub CLI not found. Add these organization variables (Settings > Secrets and variables > Actions > Variables):"
  for name in "${!VARS[@]}"; do
    echo "  $name=${VARS[$name]}"
  done
  echo "Then add the organization secret MONGODB_URI with your MongoDB Atlas connection string."
fi

echo "Done. Every repository in $GITHUB_ORG can now deploy to $PROJECT_ID."
