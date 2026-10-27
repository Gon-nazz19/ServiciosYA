#!/usr/bin/env bash
# Deploys the lab backend to Azure Container Apps with HTTP autoscaling (TP3).
# Requires: Azure CLI logged in (az login) with the containerapp extension, Docker running. Run from tools/scaling-lab/azure/.
set -euo pipefail

LOCATION="${LOCATION:-chilecentral}"  # Azure for Students only allows a few regions.
GROUP="${GROUP:-rg-serviciosya-tp3}"
ENVIRONMENT="${ENVIRONMENT:-cae-serviciosya-tp3}"
APP="${APP:-serviciosya-backend}"
ACR="${ACR:?Definí ACR con un nombre único de registry, por ejemplo serviciosyatp3abcd}"
IMAGE="$ACR.azurecr.io/$APP:v4"

az group create --name "$GROUP" --location "$LOCATION" -o none
az acr create --name "$ACR" --resource-group "$GROUP" --location "$LOCATION" --sku Basic --admin-enabled true -o none

# ACR Tasks is not available on student subscriptions, so the image is built locally.
docker build --platform linux/amd64 -t "$IMAGE" ../backend
az acr login --name "$ACR"
docker push "$IMAGE"

# Creates the managed environment and its Log Analytics workspace.
# The CLI may create an "express" environment, which rejects managed identities, so the app
# pulls the image with the registry admin credentials (stored by Container Apps as a secret).
az containerapp env create --name "$ENVIRONMENT" --resource-group "$GROUP" --location "$LOCATION" -o none

ACR_PASSWORD="$(az acr credential show --name "$ACR" --query "passwords[0].value" -o tsv)"

# External HTTPS ingress balances across replicas; KEDA adds a replica per 8 concurrent requests.
# Two minimum replicas keep the service up if one of them fails.
az containerapp create \
  --name "$APP" --resource-group "$GROUP" --environment "$ENVIRONMENT" \
  --image "$IMAGE" --registry-server "$ACR.azurecr.io" --registry-username "$ACR" --registry-password "$ACR_PASSWORD" \
  --ingress external --target-port 8000 \
  --cpu 0.25 --memory 0.5Gi \
  --min-replicas 2 --max-replicas 5 \
  --scale-rule-name http-concurrency --scale-rule-type http --scale-rule-http-concurrency 8 \
  --env-vars WORK_MS=250 UNIQUE_INSTANCE=1 \
  -o none

echo "https://$(az containerapp show --name "$APP" --resource-group "$GROUP" --query properties.configuration.ingress.fqdn -o tsv)"
