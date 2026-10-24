# Verifier 3: "SAE official docs list Spring Cloud / Dubbo but not PHP frameworks"

## Claim under review
"SAE 官方文档明确将 Spring Cloud 和 Dubbo 列为支持的微服务框架，未提及任何 PHP 框架（如 Laravel、Symfony、ThinkPHP 等）"

## Verdict: NOT REFUTED (claim is accurate)

## Evidence

1. **Primary source confirmation:** The official SAE product overview page (https://help.aliyun.com/zh/sae/product-overview/scenarios) contains the quote: "支持Spring Cloud或Dubbo微服务框架" in the microservice scenario section. No PHP framework (Laravel, Symfony, ThinkPHP, etc.) is mentioned anywhere in the scenarios documentation.

2. **Framework-agnostic deployment ≠ framework support:** SAE's ZIP package deployment and custom image deployment allow users to run any language, including PHP. However, this is generic container hosting — not "framework support" in the microservice framework sense. The documentation's microservice scenario specifically highlights Spring Cloud and Dubbo because SAE provides native integration (service discovery, configuration management, traffic governance) for these Java frameworks.

3. **No PHP framework documentation exists on SAE:** A search of SAE's official documentation for "Laravel", "ThinkPHP", "Symfony", or "PHP microservice" returns no SAE-specific best practice guides, tutorials, or integration docs — unlike Spring Cloud/Dubbo which have extensive dedicated SAE documentation.

4. **SAE's PHP runtime is deprecated:** The old SAE PHP built-in runtime (inherited from Web+) supported PHP 7.x and is no longer recommended. The current recommended way to run PHP on SAE is via custom container images, which means users must manage their own PHP runtime, extensions, and configuration — the same as running on ECS or ACK.

## Qualification (not refutation)
- The claim is narrowly about what SAE's **official documentation explicitly lists as supported microservice frameworks** — in this specific framing, the claim is undeniably accurate.
- It does NOT claim PHP cannot run on SAE at all; only that SAE does not list PHP frameworks alongside Spring Cloud/Dubbo as "supported microservice frameworks," which matches the documentation exactly.
- Source quality is sufficient: primary source (official Alibaba Cloud documentation) confirms the claim directly.
