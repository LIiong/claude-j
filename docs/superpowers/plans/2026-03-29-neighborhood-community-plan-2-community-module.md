# Plan 2: Community Module

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the Community bounded context with geo-fenced communities (PostGIS), member management (join/leave), and HTTP endpoints following the established DDD modular monolith patterns from Phase 1.

**Architecture:** New `internal/community/` module with `domain/`, `application/`, `infra/` layers. PostGIS polygon boundaries stored in infrastructure only; domain uses `GeoFenceChecker` interface. `PostgresCommunityRepo` implements both `CommunityRepository` and `GeoFenceChecker`.

**Tech Stack:** Go 1.22+, Gin, pgx/v5 + PostGIS (ST_Contains, ST_GeomFromText, ST_Centroid), testify

**Spec:** `docs/superpowers/specs/2026-03-28-neighborhood-community-platform-design.md`

---

## File Structure

```
neighbor-hub/
├── deploy/migrations/
│   ├── 000002_community.up.sql          # t_community + t_community_member tables
│   └── 000002_community.down.sql        # Drop both tables
├── internal/community/
│   ├── domain/
│   │   ├── community.go                 # Community entity, types, status, business rules
│   │   ├── member.go                    # CommunityMember entity, MemberRole
│   │   ├── repository.go               # CommunityRepository + GeoFenceChecker interfaces
│   │   └── community_test.go           # Pure domain unit tests
│   ├── application/
│   │   ├── service.go                   # CommunityService: create, join, leave, query
│   │   └── service_test.go             # Mock-based application tests
│   └── infra/
│       └── postgres_repo.go            # PostGIS repo implementing both interfaces
├── api/community/
│   └── handler.go                       # HTTP handler + request/response types
├── api/router.go                        # (modify) Add community routes
└── cmd/server/main.go                   # (modify) Wire community module
```

---

### Task 1: Database Migration (000002)

**Files:**
- Create: `deploy/migrations/000002_community.up.sql`
- Create: `deploy/migrations/000002_community.down.sql`

- [ ] **Step 1: Create up migration**

```sql
-- deploy/migrations/000002_community.up.sql

CREATE TABLE t_community (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    boundary GEOMETRY(POLYGON, 4326) NOT NULL,
    center GEOMETRY(POINT, 4326),
    max_members INT NOT NULL DEFAULT 500,
    avatar_url VARCHAR(500),
    creator_id BIGINT NOT NULL REFERENCES t_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_community_boundary ON t_community USING GIST(boundary);
CREATE INDEX idx_community_center ON t_community USING GIST(center);

CREATE TABLE t_community_member (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES t_community(id),
    user_id BIGINT NOT NULL REFERENCES t_user(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, user_id)
);

CREATE INDEX idx_member_user ON t_community_member(user_id);
CREATE INDEX idx_member_community ON t_community_member(community_id);
```

- [ ] **Step 2: Create down migration**

```sql
-- deploy/migrations/000002_community.down.sql
DROP TABLE IF EXISTS t_community_member;
DROP TABLE IF EXISTS t_community;
```

- [ ] **Step 3: Commit**

```bash
git add deploy/migrations/000002_community.up.sql deploy/migrations/000002_community.down.sql
git commit -m "feat: add community database migration with PostGIS geo-fence support"
```

---

### Task 2: Community Domain Model + Tests

**Files:**
- Create: `internal/community/domain/community.go`
- Create: `internal/community/domain/member.go`
- Create: `internal/community/domain/repository.go`
- Test: `internal/community/domain/community_test.go`

- [ ] **Step 1: Write domain tests first**

```go
// internal/community/domain/community_test.go
package domain_test

import (
	"testing"

	"github.com/user/neighbor-hub/internal/community/domain"
	"github.com/stretchr/testify/assert"
)

func TestNewCommunity(t *testing.T) {
	c := domain.NewCommunity("翠湖花园", "业主交流", domain.CommunityTypeUserCreated, 42)
	assert.Equal(t, "翠湖花园", c.Name)
	assert.Equal(t, "业主交流", c.Description)
	assert.Equal(t, domain.CommunityTypeUserCreated, c.Type)
	assert.Equal(t, domain.CommunityStatusActive, c.Status)
	assert.Equal(t, 500, c.MaxMembers)
	assert.Equal(t, int64(42), c.CreatorID)
	assert.False(t, c.CreatedAt.IsZero())
}

func TestMaxMembersForType(t *testing.T) {
	assert.Equal(t, 10000, domain.MaxMembersForType(domain.CommunityTypeOfficial))
	assert.Equal(t, 500, domain.MaxMembersForType(domain.CommunityTypeUserCreated))
	assert.Equal(t, 2000, domain.MaxMembersForType(domain.CommunityTypeMerchant))
	assert.Equal(t, 500, domain.MaxMembersForType("UNKNOWN"))
}

func TestCommunity_IsActive(t *testing.T) {
	c := domain.NewCommunity("test", "", domain.CommunityTypeUserCreated, 1)
	assert.True(t, c.IsActive())
	c.Status = domain.CommunityStatusSuspended
	assert.False(t, c.IsActive())
}

func TestCommunity_CanAcceptMembers(t *testing.T) {
	c := domain.NewCommunity("test", "", domain.CommunityTypeUserCreated, 1)
	assert.True(t, c.CanAcceptMembers(0))
	assert.True(t, c.CanAcceptMembers(499))
	assert.False(t, c.CanAcceptMembers(500))

	c.Status = domain.CommunityStatusSuspended
	assert.False(t, c.CanAcceptMembers(0))
}

func TestNewOwnerMember(t *testing.T) {
	m := domain.NewOwnerMember(10, 42)
	assert.Equal(t, int64(10), m.CommunityID)
	assert.Equal(t, int64(42), m.UserID)
	assert.Equal(t, domain.MemberRoleOwner, m.Role)
	assert.False(t, m.JoinedAt.IsZero())
}

func TestNewMember(t *testing.T) {
	m := domain.NewMember(10, 42, domain.MemberRoleMember)
	assert.Equal(t, domain.MemberRoleMember, m.Role)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `go test ./internal/community/domain/ -v`
Expected: FAIL — package not found

- [ ] **Step 3: Implement community entity**

```go
// internal/community/domain/community.go
package domain

import "time"

type CommunityType string

const (
	CommunityTypeOfficial    CommunityType = "OFFICIAL"
	CommunityTypeUserCreated CommunityType = "USER_CREATED"
	CommunityTypeMerchant    CommunityType = "MERCHANT"
)

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
	Type        CommunityType
	Status      CommunityStatus
	MaxMembers  int
	AvatarURL   string
	CreatorID   int64
	CreatedAt   time.Time
	UpdatedAt   time.Time
}

func NewCommunity(name, description string, cType CommunityType, creatorID int64) *Community {
	now := time.Now()
	return &Community{
		Name:        name,
		Description: description,
		Type:        cType,
		Status:      CommunityStatusActive,
		MaxMembers:  MaxMembersForType(cType),
		CreatorID:   creatorID,
		CreatedAt:   now,
		UpdatedAt:   now,
	}
}

func MaxMembersForType(cType CommunityType) int {
	switch cType {
	case CommunityTypeOfficial:
		return 10000
	case CommunityTypeUserCreated:
		return 500
	case CommunityTypeMerchant:
		return 2000
	default:
		return 500
	}
}

func (c *Community) IsActive() bool {
	return c.Status == CommunityStatusActive
}

func (c *Community) CanAcceptMembers(currentCount int) bool {
	return c.IsActive() && currentCount < c.MaxMembers
}
```

- [ ] **Step 4: Implement member entity**

```go
// internal/community/domain/member.go
package domain

import "time"

type MemberRole string

const (
	MemberRoleOwner  MemberRole = "OWNER"
	MemberRoleAdmin  MemberRole = "ADMIN"
	MemberRoleMember MemberRole = "MEMBER"
)

type CommunityMember struct {
	ID          int64
	CommunityID int64
	UserID      int64
	Role        MemberRole
	JoinedAt    time.Time
}

func NewMember(communityID, userID int64, role MemberRole) *CommunityMember {
	return &CommunityMember{
		CommunityID: communityID,
		UserID:      userID,
		Role:        role,
		JoinedAt:    time.Now(),
	}
}

func NewOwnerMember(communityID, userID int64) *CommunityMember {
	return NewMember(communityID, userID, MemberRoleOwner)
}
```

- [ ] **Step 5: Define repository interfaces**

```go
// internal/community/domain/repository.go
package domain

import (
	"context"

	"github.com/user/neighbor-hub/internal/shared"
)

type CommunityRepository interface {
	Save(ctx context.Context, community *Community, boundaryWKT string) (*Community, error)
	FindByID(ctx context.Context, id int64) (*Community, error)
	FindNearby(ctx context.Context, lng, lat float64) ([]*Community, error)
	Update(ctx context.Context, community *Community) error
	CountMembers(ctx context.Context, communityID int64) (int, error)
	AddMember(ctx context.Context, member *CommunityMember) error
	RemoveMember(ctx context.Context, communityID, userID int64) error
	FindMember(ctx context.Context, communityID, userID int64) (*CommunityMember, error)
	ListMembers(ctx context.Context, communityID int64, cursor shared.Cursor, limit int) ([]*CommunityMember, error)
	ListByUserID(ctx context.Context, userID int64) ([]*Community, error)
	UpdateMemberRole(ctx context.Context, communityID, userID int64, role MemberRole) error
}

type GeoFenceChecker interface {
	Contains(ctx context.Context, communityID int64, lng, lat float64) (bool, error)
}
```

- [ ] **Step 6: Run tests**

Run: `go test ./internal/community/domain/ -v`
Expected: All 6 tests PASS

- [ ] **Step 7: Commit**

```bash
git add internal/community/domain/
git commit -m "feat: add community domain model with entity, member, and repository interfaces"
```

---

### Task 3: Community Application Service + Tests

**Files:**
- Create: `internal/community/application/service.go`
- Test: `internal/community/application/service_test.go`

- [ ] **Step 1: Write application service tests**

```go
// internal/community/application/service_test.go
package application_test

import (
	"context"
	"testing"

	"github.com/user/neighbor-hub/internal/community/application"
	"github.com/user/neighbor-hub/internal/community/domain"
	"github.com/user/neighbor-hub/internal/shared"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// Mock repository
type mockCommunityRepo struct {
	communities map[int64]*domain.Community
	members     map[int64][]*domain.CommunityMember
	nextID      int64
	nextMemberID int64
}

func newMockRepo() *mockCommunityRepo {
	return &mockCommunityRepo{
		communities: make(map[int64]*domain.Community),
		members:     make(map[int64][]*domain.CommunityMember),
		nextID:      1,
		nextMemberID: 1,
	}
}

func (m *mockCommunityRepo) Save(ctx context.Context, c *domain.Community, boundaryWKT string) (*domain.Community, error) {
	c.ID = m.nextID
	m.nextID++
	m.communities[c.ID] = c
	return c, nil
}

func (m *mockCommunityRepo) FindByID(ctx context.Context, id int64) (*domain.Community, error) {
	return m.communities[id], nil
}

func (m *mockCommunityRepo) FindNearby(ctx context.Context, lng, lat float64) ([]*domain.Community, error) {
	var result []*domain.Community
	for _, c := range m.communities {
		result = append(result, c)
	}
	return result, nil
}

func (m *mockCommunityRepo) Update(ctx context.Context, c *domain.Community) error {
	m.communities[c.ID] = c
	return nil
}

func (m *mockCommunityRepo) CountMembers(ctx context.Context, communityID int64) (int, error) {
	return len(m.members[communityID]), nil
}

func (m *mockCommunityRepo) AddMember(ctx context.Context, member *domain.CommunityMember) error {
	member.ID = m.nextMemberID
	m.nextMemberID++
	m.members[member.CommunityID] = append(m.members[member.CommunityID], member)
	return nil
}

func (m *mockCommunityRepo) RemoveMember(ctx context.Context, communityID, userID int64) error {
	members := m.members[communityID]
	for i, mem := range members {
		if mem.UserID == userID {
			m.members[communityID] = append(members[:i], members[i+1:]...)
			return nil
		}
	}
	return nil
}

func (m *mockCommunityRepo) FindMember(ctx context.Context, communityID, userID int64) (*domain.CommunityMember, error) {
	for _, mem := range m.members[communityID] {
		if mem.UserID == userID {
			return mem, nil
		}
	}
	return nil, nil
}

func (m *mockCommunityRepo) ListMembers(ctx context.Context, communityID int64, cursor shared.Cursor, limit int) ([]*domain.CommunityMember, error) {
	return m.members[communityID], nil
}

func (m *mockCommunityRepo) ListByUserID(ctx context.Context, userID int64) ([]*domain.Community, error) {
	var result []*domain.Community
	for cID, members := range m.members {
		for _, mem := range members {
			if mem.UserID == userID {
				if c, ok := m.communities[cID]; ok {
					result = append(result, c)
				}
			}
		}
	}
	return result, nil
}

func (m *mockCommunityRepo) UpdateMemberRole(ctx context.Context, communityID, userID int64, role domain.MemberRole) error {
	for _, mem := range m.members[communityID] {
		if mem.UserID == userID {
			mem.Role = role
			return nil
		}
	}
	return nil
}

// Mock geo-fence checker
type mockGeoChecker struct {
	result bool
}

func (m *mockGeoChecker) Contains(ctx context.Context, communityID int64, lng, lat float64) (bool, error) {
	return m.result, nil
}

func TestCreateCommunity_Success(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	c, err := svc.CreateCommunity(context.Background(), 42, "Test Community", "desc", domain.CommunityTypeUserCreated, boundary, "")
	require.NoError(t, err)
	assert.Equal(t, int64(1), c.ID)
	assert.Equal(t, "Test Community", c.Name)
	assert.Equal(t, int64(42), c.CreatorID)

	// Verify creator is added as OWNER
	member, _ := repo.FindMember(context.Background(), c.ID, 42)
	require.NotNil(t, member)
	assert.Equal(t, domain.MemberRoleOwner, member.Role)
}

func TestJoinCommunity_Success(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	c, _ := svc.CreateCommunity(context.Background(), 1, "Test", "", domain.CommunityTypeUserCreated, boundary, "")

	err := svc.JoinCommunity(context.Background(), c.ID, 99, 113.905, 22.505)
	require.NoError(t, err)

	member, _ := repo.FindMember(context.Background(), c.ID, 99)
	require.NotNil(t, member)
	assert.Equal(t, domain.MemberRoleMember, member.Role)
}

func TestJoinCommunity_NotInGeoFence(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: false}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	c, _ := svc.CreateCommunity(context.Background(), 1, "Test", "", domain.CommunityTypeUserCreated, boundary, "")

	err := svc.JoinCommunity(context.Background(), c.ID, 99, 0, 0)
	require.Error(t, err)
	ae, ok := shared.GetAppError(err)
	require.True(t, ok)
	assert.Equal(t, shared.ErrNotInGeoFence, ae.Code)
}

func TestJoinCommunity_AlreadyJoined(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	c, _ := svc.CreateCommunity(context.Background(), 1, "Test", "", domain.CommunityTypeUserCreated, boundary, "")
	_ = svc.JoinCommunity(context.Background(), c.ID, 99, 113.905, 22.505)

	err := svc.JoinCommunity(context.Background(), c.ID, 99, 113.905, 22.505)
	require.Error(t, err)
	ae, ok := shared.GetAppError(err)
	require.True(t, ok)
	assert.Equal(t, shared.ErrAlreadyJoined, ae.Code)
}

func TestJoinCommunity_Full(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	c, _ := svc.CreateCommunity(context.Background(), 1, "Test", "", domain.CommunityTypeUserCreated, boundary, "")
	// Override max to 2 for testing
	c.MaxMembers = 2
	// Creator is already member (count=1), add one more
	_ = svc.JoinCommunity(context.Background(), c.ID, 99, 113.905, 22.505)

	err := svc.JoinCommunity(context.Background(), c.ID, 100, 113.905, 22.505)
	require.Error(t, err)
	ae, ok := shared.GetAppError(err)
	require.True(t, ok)
	assert.Equal(t, shared.ErrCommunityFull, ae.Code)
}

func TestLeaveCommunity_Success(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	c, _ := svc.CreateCommunity(context.Background(), 1, "Test", "", domain.CommunityTypeUserCreated, boundary, "")
	_ = svc.JoinCommunity(context.Background(), c.ID, 99, 113.905, 22.505)

	err := svc.LeaveCommunity(context.Background(), c.ID, 99)
	require.NoError(t, err)

	member, _ := repo.FindMember(context.Background(), c.ID, 99)
	assert.Nil(t, member)
}

func TestLeaveCommunity_OwnerCannotLeave(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	c, _ := svc.CreateCommunity(context.Background(), 42, "Test", "", domain.CommunityTypeUserCreated, boundary, "")

	err := svc.LeaveCommunity(context.Background(), c.ID, 42)
	require.Error(t, err)
	ae, ok := shared.GetAppError(err)
	require.True(t, ok)
	assert.Equal(t, shared.ErrNoPermission, ae.Code)
}

func TestGetCommunity_NotFound(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	_, err := svc.GetCommunity(context.Background(), 999)
	require.Error(t, err)
	ae, ok := shared.GetAppError(err)
	require.True(t, ok)
	assert.Equal(t, shared.ErrCommunityNotFound, ae.Code)
}

func TestMyCommunities(t *testing.T) {
	repo := newMockRepo()
	geo := &mockGeoChecker{result: true}
	svc := application.NewCommunityService(repo, geo)

	boundary := [][]float64{{113.9, 22.5}, {113.91, 22.5}, {113.91, 22.51}, {113.9, 22.51}, {113.9, 22.5}}
	_, _ = svc.CreateCommunity(context.Background(), 42, "Community A", "", domain.CommunityTypeUserCreated, boundary, "")
	_, _ = svc.CreateCommunity(context.Background(), 42, "Community B", "", domain.CommunityTypeUserCreated, boundary, "")

	communities, err := svc.MyCommunities(context.Background(), 42)
	require.NoError(t, err)
	assert.Len(t, communities, 2)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `go test ./internal/community/application/ -v`
Expected: FAIL

- [ ] **Step 3: Implement application service**

```go
// internal/community/application/service.go
package application

import (
	"context"
	"fmt"
	"strings"

	"github.com/user/neighbor-hub/internal/community/domain"
	"github.com/user/neighbor-hub/internal/shared"
)

type CommunityService struct {
	repo     domain.CommunityRepository
	geoCheck domain.GeoFenceChecker
}

func NewCommunityService(repo domain.CommunityRepository, geoCheck domain.GeoFenceChecker) *CommunityService {
	return &CommunityService{repo: repo, geoCheck: geoCheck}
}

func (s *CommunityService) CreateCommunity(ctx context.Context, userID int64, name, description string, cType domain.CommunityType, boundary [][]float64, avatarURL string) (*domain.Community, error) {
	community := domain.NewCommunity(name, description, cType, userID)
	if avatarURL != "" {
		community.AvatarURL = avatarURL
	}

	wkt := coordsToWKT(boundary)
	community, err := s.repo.Save(ctx, community, wkt)
	if err != nil {
		return nil, fmt.Errorf("save community: %w", err)
	}

	owner := domain.NewOwnerMember(community.ID, userID)
	if err := s.repo.AddMember(ctx, owner); err != nil {
		return nil, fmt.Errorf("add owner member: %w", err)
	}

	return community, nil
}

func (s *CommunityService) GetCommunity(ctx context.Context, id int64) (*domain.Community, error) {
	community, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("find community: %w", err)
	}
	if community == nil {
		return nil, shared.NewAppError(shared.ErrCommunityNotFound, "community not found")
	}
	return community, nil
}

func (s *CommunityService) FindNearby(ctx context.Context, lng, lat float64) ([]*domain.Community, error) {
	return s.repo.FindNearby(ctx, lng, lat)
}

func (s *CommunityService) JoinCommunity(ctx context.Context, communityID, userID int64, lng, lat float64) error {
	community, err := s.repo.FindByID(ctx, communityID)
	if err != nil {
		return fmt.Errorf("find community: %w", err)
	}
	if community == nil || !community.IsActive() {
		return shared.NewAppError(shared.ErrCommunityNotFound, "community not found")
	}

	existing, err := s.repo.FindMember(ctx, communityID, userID)
	if err != nil {
		return fmt.Errorf("find member: %w", err)
	}
	if existing != nil {
		return shared.NewAppError(shared.ErrAlreadyJoined, "already joined this community")
	}

	inFence, err := s.geoCheck.Contains(ctx, communityID, lng, lat)
	if err != nil {
		return fmt.Errorf("geo-fence check: %w", err)
	}
	if !inFence {
		return shared.NewAppError(shared.ErrNotInGeoFence, "not within community geo-fence")
	}

	count, err := s.repo.CountMembers(ctx, communityID)
	if err != nil {
		return fmt.Errorf("count members: %w", err)
	}
	if !community.CanAcceptMembers(count) {
		return shared.NewAppError(shared.ErrCommunityFull, "community is full")
	}

	member := domain.NewMember(communityID, userID, domain.MemberRoleMember)
	if err := s.repo.AddMember(ctx, member); err != nil {
		return fmt.Errorf("add member: %w", err)
	}
	return nil
}

func (s *CommunityService) LeaveCommunity(ctx context.Context, communityID, userID int64) error {
	member, err := s.repo.FindMember(ctx, communityID, userID)
	if err != nil {
		return fmt.Errorf("find member: %w", err)
	}
	if member == nil {
		return shared.NewAppError(shared.ErrCommunityNotFound, "not a member of this community")
	}
	if member.Role == domain.MemberRoleOwner {
		return shared.NewAppError(shared.ErrNoPermission, "owner cannot leave community")
	}
	return s.repo.RemoveMember(ctx, communityID, userID)
}

func (s *CommunityService) ListMembers(ctx context.Context, communityID int64, cursor shared.Cursor, limit int) ([]*domain.CommunityMember, error) {
	limit = shared.ClampPageSize(limit)
	return s.repo.ListMembers(ctx, communityID, cursor, limit)
}

func (s *CommunityService) MyCommunities(ctx context.Context, userID int64) ([]*domain.Community, error) {
	return s.repo.ListByUserID(ctx, userID)
}

func coordsToWKT(coords [][]float64) string {
	parts := make([]string, len(coords))
	for i, c := range coords {
		parts[i] = fmt.Sprintf("%f %f", c[0], c[1])
	}
	return fmt.Sprintf("POLYGON((%s))", strings.Join(parts, ", "))
}
```

- [ ] **Step 4: Run tests**

Run: `go test ./internal/community/application/ -v`
Expected: All 9 tests PASS

- [ ] **Step 5: Commit**

```bash
git add internal/community/application/
git commit -m "feat: add community application service with create, join, leave, and query"
```

---

### Task 4: Community PostgreSQL Repository

**Files:**
- Create: `internal/community/infra/postgres_repo.go`

- [ ] **Step 1: Implement PostgreSQL repository**

```go
// internal/community/infra/postgres_repo.go
package infra

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/user/neighbor-hub/internal/community/domain"
	"github.com/user/neighbor-hub/internal/shared"
)

type PostgresCommunityRepo struct {
	pool *pgxpool.Pool
}

func NewPostgresCommunityRepo(pool *pgxpool.Pool) *PostgresCommunityRepo {
	return &PostgresCommunityRepo{pool: pool}
}

func (r *PostgresCommunityRepo) Save(ctx context.Context, c *domain.Community, boundaryWKT string) (*domain.Community, error) {
	query := `INSERT INTO t_community (name, description, type, status, boundary, center, max_members, avatar_url, creator_id, created_at, updated_at)
		VALUES ($1, $2, $3, $4, ST_GeomFromText($5, 4326), ST_Centroid(ST_GeomFromText($5, 4326)), $6, $7, $8, $9, $10)
		RETURNING id`
	err := r.pool.QueryRow(ctx, query,
		c.Name, c.Description, string(c.Type), string(c.Status),
		boundaryWKT, c.MaxMembers, c.AvatarURL, c.CreatorID,
		c.CreatedAt, c.UpdatedAt,
	).Scan(&c.ID)
	if err != nil {
		return nil, fmt.Errorf("insert community: %w", err)
	}
	return c, nil
}

func (r *PostgresCommunityRepo) FindByID(ctx context.Context, id int64) (*domain.Community, error) {
	query := `SELECT id, name, description, type, status, max_members, avatar_url, creator_id, created_at, updated_at
		FROM t_community WHERE id = $1`
	return r.scanCommunity(ctx, query, id)
}

func (r *PostgresCommunityRepo) FindNearby(ctx context.Context, lng, lat float64) ([]*domain.Community, error) {
	query := `SELECT id, name, description, type, status, max_members, avatar_url, creator_id, created_at, updated_at
		FROM t_community
		WHERE status = 'ACTIVE'
		AND ST_Contains(boundary, ST_SetSRID(ST_MakePoint($1, $2), 4326))
		ORDER BY created_at DESC`
	rows, err := r.pool.Query(ctx, query, lng, lat)
	if err != nil {
		return nil, fmt.Errorf("query nearby communities: %w", err)
	}
	defer rows.Close()

	var communities []*domain.Community
	for rows.Next() {
		c, err := r.scanCommunityRow(rows)
		if err != nil {
			return nil, err
		}
		communities = append(communities, c)
	}
	return communities, rows.Err()
}

func (r *PostgresCommunityRepo) Update(ctx context.Context, c *domain.Community) error {
	query := `UPDATE t_community SET name=$1, description=$2, status=$3, avatar_url=$4, updated_at=$5 WHERE id=$6`
	_, err := r.pool.Exec(ctx, query, c.Name, c.Description, string(c.Status), c.AvatarURL, c.UpdatedAt, c.ID)
	return err
}

func (r *PostgresCommunityRepo) CountMembers(ctx context.Context, communityID int64) (int, error) {
	var count int
	err := r.pool.QueryRow(ctx, `SELECT COUNT(*) FROM t_community_member WHERE community_id = $1`, communityID).Scan(&count)
	return count, err
}

func (r *PostgresCommunityRepo) AddMember(ctx context.Context, m *domain.CommunityMember) error {
	query := `INSERT INTO t_community_member (community_id, user_id, role, joined_at) VALUES ($1, $2, $3, $4) RETURNING id`
	return r.pool.QueryRow(ctx, query, m.CommunityID, m.UserID, string(m.Role), m.JoinedAt).Scan(&m.ID)
}

func (r *PostgresCommunityRepo) RemoveMember(ctx context.Context, communityID, userID int64) error {
	_, err := r.pool.Exec(ctx, `DELETE FROM t_community_member WHERE community_id = $1 AND user_id = $2`, communityID, userID)
	return err
}

func (r *PostgresCommunityRepo) FindMember(ctx context.Context, communityID, userID int64) (*domain.CommunityMember, error) {
	query := `SELECT id, community_id, user_id, role, joined_at FROM t_community_member WHERE community_id = $1 AND user_id = $2`
	m := &domain.CommunityMember{}
	var role string
	err := r.pool.QueryRow(ctx, query, communityID, userID).Scan(&m.ID, &m.CommunityID, &m.UserID, &role, &m.JoinedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("scan member: %w", err)
	}
	m.Role = domain.MemberRole(role)
	return m, nil
}

func (r *PostgresCommunityRepo) ListMembers(ctx context.Context, communityID int64, cursor shared.Cursor, limit int) ([]*domain.CommunityMember, error) {
	var query string
	var args []interface{}
	if cursor.IsZero() {
		query = `SELECT id, community_id, user_id, role, joined_at FROM t_community_member
			WHERE community_id = $1 ORDER BY joined_at DESC, id DESC LIMIT $2`
		args = []interface{}{communityID, limit}
	} else {
		query = `SELECT id, community_id, user_id, role, joined_at FROM t_community_member
			WHERE community_id = $1 AND (joined_at, id) < ($3, $4)
			ORDER BY joined_at DESC, id DESC LIMIT $2`
		args = []interface{}{communityID, limit, cursor.CreatedAt, cursor.ID}
	}
	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, fmt.Errorf("query members: %w", err)
	}
	defer rows.Close()

	var members []*domain.CommunityMember
	for rows.Next() {
		m := &domain.CommunityMember{}
		var role string
		if err := rows.Scan(&m.ID, &m.CommunityID, &m.UserID, &role, &m.JoinedAt); err != nil {
			return nil, fmt.Errorf("scan member row: %w", err)
		}
		m.Role = domain.MemberRole(role)
		members = append(members, m)
	}
	return members, rows.Err()
}

func (r *PostgresCommunityRepo) ListByUserID(ctx context.Context, userID int64) ([]*domain.Community, error) {
	query := `SELECT c.id, c.name, c.description, c.type, c.status, c.max_members, c.avatar_url, c.creator_id, c.created_at, c.updated_at
		FROM t_community c
		INNER JOIN t_community_member m ON c.id = m.community_id
		WHERE m.user_id = $1
		ORDER BY m.joined_at DESC`
	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("query user communities: %w", err)
	}
	defer rows.Close()

	var communities []*domain.Community
	for rows.Next() {
		c, err := r.scanCommunityRow(rows)
		if err != nil {
			return nil, err
		}
		communities = append(communities, c)
	}
	return communities, rows.Err()
}

func (r *PostgresCommunityRepo) UpdateMemberRole(ctx context.Context, communityID, userID int64, role domain.MemberRole) error {
	_, err := r.pool.Exec(ctx, `UPDATE t_community_member SET role = $3 WHERE community_id = $1 AND user_id = $2`, communityID, userID, string(role))
	return err
}

// GeoFenceChecker implementation
func (r *PostgresCommunityRepo) Contains(ctx context.Context, communityID int64, lng, lat float64) (bool, error) {
	var contains bool
	err := r.pool.QueryRow(ctx,
		`SELECT ST_Contains(boundary, ST_SetSRID(ST_MakePoint($2, $3), 4326)) FROM t_community WHERE id = $1`,
		communityID, lng, lat,
	).Scan(&contains)
	if errors.Is(err, pgx.ErrNoRows) {
		return false, nil
	}
	if err != nil {
		return false, fmt.Errorf("geo-fence check: %w", err)
	}
	return contains, nil
}

// Private helpers

func (r *PostgresCommunityRepo) scanCommunity(ctx context.Context, query string, args ...interface{}) (*domain.Community, error) {
	c := &domain.Community{}
	var cType, status string
	err := r.pool.QueryRow(ctx, query, args...).Scan(
		&c.ID, &c.Name, &c.Description, &cType, &status,
		&c.MaxMembers, &c.AvatarURL, &c.CreatorID, &c.CreatedAt, &c.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("scan community: %w", err)
	}
	c.Type = domain.CommunityType(cType)
	c.Status = domain.CommunityStatus(status)
	return c, nil
}

func (r *PostgresCommunityRepo) scanCommunityRow(rows pgx.Rows) (*domain.Community, error) {
	c := &domain.Community{}
	var cType, status string
	if err := rows.Scan(
		&c.ID, &c.Name, &c.Description, &cType, &status,
		&c.MaxMembers, &c.AvatarURL, &c.CreatorID, &c.CreatedAt, &c.UpdatedAt,
	); err != nil {
		return nil, fmt.Errorf("scan community row: %w", err)
	}
	c.Type = domain.CommunityType(cType)
	c.Status = domain.CommunityStatus(status)
	return c, nil
}
```

- [ ] **Step 2: Verify compilation**

Run: `go build ./...`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add internal/community/infra/
git commit -m "feat: add community PostgreSQL repository with PostGIS geo-fence queries"
```

---

### Task 5: Community HTTP Handler

**Files:**
- Create: `api/community/handler.go`

- [ ] **Step 1: Implement handler**

```go
// api/community/handler.go
package community

import (
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-playground/validator/v10"
	"github.com/user/neighbor-hub/api/middleware"
	"github.com/user/neighbor-hub/internal/community/application"
	"github.com/user/neighbor-hub/internal/community/domain"
	"github.com/user/neighbor-hub/internal/shared"
	"github.com/user/neighbor-hub/pkg/response"
)

type Handler struct {
	svc      *application.CommunityService
	validate *validator.Validate
}

func NewHandler(svc *application.CommunityService) *Handler {
	return &Handler{svc: svc, validate: validator.New()}
}

type CreateCommunityRequest struct {
	Name        string      `json:"name" validate:"required,max=100"`
	Description string      `json:"description" validate:"max=1000"`
	Type        string      `json:"type" validate:"required,oneof=OFFICIAL USER_CREATED MERCHANT"`
	Boundary    [][]float64 `json:"boundary" validate:"required,min=4"`
	AvatarURL   string      `json:"avatar_url" validate:"omitempty,url,max=500"`
}

type JoinCommunityRequest struct {
	Lng float64 `json:"lng" validate:"required"`
	Lat float64 `json:"lat" validate:"required"`
}

func (h *Handler) CreateCommunity(c *gin.Context) {
	userID := middleware.GetUserID(c)
	var req CreateCommunityRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid request body"))
		return
	}
	if err := h.validate.Struct(req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, err.Error()))
		return
	}

	community, err := h.svc.CreateCommunity(c.Request.Context(), userID, req.Name, req.Description, domain.CommunityType(req.Type), req.Boundary, req.AvatarURL)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, toCommunityResponse(community))
}

func (h *Handler) GetCommunity(c *gin.Context) {
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid community id"))
		return
	}
	community, err := h.svc.GetCommunity(c.Request.Context(), id)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, toCommunityResponse(community))
}

func (h *Handler) FindNearby(c *gin.Context) {
	lng, err := strconv.ParseFloat(c.Query("lng"), 64)
	if err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid lng"))
		return
	}
	lat, err := strconv.ParseFloat(c.Query("lat"), 64)
	if err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid lat"))
		return
	}
	communities, err := h.svc.FindNearby(c.Request.Context(), lng, lat)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, toCommunityResponses(communities))
}

func (h *Handler) JoinCommunity(c *gin.Context) {
	userID := middleware.GetUserID(c)
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid community id"))
		return
	}
	var req JoinCommunityRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid request body"))
		return
	}
	if err := h.validate.Struct(req); err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, err.Error()))
		return
	}

	if err := h.svc.JoinCommunity(c.Request.Context(), id, userID, req.Lng, req.Lat); err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, nil)
}

func (h *Handler) LeaveCommunity(c *gin.Context) {
	userID := middleware.GetUserID(c)
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid community id"))
		return
	}
	if err := h.svc.LeaveCommunity(c.Request.Context(), id, userID); err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, nil)
}

func (h *Handler) ListMembers(c *gin.Context) {
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid community id"))
		return
	}
	cursor, err := shared.DecodeCursor(c.Query("cursor"))
	if err != nil {
		response.HandleError(c, shared.NewAppError(shared.ErrParamInvalid, "invalid cursor"))
		return
	}
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "20"))

	members, err := h.svc.ListMembers(c.Request.Context(), id, cursor, limit)
	if err != nil {
		response.HandleError(c, err)
		return
	}

	var nextCursor string
	if len(members) == shared.ClampPageSize(limit) {
		last := members[len(members)-1]
		nextCursor = shared.Cursor{CreatedAt: last.JoinedAt, ID: last.ID}.Encode()
	}
	response.OKPaginated(c, toMemberResponses(members), nextCursor)
}

func (h *Handler) MyCommunities(c *gin.Context) {
	userID := middleware.GetUserID(c)
	communities, err := h.svc.MyCommunities(c.Request.Context(), userID)
	if err != nil {
		response.HandleError(c, err)
		return
	}
	response.OK(c, toCommunityResponses(communities))
}

// Response mapping helpers

func toCommunityResponse(c *domain.Community) gin.H {
	return gin.H{
		"id":          c.ID,
		"name":        c.Name,
		"description": c.Description,
		"type":        string(c.Type),
		"status":      string(c.Status),
		"max_members": c.MaxMembers,
		"avatar_url":  c.AvatarURL,
		"creator_id":  c.CreatorID,
		"created_at":  c.CreatedAt.Format(time.RFC3339),
	}
}

func toCommunityResponses(communities []*domain.Community) []gin.H {
	result := make([]gin.H, len(communities))
	for i, c := range communities {
		result[i] = toCommunityResponse(c)
	}
	return result
}

func toMemberResponses(members []*domain.CommunityMember) []gin.H {
	result := make([]gin.H, len(members))
	for i, m := range members {
		result[i] = gin.H{
			"id":           m.ID,
			"community_id": m.CommunityID,
			"user_id":      m.UserID,
			"role":         string(m.Role),
			"joined_at":    m.JoinedAt.Format(time.RFC3339),
		}
	}
	return result
}
```

- [ ] **Step 2: Verify compilation**

Run: `go build ./...`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add api/community/
git commit -m "feat: add community HTTP handlers with validation and response mapping"
```

---

### Task 6: Wire into Router + main.go

**Files:**
- Modify: `api/router.go`
- Modify: `cmd/server/main.go`

- [ ] **Step 1: Update router.go**

Add import and community handler field to `Handlers` struct. Add community routes inside the protected group.

In `api/router.go`:
- Add import: `communityHandler "github.com/user/neighbor-hub/api/community"`
- Add to Handlers struct: `Community *communityHandler.Handler`
- Add routes inside `auth` group (after user routes):

```go
// Community routes
auth.GET("/communities/nearby", handlers.Community.FindNearby)
auth.POST("/communities", handlers.Community.CreateCommunity)
auth.GET("/communities/:id", handlers.Community.GetCommunity)
auth.POST("/communities/:id/join", handlers.Community.JoinCommunity)
auth.DELETE("/communities/:id/leave", handlers.Community.LeaveCommunity)
auth.GET("/communities/:id/members", handlers.Community.ListMembers)
auth.GET("/users/me/communities", handlers.Community.MyCommunities)
```

Note: `/communities/nearby` is registered before `/communities/:id` so Gin's trie router correctly resolves the static segment.

- [ ] **Step 2: Update main.go**

Add imports:
```go
communityHandler "github.com/user/neighbor-hub/api/community"
communityApp "github.com/user/neighbor-hub/internal/community/application"
communityInfra "github.com/user/neighbor-hub/internal/community/infra"
```

Add wiring after user module (before `handlers` struct):
```go
// Community module
communityRepo := communityInfra.NewPostgresCommunityRepo(pool)
communitySvc := communityApp.NewCommunityService(communityRepo, communityRepo)
```

Update handlers struct:
```go
handlers := api.Handlers{
    User:      userHandler.NewHandler(userSvc),
    Community: communityHandler.NewHandler(communitySvc),
}
```

- [ ] **Step 3: Verify compilation**

Run: `go build ./...`
Expected: No errors

- [ ] **Step 4: Run all tests**

Run: `go test ./... -race -count=1`
Expected: All tests pass (domain + application for both user and community)

- [ ] **Step 5: Commit**

```bash
git add api/router.go cmd/server/main.go
git commit -m "feat: wire community module into router and main.go"
```

---

### Task 7: Full Verification

After completing all tasks, verify:

- [ ] **Step 1: Compile and test**

```bash
cd ~/aiProject/neighbor-hub
go build ./...
go test ./... -race -count=1
```
Expected: All tests pass, no compilation errors

- [ ] **Step 2: Verify migration SQL is valid (syntax check)**

The migration files should be syntactically correct. If Docker is running:
```bash
make docker-up
DATABASE_URL="postgres://neighbor:neighbor_dev@localhost:5432/neighbor_hub?sslmode=disable" make migrate-up
```

- [ ] **Step 3: Verify route registration**

Start server and check routes are registered. If Docker is running:
```bash
go run ./cmd/server/ &
sleep 2
# Should get 401 (auth required)
curl -s http://localhost:8080/api/v1/communities/nearby?lng=113.9&lat=22.5 | jq .
curl -s -X POST http://localhost:8080/api/v1/communities -H "Content-Type: application/json" -d '{}' | jq .
kill %1
```
Expected: 401 responses (confirming routes exist and auth middleware works)

---

## Verification Checklist

After completing all tasks:

1. `go build ./...` compiles without errors
2. `go test ./... -race` passes all tests (domain + application for user and community)
3. New migration `000002_community.up.sql` creates t_community with PostGIS boundary + t_community_member
4. Community domain model: entity, member, types, status, business rules with tests
5. Community service: create (with owner), join (geo-fence + capacity check), leave (owner protected), nearby, my communities
6. Community handler: 7 endpoints with validation
7. Router wires all community routes under auth-protected group
8. main.go wires community repo (both CommunityRepository + GeoFenceChecker) and service

---

## Next Plans

- **Plan 3:** Post module (CRUD, comments, likes, feed)
- **Plan 4:** Vote module (single/multi select, anonymous, deadline)
- **Plan 5:** GroupBuy module (state machine, WeChat Pay)
- **Plan 6:** Frontend (WeChat Mini Program)
