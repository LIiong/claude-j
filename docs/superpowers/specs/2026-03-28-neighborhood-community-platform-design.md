# 邻里社区平台 — 详细设计文档

## Context

用户希望构建一个**基于地理位置的邻里社区平台**，以住宅小区为核心场景，支持图文帖子、投票、团购接龙等功能。这是一个全新独立项目（非 claude-j 子模块），采用 Go 后端 + 微信小程序/H5 前端，面向 1-2 人独立开发者。

---

## 一、市场分析与竞品调研

### 1.1 直接竞品

| 产品 | 定位 | 地理机制 | 核心功能 | 现状/教训 |
|------|------|----------|----------|-----------|
| **Nextdoor** | 美国邻里社区平台 | 四重验证（明信片/电话账单/证件/第三方数据库） | 邻里动态、本地推荐、紧急通知、商家广告 | 6000万+用户，覆盖27.5万社区；2024收入2.47亿美元(+13%)；2025年首次全年正EBITDA，但增速放缓至4%，股价低迷~$2.25 |
| **叮咚小区** | 国内小区社区 | 小区名称定位 | 邻里圈、二手市场、物业通知 | **核心案例**：获1亿天使融资，1000人地推+千万广告费烧完后崩盘；产品定位混乱（社交vs服务），沦为"小区版58同城"；大量差评后关停 |
| **考拉社区** | 小区社区+物业 | 物业合作验证 | 社区公告、缴费、报修、邻里交流 | 转型物业SaaS，纯社区模式难盈利 |
| **万科住这儿** | 万科业主专属 | 房产验证 | 物业报修、社区活动、邻里圈、缴费、访客邀请 | 仅限万科业主封闭生态，偏物业管理而非社区社交 |

### 1.2 间接竞品（实际最大竞争对手）

| 产品 | 为什么是竞品 | 优劣势 |
|------|-------------|--------|
| **微信群** | 几乎所有小区都有业主群 | ✅零迁移成本、群主信任关系强 ❌信息洪流（通知/闲聊/团购混杂）、手动统计易出错、无内容沉淀、无身份验证、无消费者评价机制 |
| **微信接龙小程序** | 群内团购接龙 | ✅轻量、习惯已养成 ❌模板化严重、无支付闭环、无售后追踪、数据不可沉淀 |
| **小红书** | 本地生活内容分享 | ✅内容质量高；2024年新增地图功能+基于位置的群聊（验证了位置社群需求） ❌核心仍是消费种草，非邻里社区 |
| **拼多多/多多买菜** | 社区团购 | ✅2025年GMV近3000亿元、日均4000万单 ❌成功依赖巨大供应链投入，非社交平台 |
| **百度贴吧（地方吧）** | 地域论坛 | ✅早期有城市级社区功能 ❌日活从2015年2500万降至2025年800-900万，地方吧几乎无活跃 |

### 1.3 市场痛点与机会

**微信群的不足（我们的机会）**：
1. 信息洪流：重要公告被聊天冲刷，无法置顶结构化展示
2. 无内容沉淀：分享的好内容（美食推荐、装修经验）无法搜索和回溯
3. 无商业闭环：团购接龙只能统计人头，支付靠手动转账，对账痛苦
4. 新住户难加入：找不到群、加群靠人拉人，体验差
5. 无社区认同感：群没有"归属感"，无法展示社区特色

**失败产品的教训**（综合叮咚小区、考拉社区等案例）：
1. **伪需求陷阱**：小区社交本身非刚需，仅特定场景（团购、投诉、求助）有互动需求
2. **密度悖论**：快速扩张→每个小区密度不足→冷清→流失→更冷清（死亡螺旋）
3. **产品同质化**：沦为"小区版58同城+BBS"，无差异化
4. **重推广轻体验**：叮咚小区1200万广告费换来大量差评
5. **与微信群直接竞争**：迁移成本过高，用户无动力

**我们的差异化定位**：
- **"微信群增强器"而非替代品** — 以小程序嵌入微信生态，不要求下载独立APP
- **工具先行、社区留存** — 高频刚需工具（团购接龙/投票/公告板）获客，逐步培育社区
- **自定义地理围栏** — GPS定位+物业配合的轻量验证，精确到小区甚至楼栋
- **三种社区类型分级运营** — 官方社区保证基础服务，商家社区创造收入
- **团购接龙专业化** — 商品展示、评价系统、订单追踪、自动统计、售后保障（微信接龙做不到的）

**市场规模参考**：中国本地生活服务2025年超35万亿元，在线渗透率约30%，增长空间大。但"小区邻里社区"细分赛道过去十年几乎全军覆没。**核心判断**：机会在于微信群的结构化升级，而非独立社区APP。

### 1.4 盈利模式

| 模式 | 阶段 | 预期占比 | 参考 |
|------|------|----------|------|
| 团购接龙佣金 | MVP | 30-40% | 行业标准10-20%佣金率，小区团购高频刚需 |
| 本地商家广告 | 二期 | 25-35% | 参考Nextdoor（自助广告占收入60%），周边商家精准触达 |
| 商家入驻费 | 二期 | 10-15% | 年费+效果付费结合 |
| 增值服务(SaaS) | 三期 | 10-15% | 物业管理工具、业委会工具、高级团购功能 |
| 信息服务费 | 三期 | 5-10% | 二手房租售、家政推荐等撮合 |

**分阶段策略**：0→1 免费获客 → 1→10 开放团购佣金+商家入驻 → 10→100 自助广告+SaaS

### 1.5 关键风险

| 风险 | 等级 | 缓解策略 |
|------|------|----------|
| 冷启动难 | 🔴高 | 选1-2城市深耕；选大型新建小区(2000户+)为种子；物业合作获天然推广渠道；工具功能少量用户也有价值 |
| 微信群替代效应 | 🔴高 | 以微信小程序为主形态（零迁移成本）；定位"增强工具"而非替代品；提供微信群无法实现的结构化功能 |
| 巨头降维打击 | 🔴高 | 微信/支付宝随时可推出类似功能。应对：深耕垂直场景（团购+物业），建立本地运营壁垒 |
| 用户密度天花板 | 🟡中 | 小区人口有限(500-5000户)，每个小区DAU天花板低。应对：动态扩大范围（小区→街道→片区）；引入商家内容 |
| 商业模式验证周期长 | 🟡中 | 广告变现需规模支撑（参考Nextdoor用了10年才首次正EBITDA）。应对：优先团购佣金变现，0-1阶段免费获客 |
| 支付合规 | 🟡中 | 使用微信支付标准商户能力，不碰资金池 |
| 内容治理 | 🟡中 | 邻里纠纷等敏感内容管理成本高。初期敏感词Trie树过滤+人工审核，后期引入AI审核 |

---

## 二、技术架构设计

### 2.1 技术栈总览

| 层 | 技术选型 | 说明 |
|----|----------|------|
| 前端 | 微信小程序 + H5 | Taro/uni-app 跨端框架 |
| 后端 | Go 1.22+ | 模块化单体架构 |
| Web框架 | Gin / Echo | 轻量 HTTP 框架 |
| 数据库 | PostgreSQL 16 + PostGIS 3.4 | 关系数据 + 地理空间 |
| 缓存 | Redis 7 | 会话、Feed 缓存、计数器 |
| 对象存储 | 腾讯云 COS | 图片/视频 + CDN |
| 支付 | 微信支付 JSAPI | wechatpay-go SDK |
| 部署 | 单台云服务器 + Docker Compose | 最简运维 |
| CI/CD | GitHub Actions | 自动测试+构建+部署 |

### 2.2 架构风格：Go 模块化单体

```
┌──────────────────────────────────────────┐
│              cmd/server                   │  启动入口
├──────────────────────────────────────────┤
│              api/                         │  HTTP Handler（路由+请求响应）
│  ├── community/   ├── post/              │
│  ├── vote/        ├── groupbuy/          │
│  └── middleware/  (auth, cors, logging)   │
├──────────────────────────────────────────┤
│              internal/                    │  业务核心
│  ├── community/                          │
│  │   ├── domain/      (实体、VO、Repository接口)
│  │   ├── application/ (用例编排)          │
│  │   └── infra/       (DB实现、外部服务)   │
│  ├── post/           (同上结构)           │
│  ├── vote/           (同上结构)           │
│  ├── groupbuy/       (同上结构)           │
│  ├── user/           (同上结构)           │
│  └── shared/         (共享内核：错误码、分页)│
├──────────────────────────────────────────┤
│              pkg/                         │  公共工具库
│  ├── geofence/   (PostGIS封装)           │
│  ├── wechat/     (微信SDK封装)           │
│  ├── storage/    (对象存储封装)           │
│  └── response/   (统一响应格式)           │
├──────────────────────────────────────────┤
│              deploy/                      │
│  ├── docker-compose.yml                  │
│  ├── Dockerfile                          │
│  └── migrations/  (数据库迁移文件)         │
└──────────────────────────────────────────┘
```

**模块间通信规则**：
- 模块间仅通过 `domain/` 下定义的 interface 通信
- 禁止直接引用其他模块的 `infra/` 内部类型
- 共享数据通过 `shared/` 传递（如 UserID、分页参数）
- 预留事件总线接口（初期进程内 channel，后期可替换 MQ）

### 2.3 数据存储架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ PostgreSQL  │    │   Redis     │    │ 腾讯云 COS  │
│ + PostGIS   │    │             │    │ + CDN       │
├─────────────┤    ├─────────────┤    ├─────────────┤
│ 社区        │    │ 会话Token   │    │ 帖子图片    │
│ 帖子        │    │ Feed缓存    │    │ 团购图片    │
│ 投票        │    │ 点赞计数    │    │ 头像        │
│ 团购+订单   │    │ 热点数据    │    │             │
│ 用户        │    │             │    │             │
│ 地理围栏    │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘
```

---

## 三、领域模型设计

### 3.1 聚合划分

| 聚合 | 聚合根 | 核心实体/VO | 职责 |
|------|--------|-------------|------|
| Community | Community | GeoFence(VO), CommunityMember | 社区生命周期+地理围栏+成员管理 |
| Post | Post | PostImage(VO), Comment, Like | 图文内容发布与互动 |
| Vote | Vote | VoteOption, VoteRecord | 投票管理 |
| GroupBuy | GroupBuy | GroupBuyOrder, PaymentRecord | 团购接龙+支付 |
| User | User | Location(VO) | 用户身份与位置 |

### 3.2 User 聚合

```go
// domain/user.go
type User struct {
    ID        int64
    OpenID    string     // 微信 openid
    UnionID   string     // 微信 unionid
    Nickname  string
    AvatarURL string
    Phone     string
    Status    UserStatus // ACTIVE / BANNED
    CreatedAt time.Time
}

type UserStatus string
const (
    UserStatusActive UserStatus = "ACTIVE"
    UserStatusBanned UserStatus = "BANNED"
)

type Location struct { // 值对象
    Lng float64
    Lat float64
}

// Repository 端口
type UserRepository interface {
    Save(ctx context.Context, user *User) error
    FindByID(ctx context.Context, id int64) (*User, error)
    FindByOpenID(ctx context.Context, openID string) (*User, error)
}
```

### 3.3 Community 聚合

```go
// domain/community.go
type CommunityStatus string
const (
    CommunityStatusPending   CommunityStatus = "PENDING"
    CommunityStatusActive    CommunityStatus = "ACTIVE"
    CommunityStatusSuspended CommunityStatus = "SUSPENDED"
)

type Community struct {
    ID          int64
    Name        string
    Description string
    Type        CommunityType   // OFFICIAL / USER_CREATED / MERCHANT
    Status      CommunityStatus // PENDING / ACTIVE / SUSPENDED
    GeoFence    GeoFence        // 值对象：多边形边界
    MaxMembers  int
    CreatorID   int64
    CreatedAt   time.Time
}

type CommunityType string
const (
    CommunityTypeOfficial    CommunityType = "OFFICIAL"
    CommunityTypeUserCreated CommunityType = "USER_CREATED"
    CommunityTypeMerchant    CommunityType = "MERCHANT"
)

// 业务规则：不同类型的社区人数上限不同
func (c *Community) MaxMembersForType() int {
    switch c.Type {
    case CommunityTypeOfficial:    return 10000
    case CommunityTypeUserCreated: return 500
    case CommunityTypeMerchant:    return 2000
    default: return 500
    }
}

// 业务规则：地理围栏校验
func (c *Community) CanJoin(userLocation Location) bool {
    return c.Status == CommunityStatusActive &&
           c.GeoFence.Contains(userLocation)
}

// 成员管理
type CommunityMember struct {
    ID          int64
    CommunityID int64
    UserID      int64
    Role        MemberRole // OWNER / ADMIN / MEMBER
    JoinedAt    time.Time
}

type MemberRole string
const (
    MemberRoleOwner  MemberRole = "OWNER"
    MemberRoleAdmin  MemberRole = "ADMIN"
    MemberRoleMember MemberRole = "MEMBER"
)

// 值对象
type GeoFence struct {
    Polygon []Coordinate // GeoJSON 多边形顶点
    Center  Coordinate   // 计算中心点
}

type Coordinate struct {
    Lng float64
    Lat float64
}

func (g GeoFence) Contains(loc Location) bool {
    // 实际实现委托给 PostGIS ST_Contains 查询
    // 领域层定义接口，基础设施层实现
    panic("delegate to infrastructure via GeoFenceChecker interface")
}

// 地理围栏校验端口（领域服务接口）
type GeoFenceChecker interface {
    Contains(ctx context.Context, communityID int64, lng, lat float64) (bool, error)
}

// Repository 端口
type CommunityRepository interface {
    Save(ctx context.Context, community *Community) error
    FindByID(ctx context.Context, id int64) (*Community, error)
    FindContainingPoint(ctx context.Context, lng, lat float64) ([]*Community, error)
    CountMembers(ctx context.Context, communityID int64) (int, error)
    AddMember(ctx context.Context, member *CommunityMember) error
    RemoveMember(ctx context.Context, communityID, userID int64) error
    FindMember(ctx context.Context, communityID, userID int64) (*CommunityMember, error)
    UpdateMemberRole(ctx context.Context, communityID, userID int64, role MemberRole) error
}
```

### 3.4 GroupBuy 聚合（含成团后支付流程）

```go
// domain/groupbuy.go
type GroupBuyStatus string
const (
    GroupBuyRecruiting GroupBuyStatus = "RECRUITING"  // 招募中
    GroupBuyFulfilled  GroupBuyStatus = "FULFILLED"   // 达到最低人数，继续招募
    GroupBuyPaying     GroupBuyStatus = "PAYING"      // 成团确认，支付期
    GroupBuyCompleted  GroupBuyStatus = "COMPLETED"   // 全部支付完成/部分支付达标
    GroupBuyFailed     GroupBuyStatus = "FAILED"      // 未成团或支付不足
)

type GroupBuy struct {
    ID               int64
    CommunityID      int64
    InitiatorID      int64
    Title            string
    Description      string
    Images           []string
    UnitPrice        decimal.Decimal
    MinParticipants  int
    MaxParticipants  int  // 0 = 不限
    Status           GroupBuyStatus
    Deadline         time.Time
    PaymentDeadline  time.Time // 成团后设置
    CurrentCount     int       // 通过 application 层保持与 order 表同步
    PaidCount        int       // 已支付人数
    CreatedAt        time.Time
    Orders           []GroupBuyOrder // 聚合内管理订单
}

// 简化状态机（移除 SUCCEEDED，合并为 PAYING）：
// RECRUITING ──(达到min)──▶ FULFILLED ──(截止+达标)──▶ PAYING ──▶ COMPLETED
//     │                       │                          │
//     └──(截止且不足)──▶ FAILED ◀──(截止且不足)─────────┘
//                                ◀──(支付截止，已付<min)──┘

// 报名（不付款）— 需在 application 层检查用户是否已报名
func (g *GroupBuy) Join(userID int64, existingOrderIDs []int64) (*GroupBuyOrder, error) {
    if g.Status != GroupBuyRecruiting && g.Status != GroupBuyFulfilled {
        return nil, ErrGroupBuyNotRecruiting
    }
    if g.MaxParticipants > 0 && g.CurrentCount >= g.MaxParticipants {
        return nil, ErrGroupBuyFull
    }
    // 防重复报名：application 层查询后传入已有 orderIDs
    for _, oid := range existingOrderIDs {
        if oid > 0 {
            return nil, ErrAlreadyJoined
        }
    }
    order := &GroupBuyOrder{
        GroupBuyID: g.ID,
        UserID:     userID,
        Quantity:   1,
        TotalAmount: g.UnitPrice,
        Status:     OrderStatusJoined,
        JoinedAt:   time.Now(),
    }
    g.CurrentCount++
    if g.CurrentCount >= g.MinParticipants {
        g.Status = GroupBuyFulfilled
    }
    return order, nil
}

// 截止时间到，检查是否成团 → 直接进入 PAYING 状态
func (g *GroupBuy) CheckDeadline() error {
    if time.Now().Before(g.Deadline) {
        return ErrGroupBuyNotExpired
    }
    if g.CurrentCount >= g.MinParticipants {
        g.Status = GroupBuyPaying
        g.PaymentDeadline = time.Now().Add(24 * time.Hour)
        return nil // 成团成功，通知支付
    }
    g.Status = GroupBuyFailed
    return nil
}

// 确认某笔支付
func (g *GroupBuy) ConfirmPayment(orderID int64) error {
    if g.Status != GroupBuyPaying {
        return ErrGroupBuyNotInPayingState
    }
    g.PaidCount++
    if g.PaidCount >= g.CurrentCount {
        g.Status = GroupBuyCompleted // 全部付款
    }
    return nil
}

// 支付截止检查
func (g *GroupBuy) CheckPaymentDeadline() error {
    if g.Status != GroupBuyPaying {
        return ErrGroupBuyNotInPayingState
    }
    if time.Now().Before(g.PaymentDeadline) {
        return ErrPaymentNotExpired
    }
    if g.PaidCount >= g.MinParticipants {
        g.Status = GroupBuyCompleted // 已付人数达标，成团
    } else {
        g.Status = GroupBuyFailed // 已付不足，需退款给已付用户
    }
    return nil
}

// 团购订单
type OrderStatus string
const (
    OrderStatusJoined    OrderStatus = "JOINED"    // 已报名
    OrderStatusPaying    OrderStatus = "PAYING"    // 支付中
    OrderStatusPaid      OrderStatus = "PAID"      // 已支付
    OrderStatusCancelled OrderStatus = "CANCELLED" // 已取消
    OrderStatusRefunded  OrderStatus = "REFUNDED"  // 已退款
)

type GroupBuyOrder struct {
    ID          int64
    GroupBuyID  int64
    UserID      int64
    Quantity    int             // 固定为 1（MVP 阶段）
    TotalAmount decimal.Decimal
    Status      OrderStatus
    OutTradeNo  string          // 微信支付商户订单号，格式: GB{groupBuyID}U{userID}T{timestamp}
    PaymentID   string          // 微信支付单号（回调填入）
    JoinedAt    time.Time
    PaidAt      time.Time
}

// 支付流水记录
type PaymentRecord struct {
    ID            int64
    OutTradeNo    string          // 商户订单号
    TransactionID string          // 微信支付单号
    OrderID       int64           // 关联 GroupBuyOrder
    Amount        decimal.Decimal
    Type          PaymentType     // PAY / REFUND
    Status        PaymentStatus   // PENDING / SUCCESS / FAILED
    RawNotify     string          // 微信原始回调 JSON（用于对账）
    CreatedAt     time.Time
    UpdatedAt     time.Time
}

type PaymentType string
const (
    PaymentTypePay    PaymentType = "PAY"
    PaymentTypeRefund PaymentType = "REFUND"
)

// Repository 端口
type GroupBuyRepository interface {
    Save(ctx context.Context, groupBuy *GroupBuy) error
    FindByID(ctx context.Context, id int64) (*GroupBuy, error)
    FindExpiredRecruiting(ctx context.Context) ([]*GroupBuy, error)
    FindExpiredPaying(ctx context.Context) ([]*GroupBuy, error)
    SaveOrder(ctx context.Context, order *GroupBuyOrder) error
    FindOrderByUserAndGroupBuy(ctx context.Context, groupBuyID, userID int64) (*GroupBuyOrder, error)
    UpdateOrderStatus(ctx context.Context, orderID int64, status OrderStatus) error
    SavePaymentRecord(ctx context.Context, record *PaymentRecord) error
    FindPaymentByOutTradeNo(ctx context.Context, outTradeNo string) (*PaymentRecord, error)
}
```

### 3.5 Post 聚合

```go
// domain/post.go
type PostStatus string
const (
    PostStatusActive  PostStatus = "ACTIVE"
    PostStatusDeleted PostStatus = "DELETED"
    PostStatusHidden  PostStatus = "HIDDEN" // 被举报隐藏，待审核
)

type Post struct {
    ID           int64
    CommunityID  int64
    AuthorID     int64
    Content      string
    Images       []PostImage  // 值对象
    LikeCount    int
    CommentCount int
    Status       PostStatus
    CreatedAt    time.Time
}

// 业务方法
func (p *Post) Delete(operatorID int64, operatorRole MemberRole) error {
    if p.AuthorID != operatorID && operatorRole != MemberRoleOwner && operatorRole != MemberRoleAdmin {
        return ErrNoPermission
    }
    p.Status = PostStatusDeleted
    return nil
}

func (p *Post) Hide() {
    p.Status = PostStatusHidden
}

type PostImage struct { // 值对象
    URL    string
    Width  int
    Height int
    Order  int
}

// Like 实体
type Like struct {
    ID        int64
    PostID    int64
    UserID    int64
    CreatedAt time.Time
}

// Comment 实体
const MaxCommentDepth = 2 // 最多支持二级评论（评论+回复），不支持无限嵌套

type CommentStatus string
const (
    CommentStatusActive  CommentStatus = "ACTIVE"
    CommentStatusDeleted CommentStatus = "DELETED"
)

type Comment struct {
    ID        int64
    PostID    int64
    AuthorID  int64
    Content   string
    ParentID  *int64        // nil = 顶级评论，非nil = 回复某条评论
    Status    CommentStatus
    CreatedAt time.Time
}

// Repository 端口
type PostRepository interface {
    Save(ctx context.Context, post *Post) error
    FindByID(ctx context.Context, id int64) (*Post, error)
    FindByCommunity(ctx context.Context, communityID int64, cursor time.Time, limit int) ([]*Post, error)
    FindFeed(ctx context.Context, userID int64, cursor time.Time, limit int) ([]*Post, error)
    SaveComment(ctx context.Context, comment *Comment) error
    FindCommentsByPost(ctx context.Context, postID int64, cursor time.Time, limit int) ([]*Comment, error)
    SaveLike(ctx context.Context, like *Like) error
    DeleteLike(ctx context.Context, postID, userID int64) error
    HasLiked(ctx context.Context, postID, userID int64) (bool, error)
}
```

### 3.6 Vote 聚合

```go
// domain/vote.go
type VoteStatus string
const (
    VoteStatusActive VoteStatus = "ACTIVE"
    VoteStatusClosed VoteStatus = "CLOSED"
)

type Vote struct {
    ID           int64
    CommunityID  int64
    CreatorID    int64
    Title        string
    Options      []VoteOption
    MultiSelect  bool
    Anonymous    bool
    Deadline     time.Time
    Status       VoteStatus
    CreatedAt    time.Time
}

type VoteOption struct {
    ID    int64
    Text  string
    Count int
}

type VoteRecord struct {
    ID       int64
    VoteID   int64
    OptionID int64
    UserID   int64
    CreatedAt time.Time
}

// Cast — 需在 application 层先查询 hasVoted 传入
func (v *Vote) Cast(userID int64, optionIDs []int64, hasVoted bool) error {
    if v.Status != VoteStatusActive {
        return ErrVoteClosed
    }
    if time.Now().After(v.Deadline) {
        return ErrVoteExpired
    }
    if hasVoted {
        return ErrAlreadyVoted
    }
    if !v.MultiSelect && len(optionIDs) > 1 {
        return ErrVoteSingleSelectOnly
    }
    // 校验 optionIDs 都属于本投票
    validIDs := make(map[int64]bool)
    for _, opt := range v.Options {
        validIDs[opt.ID] = true
    }
    for _, oid := range optionIDs {
        if !validIDs[oid] {
            return ErrInvalidVoteOption
        }
    }
    return nil
}

// Repository 端口
type VoteRepository interface {
    Save(ctx context.Context, vote *Vote) error
    FindByID(ctx context.Context, id int64) (*Vote, error)
    SaveRecords(ctx context.Context, records []*VoteRecord) error
    HasVoted(ctx context.Context, voteID, userID int64) (bool, error)
    FindExpiredActive(ctx context.Context) ([]*Vote, error)
}
```

---

## 四、API 设计

### 4.1 统一响应格式

```json
// 成功
{ "code": 0, "message": "ok", "data": { ... } }

// 分页响应
{ "code": 0, "message": "ok", "data": { "items": [...], "next_cursor": "2026-03-28T10:00:00Z_12345" } }

// 失败
{ "code": 10001, "message": "社区不存在", "data": null }
```

**分页策略**：所有列表接口使用 **游标分页（Cursor-based Pagination）**，基于 `(created_at, id)` 复合游标，避免 OFFSET 深分页性能问题。
- 请求参数：`?cursor=2026-03-28T10:00:00Z_12345&limit=20`
- 响应字段：`next_cursor`（为空表示无更多数据）

**错误码规范**：

| 范围 | 模块 | 示例 |
|------|------|------|
| 10000-10999 | 通用 | 10001 参数校验失败, 10002 未授权, 10003 无权限 |
| 11000-11999 | 社区 | 11001 社区不存在, 11002 不在地理范围内, 11003 社区已满, 11004 已加入该社区 |
| 12000-12999 | 帖子 | 12001 帖子不存在, 12002 帖子已删除 |
| 13000-13999 | 投票 | 13001 投票不存在, 13002 投票已关闭, 13003 已投过票 |
| 14000-14999 | 团购 | 14001 团购不存在, 14002 未在招募中, 14003 已满员, 14004 已报名, 14005 支付超时 |
| 15000-15999 | 支付 | 15001 支付创建失败, 15002 回调验签失败, 15003 退款失败 |

### 4.2 Community API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/communities` | 创建社区 | 需要 |
| GET | `/api/v1/communities/:id` | 社区详情 | 需要 |
| GET | `/api/v1/communities/nearby?lng=&lat=` | 附近可加入的社区 | 需要 |
| POST | `/api/v1/communities/:id/join` | 加入社区（校验地理围栏） | 需要 |
| DELETE | `/api/v1/communities/:id/leave` | 退出社区 | 需要 |
| GET | `/api/v1/communities/:id/members` | 成员列表 | 需要 |
| PUT | `/api/v1/communities/:id` | 更新社区信息（名称/描述/围栏） | 需要（OWNER/ADMIN） |
| DELETE | `/api/v1/communities/:id/members/:userId` | 移除成员（踢人） | 需要（OWNER/ADMIN） |
| PUT | `/api/v1/communities/:id/members/:userId/role` | 设置成员角色 | 需要（OWNER） |
| GET | `/api/v1/users/me/communities` | 我的社区 | 需要 |

### 4.3 Post API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/communities/:id/posts` | 发帖（图文） | 需要 |
| GET | `/api/v1/communities/:id/posts` | 社区帖子列表 | 需要 |
| GET | `/api/v1/posts/:id` | 帖子详情 | 需要 |
| DELETE | `/api/v1/posts/:id` | 删帖 | 需要 |
| POST | `/api/v1/posts/:id/like` | 点赞 | 需要 |
| DELETE | `/api/v1/posts/:id/like` | 取消点赞 | 需要 |
| POST | `/api/v1/posts/:id/comments` | 评论 | 需要 |
| GET | `/api/v1/posts/:id/comments` | 评论列表 | 需要 |
| DELETE | `/api/v1/comments/:id` | 删除评论 | 需要（作者/管理员） |
| GET | `/api/v1/feed` | 个人Feed（游标分页） | 需要 |

### 4.4 Vote API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/communities/:id/votes` | 发起投票 | 需要 |
| GET | `/api/v1/votes/:id` | 投票详情+结果 | 需要 |
| POST | `/api/v1/votes/:id/cast` | 投票 | 需要 |

### 4.5 GroupBuy API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/communities/:id/groupbuys` | 发起团购（仅商家社区） | 需要 |
| GET | `/api/v1/communities/:id/groupbuys` | 团购列表 | 需要 |
| GET | `/api/v1/groupbuys/:id` | 团购详情 | 需要 |
| POST | `/api/v1/groupbuys/:id/join` | 报名参团（不付款） | 需要 |
| DELETE | `/api/v1/groupbuys/:id/leave` | 退出（仅 RECRUITING/FULFILLED 状态） | 需要 |
| POST | `/api/v1/groupbuys/:id/pay` | 成团后发起支付（返回微信支付参数） | 需要 |
| POST | `/api/v1/webhooks/wechat-pay` | 微信支付回调（验签+幂等） | 无需（微信签名验证） |

**微信支付回调处理规则**：
- 验证微信签名（wechatpay-go SDK 自动处理）
- 通过 `out_trade_no` 查找 PaymentRecord，幂等判断
- 更新 GroupBuyOrder 状态为 PAID
- 检查是否所有人已付 → 更新 GroupBuy 状态
- 返回 HTTP 200 + `{"code": "SUCCESS"}` 给微信

### 4.6 Auth API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/wechat-login` | 微信小程序登录 |
| POST | `/api/v1/auth/refresh` | 刷新 Token |
| GET | `/api/v1/users/me` | 当前用户信息 |
| PUT | `/api/v1/users/me` | 更新个人信息 |

### 4.7 Upload API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/upload/image` | 上传图片（返回COS URL） |

---

## 五、数据库设计（PostgreSQL + PostGIS）

```sql
-- 启用 PostGIS 扩展
CREATE EXTENSION IF NOT EXISTS postgis;

-- ========== 用户 ==========
CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,
    openid VARCHAR(64) UNIQUE NOT NULL,       -- 微信 openid
    unionid VARCHAR(64),                       -- 微信 unionid
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ========== 社区 ==========
CREATE TABLE t_community (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,                 -- OFFICIAL / USER_CREATED / MERCHANT
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    boundary GEOMETRY(POLYGON, 4326) NOT NULL, -- PostGIS 多边形
    center GEOMETRY(POINT, 4326),               -- 中心点
    max_members INT NOT NULL DEFAULT 500,
    avatar_url VARCHAR(500),
    creator_id BIGINT NOT NULL REFERENCES t_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_community_boundary ON t_community USING GIST(boundary);
CREATE INDEX idx_community_center ON t_community USING GIST(center);

-- ========== 社区成员 ==========
CREATE TABLE t_community_member (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES t_community(id),
    user_id BIGINT NOT NULL REFERENCES t_user(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- OWNER / ADMIN / MEMBER
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, user_id)
);
CREATE INDEX idx_member_user ON t_community_member(user_id);

-- ========== 帖子 ==========
CREATE TABLE t_post (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES t_community(id),
    author_id BIGINT NOT NULL REFERENCES t_user(id),
    content TEXT NOT NULL,
    images JSONB DEFAULT '[]',                   -- [{url, width, height, order}]
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_post_community_time ON t_post(community_id, created_at DESC);
CREATE INDEX idx_post_author ON t_post(author_id, created_at DESC);

-- ========== 评论 ==========
CREATE TABLE t_comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES t_post(id),
    author_id BIGINT NOT NULL REFERENCES t_user(id),
    content TEXT NOT NULL,
    parent_id BIGINT DEFAULT NULL REFERENCES t_comment(id),  -- NULL = 顶级评论
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_comment_post ON t_comment(post_id, created_at);

-- ========== 点赞 ==========
CREATE TABLE t_like (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES t_post(id),
    user_id BIGINT NOT NULL REFERENCES t_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

-- ========== 投票 ==========
CREATE TABLE t_vote (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES t_community(id),
    creator_id BIGINT NOT NULL REFERENCES t_user(id),
    title VARCHAR(200) NOT NULL,
    multi_select BOOLEAN NOT NULL DEFAULT FALSE,
    anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deadline TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE t_vote_option (
    id BIGSERIAL PRIMARY KEY,
    vote_id BIGINT NOT NULL REFERENCES t_vote(id),
    text VARCHAR(200) NOT NULL,
    count INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE t_vote_record (
    id BIGSERIAL PRIMARY KEY,
    vote_id BIGINT NOT NULL REFERENCES t_vote(id),
    option_id BIGINT NOT NULL REFERENCES t_vote_option(id),
    user_id BIGINT NOT NULL REFERENCES t_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(vote_id, option_id, user_id)
);

-- ========== 团购接龙 ==========
CREATE TABLE t_group_buy (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES t_community(id),
    initiator_id BIGINT NOT NULL REFERENCES t_user(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    images JSONB DEFAULT '[]',
    unit_price DECIMAL(10,2) NOT NULL,
    min_participants INT NOT NULL,
    max_participants INT NOT NULL DEFAULT 0,    -- 0 = 不限
    status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
    deadline TIMESTAMPTZ NOT NULL,
    payment_deadline TIMESTAMPTZ,                -- 成团后设置
    current_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_groupbuy_community ON t_group_buy(community_id, status, created_at DESC);

CREATE TABLE t_group_buy_order (
    id BIGSERIAL PRIMARY KEY,
    group_buy_id BIGINT NOT NULL REFERENCES t_group_buy(id),
    user_id BIGINT NOT NULL REFERENCES t_user(id),
    quantity INT NOT NULL DEFAULT 1,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'JOINED', -- JOINED / PAYING / PAID / CANCELLED
    payment_id VARCHAR(64),                        -- 微信支付单号
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    UNIQUE(group_buy_id, user_id)
);

-- ========== 支付流水 ==========
CREATE TABLE t_payment_record (
    id BIGSERIAL PRIMARY KEY,
    out_trade_no VARCHAR(64) UNIQUE NOT NULL,    -- 商户订单号
    transaction_id VARCHAR(64),                   -- 微信支付单号
    order_id BIGINT NOT NULL REFERENCES t_group_buy_order(id),
    amount DECIMAL(10,2) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'PAY',      -- PAY / REFUND
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING / SUCCESS / FAILED
    raw_notify TEXT,                               -- 微信原始回调 JSON
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payment_order ON t_payment_record(order_id);
```

---

## 六、核心流程设计

### 6.1 加入社区流程（地理围栏校验）

```
小程序获取用户位置(wx.getLocation)
    │
    ▼
POST /api/v1/communities/:id/join { lng, lat }
    │
    ▼
后端：SELECT ST_Contains(boundary, ST_SetSRID(ST_MakePoint(:lng,:lat), 4326))
      FROM t_community WHERE id = :id
    │
    ├── false → 返回错误: "您不在该社区范围内"
    │
    ▼ true
检查社区状态、人数上限
    │
    ▼
INSERT t_community_member → 加入成功
```

### 6.2 团购接龙全流程（成团后支付）

```
阶段1: 招募
─────────────
商家发起团购 → status=RECRUITING
用户报名(不付款) → INSERT t_group_buy_order(status=JOINED)
达到 min_participants → status=FULFILLED（继续招募直到截止）

阶段2: 成团判定
─────────────
定时任务扫描到期团购:
  IF current_count >= min_participants:
    status = SUCCEEDED → PAYING
    payment_deadline = NOW() + 24h
    发送微信模板消息通知所有参与者支付
  ELSE:
    status = FAILED
    所有 order status = CANCELLED

阶段3: 支付期
─────────────
参与者收到通知 → 点击支付 → 调用微信支付JSAPI
微信支付回调 → order status = PAID

阶段4: 支付截止
─────────────
定时任务检查 payment_deadline 到期:
  IF 全部已付: status = COMPLETED
  IF 部分未付: 未付 order status = CANCELLED, 释放名额
  根据业务规则决定是否仍然成团（已付人数 >= min_participants）
```

### 6.3 Feed 流查询

```sql
-- 拉模式：查询用户已加入社区的最新帖子
SELECT p.id, p.content, p.images, p.like_count, p.comment_count,
       p.created_at, u.nickname, u.avatar_url, c.name as community_name
FROM t_post p
JOIN t_community_member cm ON p.community_id = cm.community_id
JOIN t_user u ON p.author_id = u.id
JOIN t_community c ON p.community_id = c.id
WHERE cm.user_id = :userId
  AND p.status = 'ACTIVE'
ORDER BY p.created_at DESC
LIMIT 20 OFFSET :offset;

-- Redis 缓存策略（社区级缓存，避免用户级写放大）：
-- Key: community:feed:{communityId}:gen  存储 generation counter
-- Key: community:feed:{communityId}:gen:{gen}:page:{n}  实际数据，TTL: 5min
-- 发帖时只需 INCR community:feed:{communityId}:gen（O(1)操作）
-- 读取时先获取 gen，再查对应 gen 的缓存，miss 则查 DB 并写入
```

---

## 七、部署架构

```
┌──────────────────────────────────────────────────┐
│                    云服务器                        │
│                                                   │
│  ┌─────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  Nginx  │  │ Go App   │  │ PostgreSQL+PostGIS│ │
│  │  :80    │─▶│ :8080    │─▶│ :5432            │ │
│  │  :443   │  │          │  │                   │ │
│  └─────────┘  │          │─▶│ Redis :6379       │ │
│               └──────────┘  └──────────────────┘ │
│                                                   │
│  docker-compose.yml 编排以上所有服务               │
└──────────────────────────────────────────────────┘
         │
    腾讯云 COS + CDN（图片/视频）
```

---

## 八、MVP 分期计划

### Phase 1: 基础设施 + 用户系统（1-2 周）
- 项目脚手架搭建（Go 模块化单体）
- PostgreSQL + PostGIS + Redis 环境
- 微信小程序登录 + JWT 认证
- 图片上传（COS）
- 统一响应格式 + 错误处理

### Phase 2: 社区功能（2-3 周）
- 创建社区（三种类型）
- 自定义多边形地理围栏（PostGIS）
- 加入/退出社区（地理围栏校验）
- 社区详情 + 成员列表
- 附近社区查询

### Phase 3: 帖子 + 互动（2-3 周）
- 图文帖子发布
- 帖子列表 + 详情
- 点赞 + 评论
- Feed 流（拉模式 + Redis 缓存）

### Phase 4: 投票（1 周）
- 发起投票（单选/多选/匿名）
- 投票 + 结果展示
- 投票截止自动关闭

### Phase 5: 团购接龙（2-3 周）
- 发起团购
- 报名接龙
- 成团判定定时任务
- 微信支付集成
- 支付通知 + 支付截止处理

### Phase 6: 小程序前端（3-4 周，可与后端并行）
- 首页 Feed
- 社区列表 + 详情页
- 地图选区（创建社区时画多边形）
- 发帖页（图文编辑）
- 投票组件
- 团购详情 + 支付页

**总预估：10-16 周（1 人全栈开发）**

---

## 九、安全与运维

### 9.1 安全措施

| 维度 | 方案 |
|------|------|
| 认证 | JWT（access token 2h + refresh token 7d），Redis 存储 token 白名单 |
| API 限流 | Gin middleware，单用户 60 req/min，全局 1000 req/min |
| 输入校验 | Go struct tag validation（go-playground/validator），所有用户输入 sanitize |
| SQL 注入 | 使用参数化查询（pgx/sqlx prepared statements），禁止字符串拼接 |
| XSS 防护 | 用户内容入库前 HTML escape，图片 URL 白名单（仅允许 COS 域名） |
| 支付安全 | 微信支付签名验证（wechatpay-go SDK），回调幂等（out_trade_no 唯一约束） |
| 敏感词过滤 | 帖子/评论发布前经过敏感词 Trie 树过滤，命中则拒绝发布 |

### 9.2 定时任务

使用 Go 内置 `time.Ticker` + goroutine 实现（MVP 阶段不引入外部调度框架）：

| 任务 | 频率 | 职责 |
|------|------|------|
| 团购成团检查 | 每分钟 | 扫描到期的 RECRUITING/FULFILLED 团购，判定成团/失败 |
| 支付截止检查 | 每分钟 | 扫描到期的 PAYING 团购，处理未付订单 |
| 投票截止 | 每 5 分钟 | 关闭过期投票 |
| Feed 缓存预热 | 每 10 分钟 | 热门社区 Feed 预生成 |

### 9.3 运维

| 维度 | 方案 |
|------|------|
| 数据库备份 | PostgreSQL `pg_dump` 每日全量备份 + WAL 归档，保留 30 天 |
| 监控 | Prometheus + Grafana（Docker Compose 部署），监控 QPS、延迟、错误率 |
| 日志 | 结构化 JSON 日志（zerolog），本地文件轮转 + 可选 Loki 聚合 |
| 健康检查 | `/health` 端点（检查 DB + Redis 连接），Nginx upstream health check |
| 数据库迁移 | golang-migrate，SQL 文件管理在 `deploy/migrations/` |

---

## 十、Verification（验证方式）

1. **单元测试**：每个聚合的领域逻辑 100% 测试覆盖
2. **集成测试**：PostGIS 地理围栏查询、微信支付回调
3. **API 测试**：所有端点 happy path + error path
4. **端到端**：小程序 → 创建社区 → 加入 → 发帖 → 投票 → 团购全流程
5. **地理围栏精确性**：多边形边界点测试（内部/外部/边界上）
6. **团购状态机**：覆盖所有状态转换路径，特别是成团后支付超时处理
7. **支付安全**：微信支付签名验证、回调幂等性、重放攻击防护
