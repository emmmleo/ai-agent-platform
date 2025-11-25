#!/bin/bash
# ============================================
# CodeHubix 部署脚本快捷入口
# ============================================
# 此文件是快捷方式，实际脚本在 ops/scripts/deploy.sh

exec "$(dirname "$0")/ops/scripts/deploy.sh" "$@"
