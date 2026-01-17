<!--
SYNC IMPACT REPORT
==================
Version Change: Initial → 1.0.0
Modified Principles: N/A (initial creation)
Added Sections:
  - Core Principles (5 principles focused on code quality, testing, UX, performance)
  - Quality Gates
  - Development Workflow
  - Governance
Removed Sections: N/A
Templates Status:
  ✅ plan-template.md - Aligned (Constitution Check section references this file)
  ✅ spec-template.md - Aligned (Requirements and Success Criteria sections support principles)
  ✅ tasks-template.md - Aligned (Test-first approach and user story organization match principles)
Follow-up TODOs: None
Rationale: Initial constitution establishing foundational governance for CodeNodeIO project
-->

# CodeNodeIO Constitution

## Licensing & IP: Kotlin Multiplatform & Go Standards
The Agent must treat licensing as a "breaking build" constraint. Use the following rules when modifying build.gradle.kts, go.mod, or implementing core logic.
1. The "Static Linking" Rule (Priority: Critical)
  * Context: Because Go binaries and KMP native targets (iOS/Desktop) often involve static linking, license "infection" is a primary risk.
  * Rule: The Agent is strictly forbidden from adding any GPL (v2 or v3) or LGPL dependencies to the Go backend or KMP common/native source sets.
  * Permitted: MIT, Apache 2.0, BSD-3-Clause, and MPL 2.0 (only if kept in separate files).
2. KMP Dependency Protocol (build.gradle.kts)
  * When proposing a new Multiplatform library (e.g., Ktor, SQLDelight, Decompose):
  * Audit Transitives: The Agent must check if a library brings in restrictive Java/Maven dependencies.
  * Header Management: Every .kt and .kts file must begin with the project’s standard header. For KMP, ensure the header is placed above the package declaration.
  * JetBrains/Google Alignment: Favor libraries that align with the Apache 2.0 standard used by JetBrains (Kotlin) and Google (Android).
3. Go Module Protocol (go.mod)
  * go list Verification: Before finalising a /plan that adds a Go module, the Agent should simulate or suggest running go list -m all to ensure no "Copyleft" modules are introduced.
  * Internal Packages: When the Agent generates internal Go packages, it must ensure they do not contain verbatim snippets from popular but restrictively licensed Go frameworks (e.g., certain older AGPL-based networking tools).
4. Implementation Guidelines
  * No "Stack Overflow" Copying: In Go, where "a little copying is better than a little dependency," the Agent must still synthesize logic rather than copy-pasting code that might be under a different license.
  * Expectations for Specify Phase: If a requirement in the Specify phase demands a library that violates these rules, the Agent must raise a "Blocker" alert in the Plan phase and suggest a permissive alternative (e.g., suggesting crypto/tls in Go over a third-party library with an unclear license).

## Core Principles

### I. Code Quality First

Code MUST meet these non-negotiable quality standards:

- **Readability**: Code is written for humans first, machines second. Use clear variable names, extract complex logic into named functions, and maintain consistent style.
- **Maintainability**: Every module MUST have a single, clear responsibility. Prefer composition over inheritance. Keep functions under 50 lines where possible.
- **Type Safety**: Use static typing where available. All public interfaces MUST have explicit type annotations.
- **Documentation**: Public APIs MUST include docstrings/comments explaining purpose, parameters, return values, and exceptions. Complex algorithms MUST have explanatory comments.
- **Security**: All input MUST be validated at system boundaries. No secrets in code or logs. Follow OWASP guidelines for the technology stack.

**Rationale**: Poor code quality compounds over time, creating technical debt that slows all future development. Quality standards prevent this accumulation and ensure the codebase remains maintainable as it grows.

### II. Test-Driven Development

TDD is mandatory for all new features and bug fixes:

- **Red-Green-Refactor**: Tests MUST be written first and MUST fail before implementation begins. Implementation proceeds only after test failure is verified. Code is refactored only after tests pass.
- **Test Coverage**: All new code paths MUST have test coverage. Aim for >80% line coverage, 100% for critical paths (auth, payments, data loss scenarios).
- **Test Types Required**:
  - **Unit Tests**: For all business logic, utilities, and pure functions
  - **Integration Tests**: For database operations, API calls, and inter-service communication
  - **Contract Tests**: For all public APIs, ensuring backward compatibility
- **Test Independence**: Tests MUST run in isolation. No shared state between tests. Use fixtures, mocks, or test doubles for external dependencies.

**Rationale**: TDD ensures features are designed for testability, reduces debugging time, provides living documentation, and enables confident refactoring. Tests written after implementation often miss edge cases and create false confidence.

### III. User Experience Consistency

All user-facing features MUST maintain consistent experience:

- **Design System**: Use established UI patterns from the project's design system. New patterns require design review before implementation.
- **Accessibility**: WCAG 2.1 Level AA compliance is mandatory. All interactive elements MUST be keyboard navigable and screen-reader compatible.
- **Error Handling**: User-facing errors MUST be actionable (tell user what went wrong AND what to do next). Never expose stack traces or technical jargon to end users.
- **Response Time**: User actions MUST provide feedback within 100ms. Operations taking >1s MUST show loading indicators with progress when possible.
- **Mobile First**: All web interfaces MUST be responsive and touch-friendly. Test on mobile viewports before desktop.

**Rationale**: Inconsistent UX creates confusion, increases support burden, and damages user trust. A cohesive experience is a competitive advantage and reduces cognitive load for users learning the system.

### IV. Performance Requirements

System performance is a feature, not an optimization:

- **Benchmarks**: All performance-critical code paths MUST have benchmarks. Regressions >10% require justification.
- **Response Time SLAs**:
  - API endpoints: p95 < 200ms for reads, < 500ms for writes
  - Database queries: < 100ms for simple queries, < 500ms for complex joins
  - Page load: First Contentful Paint < 1.5s, Time to Interactive < 3.5s
- **Resource Limits**:
  - Memory: Services MUST operate within configured limits (OOM is a critical bug)
  - CPU: No single request should pin CPU >500ms
  - Database connections: Connection pools MUST be sized and monitored
- **Scalability**: New features MUST analyze scalability impact. O(n²) algorithms require explicit justification and mitigation plans.
- **Monitoring**: Performance-critical paths MUST emit metrics (latency, error rate, throughput). Dashboards required for production services.

**Rationale**: Performance problems compound at scale and are expensive to fix retroactively. Proactive performance discipline prevents user-impacting incidents and reduces infrastructure costs.

### V. Observability & Debugging

Systems MUST be designed for operational excellence:

- **Structured Logging**: All logs MUST be machine-parseable (JSON). Include correlation IDs for request tracing. Log levels: ERROR (bugs), WARN (degraded), INFO (significant events), DEBUG (troubleshooting).
- **Error Context**: Exceptions MUST include sufficient context to diagnose root cause without reproducing. Capture: user action, system state, relevant IDs, timestamp, version.
- **Metrics & Alerting**: Business-critical flows MUST emit success/failure metrics. Alerts configured for: error rate spikes, latency regressions, resource exhaustion.
- **Feature Flags**: New features MUST be behind feature flags in production. Enables safe rollout, quick rollback, and A/B testing.
- **Runbooks**: Production services MUST have runbooks documenting: architecture, dependencies, common failures, debugging steps, rollback procedures.

**Rationale**: Most engineering time is spent debugging and operating systems, not writing new code. Investing in observability accelerates incident response, reduces MTTR, and enables data-driven decision making.

## Quality Gates

All code changes MUST pass these gates before merging:

1. **Automated Tests**: All tests pass in CI. No skipped or ignored tests without documented justification and tracking issue.
2. **Code Review**: At least one approval from a team member. Reviewer MUST verify: tests exist and are meaningful, code follows principles, no security vulnerabilities.
3. **Performance Check**: Benchmarks run in CI. Regressions >10% require explicit approval and mitigation plan.
4. **Linting & Formatting**: All linters pass. Code formatting is consistent. No disabled lint rules without documented justification.
5. **Documentation Updated**: If public API changed, docs updated. If behavior changed, changelog entry added.

**Breaking the Build**: Failing the build is acceptable during development. Merged code that breaks the build requires immediate fix or revert.

## Development Workflow

### Feature Development

1. **Specification**: Document user stories with acceptance criteria. Identify performance requirements and testing strategy.
2. **Design Review**: For features touching >3 files or introducing new patterns, create design doc and get team review.
3. **Test-First Implementation**: Write failing tests → Implement → Refactor → Repeat.
4. **Integration**: Merge frequently (daily if possible). Keep branches short-lived (<3 days).
5. **Deployment**: Use feature flags for gradual rollout. Monitor metrics closely for 24h post-deployment.

### Bug Fixes

1. **Reproduction**: Create failing test that reproduces the bug.
2. **Root Cause**: Document root cause and why existing tests didn't catch it.
3. **Fix**: Implement fix until test passes.
4. **Prevention**: Add additional tests to prevent similar bugs. Update code review checklist if needed.

### Technical Debt

- **Definition**: Code that violates principles or creates maintenance burden. NOT "code I don't like."
- **Tracking**: Technical debt MUST be tracked as issues with justification, impact assessment, and proposed remediation.
- **Paydown**: Allocate 20% of sprint capacity to technical debt. Prioritize debt that blocks feature development.
- **Bankruptcy**: If debt grows unbounded, declare technical bankruptcy and plan systematic remediation.

## Governance

### Constitutional Authority

This constitution supersedes all other development practices, style guides, and conventions. In case of conflict, the constitution takes precedence.

### Amendments

1. **Proposal**: Any team member can propose an amendment via documented proposal explaining: problem being solved, proposed change, impact on existing code.
2. **Review Period**: 7-day review period for team feedback.
3. **Approval**: Requires majority approval from active contributors.
4. **Migration Plan**: Amendment MUST include migration plan for bringing existing code into compliance (or explicit exception for legacy code).
5. **Versioning**: Constitution version MUST be incremented per semantic versioning:
   - **MAJOR**: Backward incompatible changes (principle removal, redefinition of core concepts)
   - **MINOR**: New principles added or expanded guidance
   - **PATCH**: Clarifications, wording improvements, typo fixes

### Compliance

- **Code Review**: Reviewers MUST verify compliance with constitutional principles.
- **Exception Process**: Violations require documented justification with: why needed, simpler alternative rejected, plan to remediate.
- **Audit**: Quarterly review of exceptions to identify systemic issues or needed constitutional updates.

### Living Document

This constitution is a living document that evolves with the project. Regular retrospectives SHOULD review whether principles are helping or hindering productivity and adjust accordingly.

**Version**: 1.0.0 | **Ratified**: 2026-01-12 | **Last Amended**: 2026-01-12
