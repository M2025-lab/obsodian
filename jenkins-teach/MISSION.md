# Mission: 从零搭一条 Java（Maven）流水线

## Why
你是 `edar-starlord`（家装交付中台，Spring Boot 2.1.9 + Maven 多模块）的后端新人，**完全没用过 Jenkins**。现在项目的构建/打包靠人工跑 `build.sh`（最终打出 `release.tar.gz`），公司侧由「大禹平台」做发布。你的目标不是背理论，而是**亲手从零搭出一条能跑的 Java 流水线**：拉代码 → Maven 编译打包 → 出制品，并能读懂/改造团队已有的 Jenkinsfile。学会后，能在本地或自建 Jenkins 上把现在手动的 `build.sh` 流程自动化，也能看懂公司发布体系背后的 CI 概念。

## Success looks like
- 能说清 Jenkins 的核心概念（Controller / Agent(Node) / Executor / Workspace / Job / Pipeline(Build)），并画出自家一次构建的链路
- 能写出一份最小的声明式 `Jenkinsfile`，把你项目里的 `build.sh` 步骤（clean package → 打 tar.gz）完整搬进去
- 能在本地把 Jenkins 跑起来，新建一个 Pipeline 任务并成功跑通一次 Maven 构建
- 能看懂 `agent { docker { ... } }`、`stages`、`post`、`environment`/`credentials` 这几块，并照着加步骤

## Constraints
- 零基础入门，先用中文、先建立直觉，不急着啃 Groovy 底层
- 一次只讲一个小而完整的点，配一个能立刻上手的小练习；进度跟你的疑问走
- 本机现状：Java 8、无 Docker、有 Maven Wrapper（`mvnw`）。Jenkins 本身需要 Java 17/21，这跟「构建目标用 Java 8」是两回事——这点要讲清

## Out of scope（暂不涉及）
- 大禹平台/公司发布体系的具体接入（只讲 Jenkins 通用概念，便于你理解底层）
- Jenkins 集群运维、Kubernetes 动态 Agent、高可用部署
- 共享库（shared library）、Blue Ocean 之外的复杂插件体系
- 超大规模流水线的性能调优
