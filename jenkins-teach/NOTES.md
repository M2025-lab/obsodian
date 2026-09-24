# NOTES（教学随记）

- 用户自述：**完全没用过 Jenkins**，中文交流，目标「从零搭一条 Java 流水线」。
- 真实项目锚点（已确认，/Users/mirror/IdeaProjects/edar-starlord）：
  - 多模块 Maven 工程，6 个 module：`adar-starlord-{api,base,dao,manager,service,web}`（web 是最终可运行 jar）
  - 打包脚本 `build.sh`：`mvn clean package -Dmaven.test.skip=true` → 建 `release/{bin,lib}` → 打 `release.tar.gz`
  - 大禹平台脚本 `build-dayu.sh`：`./mvnw --batch-mode clean -U package ...` → `release/edar-starlord-web/{bin,lib,conf}` → tar.gz
  - `Dockerfile`：基于 `centos_java_1_8` 镜像，把 `build/edar-starlord` COPY 到 nginx/htdocs 目录
  - 公司用「大禹平台」做部署，Jenkins 定位侧重：本地/自建 CI 与通用概念理解
- 本机环境（已探测）：Java 8（Corretto）、无 Docker、无独立 Maven 但有 `mvnw`、有 brew。
  - 关键教学点：Jenkins 主程序需要 Java 17/21，与构建目标 Java 8 是两套 JVM。
- 教学节奏：一次一小课 + 可立即上手的小练习；进度跟用户疑问走。
- 复用 kafka-teach 的 `assets/style.css`（Tufte 风、浅色），色值改为深蓝以区分课程。

## 课程进度
- L1 从 build.sh 到 Pipeline（心智模型 + stage 翻译 + 最小 Jenkinsfile）
- L2 安装并跑通第一次构建（brew jenkins-lts + openjdk@21 / 解锁 / hello / 接本地 git 跑 mvnw）
- L3 日常使用教程（任务类型 / 配置项 / 触发方式 / 参数化 / 读结果 / 制品归档 / 排障清单）
- 待续 L4：Jenkinsfile 进阶（environment / credentials / post 通知 / 接测试）
