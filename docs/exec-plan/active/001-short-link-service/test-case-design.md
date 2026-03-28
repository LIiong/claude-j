# 测试用例设计 — 001-short-link-service

## 测试范围
短链服务全链路：创建短链、重定向、去重、异常处理。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 一、Domain 层测试场景

### ShortCode 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法 6 位 Base62 | - | new ShortCode("a1B2c3") | 创建成功 |
| D2 | null 值 | - | new ShortCode(null) | 抛 BusinessException |
| D3 | 空字符串 | - | new ShortCode("") | 抛 BusinessException |
| D4 | 长度不是 6 | - | new ShortCode("abc") | 抛 BusinessException |
| D5 | 含非法字符 | - | new ShortCode("ab-c!3") | 抛 BusinessException |
| D6 | 相等性 | - | 两个相同值 ShortCode | equals 返回 true |
| D7 | 不相等 | - | 两个不同值 ShortCode | equals 返回 false |

### OriginalUrl 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D8 | 合法 HTTPS | - | new OriginalUrl("https://...") | 创建成功 |
| D9 | 合法 HTTP | - | new OriginalUrl("http://...") | 创建成功 |
| D10 | null 值 | - | new OriginalUrl(null) | 抛 BusinessException |
| D11 | 空白字符串 | - | new OriginalUrl("   ") | 抛 BusinessException |
| D12 | 非 http 开头 | - | new OriginalUrl("ftp://...") | 抛 BusinessException |
| D13 | 超过 2048 字符 | - | 超长 URL | 抛 BusinessException |
| D14 | 相等性 | - | 两个相同 URL | equals 返回 true |

### ShortLink 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D15 | 创建短链 | - | ShortLink.create(url) | shortCode=null, id=null |
| D16 | 分配短码 | 未分配 | assignShortCode(code) | 成功 |
| D17 | 重复分配 | 已分配 | assignShortCode(code2) | 抛 BusinessException |
| D18 | 未过期(无 expireTime) | expireTime=null | isExpired() | false |
| D19 | 已过期 | expireTime 在过去 | isExpired() | true |
| D20 | 未过期(将来) | expireTime 在未来 | isExpired() | false |
| D21 | 重建聚合根 | - | ShortLink.reconstruct(...) | 全字段正确 |
| D22 | 获取 URL 值 | - | getOriginalUrlValue() | 返回字符串 |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 创建新短链 | URL 不存在 | createShortLink(cmd) | save 调用 2 次，返回 DTO |
| A2 | 去重返回已有 | URL 已存在 | createShortLink(cmd) | save 不调用，返回已有 DTO |
| A3 | 解析短链 | 短码存在 | resolveShortLink(code) | 返回 DTO |
| A4 | 短码不存在 | - | resolveShortLink(code) | 抛 SHORT_LINK_NOT_FOUND |
| A5 | 短链已过期 | expireTime 已过 | resolveShortLink(code) | 抛 SHORT_LINK_EXPIRED |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存并按短码查询 | - | save → findByShortCode | 返回匹配记录 |
| I2 | 按原始 URL 查询 | 已保存 | findByOriginalUrl | 返回匹配记录 |
| I3 | 短码不存在 | - | findByShortCode | 返回 empty |
| I4 | URL 不存在 | - | findByOriginalUrl | 返回 empty |
| I5 | Base62 生成 6 位码 | id=1 | generate(1) | 6 位 Base62 |
| I6 | 连续 ID 生成不同码 | - | generate(1), generate(2) | 不同码 |
| I7 | 1000 个 ID 无重复 | - | generate(1..1000) | 全部唯一 |
| I8 | 千万级 ID | id=10M | generate(10M) | 6 位 Base62 |
| I9 | 非顺序码 | id=1,2 | generate(1), generate(2) | 首字符不同 |
| I10 | 混淆确定性 | 同 ID 两次 | obfuscate(1), obfuscate(1) | 结果相同 |

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建短链成功 | - | POST /api/v1/short-links | 200 + success=true |
| W2 | URL 为空 | - | POST body { originalUrl: "" } | 400 + success=false |
| W3 | URL 格式无效 | - | POST body { originalUrl: "not-a-url" } | 400 + INVALID_ORIGINAL_URL |
| W4 | 重定向成功 | 短码存在 | GET /s/{code} | 302 + Location header |
| W5 | 短码不存在 | - | GET /s/zzzzzz | 404 + SHORT_LINK_NOT_FOUND |

---

## 五、集成测试场景（全链路：HTTP → Controller → Service → Repository → H2）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 创建短链成功 | POST /api/v1/short-links {"originalUrl":"https://www.baidu.com"} | 200, shortCode 非空, shortUrl 含 /s/{code} |
| E2 | 去重返回相同短码 | 同一 URL 两次 POST | 两次返回相同 shortCode |
| E3 | 不同 URL 生成不同码 | 两个不同 URL 分别 POST | shortCode 不同 |
| E4 | URL 为空 400 | POST {"originalUrl":""} | 400, success=false |
| E5 | URL 为 null 400 | POST {} | 400, success=false |
| E6 | 非 HTTP URL 400 | POST {"originalUrl":"ftp://..."} | 400, INVALID_ORIGINAL_URL |
| E7 | 请求体缺失 | POST 无 body | 500（待优化为 400，见缺陷 #2） |
| E8 | 创建后重定向 302 | POST 创建 → GET /s/{code} | 302 + Location = originalUrl |
| E9 | 不存在短码 404 | GET /s/zzzzzz | 404, SHORT_LINK_NOT_FOUND |
| E10 | 全链路往返一致性 | 创建 → 重定向 → 再次创建（去重） | 三步结果一致 |

---

## 六、代码审查检查项

- [ ] 依赖方向正确（adapter -> application -> domain <- infrastructure）
- [ ] domain 模块无 Spring/框架 import
- [ ] ShortLink 聚合根封装业务不变量（非贫血模型）
- [ ] ShortCode/OriginalUrl 值对象不可变，equals/hashCode 正确
- [ ] Repository 接口在 domain，实现在 infrastructure
- [ ] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response
- [ ] Controller 无业务逻辑
- [ ] 异常通过 GlobalExceptionHandler 统一处理

## 六、代码风格检查项

- [ ] Java 8 兼容（无 var、records、text blocks）
- [ ] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [ ] DO 用 @Data + @TableName，DTO 用 @Data
- [ ] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [ ] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
