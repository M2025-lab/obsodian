# 基线：用户为零基础初学者

用户明确自述**完全没用过 Jenkins**，目标是从零搭一条 Java（Maven）流水线。已确认其本机环境 Java 8、无 Docker、有 Maven Wrapper（`mvnw`），公司侧用「大禹平台」做发布。

为何重要：第一课必须从「一次构建到底发生了什么」这种直观心智模型切入，不能直接上 Jenkinsfile 语法；且必须尽早点破「Jenkins 主程序需要 Java 17/21，与构建目标 Java 8 是两回事」这一极易混淆点，避免后续装环境时踩坑。后续每一课都应以 `edar-starlord` 的 `build.sh`/`mvnw` 为真实练习素材，而不是抽象 demo。
