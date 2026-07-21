
## 一、工具链概述

### 1.1 完整闭环流程

本地 Obsidian 编写笔记 → Git 插件自动提交 / 推送 → GitHub 云端仓库托管 → 飞书消息通知 + 团队入口共享 → 团队成员拉取同步本地笔记

### 1.2 核心价值

- **本地体验优先**：基于 Obsidian 原生编辑能力，离线可用，格式兼容度高
- **版本自动沉淀**：全量历史提交可追溯，支持回滚任意版本笔记
- **团队低成本协同**：统一云端仓库，多人并行编辑，自动同步差异
- **飞书原生触达**：仓库更新实时推送飞书，团队感知成本低

---

## 二、前置准备

### 2.1 所需软件与账号

表格

|类别|内容|说明|
|---|---|---|
|本地客户端|Obsidian|笔记编辑主工具|
|本地工具|Git|Mac 推荐 Homebrew 安装；Windows 推荐 Git Bash|
|云端载体|GitHub 账号|用于托管远程仓库|
|协同平台|飞书账号 + 目标飞书群|用于更新通知与团队入口聚合|

### 2.2 权限要求

- GitHub：拥有仓库创建权限，或已被添加为目标仓库协作者
- 飞书：拥有群机器人添加权限（或联系群管理员协助）

---

## 三、第一步：GitHub 远程仓库与鉴权配置

### 3.1 创建远程仓库

1. 登录 GitHub，右上角 `+` → `New repository`
2. 填写仓库名称（如 `obsidian-notes`），选择可见性：
    
    - **Public（公开）**：所有人可查看，适合公开知识库；**严禁存放敏感 / 隐私内容**
    - **Private（私有）**：仅指定成员可访问，适合内部团队笔记
    
3. 无需勾选初始化 README、.gitignore，点击 `Create repository`

### 3.2 生成个人访问令牌（PAT，必做）

GitHub 已禁用账号密码进行 Git 操作，必须使用 PAT 替代密码：

1. 右上角头像 → `Settings` → 左侧 `Developer settings` → `Personal access tokens` → `Tokens (classic)`
2. 点击 `Generate new token (classic)`
3. 配置参数：
    
    - `Note`：填写备注，如 `Obsidian Git 同步`
    - `Expiration`：建议选择 90 天，定期轮换保障安全
    - `Scope`：**必须完整勾选 `repo` 权限组**（仓库读写权限）
    
4. 滑到底部点击 `Generate token`
5. **立即复制生成的令牌字符串并保存**，令牌仅显示一次，丢失需重新生成

---

## 四、第二步：本地 Git 环境初始化

### 4.1 Git 安装与验证

- **Mac 系统**（推荐 Homebrew）：
    
    bash
    
    运行
    
    ```
    brew install git
    git --version
    ```
    
    正常输出版本号即安装成功。
- **Windows 系统**：下载安装 Git Bash，安装后在 Git Bash 中执行 `git --version` 验证。

### 4.2  鉴权注意事项

- Mac 系统会自动将凭证存入「钥匙串访问」，若后续鉴权失败，优先搜索 `github.com` 删除全部旧凭证，重新输入 PAT。
- 首次推送弹窗提示输入密码时，**粘贴 PAT 令牌，不要输入 GitHub 登录密码**。

---

## 五、第三步：Obsidian 笔记库 Git 初始化

### 5.1 初始化本地仓库

终端进入你的 Obsidian 笔记库根目录，执行：

bash

运行

```
cd /你的笔记库本地路径
git init
```


### 5.3 关联远程仓库

替换为你的 GitHub 仓库地址：

bash

运行

```
git remote add origin https://github.com/你的用户名/仓库名.git
```

### 5.4 首次提交与上游分支绑定

bash

运行

```
git add .
git commit -m "init: 初始化笔记库"
git push -u origin main
```

- `-u` 参数用于绑定本地分支与远程上游分支，执行一次即可，后续推送无需重复指定。
- 该步骤解决 `No upstream-branch is set` 报错。

---

## 六、第四步：Obsidian Git 插件安装与配置

### 6.1 插件安装

1. 打开 Obsidian → 设置 → 第三方插件 → 关闭安全模式
2. 浏览社区插件，搜索 `Obsidian Git`，安装并启用

### 6.2 核心配置项详解

表格

|配置项|推荐值|说明|
|---|---|---|
|Custom Git binary path|Mac 留空或填 `/opt/homebrew/bin/git`|Git 程序路径，正常安装无需修改|
|Custom base path (Git repository path)|**留空**|仅当 Git 仓库在笔记库子目录时填写相对路径，**禁止填写 GitHub 网址**|
|Custom Git directory path|留空|特殊场景使用，标准仓库无需配置|
|Split timers for automatic commit and sync|开启|分开控制提交、推送、拉取周期|
|Auto commit interval (minutes)|5|每 5 分钟自动本地提交|
|Auto commit after stopping file edits|开启|停止编辑后延迟提交，避免频繁触发|
|Auto push interval (minutes)|5|每 5 分钟自动推送至远程|
|Auto pull interval (minutes)|5|每 5 分钟自动拉取云端更新|
|Commit message on auto commit|`vault backup: {{date}}`|自动提交备注模板，支持日期占位符|

### 6.3 验证生效

配置完成后，修改一篇笔记，等待设定的间隔时间，查看 GitHub 仓库是否出现新提交，确认自动同步链路正常。

---

## 七、第五步：飞书侧团队共享配置

### 7.1 方案一：飞书群机器人推送更新通知（推荐）

实现效果：仓库有新推送时，自动在飞书群内发送更新通知。

1. **飞书侧创建机器人**
    
    - 目标飞书群 → 设置 → 群机器人 → 添加机器人 → 自定义机器人
    - 填写机器人名称（如「笔记库更新通知」），复制生成的 `Webhook 地址`
    
2. **GitHub 配置 Webhook**
    
    - 仓库页面 → `Settings` → `Webhooks` → `Add webhook`
    - `Payload URL`：粘贴飞书机器人 Webhook 地址
    - `Content type`：选择 `application/json`
    - `Which events would you like to trigger this webhook?`：选择 `Just the push event`
    - 勾选 `Active`，点击 `Add webhook`
    
3. 测试：本地推送一次提交，飞书群将收到更新通知。

### 7.2 方案二：飞书知识库聚合入口

将 GitHub 仓库地址添加至团队飞书知识库 / 书签目录，作为统一访问入口；配合仓库权限管理，团队成员可直接跳转查看笔记历史、下载文件。

### 7.3 多人协同权限配置

1. 仓库所有者进入 GitHub 仓库 → `Settings` → `Collaborators` → `Add people`
2. 输入协作者 GitHub 用户名 / 邮箱，发送邀请
3. 协作者接受邀请后，参照本手册完成本地 Git + Obsidian 配置，拉取仓库即可开始协同编辑

---

## 八、常见问题排查（FAQ）

### 1. 推送报错 403 Permission denied

- 排查 PAT 是否勾选 `repo` 权限、是否过期
- 清理 Mac 钥匙串中所有 `github.com` 旧凭证，重新输入 PAT
- 确认远程仓库地址拼写正确，账号拥有仓库读写权限

### 2. 插件报错 No upstream-branch is set

终端进入仓库目录，执行一次绑定命令：

bash

运行

```
git push -u origin main
```

### 3. `git config --global user.name` 无输出

未配置全局身份，重新执行身份配置命令即可。

### 4. 多人编辑出现冲突

- 编辑前先执行一次拉取，保证本地为最新版本
- 尽量按文件 / 目录分工编辑，减少多人同时修改同一文件
- 冲突发生时，打开冲突文件，手动保留需要的内容后重新提交

### 5. 插件不自动同步

- 确认自动提交 / 推送间隔不为 0
- 确认本地 Git 鉴权正常，终端手动 `git push` 无报错
- 重启 Obsidian 重载插件

---

## 九、新人配置自检清单

- [ ]  GitHub 仓库已创建，PAT 令牌已生成并保存
- [ ]  本地 Git 已安装，全局用户名、邮箱配置完成
- [ ]  本地笔记库已初始化 Git，并成功关联远程仓库
- [ ]  首次推送成功，上游分支已绑定
- [ ]  .gitignore 已配置
- [ ]  Obsidian Git 插件安装完成，自动同步参数已设置
- [ ]  手动修改测试，自动推送至 GitHub 正常
- [ ]  飞书机器人通知配置完成，推送可正常触达
- [ ]  团队协作者已添加仓库权限

---
## 相关文档

- [[会议与分享/分享/智能纪要：Obsidian工具使用经验分享 2026年7月2日]] — Obsidian使用经验分享
- [[会议与分享/分享/ai围炉]] — AI与工具链结合
- [[知识沉淀/📚 异常排查 & 调用链分析 完整手册]] — 知识沉淀实践