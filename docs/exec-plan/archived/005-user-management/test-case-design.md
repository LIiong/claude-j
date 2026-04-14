# 测试用例设计 — 005-user-management

## 测试范围
用户管理功能全量测试，包括用户注册、用户管理、邀请系统和用户订单查询。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 一、Domain 层测试场景

### UserId 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new UserId("UR1234567890ABCDEF") | 创建成功 |
| D2 | null 值 | - | new UserId(null) | 抛 BusinessException |
| D3 | 空值 | - | new UserId("") | 抛 BusinessException |
| D4 | 非 UR 开头 | - | new UserId("AB1234567890ABCDEF") | 抛 BusinessException |
| D5 | 长度不足 | - | new UserId("UR12345") | 抛 BusinessException |
| D6 | 包含非法字符 | - | new UserId("UR1234567890ABCDEO") | 抛 BusinessException |
| D7 | 生成新 ID | - | UserId.generate() | 返回 UR 开头 18 位字符串 |
| D8 | 相等性 | - | 两个相同值对象 | equals 返回 true |

### Username 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D9 | 合法值创建 | - | new Username("testuser") | 创建成功 |
| D10 | 边界值-最小 | - | new Username("ab") | 创建成功 |
| D11 | 边界值-最大 | - | new Username("a".repeat(20)) | 创建成功 |
| D12 | 太短 | - | new Username("a") | 抛 BusinessException |
| D13 | 太长 | - | new Username("a".repeat(21)) | 抛 BusinessException |
| D14 | 自动 trim | - | new Username("  test  ") | 值变为 "test" |

### Email 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D15 | 合法邮箱 | - | new Email("test@example.com") | 创建成功 |
| D16 | 无效格式 | - | new Email("invalid") | 抛 BusinessException |
| D17 | 空值 | - | new Email("") | 抛 BusinessException |

### Phone 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D18 | 合法手机号 | - | new Phone("13800138000") | 创建成功 |
| D19 | 非 1 开头 | - | new Phone("23800138000") | 抛 BusinessException |
| D20 | 长度不足 | - | new Phone("1380013800") | 抛 BusinessException |
| D21 | 包含字母 | - | new Phone("1380013800a") | 抛 BusinessException |

### UserStatus 枚举
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D22 | ACTIVE 转 FROZEN | status=ACTIVE | toFrozen() | 返回 FROZEN |
| D23 | FROZEN 转 ACTIVE | status=FROZEN | toActive() | 返回 ACTIVE |
| D24 | ACTIVE 转 INACTIVE | status=ACTIVE | toInactive() | 返回 INACTIVE |
| D25 | 非法状态转换 | status=FROZEN | toFrozen() | 抛 BusinessException |

### InviteCode 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D26 | 合法值创建 | - | new InviteCode("ABC234") | 创建成功 |
| D27 | 自动转大写 | - | new InviteCode("abc234") | 值变为 "ABC234" |
| D28 | 包含 0 | - | new InviteCode("ABC230") | 抛 BusinessException |
| D29 | 包含 O | - | new InviteCode("ABC23O") | 抛 BusinessException |
| D30 | 长度错误 | - | new InviteCode("ABC23") | 抛 BusinessException |
| D31 | 生成邀请码 | - | InviteCode.generate() | 返回 6 位有效邀请码 |

### User 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D32 | 创建用户 | - | User.create(username, inviteCode) | 状态 ACTIVE，自动生成 userId |
| D33 | null 用户名 | - | User.create(null, inviteCode) | 抛 BusinessException |
| D34 | null 邀请码 | - | User.create(username, null) | 抛 BusinessException |
| D35 | 设置邮箱 | user 已创建 | setEmail(email) | email 设置成功 |
| D36 | 设置手机 | user 已创建 | setPhone(phone) | phone 设置成功 |
| D37 | 设置邀请人 | user 已创建 | setInviter(inviterId) | inviterId 设置成功 |
| D38 | 冻结用户 | status=ACTIVE | freeze() | 状态变为 FROZEN |
| D39 | 解冻用户 | status=FROZEN | unfreeze() | 状态变为 ACTIVE |
| D40 | 停用用户 | status=ACTIVE | deactivate() | 状态变为 INACTIVE |
| D41 | 激活用户 | status=INACTIVE | activate() | 状态变为 ACTIVE |
| D42 | 非法冻结 | status=FROZEN | freeze() | 抛 BusinessException |
| D43 | 是否被邀请 | inviterId=null | isInvited() | 返回 false |
| D44 | 是否被邀请 | inviterId!=null | isInvited() | 返回 true |

---

## 二、Application 层测试场景

### UserApplicationService
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 正常创建用户 | 用户名不存在 | createUser(cmd) | 返回 UserDTO，保存到 Repository |
| A2 | 用户名为空 | username=null | createUser(cmd) | 抛 BusinessException |
| A3 | 用户名已存在 | 用户名已存在 | createUser(cmd) | 抛 BusinessException(USERNAME_ALREADY_EXISTS) |
| A4 | 带邀请人创建 | 提供有效 inviteCode | createUser(cmd) | 用户创建成功，inviterId 设置 |
| A5 | 无效邀请码 | 提供无效 inviteCode | createUser(cmd) | 抛 BusinessException(INVALID_INVITE_CODE) |
| A6 | 根据 ID 查询 | user 存在 | getUserById(userId) | 返回 UserDTO |
| A7 | 根据 ID 查询不存在 | user 不存在 | getUserById(userId) | 抛 BusinessException(USER_NOT_FOUND) |
| A8 | 根据用户名查询 | user 存在 | getUserByUsername(username) | 返回 UserDTO |
| A9 | 冻结用户 | user 存在且 ACTIVE | freezeUser(userId) | 状态变为 FROZEN |
| A10 | 解冻用户 | user 存在且 FROZEN | unfreezeUser(userId) | 状态变为 ACTIVE |
| A11 | 查询被邀请用户 | 有邀请记录 | getInvitedUsers(userId) | 返回被邀请用户列表 |
| A12 | 生成唯一邀请码 | - | generateUniqueInviteCode() | 生成不重复的 6 位邀请码 |

### UserOrderQueryService
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A13 | 查询用户订单 | user 有订单 | getUserOrders(userId) | 返回订单列表 |
| A14 | 查询订单详情 | 订单存在且属于用户 | getUserOrderDetail(userId, orderId) | 返回订单详情 |
| A15 | 查询他人订单 | 订单不属于用户 | getUserOrderDetail(userId, orderId) | 返回 null |

---

## 三、Infrastructure 层测试场景

### UserRepositoryImpl
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存新用户 | user 无 id | save(user) | 返回保存后的 user，id 已填充 |
| I2 | 更新用户 | user 有 id | save(user) | 数据库记录更新 |
| I3 | 根据 userId 查询 | user 存在 | findByUserId(userId) | 返回 Optional.of(user) |
| I4 | 根据 userId 查询不存在 | - | findByUserId(userId) | 返回 Optional.empty() |
| I5 | 根据 username 查询 | user 存在 | findByUsername(username) | 返回 Optional.of(user) |
| I6 | 根据 inviteCode 查询 | user 存在 | findByInviteCode(inviteCode) | 返回 Optional.of(user) |
| I7 | 查询被邀请用户 | 有邀请记录 | findByInviterId(inviterId) | 返回用户列表 |
| I8 | 检查用户名存在 | 用户名已存在 | existsByUsername(username) | 返回 true |
| I9 | 检查用户名不存在 | - | existsByUsername(username) | 返回 false |
| I10 | 检查邀请码存在 | 邀请码已存在 | existsByInviteCode(inviteCode) | 返回 true |
| I11 | DO-Domain 转换 | - | save → find | 字段完整还原 |

### InviteCodeGeneratorImpl
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I12 | 生成邀请码 | - | generate() | 返回 6 位 Base32 字符 |
| I13 | 多次生成 | - | 多次调用 generate() | 返回不同邀请码 |

---

## 四、Adapter 层测试场景

### UserController
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建用户成功 | - | POST /api/v1/users | 200 + success=true |
| W2 | 创建用户参数校验失败 | username=null | POST /api/v1/users | 400 + success=false |
| W3 | 查询用户成功 | user 存在 | GET /api/v1/users/{userId} | 200 + success=true |
| W4 | 查询用户不存在 | - | GET /api/v1/users/{userId} | 404 |
| W5 | 冻结用户成功 | user 存在 | POST /api/v1/users/{userId}/freeze | 200 + success=true |
| W6 | 查询被邀请用户 | 有邀请记录 | GET /api/v1/users/{userId}/invited-users | 200 + 返回用户列表 |
| W7 | 验证邀请码成功 | inviteCode 有效 | POST /api/v1/users/validate-invite-code | 200 + 返回用户信息 |

### UserOrderController
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W8 | 查询用户订单列表 | user 有订单 | GET /api/v1/users/{userId}/orders | 200 + 返回订单列表 |
| W9 | 查询订单详情成功 | 订单存在且属于用户 | GET /api/v1/users/{userId}/orders/{orderId} | 200 + 返回订单详情 |
| W10 | 查询他人订单详情 | 订单不属于用户 | GET /api/v1/users/{userId}/orders/{orderId} | 返回错误信息 |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 用户注册全流程 | POST /api/v1/users → GET /api/v1/users/{userId} | 用户创建成功，可正确查询 |
| E2 | 邀请注册全流程 | 用户A注册 → 生成邀请链接 → 用户B使用邀请码注册 → 查询被邀请用户 | 邀请关系正确记录 |
| E3 | 用户订单查询全流程 | 创建订单 → 查询用户订单列表 → 查询订单详情 | 订单数据正确返回 |
| E4 | 用户状态管理全流程 | 注册 → 冻结 → 查询状态 → 解冻 → 查询状态 | 状态变更正确 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（非贫血模型）
- [x] 值对象不可变，equals/hashCode 正确
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response
- [x] Controller 无业务逻辑
- [x] 异常通过 GlobalExceptionHandler 统一处理

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [x] DO 用 @Data + @TableName，DTO 用 @Data
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx
