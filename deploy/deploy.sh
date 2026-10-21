#!/bin/bash
set -euo pipefail

IMAGE_TAG=${1:?用法: ./deploy.sh <镜像tag>，例如 ./deploy.sh v1.0.0}
REGISTRY=${REGISTRY:-registry.example.com}

echo ">>> 构建镜像"
docker build -t ${REGISTRY}/orangecrave/admin:${IMAGE_TAG} .
docker push ${REGISTRY}/orangecrave/admin:${IMAGE_TAG}

echo ">>> 更新 K8s Deployment"
kubectl -n orangecrave set image deployment/orangecrave-admin \
  app=${REGISTRY}/orangecrave/admin:${IMAGE_TAG}

echo ">>> 等待滚动更新完成"
kubectl -n orangecrave rollout status deployment/orangecrave-admin --timeout=300s

echo ">>> 验证"
kubectl -n orangecrave get pods -l app=orangecrave-admin
curl -s https://admin.orangecrave.example.com/admin-api/health | grep '"code":0' \
  && echo ">>> 部署成功" \
  || (echo ">>> 健康检查失败，执行回滚"; kubectl -n orangecrave rollout undo deployment/orangecrave-admin; exit 1)
