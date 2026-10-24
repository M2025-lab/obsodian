# Verifier 1: "SAE supports PHP"

## Claim under review
"SAE（Serverless 应用引擎）是阿里云推荐的 PHP 部署方案，原生支持 PHP 框架（如 Laravel、ThinkPHP），对标 Java 的 Spring Cloud 支持。"

## Verdict: REFUTED

## Evidence

1. **SAE official documentation (https://help.aliyun.com/zh/sae/product-overview/scenarios) explicitly lists "Spring Cloud or Dubbo microservice framework" as supported microservice frameworks.** PHP frameworks (Laravel, Symfony, ThinkPHP, etc.) are NOT listed in the official scenario description for microservice framework support.

2. **SAE does support non-Java applications — but only via generic container image deployment or ZIP package deployment, not via native PHP runtime integration.** The SAE documentation states that users can deploy applications using custom container images (https://help.aliyun.com/zh/sae/user-guide/deploy-an-application-by-using-a-custom-image), which means PHP apps CAN run on SAE, but SAE does not provide a managed PHP runtime, lifecycle hooks, or framework-level integration the way it does for Spring Cloud / Dubbo.

3. **SAE's PHP built-in runtime (the old PHP 7.x Web+ era runtime) has been deprecated.** The PHP built-in runtime that existed in the Web+ era was migrated to SAE but is no longer actively developed or recommended. The SAE documentation for PHP built-in runtime is archived/stale.

## Nuance (non-refuting context)
- SAE is NOT a pure Java-only platform. Users CAN deploy PHP via container images (Docker) or ZIP packages with custom Tomcat images.
- However, this is a generic container hosting capability — it lacks PHP-specific optimizations, buildpack support, or framework-aware features that would make it a "recommended PHP deployment solution."
- The claim is refuted because it overstates SAE's PHP support as "原生支持" (native support) at the same level as Spring Cloud, when in reality PHP support is via generic container/ZIP deployment with no native PHP runtime or framework integration.
