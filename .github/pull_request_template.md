## Summary

<!-- Brief description of changes -->

## Changes

-

## DDD Architecture Checklist

### Architecture Compliance
- [ ] Dependency direction correct: adapter -> application -> domain <- infrastructure
- [ ] Domain layer has zero Spring/MyBatis imports
- [ ] DO objects not leaked above infrastructure layer
- [ ] Request/Response objects not leaked below adapter layer

### DDD Pattern Compliance
- [ ] Aggregate root encapsulates business invariants (no anemic model)
- [ ] Value objects are immutable (final fields, equals/hashCode)
- [ ] State changes only through aggregate methods (no public setters)
- [ ] Repository interface in domain, implementation in infrastructure

### Coding Standards
- [ ] Java 8 compatible (no var, records, text blocks, List.of/Map.of)
- [ ] Lombok usage correct (aggregate: @Getter only, DO/DTO: @Data)
- [ ] Naming conventions followed (DO, DTO, Mapper, Repository suffixes)
- [ ] Object conversion via MapStruct (@Mapper componentModel = "spring")

### Testing
- [ ] Domain tests: pure JUnit 5 + AssertJ (no Spring context)
- [ ] Application tests: Mockito mocks for Repository ports
- [ ] Infrastructure tests: @SpringBootTest + H2
- [ ] Adapter tests: @WebMvcTest + MockMvc

### Verification
- [ ] `mvn test` passes
- [ ] `mvn checkstyle:check` passes
- [ ] `./scripts/entropy-check.sh` passes
