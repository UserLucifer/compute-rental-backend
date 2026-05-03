# AGENTS.md

## Mission
Make the smallest correct change that solves the user's request.

Optimize for:
1. **Correctness**: (Type safety, Spring Bean lifecycle, and Thread safety)
2. **Minimal diffs**: Don't touch what isn't broken.
3. **Preserving existing patterns**: Follow the established directory structure.
4. **Low maintenance cost**: Simplicity over cleverness.
5. **Clear validation**: Ensure code is error-free and compiles properly.

---

## Default operating mode

### Before making changes:
- **Read the relevant files first.**
- **Analyze Spring Context**: Trace how `@Component`, `@Service`, and `@Mapper` are injected.
- **Trace DB Flow**: Understand the MyBatis-Plus entity mapping and QueryWrapper logic.
- **Understand the existing abstraction** before adding a new one.

### When changing code:
- **JDK 17 Standards**: Use `var` for local variables and `record` for DTOs where appropriate.
- **Spring Boot 3 Conventions**: Follow the **Spring Security 6 Lambda DSL** and relevant annotations.
- **Fix the root cause**, not just the symptom.
- **Prefer the smallest viable patch.**
- **Reuse existing utilities**, helpers, and components (e.g., Redis lock utils, Result wrappers).

### Avoid:
- Unnecessary renames or file moves.
- Introducing new dependencies without strong justification.
- **DO NOT attempt to open a browser for testing.** Focus on compilation and logic.

---

## Simplicity & Framework Rules

- **Direct logic** over indirection.
- **MyBatis-Plus**: Prefer LambdaQueryWrapper for type-safe database operations.
- **Resource Management**: Ensure Redis keys have appropriate TTLs and Redis locks are released in `finally` blocks.
- **Security**: Always consider how changes impact JWT authentication and Spring Security filters.

---

## Change scope discipline

Keep edits tightly scoped to the task.
- Do not touch unrelated files unless required for correctness.
- Do not bundle opportunistic cleanups into the same change.
- If a change impacts the API, ensure Knife4j/OpenAPI annotations are updated.
- If any backend API endpoint, request parameter, request body, response body, path, method, authentication requirement, or API annotation changes, synchronously update `E:\业务开发\qianduan\后端api文档.md` in the same change.

---

## Validation (The "Backend-Fast" Rule)

**Validation Order:**
1. **Compilation Check**: Run `mvn compile` to ensure no syntax or dependency errors.
2. **Type Safety**: Ensure no unchecked conversions or raw types are introduced.
3. **Targeted Test**: If a unit test exists for the logic, run it with `mvn test -Dtest=...`.
4. **NO UI TEST**: Never try to verify changes via a browser or interactive session.

In your final response, report:
- What changed and why.
- **Confirmation that `mvn compile` was successful.**
- Any remaining risk or unverified areas (e.g., MQ message flow).

---

## Communication

Be concise, concrete, and technical.
- **Plan First**: For non-trivial tasks, provide a short plan with likely touch points.
- **Surface findings early**: Wrong assumptions, hidden constraints, or root cause discoveries.

---

## Testing & Build Hooks (Maven)

Primary install command:
`mvn clean install -DskipTests`

Primary dev/run command:
`mvn spring-boot:run`

Primary targeted test command:
`mvn test -Dtest=[file_path]`

Primary compilation check:
`mvn compile`

Primary lint/checkstyle:
`mvn checkstyle:check` (if applicable)

---

## Final quality bar

A change is not done until it is:
- **Correct and Type-safe.**
- **Compile-ready with zero errors.**
- **Minimal and Understandable.**
- **Consistent with existing project patterns.**
