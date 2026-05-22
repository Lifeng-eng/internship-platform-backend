# ADR-0001: 前后端分离架构（React SPA + REST API）

**日期**: 2026-05-21
**状态**: 已接受

## 背景

项目技术约束要求使用 HTML/CSS/JavaScript。团队希望使用 React 框架构建现代化 SPA。原计划使用 Thymeleaf 后端渲染，但团队决定改为前后端分离。

## 决策

采用前后端分离架构：
- 后端：Spring Boot 提供 REST API（JSON）
- 前端：React SPA（TypeScript + Ant Design）
- 认证：JWT（access token + refresh token 存 Redis）
- CORS 配置允许前端跨域访问

## 后果

- **正面**：前后端独立开发部署，前端用户体验更好，React 生态组件丰富
- **负面**：需额外处理 CORS、JWT 管理、refresh token 逻辑；需引入 Redis
- **风险**：JWT 泄露风险（localStorage 存储），需做好 XSS 防护
