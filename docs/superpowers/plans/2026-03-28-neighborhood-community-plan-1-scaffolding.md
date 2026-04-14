# Plan 1: Project Scaffolding + User System

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a new Go project with modular monolith structure, Docker Compose dev environment (PostgreSQL+PostGIS, Redis), WeChat Mini Program login with JWT auth, and shared infrastructure (error codes, response format, cursor pagination, middleware). Image upload (COS) and rate limiting are deferred to later phases.

**Architecture:** Go modular monolith using Gin framework. Each business module lives under `internal/{module}/` with `domain/`, `application/`, `infra/` subdirectories. Shared kernel under `internal/shared/`. HTTP handlers under `api/`. Infrastructure tooling under `pkg/`.

**Tech Stack:** Go 1.22+, Gin, pgx (PostgreSQL driver), go-redis, golang-migrate, go-playground/validator, golang-jwt, wechatpay-go, zerolog

**Spec:** `docs/superpowers/specs/2026-03-28-neighborhood-community-platform-design.md`

**Scope:** This plan covers Phase 1 only. Subsequent phases (Community, Posts, Votes, GroupBuy, Frontend) will have separate plans.

---

## File Structure

```
neighbor-hub/                              # Project root (new repo)
├── cmd/server/main.go                     # Entry point: wire deps, start server
├── api/
│   ├── middleware/
│   │   ├── auth.go                        # JWT auth middleware
│   │   ├── cors.go                        # CORS config
│   │   ├── ratelimit.go                   # Per-user rate limiting (deferred to Phase 2)
│   │   └── logging.go                     # Request logging (zerolog)
│   ├── router.go                          # Central route registration
│   └── user/
│       └── handler.go                     # Auth + user profile endpoints
├── internal/
│   ├── user/
│   │   ├── domain/
│   │   │   ├── user.go                    # User entity, UserStatus, Location VO
│   │   │   └── repository.go             # UserRepository interface
│   │   ├── application/
│   │   │   └── service.go                # UserService: login, get profile, update
│   │   └── infra/
│   │       └── postgres_repo.go          # PostgreSQL implementation of UserRepository
│   └── shared/
│       ├── errors.go                      # ErrorCode enum, AppError type
│       └── pagination.go                 # Cursor struct, parse/encode helpers
├── pkg/
│   ├── response/
│   │   └── response.go                   # ApiResult[T], Success(), Error(), Paginated()
│   ├── database/
│   │   └── postgres.go                   # pgx pool init, health check
│   ├── cache/
│   │   └── redis.go                      # go-redis client init, health check
│   ├── auth/
│   │   └── jwt.go                        # GenerateTokenPair, ParseToken, RefreshToken
│   ├── wechat/
│   │   └── miniapp.go                    # Code2Session (login)
│   └── storage/
│       └── cos.go                        # Upload image, generate presigned URL (deferred to Phase 3: Posts)
├── deploy/
│   ├── docker-compose.yml                # PostgreSQL+PostGIS, Redis
│   ├── Dockerfile                        # Multi-stage Go build
│   └── migrations/
│       └── 000001_init_schema.up.sql     # t_user table + PostGIS extension
│       └── 000001_init_schema.down.sql   # Drop t_user
├── config/
│   ├── config.go                         # Config struct (env-based)
│   └── config.dev.yaml                   # Dev defaults
├── go.mod
├── go.sum
├── .gitignore
└── Makefile                              # Common commands
```

---

### Task 1: Initialize Go Module + Project Skeleton

**Files:**
- Create: `neighbor-hub/go.mod`
- Create: `neighbor-hub/Makefile`
- Create: `neighbor-hub/.gitignore`
- Create: `neighbor-hub/cmd/server/main.go` (placeholder)

- [ ] **Step 1: Create project directory and init Go module**

```bash
mkdir -p ~/aiProject/neighbor-hub
cd ~/aiProject/neighbor-hub
go mod init github.com/user/neighbor-hub
```

- [ ] **Step 2: Create directory structure**

```bash
mkdir -p cmd/server api/{middleware,user} internal/{user/{domain,application,infra},shared} pkg/{response,database,cache,auth,wechat,storage} deploy/migrations config
```

- [ ] **Step 3: Create .gitignore**

```gitignore
# Binaries
/bin/
*.exe
*.out

# IDE
.idea/
.vscode/
*.swp

# Environment
.env
*.local.yaml

# OS
.DS_Store
Thumbs.db

# Go
vendor/
```

- [ ] **Step 4: Create Makefile**

```makefile
.PHONY: build run test lint migrate-up migrate-down docker-up docker-down

APP_NAME := neighbor-hub
MAIN := ./cmd/server

build:
	go build -o bin/$(APP_NAME) $(MAIN)

run:
	go run $(MAIN)

test:
	go test ./... -v -race -count=1

lint:
	golangci-lint run ./...

migrate-up:
	migrate -path deploy/migrations -database "$(DATABASE_URL)" up

migrate-down:
	migrate -path deploy/migrations -database "$(DATABASE_URL)" down 1

docker-up:
	docker compose -f deploy/docker-compose.yml up -d

docker-down:
	docker compose -f deploy/docker-compose.yml down
```

- [ ] **Step 5: Create placeholder main.go**

```go
// cmd/server/main.go
package main

import "fmt"

func main() {
	fmt.Println("neighbor-hub starting...")
}
```

- [ ] **Step 6: Verify it compiles**

Run: `go build ./cmd/server/`
Expected: No errors, binary created

- [ ] **Step 7: Git init and commit**

```bash
git init
git add .
git commit -m "feat: initialize project skeleton with Go module"
```

---

### Task 2: Docker Compose + Database Migration

**Files:**
- Create: `deploy/docker-compose.yml`
- Create: `deploy/migrations/000001_init_schema.up.sql`
- Create: `deploy/migrations/000001_init_schema.down.sql`
- Create: `deploy/Dockerfile`

- [ ] **Step 1: Create docker-compose.yml**

```yaml
# deploy/docker-compose.yml
version: "3.8"
services:
  postgres:
    image: postgis/postgis:16-3.4
    environment:
      POSTGRES_USER: neighbor
      POSTGRES_PASSWORD: neighbor_dev
      POSTGRES_DB: neighbor_hub
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U neighbor"]
      interval: 5s
      timeout: 3s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  pgdata:
```

- [ ] **Step 2: Create initial migration (up)**

```sql
-- deploy/migrations/000001_init_schema.up.sql
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,
    openid VARCHAR(64) UNIQUE NOT NULL,
    unionid VARCHAR(64),
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_openid ON t_user(openid);
```

- [ ] **Step 3: Create initial migration (down)**

```sql
-- deploy/migrations/000001_init_schema.down.sql
DROP TABLE IF EXISTS t_user;
DROP EXTENSION IF EXISTS postgis;
```

- [ ] **Step 4: Create Dockerfile**

```dockerfile
# deploy/Dockerfile
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /bin/neighbor-hub ./cmd/server

FROM alpine:3.19
RUN apk add --no-cache ca-certificates tzdata
COPY --from=builder /bin/neighbor-hub /bin/neighbor-hub
EXPOSE 8080
CMD ["/bin/neighbor-hub"]
```

- [ ] **Step 5: Start Docker Compose and verify**

Run: `make docker-up && sleep 3 && docker compose -f deploy/docker-compose.yml ps`
Expected: Both postgres and redis containers healthy

- [ ] **Step 6: Install golang-migrate and run migration**

Run:
```bash
go install -tags 'postgres' github.com/golang-migrate/migrate/v4/cmd/migrate@latest
DATABASE_URL="postgres://neighbor:neighbor_dev@localhost:5432/neighbor_hub?sslmode=disable" make migrate-up
```
Expected: Migration 000001 applied successfully

- [ ] **Step 7: Verify PostGIS works**

Run: `docker compose -f deploy/docker-compose.yml exec postgres psql -U neighbor neighbor_hub -c "SELECT PostGIS_Version();"`
Expected: Returns PostGIS version string

- [ ] **Step 8: Commit**

```bash
git add deploy/
git commit -m "feat: add Docker Compose (PostGIS + Redis) and initial migration"
```

---

### Task 3: Config + Database + Redis Packages

**Files:**
- Create: `config/config.go`
- Create: `config/config.dev.yaml`
- Create: `pkg/database/postgres.go`
- Create: `pkg/cache/redis.go`

- [ ] **Step 1: Install dependencies**

```bash
go get github.com/jackc/pgx/v5/pgxpool
go get github.com/redis/go-redis/v9
go get github.com/rs/zerolog
go get gopkg.in/yaml.v3
```

- [ ] **Step 2: Create config struct**

```go
// config/config.go
package config

import (
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server   ServerConfig   `yaml:"server"`
	Database DatabaseConfig `yaml:"database"`
	Redis    RedisConfig    `yaml:"redis"`
	JWT      JWTConfig      `yaml:"jwt"`
	WeChat   WeChatConfig   `yaml:"wechat"`
	COS      COSConfig      `yaml:"cos"`
}

type ServerConfig struct {
	Port string `yaml:"port"`
}

type DatabaseConfig struct {
	URL string `yaml:"url"`
}

type RedisConfig struct {
	Addr     string `yaml:"addr"`
	Password string `yaml:"password"`
	DB       int    `yaml:"db"`
}

type JWTConfig struct {
	Secret          string `yaml:"secret"`
	AccessTokenTTL  int    `yaml:"access_token_ttl"`  // minutes
	RefreshTokenTTL int    `yaml:"refresh_token_ttl"` // minutes
}

type WeChatConfig struct {
	AppID     string `yaml:"app_id"`
	AppSecret string `yaml:"app_secret"`
}

type COSConfig struct {
	BucketURL string `yaml:"bucket_url"`
	SecretID  string `yaml:"secret_id"`
	SecretKey string `yaml:"secret_key"`
	Region    string `yaml:"region"`
}

func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	data = []byte(os.ExpandEnv(string(data)))
	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, err
	}
	return &cfg, nil
}
```

- [ ] **Step 3: Create dev config file**

```yaml
# config/config.dev.yaml
server:
  port: ":8080"
database:
  url: "postgres://neighbor:neighbor_dev@localhost:5432/neighbor_hub?sslmode=disable"
redis:
  addr: "localhost:6379"
  password: ""
  db: 0
jwt:
  secret: "dev-secret-change-in-production"
  access_token_ttl: 120    # 2 hours
  refresh_token_ttl: 10080 # 7 days
wechat:
  app_id: "${WECHAT_APP_ID}"
  app_secret: "${WECHAT_APP_SECRET}"
cos:
  bucket_url: "${COS_BUCKET_URL}"
  secret_id: "${COS_SECRET_ID}"
  secret_key: "${COS_SECRET_KEY}"
  region: "ap-guangzhou"
```

- [ ] **Step 4: Create database package**

```go
// pkg/database/postgres.go
package database

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rs/zerolog/log"
)

func NewPostgresPool(ctx context.Context, databaseURL string) (*pgxpool.Pool, error) {
	config, err := pgxpool.ParseConfig(databaseURL)
	if err != nil {
		return nil, fmt.Errorf("parse database url: %w", err)
	}
	config.MaxConns = 20
	config.MinConns = 2
	config.MaxConnLifetime = 30 * time.Minute
	config.MaxConnIdleTime = 5 * time.Minute

	pool, err := pgxpool.NewWithConfig(ctx, config)
	if err != nil {
		return nil, fmt.Errorf("create pool: %w", err)
	}

	if err := pool.Ping(ctx); err != nil {
		return nil, fmt.Errorf("ping database: %w", err)
	}
	log.Info().Msg("PostgreSQL connected")
	return pool, nil
}

func HealthCheck(ctx context.Context, pool *pgxpool.Pool) error {
	return pool.Ping(ctx)
}
```

- [ ] **Step 5: Create Redis package**

```go
// pkg/cache/redis.go
package cache

import (
	"context"
	"fmt"

	"github.com/redis/go-redis/v9"
	"github.com/rs/zerolog/log"
)

func NewRedisClient(ctx context.Context, addr, password string, db int) (*redis.Client, error) {
	client := redis.NewClient(&redis.Options{
		Addr:     addr,
		Password: password,
		DB:       db,
	})

	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("ping redis: %w", err)
	}
	log.Info().Msg("Redis connected")
	return client, nil
}

func HealthCheck(ctx context.Context, client *redis.Client) error {
	return client.Ping(ctx).Err()
}
```

- [ ] **Step 6: Verify compilation**

Run: `go build ./...`
Expected: No errors

- [ ] **Step 7: Commit**

```bash
git add config/ pkg/database/ pkg/cache/ go.mod go.sum
git commit -m "feat: add config loader, PostgreSQL pool, and Redis client"
```

---

### Task 4: Shared Kernel — Error Codes + Response Format + Pagination

**Files:**
- Create: `internal/shared/errors.go`
- Create: `internal/shared/pagination.go`
- Create: `pkg/response/response.go`
- Test: `internal/shared/errors_test.go`
- Test: `internal/shared/pagination_test.go`
- Test: `pkg/response/response_test.go`

- [ ] **Step 1: Write error codes test**

```go
// internal/shared/errors_test.go
package shared_test

import (
	"testing"

	"github.com/user/neighbor-hub/internal/shared"
	"github.com/stretchr/testify/assert"
)

func TestAppError_Error(t *testing.T) {
	err := shared.NewAppError(shared.ErrUnauthorized, "token expired")
	assert.Equal(t, "token expired", err.Error())
	assert.Equal(t, shared.ErrUnauthorized, err.Code)
}

func TestAppError_Is(t *testing.T) {
	err := shared.NewAppError(shared.ErrCommunityNotFound, "not found")
	assert.True(t, shared.IsAppError(err))
	assert.False(t, shared.IsAppError(assert.AnError))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `go test ./internal/shared/ -v -run TestAppError`
Expected: FAIL — package not found

- [ ] **Step 3: Implement error codes**

```go
// internal/shared/errors.go
package shared

import "fmt"

type ErrorCode int

const (
	// General 10000-10999
	ErrParamInvalid  ErrorCode = 10001
	ErrUnauthorized  ErrorCode = 10002
	ErrNoPermission  ErrorCode = 10003
	ErrInternal      ErrorCode = 10004

	// Community 11000-11999
	ErrCommunityNotFound  ErrorCode = 11001
	ErrNotInGeoFence      ErrorCode = 11002
	ErrCommunityFull      ErrorCode = 11003
	ErrAlreadyJoined      ErrorCode = 11004

	// Post 12000-12999
	ErrPostNotFound ErrorCode = 12001
	ErrPostDeleted  ErrorCode = 12002

	// Vote 13000-13999
	ErrVoteNotFound ErrorCode = 13001
	ErrVoteClosed   ErrorCode = 13002
	ErrAlreadyVoted ErrorCode = 13003

	// GroupBuy 14000-14999
	ErrGroupBuyNotFound      ErrorCode = 14001
	ErrGroupBuyNotRecruiting ErrorCode = 14002
	ErrGroupBuyFull          ErrorCode = 14003
	ErrGroupBuyAlreadyJoined ErrorCode = 14004
	ErrPaymentTimeout        ErrorCode = 14005

	// Payment 15000-15999
	ErrPaymentCreateFailed ErrorCode = 15001
	ErrPaymentVerifyFailed ErrorCode = 15002
	ErrRefundFailed        ErrorCode = 15003
)

type AppError struct {
	Code    ErrorCode
	Message string
}

func NewAppError(code ErrorCode, message string) *AppError {
	return &AppError{Code: code, Message: message}
}

func (e *AppError) Error() string {
	return e.Message
}

func Errorf(code ErrorCode, format string, args ...interface{}) *AppError {
	return &AppError{Code: code, Message: fmt.Sprintf(format, args...)}
}

func IsAppError(err error) bool {
	_, ok := err.(*AppError)
	return ok
}

func GetAppError(err error) (*AppError, bool) {
	ae, ok := err.(*AppError)
	return ae, ok
}
```

- [ ] **Step 4: Run error test to verify it passes**

Run: `go test ./internal/shared/ -v -run TestAppError`
Expected: PASS

- [ ] **Step 5: Write pagination test**

```go
// internal/shared/pagination_test.go
package shared_test

import (
	"testing"
	"time"

	"github.com/user/neighbor-hub/internal/shared"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestCursor_EncodeDecode(t *testing.T) {
	ts := time.Date(2026, 3, 28, 10, 0, 0, 0, time.UTC)
	cursor := shared.Cursor{CreatedAt: ts, ID: 12345}

	encoded := cursor.Encode()
	assert.NotEmpty(t, encoded)

	decoded, err := shared.DecodeCursor(encoded)
	require.NoError(t, err)
	assert.Equal(t, int64(12345), decoded.ID)
	assert.True(t, ts.Equal(decoded.CreatedAt))
}

func TestDecodeCursor_Empty(t *testing.T) {
	cursor, err := shared.DecodeCursor("")
	require.NoError(t, err)
	assert.True(t, cursor.IsZero())
}

func TestDecodeCursor_Invalid(t *testing.T) {
	_, err := shared.DecodeCursor("not-a-valid-cursor")
	assert.Error(t, err)
}
```

- [ ] **Step 6: Run pagination test to verify it fails**

Run: `go test ./internal/shared/ -v -run TestCursor`
Expected: FAIL

- [ ] **Step 7: Implement pagination**

```go
// internal/shared/pagination.go
package shared

import (
	"encoding/base64"
	"fmt"
	"strconv"
	"strings"
	"time"
)

const DefaultPageSize = 20
const MaxPageSize = 100

type Cursor struct {
	CreatedAt time.Time
	ID        int64
}

func (c Cursor) IsZero() bool {
	return c.ID == 0 && c.CreatedAt.IsZero()
}

func (c Cursor) Encode() string {
	raw := fmt.Sprintf("%d_%d", c.CreatedAt.UnixMilli(), c.ID)
	return base64.URLEncoding.EncodeToString([]byte(raw))
}

func DecodeCursor(s string) (Cursor, error) {
	if s == "" {
		return Cursor{}, nil
	}
	data, err := base64.URLEncoding.DecodeString(s)
	if err != nil {
		return Cursor{}, fmt.Errorf("invalid cursor encoding: %w", err)
	}
	parts := strings.SplitN(string(data), "_", 2)
	if len(parts) != 2 {
		return Cursor{}, fmt.Errorf("invalid cursor format")
	}
	ms, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		return Cursor{}, fmt.Errorf("invalid cursor timestamp: %w", err)
	}
	id, err := strconv.ParseInt(parts[1], 10, 64)
	if err != nil {
		return Cursor{}, fmt.Errorf("invalid cursor id: %w", err)
	}
	return Cursor{
		CreatedAt: time.UnixMilli(ms),
		ID:        id,
	}, nil
}

func ClampPageSize(limit int) int {
	if limit <= 0 || limit > MaxPageSize {
		return DefaultPageSize
	}
	return limit
}
```

- [ ] **Step 8: Run pagination test to verify it passes**

Run: `go test ./internal/shared/ -v -run TestCursor`
Expected: PASS

- [ ] **Step 9: Write response format test**

```go
// pkg/response/response_test.go
package response_test

import (
	"encoding/json"
	"testing"

	"github.com/user/neighbor-hub/pkg/response"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSuccess(t *testing.T) {
	r := response.Success(map[string]string{"id": "123"})
	data, err := json.Marshal(r)
	require.NoError(t, err)
	assert.Contains(t, string(data), `"code":0`)
	assert.Contains(t, string(data), `"message":"ok"`)
}

func TestError(t *testing.T) {
	r := response.Error(10001, "invalid param")
	data, err := json.Marshal(r)
	require.NoError(t, err)
	assert.Contains(t, string(data), `"code":10001`)
	assert.Contains(t, string(data), `"message":"invalid param"`)
	assert.Contains(t, string(data), `"data":null`)
}

func TestPaginated(t *testing.T) {
	items := []string{"a", "b"}
	r := response.Paginated(items, "cursor123")
	data, err := json.Marshal(r)
	require.NoError(t, err)
	assert.Contains(t, string(data), `"next_cursor":"cursor123"`)
	assert.Contains(t, string(data), `"items"`)
}
```

- [ ] **Step 10: Run response test to verify it fails**

Run: `go test ./pkg/response/ -v`
Expected: FAIL

- [ ] **Step 11: Implement response format**

```go
// pkg/response/response.go
package response

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/user/neighbor-hub/internal/shared"
)

type ApiResult struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data"`
}

type PaginatedData struct {
	Items      interface{} `json:"items"`
	NextCursor string      `json:"next_cursor"`
}

func Success(data interface{}) ApiResult {
	return ApiResult{Code: 0, Message: "ok", Data: data}
}

func Error(code int, message string) ApiResult {
	return ApiResult{Code: code, Message: message, Data: nil}
}

func Paginated(items interface{}, nextCursor string) ApiResult {
	return ApiResult{
		Code:    0,
		Message: "ok",
		Data: PaginatedData{
			Items:      items,
			NextCursor: nextCursor,
		},
	}
}

// Gin helpers

func OK(c *gin.Context, data interface{}) {
	c.JSON(http.StatusOK, Success(data))
}

func OKPaginated(c *gin.Context, items interface{}, nextCursor string) {
	c.JSON(http.StatusOK, Paginated(items, nextCursor))
}

func HandleError(c *gin.Context, err error) {
	if ae, ok := shared.GetAppError(err); ok {
		status := mapCodeToHTTPStatus(ae.Code)
		c.JSON(status, Error(int(ae.Code), ae.Message))
		return
	}
	c.JSON(http.StatusInternalServerError, Error(int(shared.ErrInternal), "internal server error"))
}

func mapCodeToHTTPStatus(code shared.ErrorCode) int {
	switch {
	case code == shared.ErrUnauthorized:
		return http.StatusUnauthorized
	case code == shared.ErrNoPermission:
		return http.StatusForbidden
	case code == shared.ErrParamInvalid:
		return http.StatusBadRequest
	case code == shared.ErrCommunityNotFound,
		code == shared.ErrPostNotFound,
		code == shared.ErrVoteNotFound,
		code == shared.ErrGroupBuyNotFound:
		return http.StatusNotFound
	default:
		return http.StatusBadRequest
	}
}
```

- [ ] **Step 12: Install Gin + testify, run all tests**

```bash
go get github.com/gin-gonic/gin
go get github.com/stretchr/testify
go test ./internal/shared/ ./pkg/response/ -v
```
Expected: All PASS

- [ ] **Step 13: Commit**

```bash
git add internal/shared/ pkg/response/ go.mod go.sum
git commit -m "feat: add shared error codes, cursor pagination, and response format"
```

---

### Task 5: JWT Auth Package

**Files:**
- Create: `pkg/auth/jwt.go`
- Test: `pkg/auth/jwt_test.go`

- [ ] **Step 1: Write JWT test**

```go
// pkg/auth/jwt_test.go
package auth_test

import (
	"testing"
	"time"

	"github.com/user/neighbor-hub/pkg/auth"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const testSecret = "test-secret-key-at-least-32-chars"

func TestGenerateAndParse(t *testing.T) {
	pair, err := auth.GenerateTokenPair(testSecret, 42, 120, 10080)
	require.NoError(t, err)
	assert.NotEmpty(t, pair.AccessToken)
	assert.NotEmpty(t, pair.RefreshToken)

	claims, err := auth.ParseToken(testSecret, pair.AccessToken)
	require.NoError(t, err)
	assert.Equal(t, int64(42), claims.UserID)
	assert.Equal(t, "access", claims.TokenType)
}

func TestParseToken_Expired(t *testing.T) {
	pair, err := auth.GenerateTokenPair(testSecret, 1, 0, 0) // 0 minute TTL
	require.NoError(t, err)
	time.Sleep(10 * time.Millisecond)

	_, err = auth.ParseToken(testSecret, pair.AccessToken)
	assert.Error(t, err)
}

func TestParseToken_InvalidSecret(t *testing.T) {
	pair, err := auth.GenerateTokenPair(testSecret, 1, 120, 10080)
	require.NoError(t, err)

	_, err = auth.ParseToken("wrong-secret", pair.AccessToken)
	assert.Error(t, err)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `go test ./pkg/auth/ -v`
Expected: FAIL

- [ ] **Step 3: Implement JWT package**

```go
// pkg/auth/jwt.go
package auth

import (
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

type TokenPair struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
}

type Claims struct {
	UserID    int64  `json:"user_id"`
	TokenType string `json:"token_type"` // "access" or "refresh"
	jwt.RegisteredClaims
}

func GenerateTokenPair(secret string, userID int64, accessTTLMinutes, refreshTTLMinutes int) (*TokenPair, error) {
	accessToken, err := generateToken(secret, userID, "access", time.Duration(accessTTLMinutes)*time.Minute)
	if err != nil {
		return nil, fmt.Errorf("generate access token: %w", err)
	}

	refreshToken, err := generateToken(secret, userID, "refresh", time.Duration(refreshTTLMinutes)*time.Minute)
	if err != nil {
		return nil, fmt.Errorf("generate refresh token: %w", err)
	}

	return &TokenPair{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
	}, nil
}

func generateToken(secret string, userID int64, tokenType string, ttl time.Duration) (string, error) {
	now := time.Now()
	claims := Claims{
		UserID:    userID,
		TokenType: tokenType,
		RegisteredClaims: jwt.RegisteredClaims{
			IssuedAt:  jwt.NewNumericDate(now),
			ExpiresAt: jwt.NewNumericDate(now.Add(ttl)),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(secret))
}

func ParseToken(secret string, tokenString string) (*Claims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(secret), nil
	})
	if err != nil {
		return nil, err
	}
	claims, ok := token.Claims.(*Claims)
	if !ok || !token.Valid {
		return nil, fmt.Errorf("invalid token")
	}
	return claims, nil
}
```

- [ ] **Step 4: Install jwt dependency and run tests**

```bash
go get github.com/golang-jwt/jwt/v5
go test ./pkg/auth/ -v
```
Expected: All PASS

- [ ] **Step 5: Commit**

```bash
git add pkg/auth/ go.mod go.sum
git commit -m "feat: add JWT token generation and parsing"
```

---

### Task 6: User Domain + Repository

**Files:**
- Create: `internal/user/domain/user.go`
- Create: `internal/user/domain/repository.go`
- Test: `internal/user/domain/user_test.go`
- Create: `internal/user/infra/postgres_repo.go`

- [ ] **Step 1: Write user domain test**

```go
// internal/user/domain/user_test.go
package domain_test

import (
	"testing"

	"github.com/user/neighbor-hub/internal/user/domain"
	"github.com/stretchr/testify/assert"
)

func TestNewUser(t *testing.T) {
	u := domain.NewUser("openid123", "unionid456")
	assert.Equal(t, "openid123", u.OpenID)
	assert.Equal(t, "unionid456", u.UnionID)
	assert.Equal(t, domain.UserStatusActive, u.Status)
	assert.False(t, u.CreatedAt.IsZero())
}

func TestUser_UpdateProfile(t *testing.T) {
	u := domain.NewUser("openid123", "")
	u.UpdateProfile("Alice", "https://img.example.com/a.jpg")
	assert.Equal(t, "Alice", u.Nickname)
	assert.Equal(t, "https://img.example.com/a.jpg", u.AvatarURL)
}

func TestUser_IsBanned(t *testing.T) {
	u := domain.NewUser("openid123", "")
	assert.False(t, u.IsBanned())
	u.Status = domain.UserStatusBanned
	assert.True(t, u.IsBanned())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `go test ./internal/user/domain/ -v`
Expected: FAIL

- [ ] **Step 3: Implement user domain**

```go
// internal/user/domain/user.go
package domain

import "time"

type UserStatus string

const (
	UserStatusActive UserStatus = "ACTIVE"
	UserStatusBanned UserStatus = "BANNED"
)

type User struct {
	ID        int64
	OpenID    string
	UnionID   string
	Nickname  string
	AvatarURL string
	Phone     string
	Status    UserStatus
	CreatedAt time.Time
	UpdatedAt time.Time
}

func NewUser(openID, unionID string) *User {
	now := time.Now()
	return &User{
		OpenID:    openID,
		UnionID:   unionID,
		Status:    UserStatusActive,
		CreatedAt: now,
		UpdatedAt: now,
	}
}

func (u *User) UpdateProfile(nickname, avatarURL string) {
	u.Nickname = nickname
	u.AvatarURL = avatarURL
	u.UpdatedAt = time.Now()
}

func (u *User) IsBanned() bool {
	return u.Status == UserStatusBanned
}
```

```go
// internal/user/domain/repository.go
package domain

import "context"

type UserRepository interface {
	Save(ctx context.Context, user *User) (*User, error)
	FindByID(ctx context.Context, id int64) (*User, error)
	FindByOpenID(ctx context.Context, openID string) (*User, error)
	Update(ctx context.Context, user *User) error
}
```

- [ ] **Step 4: Run domain test**

Run: `go test ./internal/user/domain/ -v`
Expected: All PASS

- [ ] **Step 5: Implement PostgreSQL repository**

```go
// internal/user/infra/postgres_repo.go
package infra

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/user/neighbor-hub/internal/user/domain"
)

type PostgresUserRepo struct {
	pool *pgxpool.Pool
}

func NewPostgresUserRepo(pool *pgxpool.Pool) *PostgresUserRepo {
	return &PostgresUserRepo{pool: pool}
}

func (r *PostgresUserRepo) Save(ctx context.Context, user *domain.User) (*domain.User, error) {
	query := `INSERT INTO t_user (openid, unionid, nickname, avatar_url, phone, status, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		RETURNING id`
	err := r.pool.QueryRow(ctx, query,
		user.OpenID, user.UnionID, user.Nickname, user.AvatarURL,
		user.Phone, string(user.Status), user.CreatedAt, user.UpdatedAt,
	).Scan(&user.ID)
	if err != nil {
		return nil, fmt.Errorf("insert user: %w", err)
	}
	return user, nil
}

func (r *PostgresUserRepo) FindByID(ctx context.Context, id int64) (*domain.User, error) {
	return r.scanUser(ctx, `SELECT id, openid, unionid, nickname, avatar_url, phone, status, created_at, updated_at FROM t_user WHERE id = $1`, id)
}

func (r *PostgresUserRepo) FindByOpenID(ctx context.Context, openID string) (*domain.User, error) {
	return r.scanUser(ctx, `SELECT id, openid, unionid, nickname, avatar_url, phone, status, created_at, updated_at FROM t_user WHERE openid = $1`, openID)
}

func (r *PostgresUserRepo) Update(ctx context.Context, user *domain.User) error {
	query := `UPDATE t_user SET nickname=$1, avatar_url=$2, phone=$3, status=$4, updated_at=$5 WHERE id=$6`
	_, err := r.pool.Exec(ctx, query, user.Nickname, user.AvatarURL, user.Phone, string(user.Status), user.UpdatedAt, user.ID)
	return err
}

func (r *PostgresUserRepo) scanUser(ctx context.Context, query string, args ...interface{}) (*domain.User, error) {
	u := &domain.User{}
	var status string
	err := r.pool.QueryRow(ctx, query, args...).Scan(
		&u.ID, &u.OpenID, &u.UnionID, &u.Nickname, &u.AvatarURL,
		&u.Phone, &status, &u.CreatedAt, &u.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("scan user: %w", err)
	}
	u.Status = domain.UserStatus(status)
	return u, nil
}
```

- [ ] **Step 6: Verify compilation**

Run: `go build ./...`
Expected: No errors

- [ ] **Step 7: Commit**

```bash
git add internal/user/
git commit -m "feat: add User domain model and PostgreSQL repository"
```

---

### Task 7: User Application Service (Login + Profile)

**Files:**
- Create: `internal/user/application/service.go`
- Create: `pkg/wechat/miniapp.go`
- Test: `internal/user/application/service_test.go`

- [ ] **Step 1: Write WeChat miniapp stub**

```go
// pkg/wechat/miniapp.go
package wechat

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
)

type MiniAppClient struct {
	AppID     string
	AppSecret string
}

type Code2SessionResp struct {
	OpenID     string `json:"openid"`
	UnionID    string `json:"unionid"`
	SessionKey string `json:"session_key"`
	ErrCode    int    `json:"errcode"`
	ErrMsg     string `json:"errmsg"`
}

func NewMiniAppClient(appID, appSecret string) *MiniAppClient {
	return &MiniAppClient{AppID: appID, AppSecret: appSecret}
}

func (c *MiniAppClient) Code2Session(ctx context.Context, code string) (*Code2SessionResp, error) {
	url := fmt.Sprintf(
		"https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
		c.AppID, c.AppSecret, code,
	)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("wechat api call: %w", err)
	}
	defer resp.Body.Close()

	var result Code2SessionResp
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("decode response: %w", err)
	}
	if result.ErrCode != 0 {
		return nil, fmt.Errorf("wechat error: %d %s", result.ErrCode, result.ErrMsg)
	}
	return &result, nil
}
```

- [ ] **Step 2: Write application service test**

```go
// internal/user/application/service_test.go
package application_test

import (
	"context"
	"testing"

	"github.com/user/neighbor-hub/internal/user/application"
	"github.com/user/neighbor-hub/internal/user/domain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// Mock repository
type mockUserRepo struct {
	users  map[string]*domain.User
	nextID int64
}

func newMockUserRepo() *mockUserRepo {
	return &mockUserRepo{users: make(map[string]*domain.User), nextID: 1}
}

func (m *mockUserRepo) Save(ctx context.Context, user *domain.User) (*domain.User, error) {
	user.ID = m.nextID
	m.nextID++
	m.users[user.OpenID] = user
	return user, nil
}

func (m *mockUserRepo) FindByID(ctx context.Context, id int64) (*domain.User, error) {
	for _, u := range m.users {
		if u.ID == id {
			return u, nil
		}
	}
	return nil, nil
}

func (m *mockUserRepo) FindByOpenID(ctx context.Context, openID string) (*domain.User, error) {
	return m.users[openID], nil
}

func (m *mockUserRepo) Update(ctx context.Context, user *domain.User) error {
	m.users[user.OpenID] = user
	return nil
}

// Mock WeChat
type mockWeChatClient struct{}

func (m *mockWeChatClient) Code2Session(ctx context.Context, code string) (openID, unionID string, err error) {
	return "mock_openid_" + code, "mock_unionid", nil
}

func TestLoginOrRegister_NewUser(t *testing.T) {
	repo := newMockUserRepo()
	svc := application.NewUserService(repo, &mockWeChatClient{}, "secret", 120, 10080)

	result, err := svc.LoginOrRegister(context.Background(), "test_code")
	require.NoError(t, err)
	assert.NotEmpty(t, result.AccessToken)
	assert.NotEmpty(t, result.RefreshToken)
	assert.Equal(t, int64(1), result.UserID)
}

func TestLoginOrRegister_ExistingUser(t *testing.T) {
	repo := newMockUserRepo()
	svc := application.NewUserService(repo, &mockWeChatClient{}, "secret", 120, 10080)

	// First login creates user
	_, err := svc.LoginOrRegister(context.Background(), "abc")
	require.NoError(t, err)

	// Second login finds existing user
	result, err := svc.LoginOrRegister(context.Background(), "abc")
	require.NoError(t, err)
	assert.Equal(t, int64(1), result.UserID)
}

func TestGetProfile(t *testing.T) {
	repo := newMockUserRepo()
	svc := application.NewUserService(repo, &mockWeChatClient{}, "secret", 120, 10080)

	_, _ = svc.LoginOrRegister(context.Background(), "xyz")
	user, err := svc.GetProfile(context.Background(), 1)
	require.NoError(t, err)
	assert.Equal(t, "mock_openid_xyz", user.OpenID)
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `go test ./internal/user/application/ -v`
Expected: FAIL

- [ ] **Step 4: Implement application service**

```go
// internal/user/application/service.go
package application

import (
	"context"
	"fmt"

	"github.com/user/neighbor-hub/internal/shared"
	"github.com/user/neighbor-hub/internal/user/domain"
	"github.com/user/neighbor-hub/pkg/auth"
)

type WeChatClient interface {
	Code2Session(ctx context.Context, code string) (openID, unionID string, err error)
}

type LoginResult struct {
	UserID       int64  `json:"user_id"`
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
}

type UserService struct {
	repo            domain.UserRepository
	wechat          WeChatClient
	jwtSecret       string
	accessTokenTTL  int
	refreshTokenTTL int
}

func NewUserService(repo domain.UserRepository, wechat WeChatClient, jwtSecret string, accessTTL, refreshTTL int) *UserService {
	return &UserService{
		repo:            repo,
		wechat:          wechat,
		jwtSecret:       jwtSecret,
		accessTokenTTL:  accessTTL,
		refreshTokenTTL: refreshTTL,
	}
}

func (s *UserService) LoginOrRegister(ctx context.Context, code string) (*LoginResult, error) {
	openID, unionID, err := s.wechat.Code2Session(ctx, code)
	if err != nil {
		return nil, fmt.Errorf("wechat login: %w", err)
	}

	user, err := s.repo.FindByOpenID(ctx, openID)
	if err != nil {
		return nil, fmt.Errorf("find user: %w", err)
	}

	if user == nil {
		user = domain.NewUser(openID, unionID)
		user, err = s.repo.Save(ctx, user)
		if err != nil {
			return nil, fmt.Errorf("create user: %w", err)
		}
	}

	if user.IsBanned() {
		return nil, shared.NewAppError(shared.ErrNoPermission, "user is banned")
	}

	pair, err := auth.GenerateTokenPair(s.jwtSecret, user.ID, s.accessTokenTTL, s.refreshTokenTTL)
	if err != nil {
		return nil, fmt.Errorf("generate token: %w", err)
	}

	return &LoginResult{
		UserID:       user.ID,
		AccessToken:  pair.AccessToken,
		RefreshToken: pair.RefreshToken,
	}, nil
}

func (s *UserService) GetProfile(ctx context.Context, userID int64) (*domain.User, error) {
	user, err := s.repo.FindByID(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("find user: %w", err)
	}
	if user == nil {
		return nil, shared.NewAppError(shared.ErrUnauthorized, "user not found")
	}
	return user, nil
}

func (s *UserService) UpdateProfile(ctx context.Context, userID int64, nickname, avatarURL string) (*domain.User, error) {
	user, err := s.repo.FindByID(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("find user: %w", err)
	}
	if user == nil {
		return nil, shared.NewAppError(shared.ErrUnauthorized, "user not found")
	}
	user.UpdateProfile(nickname, avatarURL)
	if err := s.repo.Update(ctx, user); err != nil {
		return nil, fmt.Errorf("update user: %w", err)
	}
	return user, nil
}

func (s *UserService) RefreshToken(ctx context.Context, refreshTokenStr string) (*LoginResult, error) {
	claims, err := auth.ParseToken(s.jwtSecret, refreshTokenStr)
	if err != nil {
		return nil, shared.NewAppError(shared.ErrUnauthorized, "invalid refresh token")
	}
	if claims.TokenType != "refresh" {
		return nil, shared.NewAppError(shared.ErrUnauthorized, "not a refresh token")
	}

	pair, err := auth.GenerateTokenPair(s.jwtSecret, claims.UserID, s.accessTokenTTL, s.refreshTokenTTL)
	if err != nil {
		return nil, fmt.Errorf("generate token: %w", err)
	}
	return &LoginResult{
		UserID:       claims.UserID,
		AccessToken:  pair.AccessToken,
		RefreshToken: pair.RefreshToken,
	}, nil
}
```

- [ ] **Step 5: Run tests**

Run: `go test ./internal/user/application/ -v`
Expected: All PASS

- [ ] **Step 6: Commit**

```bash
git add internal/user/application/ pkg/wechat/
git commit -m "feat: add user application service with WeChat login and JWT"
```

---

### Task 8: Auth Middleware + User HTTP Handler + Router

**Files:**
- Create: `api/middleware/auth.go`
- Create: `api/middleware/logging.go`
- Create: `api/middleware/cors.go`
- Create: `api/user/handler.go`
- Create: `api/router.go`

- [ ] **Step 1: Create auth middleware**

```go
// api/middleware/auth.go
package middleware

import (
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/user/neighbor-hub/internal/shared"
	"github.com/user/neighbor-hub/pkg/auth"
	"github.com/user/neighbor-hub/pkg/response"
)

const UserIDKey = "user_id"

func AuthRequired(jwtSecret string) gin.HandlerFunc {
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if header == "" {
			response.HandleError(c, shared.NewAppError(shared.ErrUnauthorized, "missing authorization header"))
			c.Abort()
			return
		}

		parts := strings.SplitN(header, " ", 2)
		if len(parts) != 2 || parts[0] != "Bearer" {
			response.HandleError(c, shared.NewAppError(shared.ErrUnauthorized, "invalid authorization format"))
			c.Abort()
			return
		}

		claims, err := auth.ParseToken(jwtSecret, parts[1])
		if err != nil {
			response.HandleError(c, shared.NewAppError(shared.ErrUnauthorized, "invalid or expired token"))
			c.Abort()
			return
		}

		if claims.TokenType != "access" {
			response.HandleError(c, shared.NewAppError(shared.ErrUnauthorized, "not an access token"))
			c.Abort()
			return
		}

		c.Set(UserIDKey, claims.UserID)
		c.Next()
	}
}

func GetUserID(c *gin.Context) int64 {
	id, exists := c.Get(UserIDKey)
	if !exists {
		return 0
	}
	userID, ok := id.(int64)
	if !ok {
		return 0
	}
	return userID
}
```

- [ ] **Step 2: Create logging middleware**

```go
// api/middleware/logging.go
package middleware

import (
	"time"

	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog/log"
)

func RequestLogger() gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		c.Next()
		log.Info().
			Str("method", c.Request.Method).
			Str("path", c.Request.URL.Path).
			Int("status", c.Writer.Status()).
			Dur("latency", time.Since(start)).
			Msg("request")
	}
}
```

- [ ] **Step 3: Create CORS middleware**

```go
// api/middleware/cors.go
package middleware

import (
	"github.com/gin-gonic/gin"
)

func CORS() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization")
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}
		c.Next()
	}
}
```

- [ ] **Step 4: Create user handler**

```go
// api/user/handler.go
package user

import (
	"github.com/gin-gonic/gin"
	"github.com/go-playground/validator/v10"
	"github.com/user/neighbor-hub/api/middleware"
	"github.com/user/neighbor-hub/internal/shared"
	"github.com/user/neighbor-hub/internal/user/application"
	"github.com/user/neighbor-hub/pkg/response"
)

type Handler struct {
	svc      *application.UserService
	validate *validator.Validate
}

func NewHandler(svc *application.UserService) *Handler {
	return &Handler{svc: svc, validate: validator.New()}
}

type LoginRequest struct {
	Code string `json:"code" validate:"required"`
}

type UpdateProfileRequest struct {
	Nickname  string `json:"nickname" validate:"max=50"`
	AvatarURL string `json:"avatar_url" validate:"omitempty,url,max=500"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refresh_token" validate:"required"`
}

func (h *Handler) Login(c *gin.Context) {
	var req LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid request body"))
		return
	}
	if err := h.validate.Struct(req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, err.Error()))
		return
	}

	result, err := h.svc.LoginOrRegister(c.Request.Context(), req.Code)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, result)
}

func (h *Handler) Refresh(c *gin.Context) {
	var req RefreshRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid request body"))
		return
	}

	result, err := h.svc.RefreshToken(c.Request.Context(), req.RefreshToken)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, result)
}

func (h *Handler) GetProfile(c *gin.Context) {
	userID := middleware.GetUserID(c)
	user, err := h.svc.GetProfile(c.Request.Context(), userID)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, gin.H{
		"id":         user.ID,
		"nickname":   user.Nickname,
		"avatar_url": user.AvatarURL,
		"phone":      user.Phone,
	})
}

func (h *Handler) UpdateProfile(c *gin.Context) {
	userID := middleware.GetUserID(c)
	var req UpdateProfileRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid request body"))
		return
	}

	user, err := h.svc.UpdateProfile(c.Request.Context(), userID, req.Nickname, req.AvatarURL)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, gin.H{
		"id":         user.ID,
		"nickname":   user.Nickname,
		"avatar_url": user.AvatarURL,
	})
}
```

- [ ] **Step 5: Create router**

```go
// api/router.go
package api

import (
	"github.com/gin-gonic/gin"
	"github.com/user/neighbor-hub/api/middleware"
	userHandler "github.com/user/neighbor-hub/api/user"
)

type Handlers struct {
	User *userHandler.Handler
}

func SetupRouter(jwtSecret string, handlers Handlers) *gin.Engine {
	r := gin.New()
	r.Use(middleware.RequestLogger())
	r.Use(middleware.CORS())
	r.Use(gin.Recovery())

	// Health check
	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok"})
	})

	v1 := r.Group("/api/v1")

	// Public auth routes
	v1.POST("/auth/wechat-login", handlers.User.Login)
	v1.POST("/auth/refresh", handlers.User.Refresh)

	// Protected routes
	auth := v1.Group("")
	auth.Use(middleware.AuthRequired(jwtSecret))
	{
		auth.GET("/users/me", handlers.User.GetProfile)
		auth.PUT("/users/me", handlers.User.UpdateProfile)
	}

	return r
}
```

- [ ] **Step 6: Install validator dependency**

```bash
go get github.com/go-playground/validator/v10
```

- [ ] **Step 7: Verify compilation**

Run: `go build ./...`
Expected: No errors

- [ ] **Step 8: Commit**

```bash
git add api/
git commit -m "feat: add auth middleware, user handler, and router setup"
```

---

### Task 9: Wire Everything in main.go + Health Check

**Files:**
- Modify: `cmd/server/main.go`

- [ ] **Step 1: Wire all dependencies in main.go**

```go
// cmd/server/main.go
package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"

	"github.com/user/neighbor-hub/api"
	userHandler "github.com/user/neighbor-hub/api/user"
	"github.com/user/neighbor-hub/config"
	userApp "github.com/user/neighbor-hub/internal/user/application"
	userInfra "github.com/user/neighbor-hub/internal/user/infra"
	"github.com/user/neighbor-hub/pkg/cache"
	"github.com/user/neighbor-hub/pkg/database"
	"github.com/user/neighbor-hub/pkg/wechat"
)

func main() {
	// Logger
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnix
	log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stdout, TimeFormat: time.RFC3339})

	// Config
	cfgPath := "config/config.dev.yaml"
	if p := os.Getenv("CONFIG_PATH"); p != "" {
		cfgPath = p
	}
	cfg, err := config.Load(cfgPath)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to load config")
	}

	ctx := context.Background()

	// Database
	pool, err := database.NewPostgresPool(ctx, cfg.Database.URL)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect database")
	}
	defer pool.Close()

	// Redis
	rdb, err := cache.NewRedisClient(ctx, cfg.Redis.Addr, cfg.Redis.Password, cfg.Redis.DB)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to connect redis")
	}
	defer rdb.Close()

	// WeChat client
	wxClient := wechat.NewMiniAppClient(cfg.WeChat.AppID, cfg.WeChat.AppSecret)
	_ = wxClient // Will be wrapped in an adapter implementing WeChatClient interface

	// Repos
	userRepo := userInfra.NewPostgresUserRepo(pool)

	// Application services
	// Note: wxClient needs an adapter to implement application.WeChatClient
	// For now we pass nil; see Task 10 for the adapter
	userSvc := userApp.NewUserService(userRepo, nil, cfg.JWT.Secret, cfg.JWT.AccessTokenTTL, cfg.JWT.RefreshTokenTTL) // FIXME: nil WeChat client — login will panic until Task 10

	// Handlers
	handlers := api.Handlers{
		User: userHandler.NewHandler(userSvc),
	}

	// Router
	router := api.SetupRouter(cfg.JWT.Secret, handlers)

	// Server
	srv := &http.Server{
		Addr:         cfg.Server.Port,
		Handler:      router,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	go func() {
		log.Info().Str("addr", cfg.Server.Port).Msg("server starting")
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("server error")
		}
	}()

	// Graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Info().Msg("shutting down...")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := srv.Shutdown(shutdownCtx); err != nil {
		log.Error().Err(err).Msg("server shutdown error")
	}
	log.Info().Msg("server stopped")
}
```

- [ ] **Step 2: Verify compilation**

Run: `go build ./cmd/server/`
Expected: No errors

- [ ] **Step 3: Start Docker Compose + run the app, test health endpoint**

```bash
make docker-up
go run ./cmd/server/ &
sleep 2
curl http://localhost:8080/health
kill %1
```
Expected: `{"status":"ok"}`

- [ ] **Step 4: Commit**

```bash
git add cmd/server/main.go
git commit -m "feat: wire dependencies in main.go with graceful shutdown"
```

---

### Task 10: WeChat Client Adapter + End-to-End Smoke Test

**Files:**
- Create: `pkg/wechat/adapter.go` (wraps MiniAppClient to implement application.WeChatClient)
- Modify: `cmd/server/main.go` (wire the adapter)

- [ ] **Step 1: Create adapter wrapping MiniAppClient to application.WeChatClient**

```go
// pkg/wechat/adapter.go
package wechat

import "context"

// WeChatAdapter adapts MiniAppClient to the application.WeChatClient interface.
type WeChatAdapter struct {
	client *MiniAppClient
}

func NewWeChatAdapter(client *MiniAppClient) *WeChatAdapter {
	return &WeChatAdapter{client: client}
}

func (a *WeChatAdapter) Code2Session(ctx context.Context, code string) (openID, unionID string, err error) {
	resp, err := a.client.Code2Session(ctx, code)
	if err != nil {
		return "", "", err
	}
	return resp.OpenID, resp.UnionID, nil
}
```

- [ ] **Step 2: Update main.go to wire the adapter**

In `cmd/server/main.go`, replace the `userSvc` line:

```go
wxAdapter := wechat.NewWeChatAdapter(wxClient)
userSvc := userApp.NewUserService(userRepo, wxAdapter, cfg.JWT.Secret, cfg.JWT.AccessTokenTTL, cfg.JWT.RefreshTokenTTL)
```

- [ ] **Step 3: Run full test suite**

Run: `go test ./... -v -race -count=1`
Expected: All tests pass

- [ ] **Step 4: Run the full application and test auth error handling**

```bash
make docker-up
go run ./cmd/server/ &
sleep 2
# Should get 401 unauthorized
curl -s http://localhost:8080/api/v1/users/me | jq .
# Should get error for missing code
curl -s -X POST http://localhost:8080/api/v1/auth/wechat-login -H "Content-Type: application/json" -d '{}' | jq .
kill %1
```
Expected: Proper error responses with error codes

- [ ] **Step 5: Commit**

```bash
git add pkg/wechat/adapter.go cmd/server/main.go
git commit -m "feat: wire WeChat adapter and complete Phase 1 scaffolding"
```

---

## Verification Checklist

After completing all tasks:

1. `make docker-up` starts PostgreSQL+PostGIS and Redis
2. `make migrate-up` applies the schema
3. `go run ./cmd/server/` starts the server on :8080
4. `GET /health` returns `{"status":"ok"}`
5. `POST /api/v1/auth/wechat-login` with `{"code":"xxx"}` attempts WeChat login
6. `GET /api/v1/users/me` without token returns 401
7. `GET /api/v1/users/me` with valid token returns user profile
8. `go test ./... -race` passes all tests
9. `docker compose -f deploy/docker-compose.yml exec postgres psql ... -c "SELECT PostGIS_Version();"` confirms PostGIS is available

---

## Next Plans

- **Plan 2:** Community module (geo-fencing with PostGIS, join/leave, admin)
- **Plan 3:** Post module (CRUD, comments, likes, feed)
- **Plan 4:** Vote module
- **Plan 5:** GroupBuy module (state machine, WeChat Pay)
- **Plan 6:** Frontend (WeChat Mini Program)
