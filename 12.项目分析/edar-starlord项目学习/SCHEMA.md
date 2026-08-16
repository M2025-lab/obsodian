# Wiki Schema

## Domain
家装业务交付模块（edar-starlord）- 贝壳/链家家装交付中台系统。覆盖物料任务调度、交付流程管理、量房、验收、延期处理、协调器、人力管理等核心业务领域。

## Conventions
- File names: lowercase, hyphens, no spaces (e.g., `material-task-dispatch.md`)
- Every wiki page starts with YAML frontmatter
- Use `[[wikilinks]]` to link between pages (minimum 2 outbound links per page)
- When updating a page, bump the `updated` date
- Every new page must be added to `index.md` under the correct section
- Every action must be appended to `log.md`
- **Provenance markers:** Append `^[code-path]` at the end of paragraphs whose claims trace to specific source files
- Business logic pages MUST list all entry points (Controller, Feign, MQ Listener, Schedule) that can trigger the service

## Frontmatter
```yaml
---
title: Page Title
created: YYYY-MM-DD
updated: YYYY-MM-DD
type: entity | concept | comparison | query
tags: [from taxonomy below]
sources: [code paths]
confidence: high | medium | low
---
```

## Tag Taxonomy
- domain: delivery, material-task, task-dispatch, measure, acceptance, delay, coordinator, material-flow, manpower, self-buy, foreman
- layer: service, controller, manager, dao, api
- pattern: event-driven, workflow, state-machine, scheduler
- role: butler, installer, foreman, designer, customer, supplier
- concept: config, template, rule, route, process, node

## Page Thresholds
- **Create a page** when a service/business concept is central to the delivery domain
- **Add to existing page** for minor variants or simple wrappers
- **Split a page** when it exceeds ~200 lines

## Entity Pages (Service)
Each core service gets a page:
- Overview / what it does
- Key methods and their business logic
- Entry points (all controllers, feign interfaces, MQ listeners, scheduled tasks that call this service)
- Dependencies (other services/managers it calls)
- State machine / workflow if applicable

## Concept Pages
Business concepts and cross-cutting concerns:
- Process flow definitions
- Rule engine logic
- State enums and transitions
- Event/message contracts

## Source Code Mapping
- `edar-starlord-service/` = core business logic (Service layer)
- `edar-starlord-manager/` = external integration & orchestration
- `edar-starlord-dao/` = data access (MyBatis)
- `edar-starlord-api/` = Feign interfaces & DTOs
- `edar-starlord-web/` = HTTP Controllers (REST endpoints + MQ listeners)
- `edar-starlord-base/` = shared enums, utils
