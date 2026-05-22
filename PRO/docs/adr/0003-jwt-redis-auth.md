# ADR-0003: JWT 认证与 Redis refresh token

**日期**: 2026-05-21
**状态**: 已接受

## 背景

前后端分离架构需要无状态认证。JWT 是 SPA 标准选择，但存在 token 吊销问题。纯无状态 JWT 无法主动吊销（如用户登出、密码泄露）。

## 决策

- access token：JWT，TTL 2 小时，存 localStorage
- refresh token：JWT，TTL 7 天，值存 Redis（key: `refresh_token:{userId}`）
- 登出时从 Redis 删除 refresh token
- 密码重置验证码存 Redis（TTL 5 分钟）

## 后果

- **正面**：access token 短有效期降低泄露风险，refresh token 可主动吊销（登出/封号）
- **负面**：需额外部署 Redis 服务
- **风险**：Redis 宕机导致认证不可用，需有降级策略
