# 需求拆分设计 — 001-short-link-service

## 需求描述
实现短链服务：根据长链生成短链，访问短链 302 重定向到长链。数据库表支撑千万级数据量。

## 领域分析

### 聚合根: ShortLink
- shortCode (ShortCode) — 6 位 Base62 短码
- originalUrl (OriginalUrl) — 原始 URL，最长 2048 字符
- createTime, expireTime

### 值对象
- **ShortCode**: 6 位 Base62 字符，不可变
- **OriginalUrl**: http/https URL，最长 2048，不可变

### 端口接口
- **ShortLinkRepository**: save, findByShortCode, findByOriginalUrl
- **ShortCodeGenerator**: generate(id) → ShortCode

## 短码生成算法
Base62(自增ID) + 乘法逆元位混淆，零碰撞 + 不可预测。

## API 设计
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/v1/short-links | 创建短链 |
| GET | /s/{shortCode} | 302 重定向 |

## 影响范围
- domain: 新增 ShortLink 聚合、值对象、Repository 端口、ShortCodeGenerator 端口、BusinessException/ErrorCode
- application: 新增 CreateShortLinkCommand、ShortLinkDTO、ShortLinkAssembler、ShortLinkApplicationService
- infrastructure: 新增 ShortLinkDO、ShortLinkMapper、ShortLinkConverter、ShortLinkRepositoryImpl、Base62ShortCodeGenerator
- adapter: 新增 ShortLinkController、ShortLinkRedirectController、ApiResult、GlobalExceptionHandler
- start: schema.sql 追加 t_short_link DDL
