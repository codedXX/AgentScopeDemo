# 从零开始搭建 AiTripPlan 多智能体旅游规划系统（保姆级教程）

> 本教程带你**从 0 开始**，一行代码、一个文件地搭出这个项目，最终代码与现有项目**一模一样**。
> 每完成一步，都会告诉你**怎么验证这一步做对了**，验证通过再进入下一步，绝不让你“写完一大堆代码却不知道对没对”。

---

## 阅读前必看

**这个项目是什么？**
一个能听懂“帮我规划深圳到惠州 3 日自驾游”这种话，然后自己拆任务、查路线、排行程、最后给你一份完整旅游计划的 AI 系统。它由 **4 个独立的 Spring Boot 程序（Agent）** 组成，互相通过网络协作。

**搭建顺序（也是本教程的章节顺序）：**

```
第0章  搞懂架构（不写代码，先看懂全貌）
第1章  环境准备（装软件、申请 Key）
第2章  父工程         —— 管依赖、聚合 4 个模块
第3章  commons 模块   —— 公共工具，所有 Agent 都要用它
第4章  routeMaking_agent  —— 路线专家（先建，因为它要先注册上线）
第5章  tripPlanner_agent  —— 行程专家
第6章  manager_agent      —— 主管（最后建，它指挥前面两个）
第7章  全流程联调          —— 真正跑通一次完整规划
第8章  常见问题 + 知识回顾
```

**最终的目录结构长这样**（你照着这个建文件夹就不会乱）：

```
AiTripPlan-AgentScope/
├── pom.xml                         ← 父工程（第2章）
├── .env                            ← 密钥配置（第3章）
├── commons/                        ← 公共模块（第3章）
│   ├── pom.xml
│   └── src/main/java/com/imooc/commons/
│       ├── conf/Properties.java
│       ├── data/PromptSchema.java
│       ├── data/ResponseSchema.java
│       └── utils/
│           ├── AgentUtils.java
│           ├── ToolUtils.java
│           ├── PromptUtils.java
│           ├── NacosUtil.java
│           └── LangFuseUtils.java
├── routeMaking_agent/              ← 路线 Agent（第4章）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/imooc/routeMakingAgent/
│       │   ├── RouteMakingAgentApplication.java
│       │   ├── agents/RouteMakingAgent.java
│       │   └── mcp/BaiduMapMCP.java
│       └── resources/
│           ├── application.yml
│           └── logback.xml
├── tripPlanner_agent/              ← 行程 Agent（第5章）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/imooc/tripPlannerAgent/
│       │   ├── TripPlannerAgentApplication.java
│       │   └── agents/
│       │       ├── TripPlannerAgent.java
│       │       └── SuggestSightAgent.java
│       └── resources/
│           ├── application.yml
│           ├── logback.xml
│           └── skills/
│               ├── Suggest-Sights/SKILL.md
│               └── Make-Table/SKILL.md
│               └── Make-Table/scripts/table.sh
└── manager_agent/                  ← 主管 Agent（第6章）
    ├── pom.xml
    └── src/main/
        ├── java/com/imooc/managerAgent/
        │   ├── ManagerAgentApplication.java
        │   ├── controller/ManagerAgentController.java
        │   ├── agents/ManagerAgent.java
        │   ├── tool/RemoteAgentTool.java
        │   ├── plan/TripPlan.java
        │   └── hook/planHook.java
        └── resources/
            ├── application.yml
            └── logback.xml
```

> ⚠️ 你在现有项目里可能看到 `pom(1).xml`、`application(1).yml` 这种带 `(1)` 的文件——那是 IDE/下载产生的**重复副本，不是项目的一部分**，本教程不创建它们，你也可以忽略/删除。

> 🔑 **安全提示**：本教程所有密钥都用占位符（如 `你的通义千问Key`）。**千万不要把真实 Key 提交到 Git 或发给别人**。

---

## 第 0 章：先看懂这个项目（不写代码）

### 0.1 一句话理解
你对**主管**说一句话，主管把活拆成几步，分别派给**路线专家**和**行程专家**去干，干完汇总给你。就像一个项目经理带两个下属。

### 0.2 四个模块各干什么

| 模块 | 角色 | 干的活 | 端口 |
|------|------|--------|------|
| `commons` | 工具箱 | 不是程序，是被其他三个共用的代码库（配置、工具类） | 无 |
| `manager_agent` | 主管 | 接收用户请求 → 拆任务 → 派活 → 汇总 | 8081 |
| `routeMaking_agent` | 路线专家 | 调百度地图，算自驾路线、里程、过路费 | 8082 |
| `tripPlanner_agent` | 行程专家 | 推荐景点、美食、住宿，做行程表 | 8085 |

### 0.3 一张图看懂运行流程

```
   你（用户）
     │  POST /trip  "帮我规划深圳到惠州3日自驾游"
     ▼
┌─────────────────────────┐
│   manager_agent (8081)  │  主管：用 PlanNotebook 把任务拆成几步
│   "第1步查路线，第2步排行程" │
└───────┬─────────────────┘
        │ 通过 Nacos 找到下属，用 A2A 协议远程调用
        ├──────────────────────────────┐
        ▼                              ▼
┌──────────────────────┐   ┌──────────────────────────┐
│ routeMaking (8082)   │   │ tripPlanner (8085)       │
│ 调百度地图MCP查路线   │   │ 内部还有个子Agent+技能库  │
└──────────────────────┘   └──────────────────────────┘
        │                              │
        └──────────────┬───────────────┘
                       ▼
              主管汇总 → 返回完整计划给你
```

### 0.4 五个名词扫盲（看不懂没关系，后面用到再细讲）

1. **ReActAgent**：一种会“**想一步、做一步、看结果、再想**”的智能体（Reason + Act）。本项目所有 Agent 都是它。
2. **PlanNotebook（计划本）**：让 Agent 把复杂任务**自动拆成有序步骤**并跟踪每步状态的工具。只有主管用。
3. **A2A + Nacos**：A2A 是“Agent 之间打电话”的协议；Nacos 是“**电话簿**”——每个 Agent 上线时把自己登记进去，别人才能按名字找到它。
4. **MCP**：一种“**给大模型外接工具**”的标准协议。路线专家通过它接上百度地图，模型就能查真实路况数据。
5. **Skill（技能）**：以 `.md` 文件描述的“**操作手册 + 脚本**”，告诉 Agent 遇到某类任务该怎么一步步做。行程专家用它来制表。

> 看到这里，你已经懂了全貌。下面开始动手。**强烈建议：严格按章节顺序，每章验证通过再往下。**

### 关于「验证」的重要约定（务必先读）

本教程的验证，**验证的是「功能有没有真的生效」，不是「代码能不能编译/启动」**。也就是说：

- ❌ 不是只看到 `BUILD SUCCESS` 或 `Tomcat started` 就算过；
- ✅ 而是要**亲眼看到功能产生效果**——大模型真的回了话、路线 Agent 真的算出了里程和过路费、行程 Agent 真的推荐了具体景点、主管真的把任务拆成了步骤并派给了对应专家。

为了在「半成品阶段」也能验证功能，有些章节会让你**临时写一小段「验证脚手架」代码**（比如一个临时的 `CommandLineRunner` 或 `main` 方法），用它主动触发功能、打印结果。这类代码我都会用 **`【临时验证代码：验证后请删除】`** 标明。

> 🔁 **重要**：这些脚手架是**一次性的，验证完务必删掉或注释掉**。它们不属于原项目——删掉之后，你的最终代码才和现有项目**一模一样**（这正是需求第 3 条与第 4 条的结合：用临时代码验证功能，但最终交付物保持 1:1）。

---

## 第 1 章：环境准备（搭地基）

### 1.1 必装软件

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| **JDK** | **21**（必须，不能低） | 项目用了 Java 21 的语法（文本块、switch 模式匹配），低版本编译不过 |
| **Maven** | 3.8+ | 项目构建工具，IDEA 一般自带 |
| **IntelliJ IDEA** | 2023+ | 社区版即可 |
| **Nacos** | **3.x**（需支持 AI/MCP 注册能力） | Agent 的“电话簿”，A2A 通信的核心 |

> 为什么 Nacos 要 3.x？因为本项目用的是 Nacos 的 **AI 服务注册**能力（`com.alibaba.nacos.api.ai.*`），这是 Nacos 3.0 之后才有的。装 2.x 会找不到这些类对应的服务端能力。

### 1.2 需要申请的 Key（不花钱也能拿到测试额度）

| Key | 用途 | 去哪申请 |
|-----|------|---------|
| **通义千问 API Key** | 大模型“大脑” | 阿里云百炼平台 [bailian.console.aliyun.com](https://bailian.console.aliyun.com) → API-KEY 管理，形如 `sk-xxxx` |
| **百度地图 MCP 地址（含 ak）** | 查路线数据 | 百度地图开放平台 [lbsyun.baidu.com](https://lbsyun.baidu.com) 创建应用拿到 `ak`，拼成 SSE 地址 |

### 1.3 可选（不影响主流程，先跳过也行）

- **LangFuse**：大模型调用链路追踪（监控用）。本教程会配上参数但留空，不影响运行。
- **bash 环境**：行程专家的“制表技能”要执行 `.sh` 脚本。Windows 上需装 **Git Bash** 或 **WSL**；不装的话，制表那一步会失败，但不影响整体跑通（第5章会说明）。

### ✅ 第 1 章验证（验证「外部依赖的功能」真的可用）

环境装好只是前提，本章真正要验证的是：**你申请的 Key、地图服务、Nacos 这些「外部能力」本身是好用的**。先把它们验通，后面写代码出问题时才能快速判断「是我代码的锅，还是外部服务的锅」。

**① 验证大模型真的能对话（最关键的功能验证）**

直接用 curl 调通义千问，不写一行 Java，先确认「这个 Key 真能让模型回话」：

```powershell
curl.exe -X POST "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" `
  -H "Authorization: Bearer 你的通义千问Key" `
  -H "Content-Type: application/json" `
  -d '{\"model\":\"qwen-plus\",\"messages\":[{\"role\":\"user\",\"content\":\"用一句话介绍你自己\"}]}'
```

✅ **功能达标**：返回的 JSON 里 `choices[0].message.content` 有模型的中文回答（例如“我是通义千问……”）。
❌ 如果返回 401/403 → Key 无效或没开通；返回模型不存在 → 换个模型名（如 `qwen-max`）。
> 这一步同时帮你确认 `.env` 里的 `MODEL_NAME`（项目用 `qwen3.7-max`）是否可用：把上面命令里的 `qwen-plus` 换成 `qwen3.7-max` 再发一次，能回话就说明项目配的模型名 OK；若报模型不存在，就把 `.env` 的 `MODEL_NAME` 改成这里能用的名字。

**② 验证百度地图 MCP 地址可达**

```powershell
curl.exe -N "你的BAIDU_MAP_ADDR地址（https://mcp.map.baidu.com/sse?ak=...）"
```

✅ **功能达标**：连接成功、开始返回 `event:`/`data:` 这样的 SSE 事件流（看到有数据流出就说明地址和 ak 有效），按 `Ctrl+C` 退出即可。
❌ 立刻断开或 401 → `ak` 无效，回百度地图开放平台检查。

**③ 验证 Nacos 真的起来了**

进入 Nacos 的 `bin` 目录启动：

```powershell
.\startup.cmd -m standalone
```

浏览器打开 **http://localhost:8848/nacos**，用 `nacos/nacos` 能登录进控制台 = 电话簿在线可用。

> ⚠️ 这三项任意一项不通，**先别往下写代码**——否则后面 Agent 跑不起来时，你会分不清是代码问题还是这里没通。基础项（`java -version` 要是 21、`mvn -v` 正常）顺带确认一下即可。

---

## 第 2 章：创建父工程（管依赖 + 聚合模块）

### 2.1 这一步要干什么
Maven 多模块项目需要一个“**总管 pom**”：它自己不写业务代码，只干两件事——
1. **聚合**：声明它下面有哪几个子模块；
2. **依赖管理**：把大家公用的依赖版本、公共依赖统一定义在这里，子模块直接继承，不用各写各的。

### 2.2 动手

1. 新建一个空文件夹 `AiTripPlan-AgentScope`，用 IDEA 打开。
2. 在根目录新建文件 `pom.xml`，内容如下（**完整复制**）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.imooc</groupId>
    <artifactId>AiTripPlan</artifactId>
    <packaging>pom</packaging>
    <version>1.0-SNAPSHOT</version>
    <modules>
        <!--   主管Agent 负责整体执行计划的制定    -->
        <module>manager_agent</module>
        <!--   路线制定Agent     -->
        <module>routeMaking_agent</module>
        <!--   行程规划Agent     -->
        <module>tripPlanner_agent</module>
        <!--   公共模块    -->
        <module>commons</module>
    </modules>

    <properties>

        <!--  SpringBoot 4      -->
        <spring-boot.version>4.0.2</spring-boot.version>
        <!--   AgentScope     -->
        <AgentScope.version>1.0.8</AgentScope.version>
        <logback.version>1.5.25</logback.version>

        <!--   Opentelemetry Reactor   -->
        <opentelemetry-reactor.version>2.26.0-alpha</opentelemetry-reactor.version>

        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <groupId>io.agentscope</groupId>
                <artifactId>agentscope-a2a-spring-boot-starter</artifactId>
                <version>${AgentScope.version}</version>
            </dependency>

        </dependencies>
    </dependencyManagement>

    <dependencies>
<!--   AgentScope 和 SpringBoot 的集成     -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-spring-boot-starter</artifactId>
            <version>${AgentScope.version}</version>
        </dependency>

        <!--   实现slf4j接口，不然日志打印不出来    -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

<!--   以 SpringBoot 方式添加 A2A 依赖     -->
<!--        <dependency>-->
<!--            <groupId>io.agentscope</groupId>-->
<!--            <artifactId>agentscope-a2a-spring-boot-starter</artifactId>-->
<!--            <version>${AgentScope.version}</version>-->
<!--        </dependency>-->

        <!-- 额外添加 Nacos Spring Boot starter 的依赖 -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-nacos-spring-boot-starter</artifactId>
            <version>${AgentScope.version}</version>
        </dependency>

<!-- ======= Opentelemetry集成LangFuse 进行Agent数据观测 ====== -->
<!-- 课程视频说错了, 应该导入下面3个依赖 -->

<!--  SpanExporter 依赖      -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk-trace</artifactId>
        </dependency>
<!--   OtlpHttpSpanExporter 依赖    -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-otlp</artifactId>
        </dependency>
<!--   reactor-V3 依赖     -->
        <dependency>
            <groupId>io.opentelemetry.instrumentation</groupId>
            <artifactId>opentelemetry-reactor-3.1</artifactId>
            <version>${opentelemetry-reactor.version}</version>
            <scope>runtime</scope>
        </dependency>



    </dependencies>
    


</project>
```

### 2.3 逐段讲人话

- `<packaging>pom</packaging>`：告诉 Maven“我是个管理者，不产出 jar”。
- `<modules>`：声明 4 个子模块。**此刻这些文件夹还不存在**，IDEA 会标红——正常，等后面章节建好就好了。
- `<properties>` 里的版本号：**SpringBoot 4.0.2 + AgentScope 1.0.8 + JDK 21**，这是项目的“地基版本”，别乱改。
- `<dependencyManagement>`：只“**声明版本**”不“引入”。这里 import 了 SpringBoot 的 BOM（一份庞大的版本清单），这样子模块用 spring 相关依赖时**不用写版本号**。
- `<dependencies>`（注意这个是直接引入，不在 management 里）：这里放的依赖会被**所有子模块自动继承**。这就是为什么后面 `commons` 的 pom 几乎是空的——公共依赖（AgentScope、logback、lombok、web、nacos、opentelemetry）全在这里一次性给齐了。

> 💡 关键认知：**父 pom 的 `<dependencies>` = 所有子模块都有的东西**。理解这点，后面看到子模块 pom 很短就不会困惑。

### ✅ 第 2 章验证

> 说句实话：父工程是**纯依赖管理，没有任何可运行的功能**，所以这里没法做「功能验证」。它的作用是为后面所有功能**备齐弹药（依赖）**。本章我们只确认一件和功能相关的事：**AgentScope 这些依赖能不能真的拉下来**——拉不到，后面大模型、Agent 功能就全是空谈。真正的功能验证从**第 3 章末尾「让 commons 驱动大模型说第一句话」**开始。

在根目录运行，确认依赖能被解析下载：

```powershell
mvn -N validate
mvn dependency:tree -N
```

- 第一条看到 **`BUILD SUCCESS`**；第二条能打印出依赖树、其中包含 `io.agentscope:agentscope-spring-boot-starter` 等条目，说明依赖**真的下载到本地了**（这才是功能的前提）。
- IDEA 右侧 Maven 面板能看到 `AiTripPlan` 父工程（子模块此刻标红正常，下一章就建）。

> 如果报错找不到 `agentscope-*` 依赖：检查网络，或确认 Maven 能访问公共仓库（AgentScope 在 Maven 中央仓库有发布）。依赖拉不下来，请先解决，否则后续章节寸步难行。

---

## 第 3 章：commons 公共模块（公共工具箱）

### 3.1 这一步要干什么
`commons` 不是一个能启动的程序，它是个**代码库（jar）**，把三个 Agent 都要用的东西集中放这里：读配置、构建 Prompt、创建 Agent、注册工具……建好后用 `mvn install` 装到本地仓库，别的模块就能依赖它。

**本章建 10 个文件**（1 个 pom + 1 个 .env + 8 个 Java 类）。建议按下面顺序建，因为后面的类会用到前面的。

### 3.2 commons/pom.xml

在根目录新建文件夹 `commons`，里面建 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>AiTripPlan</artifactId>
        <groupId>com.imooc</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>commons</artifactId>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

</project>
```

> 看到没？**几乎是空的**。因为它认了父工程（`<parent>`），所有公共依赖都从父工程继承了（回顾第 2.3 节）。它只需声明“我叫 commons”。

### 3.3 .env（密钥配置，放在项目根目录）

在**项目根目录**（不是 commons 里）新建 `.env` 文件。这里集中放所有密钥，好处是密钥不写死在代码里，且可以 `.gitignore` 掉不上传。

```properties
ALIBABA_DASHCOPE_KEY=你的通义千问Key（形如sk-xxxx）
BAIDU_MAP_ADDR=https://mcp.map.baidu.com/sse?ak=你的百度地图ak
MODEL_NAME=qwen3.7-max
LANGFUSE_ADDR=http://127.0.0.1:3000
LANGFUSE_SECRET_KEY=your LANGFUSE_SECRET_KEY
LANGFUSE_PUBLIC_KEY=your LANGFUSE_PUBLIC_KEY
```

> ⚠️ 几个坑：
> 1. 变量名 `ALIBABA_DASHCOPE_KEY` 拼写少了个 S（原项目就这么写的），**保持原样**，因为后面 yml 里也是这么引用的，改了就对不上。
> 2. `MODEL_NAME=qwen3.7-max` 是模型名，如果这个名字将来失效，换成百炼平台上有效的千问模型名即可。
> 3. LangFuse 两个 Key 留着占位值不管它，不影响运行。

### 3.4 Properties.java —— 把配置读进程序

路径：`commons/src/main/java/com/imooc/commons/conf/Properties.java`

```java
package com.imooc.commons.conf;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

/**
 * author: Imooc
 * description: 配置类
 * date: 2026
 */

@Configuration
@Getter
public class Properties {

    //大模型名称
    @Value("${agent.model_name}")
    private String modelName;

    //阿里云DashScope Key
    @Value("${agent.alibaba_dashscope_key}")
    private String alibabaDashscopeKey;

    //百度地图MCP服务端地址
    @Value("${mcp.baidu_map_addr}")
    private String baiduMapAddr;

    //LangFuse 服务地址
    @Value("${spring.ai.observation.langfuse.endpoint}")
    private String endpoint;

    //LangFuse 私钥
    @Value("${spring.ai.observation.langfuse.secret-key}")
    private String secretKey;

    //LangFuse 公钥
    @Value("${spring.ai.observation.langfuse.public-key}")
    private String publicKey;
}
```

**讲人话**：
- `@Configuration`：让 Spring 把它当成一个配置 Bean，启动时自动创建。
- `@Value("${agent.model_name}")`：从 `application.yml` 里读 `agent.model_name` 的值塞进这个字段。而 yml 里这个值又是 `${MODEL_NAME}`，最终来自 `.env`。**这条链是：`.env` → `application.yml` → `Properties` 字段 → 代码里用**。
- `@Getter`（Lombok）：自动生成 `getModelName()` 等方法，省得手写。
- 顶部那一堆 import 大多没实际用到（原项目残留），保持原样不影响编译。

### 3.5 PromptSchema.java —— 用户请求的格式

路径：`commons/src/main/java/com/imooc/commons/data/PromptSchema.java`

```java
package com.imooc.commons.data;

import lombok.Getter;

/**
 * author: Imooc
 * description: 用户提交Prompt的Json格式
 * date: 2026
 */

@Getter
public class PromptSchema {

    private String prompt;
}
```

**讲人话**：用户 POST 过来的 JSON 是 `{"prompt": "帮我规划..."}`，Spring 会自动把它转成这个对象。就一个字段。

### 3.6 ResponseSchema.java —— 返回给用户的格式

路径：`commons/src/main/java/com/imooc/commons/data/ResponseSchema.java`

```java
package com.imooc.commons.data;

import org.springframework.context.annotation.Configuration;

/**
 * author: Imooc
 * description: 响应格式
 * date: 2026
 */

public class ResponseSchema {

    //LLM响应
    public String response;

    // 必须有无参构造函数
    public ResponseSchema(){}
}
```

**讲人话**：最终返回给用户的是 `{"response": "你的旅游计划是..."}`。`response` 用了 `public` 直接暴露，无参构造函数是序列化框架要求的，别删。

### 3.7 PromptUtils.java —— 把字符串包装成大模型消息

路径：`commons/src/main/java/com/imooc/commons/utils/PromptUtils.java`

```java
package com.imooc.commons.utils;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * author: Imooc
 * description: Prompt构建工具类
 * date: 2026
 */

@Slf4j
public class PromptUtils {

    public Msg getPrompt(String prompt)
    {

        log.info("====== 构建的Prompt ======");
        log.info(prompt);
        log.info("============");


        //Prompt
       return Msg.builder()
                //消息角色
                .role(MsgRole.USER)
                //消息内容 (Prompt)
                .content(List.of(
                        TextBlock.builder()
                                .text(prompt)
                                .build()
                ))
                .build();


    }
}
```

**讲人话**：大模型不收“裸字符串”，它收的是 `Msg`（消息）对象——要标明“**谁说的**（角色 USER）”和“**说了啥**（TextBlock 文本块）”。这个工具就干这个包装活。`@Slf4j`（Lombok）给你一个 `log` 对象用来打日志。

### 3.8 NacosUtil.java —— 连接电话簿

路径：`commons/src/main/java/com/imooc/commons/utils/NacosUtil.java`

```java
package com.imooc.commons.utils;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Properties;

/**
 * author: Imooc
 * description: Nacos 工具类
 * date: 2026
 */

public class NacosUtil {

    public static AiService getNacosClient() throws NacosException {

        // 设置 Nacos 地址
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, "localhost:8848");
        // 创建 Nacos Client
        return AiFactory.createAiService(properties);

    }
}
```

**讲人话**：返回一个连到 `localhost:8848`（你第1章启动的 Nacos）的客户端。主管 Agent 后面要靠它去“电话簿”里查下属 Agent 的地址。注意这里 import 的 `java.util.Properties` 是 JDK 自带的，跟上面我们写的 `com.imooc...Properties` 是**两个不同的类**，别搞混。

### 3.9 ⭐ AgentUtils.java —— 创建 Agent 的“大脑”（核心类，重点看）

路径：`commons/src/main/java/com/imooc/commons/utils/AgentUtils.java`

```java
package com.imooc.commons.utils;

import com.imooc.commons.conf.Properties;
import com.imooc.commons.data.ResponseSchema;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.StructuredOutputReminder;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static io.agentscope.core.message.GenerateReason.MODEL_STOP;

/**
 * author: Imooc
 * description: ReActAgent 工具类
 * date: 2026
 */

@Service
@Slf4j
public class AgentUtils {

    @Resource
    private Properties properties;


    //===========
    // 从配置中读取敏感资源, 不能使用static
    //=============



    /**
     * author: Imooc
     * description: 创建ReAct Agent Builder
     * @param name:
     * @param description:
     * @return io.agentscope.core.ReActAgent.Builder
     */
    public ReActAgent.Builder getReActAgentBuilder(
            String name,
            String description
    ) {


        String aliApiKey = properties.getAlibabaDashscopeKey();
        String modelName = properties.getModelName();
        log.info("============");
        log.info("Agent使用的大模型: "+modelName);
        log.info("============");


        return ReActAgent.builder()
                        .name(name)
                        .description(description)
                        //大模型配置
                        .model(
                                DashScopeChatModel.builder()
                                        //请求语言大模型的apikey
                                        .apiKey(aliApiKey)
                                        //所使用的语言大模型
                                        .modelName(modelName)
                                        //是否流式响应
                                        .stream(true)
                                        //开启思考模式
                                        .enableThinking(true)
                                        .build()
                        )
                        //智能体并发检查
                        .checkRunning(true)
                        //工具执行超时配置
                        .toolExecutionConfig(ExecutionConfig.builder()
                                //工具执行超时 3分钟
                                .timeout(Duration.ofSeconds(120))
                                //最大尝试次数
                                .maxAttempts(1)
                                .build())

                ;

    }

    /**
     * author: Imooc
     * description: ReAct Agent 流式响应
     * @param agent:
     * @param prompt:
     * @return reactor.core.publisher.Flux<io.agentscope.core.agent.Event>
     */
    public Flux<Event> streamResponse(
            AgentBase agent,
            String prompt) {

        String name = agent.getName();
        //Prompt工具
        PromptUtils promptUtils =  new PromptUtils();

        try {

            return agent.stream(
                    //构建Prompt
                    promptUtils.getPrompt(prompt)
                    ,
                    //流式响应配置
                    StreamOptions.defaults(),
                    //响应格式
                    ResponseSchema.class
            );

        }catch (Exception e) {
            log.error("===================");
            log.error(name+"  正在忙......");
            log.error(e.getMessage());
            log.error("===================");

            return null;
        }

    }


}
```

**讲人话（这是全项目最该懂的类）**：

它提供一个工厂方法 `getReActAgentBuilder(名字, 描述)`，返回一个**配好大脑、还没成型的 Agent 半成品（Builder）**。为什么返回半成品？因为每个 Agent 还要加自己的“个性”（系统提示词、工具、计划），所以这里只把**公共部分**配好，各 Agent 再 `.xxx().build()` 完成。

公共部分配了什么：
- `.model(...)`：给 Agent 装“大脑”——用阿里云通义千问（`DashScopeChatModel`）。
  - `.apiKey` / `.modelName`：从 `Properties`（最终来自 `.env`）读。
  - `.stream(true)`：流式输出（像打字机一样一段段出）。
  - `.enableThinking(true)`：开启思考模式，让模型先想再答，更聪明。
- `.checkRunning(true)`：并发检查，防止同一个 Agent 被同时跑两次出乱子。
- `.toolExecutionConfig(...)`：**就是你最初问的那个**——配置“Agent 调用工具时的规矩”：
  - `.timeout(Duration.ofSeconds(120))`：一个工具最多跑 **120 秒**，超时就掐断（防止卡死）。
  - `.maxAttempts(1)`：失败**只试 1 次**，不重试。
  - 👉 注意代码注释写的“3分钟”是**笔误**，`120秒` 其实是 **2 分钟**。网络不稳时可把 `maxAttempts` 调大到 2~3 增加成功率。

> 下面那个 `streamResponse` 方法是“流式调用”的封装（边算边返回）。本项目主流程实际走的是后面 ManagerAgent 里的 `.call().block()`（一次性返回），`streamResponse` 暂时用不到，但保留它，保证代码一模一样。

### 3.10 ToolUtils.java —— 给 Agent 装工具

路径：`commons/src/main/java/com/imooc/commons/utils/ToolUtils.java`

```java
package com.imooc.commons.utils;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * author: Imooc
 * description: Agent Tool 工具类
 * date: 2026
 */

@Slf4j
public class ToolUtils {

    private final Toolkit toolkit ;

    public ToolUtils() {
        //创建工具包
        toolkit = new Toolkit();
    }

    /**
     * author: Imooc
     * description: 获取工具包
     * @param tool:
     * @return io.agentscope.core.tool.Toolkit
     */
    public Toolkit getToolkit(Object tool) {

        //把工具添加到工具包，能自动扫描@Tool所注释的方法，作为Agent的工具
        toolkit.registerTool(tool);

        return toolkit;
    }

    /**
     * author: Imooc
     * description: 获取工具包
     * @param mcp:
     * @return io.agentscope.core.tool.Toolkit
     */
    public Toolkit getToolkit(McpClientWrapper mcp) {

        //把MCP服务端的所有工具添加到工具包
        toolkit.registerMcpClient(mcp).block();

        return toolkit;
    }


    /**
     * author: Imooc
     * description: 获取工具包
     * @param agent:
     * @return io.agentscope.core.tool.Toolkit
     */
    public Toolkit getToolkit(ReActAgent agent) {

        //将智能体(子Agent)作为工具
        toolkit.registration().subAgent(
                ()->agent
        ).apply();

        return toolkit;
    }


    public void getTools() {

        // 获取所有工具信息
        log.info("========= 已加载的工具 ==========");
        List<ToolSchema> tools = toolkit.getToolSchemas();

        for (ToolSchema tool : tools) {
            log.info("工具: " + tool.getName());
            log.info("描述: " + tool.getDescription());
//            log.info("参数: " + tool.getParameters());
            log.info("---------------");
        }
        log.info("===================");
    }
}
```

**讲人话**：`Toolkit`（工具包）就是 Agent 能用的工具集合。这个类提供了**三种往工具包里塞工具的方式**（方法重载，名字都叫 `getToolkit`，靠参数类型区分）：
1. `getToolkit(Object tool)`：塞一个普通 Java 对象，框架自动扫描它里面带 `@Tool` 注解的方法当工具（主管用，把“远程调用”封成工具）。
2. `getToolkit(McpClientWrapper mcp)`：把一个 MCP 服务端的所有工具一次性塞进来（路线专家用，接百度地图）。
3. `getToolkit(ReActAgent agent)`：**把另一个 Agent 当成工具**塞进来（行程专家用，把景点子专家当工具）——这就是“父子 Agent 嵌套”。

`getTools()`：把工具包里有哪些工具打印出来，方便你启动时在日志里确认“工具装上了没”。

### 3.11 LangFuseUtils.java —— 监控（可选功能）

路径：`commons/src/main/java/com/imooc/commons/utils/LangFuseUtils.java`

```java
package com.imooc.commons.utils;

import com.imooc.commons.conf.Properties;
import io.agentscope.core.tracing.TracerRegistry;
import io.agentscope.core.tracing.telemetry.TelemetryTracer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * author: Imooc
 * description: LangFuse Agent观测 工具类
 * date: 2026
 */

@Service
@Slf4j
public class LangFuseUtils {


    @Resource
    private Properties properties;

    //初始化 LangFuse Agent观测
    public TelemetryTracer initLangfuseTracing() {

        String endpoint = properties.getEndpoint();
        String authHeader = getAuthHeader();

        // 创建TelemetryTracer并配置Langfuse
        TelemetryTracer langfuseTracer = TelemetryTracer.builder()
                .endpoint(endpoint)
                .addHeader("Authorization", authHeader)
                .build();

        return langfuseTracer;

    }

    /**
     * author: Imooc
     * description: 构建LangFuse的认证头
     * @param :
     * @return java.lang.String
     */
    private String getAuthHeader() {

        String publicKey = properties.getPublicKey();
        String secretKey = properties.getSecretKey();

        String credentials = publicKey + ":" + secretKey;
        String authHeader = "Basic " +
                Base64.getEncoder().encodeToString(credentials.getBytes());

        return authHeader;

    }
}
```

**讲人话**：LangFuse 是个“给大模型调用录像”的监控平台。这个类构造连接它的凭证（公钥:私钥 用 Base64 编码成认证头）。**本项目主流程没真正启用它**（ManagerAgent 里相关代码没调用），所以你 LangFuse 没装也能跑。建好这个文件只为“代码一模一样”。

### ✅ 第 3 章验证（让 commons 的依赖真的驱动大模型说出第一句话）

commons 是个库、没有 main，但我们要验证的不是「它能不能装进仓库」，而是「**用它引入的 AgentScope 依赖，能不能在 Java 里真正驱动大模型对话**」。所以先安装，再用一段临时代码触发真实对话。

**第 1 步：安装到本地仓库（功能的前提）**

```powershell
mvn -pl commons -am clean install -DskipTests
```
看到 `BUILD SUCCESS` 即可（本地仓库 `~/.m2/repository/com/imooc/commons/1.0-SNAPSHOT/` 下会出现 jar）。

**第 2 步：写一段临时代码，让模型真的回话**

在 `commons/src/main/java/com/imooc/commons/` 下临时新建 `QuickModelTest.java`：

```java
// 【临时验证代码：验证后请删除整个文件，它不属于原项目】
package com.imooc.commons;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;

import java.util.List;

public class QuickModelTest {
    public static void main(String[] args) {
        // 临时把你的真实 Key 填这里（验证完连同本文件一起删除，别提交 Git）
        String apiKey = "你的通义千问Key";
        // 先用通用名验证连通；通了再换成 .env 里的 qwen3.7-max 复测一次
        String modelName = "qwen-plus";

        ReActAgent agent = ReActAgent.builder()
                .name("QuickTest")
                .description("临时验证")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .stream(true)
                        .enableThinking(true)
                        .build())
                .build();

        Msg reply = agent.call(List.of(
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(List.of(TextBlock.builder().text("用一句话介绍你自己").build()))
                        .build()
        )).block();

        System.out.println("==== 大模型功能验证回答 ====");
        System.out.println(reply == null ? "无返回（功能未通）" : reply.getTextContent());
        System.out.println("===========================");
    }
}
```

右键 `Run 'QuickModelTest.main()'`。

✅ **功能达标**：控制台 `==== 大模型功能验证回答 ====` 下面打印出一句**大模型真实生成的中文自我介绍**。这一刻证明了：你引入的 `agentscope` + `DashScopeChatModel` 依赖能在代码里**真正驱动大模型**，`ReActAgent` 的基础对话功能跑通了——这正是整个项目的地基。
❌ 报鉴权错 → Key/模型名问题（回第 1 章用 curl 复核）；报类找不到 → commons 没 install 成功。

> 🔁 **验证完，删除 `QuickModelTest.java`**。它没经过 Spring、也没用到 `AgentUtils`（`AgentUtils` 需要 Spring 注入 `Properties`），只是用来证明「依赖整合 + 大模型对话」这条命脉是通的。`AgentUtils`/`Properties` 的完整功能，会在**第 4 章第一个真正的 Spring Boot Agent 跑起来时**被自然验证。

---

## 第 4 章：routeMaking_agent 路线制定 Agent

### 4.1 这一步要干什么
做第一个**能独立启动的 Agent**：路线专家。它启动后会①连百度地图 MCP 拿到“查路线”的能力，②把自己注册进 Nacos 电话簿等着主管来叫。

> 为什么先做它、不先做主管？因为主管启动时要去 Nacos 找下属，**下属得先在线**。所以专家先建、先启动。

**本章建 5 个文件**：pom + 启动类 + BaiduMapMCP + RouteMakingAgent + 两个资源文件。

### 4.2 routeMaking_agent/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>AiTripPlan</artifactId>
        <groupId>com.imooc</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>routeMaking_agent</artifactId>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!--    公共模块    -->
        <dependency>
            <groupId>com.imooc</groupId>
            <artifactId>commons</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

        <!--   以 SpringBoot 方式添加 A2A 依赖     -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-a2a-spring-boot-starter</artifactId>
        </dependency>

    </dependencies>


    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>4.0.3</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

**讲人话**：
- 依赖 `commons`（用第3章那些工具类）。
- 依赖 `agentscope-a2a-spring-boot-starter`：**A2A 服务端能力**——让这个 Agent 能被别人远程调用，并自动注册到 Nacos。注意这里**没写版本号**，因为父工程的 `<dependencyManagement>` 已经管好了版本（回顾 2.3）。
- `spring-boot-maven-plugin`：能启动的程序才需要它（commons 就没有），它负责把程序打成可运行的 jar。

### 4.3 RouteMakingAgentApplication.java —— 启动类

路径：`routeMaking_agent/src/main/java/com/imooc/routeMakingAgent/RouteMakingAgentApplication.java`

```java
package com.imooc.routeMakingAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * author: Imooc
 * description: 启动类
 * date: 2026
 */

@SpringBootApplication(scanBasePackages = {"com.imooc"})
public class RouteMakingAgentApplication {
    public static void main(String[] args) {

        SpringApplication.run(RouteMakingAgentApplication.class, args);
    }
}
```

**讲人话**：标准 Spring Boot 启动类。`scanBasePackages = {"com.imooc"}` 很关键——它让 Spring 不仅扫描本模块，还扫描 `commons` 模块里的 `com.imooc.commons.*`（否则 `AgentUtils` 这些 `@Service` 不会被加载）。

### 4.4 ⭐ BaiduMapMCP.java —— 接入百度地图（MCP 核心）

路径：`routeMaking_agent/src/main/java/com/imooc/routeMakingAgent/mcp/BaiduMapMCP.java`

```java
package com.imooc.routeMakingAgent.mcp;

import com.imooc.commons.conf.Properties;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.util.Optional;

/**
 * author: Imooc
 * description: baidu Map MCP Server
 * date: 2026
 */

@Component
@Slf4j
public class BaiduMapMCP {

    @Resource
    private Properties properties;

    //MCP 客户端
    private McpClientWrapper baiduMapMCP = null;
    //MCP 客户端初始化
    private boolean mcpInitialized = false;

    /**
     * author: Imooc
     * description: 创建百度地图MCP客户端
     * @param :
     * @return void
     */
    public McpClientWrapper getBaiduMapMCP() {


        //创建MCP客户端
        McpClientWrapper baiduMapMCP = McpClientBuilder.create("BaiduMap-mcp")
                //和MCP Server以SSE方式进行通信
                .sseTransport(properties.getBaiduMapAddr())
                //请求超时
                .timeout(Duration.ofSeconds(120))
                //异步请求
                .buildAsync()
                .block();

        return baiduMapMCP;


    }



    /**
     * author: Imooc
     * description: 初始化百度地图MCP客户端
     * @param :
     * @return io.agentscope.core.tool.mcp.McpClientWrapper
     */
    public McpClientWrapper initBaiduMapMCP(McpClientWrapper baiduMapMCP) {

        //通过Optional判断百度MCP客户端是否为null
        Optional<McpClientWrapper> mcpClientWrapper = Optional.ofNullable(baiduMapMCP);
        if(mcpClientWrapper.isPresent()) {
            log.info("==================");
            log.info("百度MCP客户端已经创建");
            log.info("==================");


            if(!mcpInitialized) {
                synchronized (this) {
                    if (!mcpInitialized) {

                        //MCP客户端初始化
                        baiduMapMCP.initialize().block();

                        //获取MCP服务端工具列表
                        if(baiduMapMCP.isInitialized()) {

                            log.info("=============");
                            log.info("百度地图MCP 客户端初始化成功！");
                            log.info("=============");

                            mcpInitialized=true;
                        }

                    }
                }
            }

        }

        return baiduMapMCP;

    }

}
```

**讲人话（MCP 是本章重点）**：

MCP 协议让大模型“即插即用”地接外部工具，**不用你手写一行调用百度地图的 HTTP 代码**。这里两个方法：
- `getBaiduMapMCP()`：**创建**一个 MCP 客户端，用 SSE 方式连到百度地图的 MCP 服务器（地址来自 `.env` 的 `BAIDU_MAP_ADDR`），超时 120 秒。
- `initBaiduMapMCP(...)`：**初始化**这个客户端（握手、拉取对方提供的工具列表）。里面用了 **双重检查锁（double-checked locking）**——`if(!mcpInitialized)` 套 `synchronized` 再套一层 `if`——保证多线程下只初始化一次，不重复连接。初始化成功会打印“百度地图MCP 客户端初始化成功！”（这是你后面验证的关键日志）。

### 4.5 RouteMakingAgent.java —— 路线 Agent 本体

路径：`routeMaking_agent/src/main/java/com/imooc/routeMakingAgent/agents/RouteMakingAgent.java`

```java
package com.imooc.routeMakingAgent.agents;

import com.imooc.commons.utils.AgentUtils;
import com.imooc.routeMakingAgent.mcp.BaiduMapMCP;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.imooc.commons.utils.ToolUtils;

/**
 * author: Imooc
 * description: 路线制定Agent
 * date: 2026
 */

@Component
@Slf4j
public class RouteMakingAgent {

    @Resource
    private AgentUtils agentUtils;

    @Resource
    private BaiduMapMCP mcp;


    @Bean
    public ReActAgent getRouteMakingAgent() {

        //创建百度地图MCP客户端
        McpClientWrapper baiduMapMCP = mcp.getBaiduMapMCP();
        //初始化百度地图MCP客户端
        McpClientWrapper mcpClient = mcp.initBaiduMapMCP(baiduMapMCP);

        //Toolkit
        ToolUtils toolUtils = new ToolUtils();
        //注册MCP
        Toolkit toolkit = toolUtils.getToolkit(mcpClient);
        // 打印所有工具信息
        toolUtils.getTools();


        //注入到Nacos
        return agentUtils.getReActAgentBuilder(
                "RouteMakingAgent",
                "擅长制定性价比最优的驾车路线"
        )
                .sysPrompt("""
                    你是一个驾车路线制定助手。
                    请调用合适的API接口，
                    制定包括行程距离，高速费用这些方面，
                    性价比最优的路线方案。
                    """)
                //工具包
                .toolkit(toolkit)
                .build();
    }


}
```

**讲人话**：
- 流程：创建 MCP 客户端 → 初始化 → 把它的工具塞进 `toolkit` → 打印工具 → 用 `AgentUtils`（第3.9节那个工厂）拿到半成品 Builder → 加上**系统提示词**（告诉模型“你是路线助手，要算距离和高速费”）→ 装上工具包 → `.build()` 成型。
- `@Bean` 是关键：方法返回的这个 `ReActAgent` 会被注册成 Spring Bean，而 **A2A starter 会自动把这个 Bean 形式的 Agent 注册到 Nacos**（结合 application.yml 里 `agentscope.a2a.server.enabled=true`）。所以注释写“注入到Nacos”。
- `.sysPrompt(""" ... """)`：用了 Java 21 的**文本块**（三引号），可以多行写提示词，不用拼 `\n`。

### 4.6 application.yml

路径：`routeMaking_agent/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  config:
    import: optional:file:.env[.properties]
  ai:
    # LangFuse Agent数据观测
    observation:
      langfuse:
        # LangFuse 服务地址
        endpoint: ${LANGFUSE_ADDR}
        # 私钥
        secret-key: ${LANGFUSE_SECRET_KEY}
        # 公钥
        public-key: ${LANGFUSE_PUBLIC_KEY}
        # 采样率（生产环境建议0.1-0.5）
        sampling-rate: 1.0

# A2A协议服务端
agentscope:
  a2a:
    server:
      enabled: true
    nacos:
      server-addr: localhost:8848


agent:
  model_name: ${MODEL_NAME}
  alibaba_dashscope_key: ${ALIBABA_DASHCOPE_KEY}

mcp:
  baidu_map_addr: ${BAIDU_MAP_ADDR}
```

**讲人话**：
- `server.port: 8082`：路线专家占 8082 端口。
- `spring.config.import: optional:file:.env[.properties]`：**把根目录的 `.env` 当配置文件读进来**，于是下面的 `${MODEL_NAME}` 这些就能取到 .env 里的值。
- `agentscope.a2a.server.enabled: true` + `nacos.server-addr: localhost:8848`：开启 A2A 服务端，并指定注册到本地 Nacos。**这是它能被主管远程调用的开关。**
- `agent.*` / `mcp.*`：对应第3.4节 `Properties` 类里 `@Value` 读取的那些键。

> ⚠️ **关于 Key 的重要说明**：现有项目的 routeMaking_agent 这个 yml 里，`alibaba_dashscope_key` 和 `baidu_map_addr` 是**直接写死了真实 Key**的（没用 `${...}`）。本教程统一改成 `${...}` 占位、从 `.env` 读取——这样更安全、更规范，效果完全一样。**强烈建议你也用 `${...}` 写法**，不要把真实 Key 写死在 yml 里。如果你执意要 100% 字符级复刻原文件，把这两行换成你自己的真实 Key 即可，但别提交到 Git。

### 4.7 logback.xml

路径：`routeMaking_agent/src/main/resources/logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration  scan="true" scanPeriod="10 seconds">
    <contextName>logback</contextName>


    <!-- 输出到控制台-->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>
        </filter>
        <encoder>
            <Pattern>%date{yyyy} [%level] %msg%n</Pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>


    <root level="info">
        <appender-ref ref="CONSOLE" />
    </root>

</configuration>
```

**讲人话**：日志配置，让 `log.info(...)` 的内容以 UTF-8 打到控制台（中文不乱码），级别 info。**三个 Agent 模块的 logback.xml 内容完全一样**，第5、6章直接复制这份即可。

### ✅ 第 4 章验证（让路线 Agent 真的算出一条带里程和过路费的路线）

本章的功能验证目标：**路线 Agent 真的能调百度地图、算出深圳到惠州的具体里程和过路费**——这才证明 MCP 接通了、AgentUtils 造的 Agent 真能干活。光看到 `Tomcat started` 不算数。

**第 1 步：前提确认**（启动起来 + 外部都在线）
- 确认 Nacos 在线、`.env` Key 已填、`mvn -pl commons -am clean install -DskipTests` 已执行。
- 先别急着验证，留意启动日志里应出现 `百度地图MCP 客户端初始化成功！` 和 `========= 已加载的工具 ==========`（后面跟着百度地图的工具名）——这说明 MCP 这个「能力来源」挂上了。

**第 2 步：写临时代码，让 Agent 真的跑一次路线规划**

在 `routeMaking_agent/src/main/java/com/imooc/routeMakingAgent/` 下临时新建 `RouteFunctionTest.java`：

```java
// 【临时验证代码：验证后请删除整个文件，它不属于原项目】
package com.imooc.routeMakingAgent;

import com.imooc.commons.utils.PromptUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteFunctionTest implements CommandLineRunner {

    // 注入第 4.5 节 @Bean 方法产出的路线 Agent（Bean 名就是方法名 getRouteMakingAgent）
    @Resource(name = "getRouteMakingAgent")
    private ReActAgent routeAgent;

    @Override
    public void run(String... args) {
        PromptUtils promptUtils = new PromptUtils();
        Msg reply = routeAgent.call(List.of(
                promptUtils.getPrompt("帮我规划从深圳市到惠州市的自驾路线，给出大致里程和高速过路费")
        )).block();

        System.out.println("======= 路线Agent功能验证回答 =======");
        System.out.println(reply == null ? "无返回" : reply.getTextContent());
        System.out.println("====================================");
    }
}
```

`CommandLineRunner` 会在 Spring 启动完成后**自动执行一次**。运行 `RouteMakingAgentApplication`。

✅ **功能达标**：控制台 `======= 路线Agent功能验证回答 =======` 下面打印出一段路线方案，**里面有具体的里程数（约 xx 公里）和过路费金额（约 xx 元）**。有具体数字 = 它真的调用了百度地图 MCP 拿真实数据，而不是模型凭空编。过程中你还能看到日志里 Agent「想了一步 → 调用地图工具 → 拿到结果」。
❌ 只有泛泛而谈、没有任何数字 → MCP 可能没真正生效（回看启动日志里 MCP 是否初始化成功）。

**第 3 步：确认它已上线登记**
去 Nacos 控制台（http://localhost:8848/nacos）的服务/AI 注册列表，能找到 `RouteMakingAgent` —— 说明它已经准备好被主管远程调用了。

> 🔁 **验证完，删除 `RouteFunctionTest.java`**（原项目没有它）。
> 排查：MCP 初始化失败/SSE 报错 → `BAIDU_MAP_ADDR` 的 `ak` 或网络；模型 401 → 通义千问 Key；Nacos 看不到 → 检查 yml 的 `agentscope.a2a.server.enabled: true`、`nacos.server-addr`，以及 Nacos 是否 3.x。

---

## 第 5 章：tripPlanner_agent 行程规划 Agent

### 5.1 这一步要干什么
做第二个专家：行程专家。它有个高级玩法——**父子 Agent 嵌套**：行程专家（父）自己不查景点，而是把一个“景点推荐专家”（子 `SuggestSightAgent`）当成工具挂在身上。子专家还会用**技能（Skill）+ 执行脚本**来制表。

**本章建 9 个文件**：pom + 启动类 + 两个 Agent + 3 个 skill 文件 + 两个资源文件。

### 5.2 tripPlanner_agent/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>AiTripPlan</artifactId>
        <groupId>com.imooc</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>tripPlanner_agent</artifactId>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!--    公共模块    -->
        <dependency>
            <groupId>com.imooc</groupId>
            <artifactId>commons</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

        <!--   以 SpringBoot 方式添加 A2A 依赖     -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-a2a-spring-boot-starter</artifactId>
        </dependency>

    </dependencies>


    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>4.0.3</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

> 和路线专家的 pom **几乎一模一样**，只有 `artifactId` 不同（`tripPlanner_agent`）。因为它俩定位一样：依赖 commons + A2A，是个能启动并注册的专家。

### 5.3 TripPlannerAgentApplication.java —— 启动类

路径：`tripPlanner_agent/src/main/java/com/imooc/tripPlannerAgent/TripPlannerAgentApplication.java`

```java
package com.imooc.tripPlannerAgent;

import com.imooc.tripPlannerAgent.agents.TripPlannerAgent;
import io.agentscope.core.ReActAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * author: Imooc
 * description: 启动类
 * date: 2026
 */

@SpringBootApplication(scanBasePackages = {"com.imooc"})
public class TripPlannerAgentApplication {
    public static void main(String[] args) {

        SpringApplication.run(TripPlannerAgentApplication.class, args);

    }
}
```

### 5.4 ⭐ SuggestSightAgent.java —— 景点推荐子专家（含代码执行 + 技能）

路径：`tripPlanner_agent/src/main/java/com/imooc/tripPlannerAgent/agents/SuggestSightAgent.java`

```java
package com.imooc.tripPlannerAgent.agents;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.util.JarSkillRepositoryAdapter;
import io.agentscope.core.tool.Toolkit;
import com.imooc.commons.utils.AgentUtils;
import io.agentscope.core.tool.coding.ShellCommandTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Set;

/**
 * author: Imooc
 * description: 景点推荐Agent
 * date: 2026
 */


@Component
@Slf4j
public class SuggestSightAgent {

    @Resource
    private AgentUtils agentUtils;

    //创建景点推荐Agent
    public ReActAgent getSuggestSightAgent() {


        Toolkit toolkit = new Toolkit();
        //构建Skill，并将工具包和Skill结合
        SkillBox skillBox = new SkillBox(toolkit);


        //====== 如果要用Python, 系统要安装好Python,以及放在在环境变量里 ==== //

        // 启用所有代码执行工具(Shell、读文件、写文件)
        skillBox.codeExecution()
                // 指定工作目录
                .workDir("./skill_file")
//                // 使用shell 工具
                .withShell(
                        new ShellCommandTool(
                                Set.of("bash")
                        ))
//                 启用文件读取
                .withRead()
                // 启用文件写入
                .withWrite()
                .enable();


        //以文件形式读取Skill.md
        JarSkillRepositoryAdapter repo = null;
        try {
            repo = new JarSkillRepositoryAdapter(
                    "skills"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //景点住宿推荐技能
        AgentSkill SuggestSightsSkill = repo.getSkill("Suggest-Sights");
        //表格制作技能
        AgentSkill tableSkill = repo.getSkill("Make-Table");
        skillBox.registerSkill(SuggestSightsSkill);

        log.info("========== 所有 Skills：===========");
        skillBox.getAllSkillIds().stream().forEach(item->log.info(item));
        log.info("==============================");


        return agentUtils.getReActAgentBuilder(
                        "SuggestSightAgent",
                        "专注于景点推荐的 SubAgent"
                )
                //挂载工具包
                .toolkit(toolkit)
                //挂载Skills
                .skillBox(skillBox)
                .build()

                ;

    }
}
```

**讲人话（本章重点之一）**：
- `SkillBox`（技能盒）= 工具包 + 一组技能。
- `skillBox.codeExecution()...enable()`：给这个子 Agent **开通“写代码并执行”的能力**——能跑 Shell（`bash`）、能读文件、能写文件，工作目录限定在 `./skill_file`。这意味着模型可以自己写脚本处理数据。
  - ⚠️ 它用的是 **bash**。Windows 上**必须装 Git Bash 或 WSL** 才能真正执行；否则到“制表”那步会报找不到 bash。但**不影响整体跑通**（景点推荐文字部分仍正常）。
- `JarSkillRepositoryAdapter("skills")`：从 `resources/skills` 目录加载技能。
- 加载了两个技能：`Suggest-Sights`（景点推荐手册）和 `Make-Table`（制表手册），但**只 `registerSkill` 注册了 `Suggest-Sights`**（原项目如此，`tableSkill` 取了却没注册，保持原样）。
- 最后用工厂建 Agent，挂上 `toolkit` 和 `skillBox`。**注意它没设 sysPrompt**——靠技能文件里的说明来指导行为。

### 5.5 TripPlannerAgent.java —— 行程专家（父，把子专家当工具）

路径：`tripPlanner_agent/src/main/java/com/imooc/tripPlannerAgent/agents/TripPlannerAgent.java`

```java
package com.imooc.tripPlannerAgent.agents;

import com.imooc.commons.data.ResponseSchema;
import com.imooc.commons.utils.PromptUtils;
import com.imooc.commons.utils.ToolUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.imooc.commons.utils.AgentUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;

/**
 * author: Imooc
 * description: 行程规划Agent
 * date: 2026
 */

@Component
@Slf4j
public class TripPlannerAgent {

    @Resource
    private AgentUtils agentUtils;

    //景点推荐SubAgent
    @Resource
    private SuggestSightAgent suggestSightAgent;

    @Bean
    public ReActAgent getTripPlannerAgent() throws URISyntaxException, IOException {


        ToolUtils toolUtils = new ToolUtils();
        //将智能体(子Agent)作为工具
        Toolkit toolkit = toolUtils.getToolkit(suggestSightAgent.getSuggestSightAgent());
        // 打印所有工具信息
        toolUtils.getTools();



        /* **********************
         *
         * 1.
         * AgentScope框架自带了注册中心： AgentScopeA2aServer
         *
         * 2.
         * AgentScope框架将智能体卡片注册到注册中心,有2种方案：
         * a. 通过SpringBoot, 以Bean的形式自动注入
         * b. 手动写入注册中心, 主要针对于AgentScopeA2aServer
         *
         *
         * *********************/




        //行程规划Agent
        return agentUtils.getReActAgentBuilder(
                "TripPlannerAgent",
                "擅长处理旅游行程规划"
        )
                .sysPrompt(
                    """
                    你是一个旅游行程规划助手。
                    制定包括旅游景点，小吃，住宿这些方面，
                    并且费用性价比高的旅游行程。
                    """)
                //挂载工具包
                .toolkit(toolkit)
                .build();




        //=========== 手动写入注册中心，项目不用种方式 START ====

//        //行程规划Agent 智能体卡片
//        ConfigurableAgentCard agentCard =  new ConfigurableAgentCard.Builder()
//                .name("TripPlannerAgent")
//                .description("行程规划Agent")
//                .build();
//
//        //将智能体卡片写入到AgentScope自带的注册中心
//        AgentScopeA2aServer.builder(builder)
//                .agentCard(agentCard)
//                .deploymentProperties(
//                       new DeploymentProperties(
//                               "localhost",
//                               8080)
//                )
//                .build();

        //还需要AgentScopeA2aServer启动


        //======== 手动写入注册中心，项目不用种方式 END ====


    }


}
```

**讲人话**：
- `@Resource private SuggestSightAgent suggestSightAgent;`：把子专家注入进来。
- `toolUtils.getToolkit(suggestSightAgent.getSuggestSightAgent())`：调用第3.10节**第三个重载**——**把子 Agent 当成一个工具**塞进工具包。于是行程专家在干活时，可以像调工具一样“喊”景点子专家来帮忙。
- `@Bean`：同样，这个返回的 Agent 会被 A2A starter 自动注册到 Nacos，名字 `TripPlannerAgent`，供主管调用。
- 下面那一大段被注释的代码是“手动注册到注册中心”的**另一种方案**，项目没用（用的是 Bean 自动注册）。保留注释是为了一模一样，也方便你理解“原来还有别的写法”。

### 5.6 技能文件（Skill）

技能就是放在 `resources/skills/` 下的说明书。建三个文件：

**① `tripPlanner_agent/src/main/resources/skills/Suggest-Sights/SKILL.md`**

```markdown
---
name: Suggest-Sights
description: 擅长有限预算内规划出精彩的旅行体验
---

## 核心能力

### 1. 免费景点推荐
- 推荐自然风光、历史古迹、特色街区
- 提供最佳游览时间和小众路线
- 分享避开人群的技巧

### 2. 美食探索

- 推荐当地特色小吃和地道餐厅
- 侧重性价比高的本地美食
- 分享当地人常去的隐瞒美食店

### 3. 打卡点推荐

- 推荐网红打卡点和拍照圣地
- 分享小众但出片的秘密景点

### 4. 住宿建议

提供住宿方案：
- **经济型**：青旅、民宿
- **舒适型**：连锁酒店

## 工作流程

1. **分析规划**：综合考虑距离、时间、费用、景点分布因素
2. **方案输出**：提供景点推荐、美食指南、住宿方案

## 回答原则

1. **务实优先**：所有建议要考虑实际可行性和经济性
2. **信息准确**：提供具体的地址、价格区间、开放时间
```

**② `tripPlanner_agent/src/main/resources/skills/Make-Table/SKILL.md`**

```markdown
---
name: Make-Table
description: 电子表格。
---

## 核心能力
1. 创建表格
2. 生成文件


## 核心指令

请严格按照以下步骤执行任务：

1. **获取生成的内容**：
    - 将生成的内容写入表格。

2. **执行脚本**：
    - **调用工具**：使用 `bash` 工具运行位于 `scripts/table.sh` 的脚本。
    - **参数传递**：将推荐的景点小吃住宿以及预估费用作为参数传递给脚本。
    - **命令示例**：
      ```bash
      sh scripts/table.sh "景点规划Agent推荐的景点小吃住宿以及预估费用"

      ```

3. **处理输出**：
    - 读取脚本的标准输出(stdout)。
    - 如果脚本执行成功，将输出的 JSON 数据格式化为易读的 Markdown 表格或列表展示给用户。
    - 如果脚本报错(stderr)，请将错误信息完整反馈给用户，并建议检查文件格式。

## 输出格式
- 格式为 Markdown文档。

## 错误处理
- 如果 `scripts/table.sh` 不存在或无法执行，请提示用户检查技能安装。
```

**③ `tripPlanner_agent/src/main/resources/skills/Make-Table/scripts/table.sh`**

```bash
#!/bin/bash

sudo echo $1  >> trip_plan.md
```

**讲人话**：
- 技能文件用 `---` 开头的 frontmatter 写元信息（`name`、`description`），下面是给模型看的“操作手册”。模型读到它就知道遇到这类任务该怎么做。
- `Make-Table` 技能指导模型去执行 `table.sh`，把内容追加写进 `trip_plan.md`。
- ⚠️ `table.sh` 里用了 `sudo`——这是为 Linux 写的，**Windows 上没有 `sudo`，这一步会失败**。这不影响整体规划流程（只是少了写文件那步），知道即可。要在 Windows 跑通这步，得用 WSL，或把脚本里的 `sudo` 去掉。

### 5.7 application.yml 和 logback.xml

**`tripPlanner_agent/src/main/resources/application.yml`**（注意端口是 **8085**）：

```yaml
server:
  port: 8085

spring:
  config:
    import: optional:file:.env[.properties]
  ai:
    # LangFuse Agent数据观测
    observation:
      langfuse:
        # LangFuse 服务地址
        endpoint: ${LANGFUSE_ADDR}
        # 私钥
        secret-key: ${LANGFUSE_SECRET_KEY}
        # 公钥
        public-key: ${LANGFUSE_PUBLIC_KEY}
        # 采样率（生产环境建议0.1-0.5）
        sampling-rate: 1.0

# A2A协议服务端
agentscope:
  a2a:
    server:
      enabled: true
    nacos:
      server-addr: localhost:8848


agent:
  model_name: ${MODEL_NAME}
  alibaba_dashscope_key: ${ALIBABA_DASHCOPE_KEY}

mcp:
  baidu_map_addr: ${BAIDU_MAP_ADDR}
```

**`tripPlanner_agent/src/main/resources/logback.xml`**（和第4.7节完全一样，直接复制）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration  scan="true" scanPeriod="10 seconds">
    <contextName>logback</contextName>


    <!-- 输出到控制台-->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>
        </filter>
        <encoder>
            <Pattern>%date{yyyy} [%level] %msg%n</Pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>


    <root level="info">
        <appender-ref ref="CONSOLE" />
    </root>

</configuration>
```

### ✅ 第 5 章验证（让行程 Agent 真的推荐出具体景点，并看到父子 Agent 协作）

功能验证目标：行程 Agent 真的能**给出惠州的具体景点/美食/住宿**，并且过程中**真的去喊了子 Agent（SuggestSightAgent）来帮忙**——这才证明「父子 Agent 嵌套」这个高级玩法生效了。

**第 1 步：前提确认**
启动 `TripPlannerAgentApplication`，留意日志里 `========== 所有 Skills：===========` 后打印出 `Suggest-Sights`、`========= 已加载的工具 ==========` 后出现子 Agent 作为工具（名字含 `SuggestSightAgent`）—— 说明技能和子 Agent 都挂上了。

**第 2 步：写临时代码，让它真的规划一次行程**

在 `tripPlanner_agent/src/main/java/com/imooc/tripPlannerAgent/` 下临时新建 `TripFunctionTest.java`：

```java
// 【临时验证代码：验证后请删除整个文件，它不属于原项目】
package com.imooc.tripPlannerAgent;

import com.imooc.commons.utils.PromptUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TripFunctionTest implements CommandLineRunner {

    // 注入第 5.5 节 @Bean 方法产出的行程 Agent（Bean 名就是方法名 getTripPlannerAgent）
    @Resource(name = "getTripPlannerAgent")
    private ReActAgent tripAgent;

    @Override
    public void run(String... args) {
        PromptUtils promptUtils = new PromptUtils();
        Msg reply = tripAgent.call(List.of(
                promptUtils.getPrompt("推荐惠州值得去的景点、当地特色美食和高性价比住宿")
        )).block();

        System.out.println("======= 行程Agent功能验证回答 =======");
        System.out.println(reply == null ? "无返回" : reply.getTextContent());
        System.out.println("====================================");
    }
}
```

运行 `TripPlannerAgentApplication`，它启动后会自动跑一次。

✅ **功能达标**：
1. 控制台打印出**具体的惠州景点（如惠州西湖、巽寮湾等）、美食、住宿建议**，而不是空话；
2. 日志中能看到行程父 Agent **调用了名为 `SuggestSightAgent` 的工具**（即它把活派给了子专家）——这就是父子 Agent 协作的实锤。
❌ 若完全没调子 Agent、或回答空洞 → 检查第 5.5 节 `toolkit` 是否正确挂上了子 Agent。

> ⚠️ Windows 注意：如果模型尝试用 `Make-Table` 技能执行 `table.sh`，因为没有 bash/sudo 会报错——**这不影响景点推荐文字结果的产出**，看到景点/美食/住宿建议就算功能达标。想跑通制表，装 Git Bash/WSL。

**第 3 步**：Nacos 控制台应新增 `TripPlannerAgent`。此时两个专家（8082、8085）都在线了。

> 🔁 **验证完，删除 `TripFunctionTest.java`**。排查：报 `skills` 找不到 → 检查目录 `src/main/resources/skills/` 及名字大小写 `Suggest-Sights`。

---

## 第 6 章：manager_agent 主管 Agent（中枢）

### 6.1 这一步要干什么
做**总指挥**。它对外提供 `/trip` 接口接用户请求，内部用 **PlanNotebook 拆任务**，再通过 **A2A + Nacos** 远程调用前面两个专家，最后汇总结果。这是把整个系统串起来的模块。

**本章建 8 个文件**：pom + 启动类 + Controller + ManagerAgent + RemoteAgentTool + TripPlan + planHook + 两个资源文件（共 9 个，logback 复用）。

### 6.2 manager_agent/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>AiTripPlan</artifactId>
        <groupId>com.imooc</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>manager_agent</artifactId>
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!--    公共模块    -->
        <dependency>
            <groupId>com.imooc</groupId>
            <artifactId>commons</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

</project>
```

**讲人话**：主管的 pom **只依赖 commons**，比两个专家还简单——它不需要 `a2a-spring-boot-starter`（那是“被别人调用”的服务端能力），主管是**主动方**，它要的“打电话给别人”的客户端能力通过 commons → 父工程的依赖（含 nacos starter / agentscope core）传递过来。也**没有** `spring-boot-maven-plugin`，所以主管用 IDEA 直接 Run 启动即可。

### 6.3 ManagerAgentApplication.java —— 启动类

路径：`manager_agent/src/main/java/com/imooc/managerAgent/ManagerAgentApplication.java`

```java
package com.imooc.managerAgent;

import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * author: Imooc
 * description: 启动类
 * date: 2026
 */

@SpringBootApplication(scanBasePackages = {"com.imooc"})
public class ManagerAgentApplication {
    public static void main(String[] args) throws NacosException
    {

        SpringApplication.run(ManagerAgentApplication.class, args);
    }
}
```

### 6.4 TripPlan.java —— 自定义“计划本”

路径：`manager_agent/src/main/java/com/imooc/managerAgent/plan/TripPlan.java`

```java
package com.imooc.managerAgent.plan;

import io.agentscope.core.plan.PlanNotebook;

/**
 * author: Imooc
 * description: 自定义 Agent自主分解旅游规划任务
 * date: 2026
 */

public class TripPlan {

    /**
     * author: Imooc
     * description: 自定义 PlanNotebook 实例
     * @param :
     * @return io.agentscope.core.plan.PlanNotebook
     */
    public PlanNotebook getPlan() {
        return PlanNotebook.builder()
                //计划步骤是否需要用户确认
                .needUserConfirm(false)
                //分解出来的子任务数量限制
                .maxSubtasks(5)
                //计划的存储方式
//                .storage()
                .build();
    }
}
```

**讲人话**：`PlanNotebook` 是让主管“自动把大任务拆成有序小步骤”的核心组件。这里自定义了两个参数：
- `needUserConfirm(false)`：拆完计划**不用等用户点确认**，直接执行（true 的话会卡住等输入）。
- `maxSubtasks(5)`：最多拆成 5 个子任务，防止拆太碎。

### 6.5 ⭐ planHook.java —— 计划拦截器（看 Agent “内心戏”）

路径：`manager_agent/src/main/java/com/imooc/managerAgent/hook/planHook.java`

> 注意类名是小写开头的 `planHook`（不符合 Java 规范，但原项目如此，**保持一模一样**，文件名也叫 `planHook.java`）。

```java
package com.imooc.managerAgent.hook;

import io.agentscope.core.agent.user.UserAgent;
import io.agentscope.core.hook.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.SubTask;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * author: Imooc
 * description:  计划拦截器
 * date: 2026
 */

@Slf4j
public class planHook implements Hook {

    //监听用户输入
    private final UserAgent user;
    //计划步骤
    private final PlanNotebook plan;

    //第n轮思考
    private int thinkingNum = 1;

    public planHook(PlanNotebook planNotebook) {
        this.user = UserAgent.builder()
                .name("User")
                .build();

        this.plan = planNotebook;

    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {

        /* **********************
         *
         * Hook 是对 HookEvent事件 的拦截
         * HookEvent事件：
         *
         * PreReasoningEvent：用户的输入事件
         * PostReasoningEvent: Agent推理思考过程事件
         * PreActingEvent： Agent执行过程准备调用工具的事件
         * PostActingEvent：Agent执行过程调用工具完成的事件
         *
         *
         * *********************/

        //匹配不同的事件
        switch (event) {

            //用户输入事件
            case PreReasoningEvent e -> {

//                String prompt = e.getInputMessages().get(0).getTextContent();
//                if(prompt != null) {
//                    log.info("========= 用户的Prompt：===========");
//                    log.info(prompt);
//                    log.info("=============================");
//                }

            }

            //推理思考事件
            case PostReasoningEvent e -> {

                String reason = e.getReasoningMessage().getTextContent();
                String agent = e.getReasoningMessage().getName();
                if(reason != null) {
                    log.info("=============="+agent+" 第 "+thinkingNum+" 轮思考：==================");
                    log.info(reason);
                    log.info("=============================");
                }

                thinkingNum++;

                //当计划列表已生成
//                Plan currentPlan = plan.getCurrentPlan();
//                if (currentPlan != null) {
//                    System.out.println("请输入修改意见: ");
//                    user.call().block();
//                }

            }


            //调用工具事件
            case PreActingEvent e -> {

                String toolName = e.getToolUse().getName();
                String agent = e.getAgent().getName();
                log.info("============"+agent+" 准备调用工具："+toolName+"=============");

            }

            //工具调用结果事件
            case PostActingEvent e -> {

                String res = e.getToolResultMsg().getTextContent();
                String tool = e.getToolUse().getName();
                String agent = e.getAgent().getName();

                //打印计划状态
                printPlanState(plan, "调用此工具：" +tool+"  的计划步骤");

                log.info("============"+agent+" 调用工具 "+tool+" 结果：=============");
                if(res !=null) {
                    log.info(res);
                }
                log.info("=========================");
            }


            default -> {
                // 其他事件忽略
            }
        }

        // 返回原事件
        return Mono.just(event);
    }



    /**
     * author: Imooc
     * description: 打印计划执行状态
     * @param notebook:
     * @param event:
     * @return void
     */
    private static void printPlanState(PlanNotebook notebook, String event) {
        Plan currentPlan = notebook.getCurrentPlan();
        if (currentPlan == null) {
            log.info(" [" + event + "] 没有需要执行的计划");
            return;
        }

        log.info("======= 已生成本轮思考后的执行计划: "+event+"  ==========");
        log.info("计划名称: " + currentPlan.getName());
        log.info("执行状态: " + currentPlan.getState());
        log.info("====子任务:============");

        for (int i = 0; i < currentPlan.getSubtasks().size(); i++) {
            SubTask subtask = currentPlan.getSubtasks().get(i);
            String icon =
                    switch (subtask.getState()) {
                        case TODO -> "⏸  ";
                        case IN_PROGRESS -> "▶️";
                        case DONE -> "✅ ";
                        case ABANDONED -> "❌ ";
                    };
            System.out.printf(
                    "  %s [%d] %s - %s%n", icon, i, subtask.getName(), subtask.getState());
        }
        log.info("======================");
    }
}
```

**讲人话（拦截器，理解 Agent 运行的好窗口）**：

`Hook` 能在 Agent 运行的关键节点“插一脚”，方便你观察/干预。`onEvent` 里用 Java 21 的 **switch 模式匹配**（`case PreReasoningEvent e ->`）按事件类型分别处理四种事件：
- `PreReasoningEvent`：模型**开始思考前**（用户输入到了）。
- `PostReasoningEvent`：模型**思考完一轮**——打印它“第 N 轮思考”的内容（你能看到 AI 的内心戏）。
- `PreActingEvent`：模型**决定调某个工具前**——打印“准备调用工具 xxx”。
- `PostActingEvent`：工具**调完拿到结果后**——打印计划当前状态（哪几步 ✅ 完成、哪步 ▶️ 进行中）和工具返回。

`printPlanState` 把计划本里每个子任务的状态用图标（⏸/▶️/✅/❌）打印出来，运行时你能清楚看到“计划执行到第几步了”。这个 Hook 主要是**日志可视化**用途，不改变流程（最后 `return Mono.just(event)` 原样放行）。

### 6.6 ⭐ RemoteAgentTool.java —— 把远程专家封装成“工具”

路径：`manager_agent/src/main/java/com/imooc/managerAgent/tool/RemoteAgentTool.java`

```java
package com.imooc.managerAgent.tool;

import com.alibaba.nacos.api.exception.NacosException;
import com.imooc.commons.utils.AgentUtils;
import com.imooc.commons.utils.PromptUtils;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.server.executor.runner.AgentRequestOptions;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.*;
import io.agentscope.core.nacos.a2a.discovery.NacosAgentCardResolver;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.imooc.commons.utils.NacosUtil;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * author: Imooc
 * description: 将远程Agent封装为工具
 * date: 2026
 */

@Service
@Slf4j
public class RemoteAgentTool {

    @Resource
    private AgentUtils agentUtils;

    /**
     * author: Imooc
     * description: 基于A2A协议获取路线制定Agent
     * @param :
     * @return void
     */
    @Tool(description = "擅长制定最优驾车路线")
    public String callRouteMakingAgent(
            //工具参数
            @ToolParam(name = "prompt", description = "驾车的起点和终点")
            String prompt) throws NacosException {

        log.info("============");
        log.info("工具方法：路线制定智能体...正在调用中");
        log.info("============");

        A2aAgent agent = A2aAgent.builder()
                .name("RouteMakingAgent")
                .agentCardResolver(
                        //创建 Nacos 的 AgentCardResolver
                        new NacosAgentCardResolver(NacosUtil.getNacosClient()))
                .build();

        log.info("============");
        log.info("获取到的远程Agent描述："+agent.getDescription());
        log.info("============");


        log.info("============");
        log.info("这个工具方法传入的参数："+ prompt);
        log.info("============");

        prompt = "制定最优驾车路线："+prompt+", 并预估费用";

        //组装Prompt
        PromptUtils promptUtils =  new PromptUtils();
        Msg userMsg = promptUtils.getPrompt(prompt);

        //远程Agent运行
        Msg remoteAgentResponse = null;
        try {

            log.info("============");
            log.info("远程 "+ agent.getName()+" 开始执行任务....");
            log.info("============");


            //阻塞运行
            remoteAgentResponse = agent.call(userMsg).block();
            System.out.println(remoteAgentResponse.getContent());
            String response = remoteAgentResponse.getTextContent();

            log.info("======= 远程Agent返回 ========");
            log.info(response);
            log.info("======================");

            //判断任务是否完成
            GenerateReason reason = remoteAgentResponse.getGenerateReason();

            log.info("============================");
            switch (reason) {
                case MODEL_STOP:
                    // 任务正常完成
                    log.info("此轮任务正常完成");
                    break;
                case INTERRUPTED:
                    // 任务被中断
                    log.info("此轮任务被中断");
                    break;
            }

            log.info("============================");

            return response;

        }catch (Exception e) {
            log.error("===================");
            log.error("Agent执行任务出错了！！");
            log.error("错误："+e.getMessage());
            log.error("===================");

            return "Agent执行出错了";
        }


    }

    /**
     * author: Imooc
     * description: 基于A2A协议获取行程规划Agent
     * @param :
     * @return void
     */
    @Tool(description = "擅长制定旅游景点,饮食,住宿的行程安排")
    public String callTripPlannerAgent(
            //工具参数
            @ToolParam(name = "prompt", description = "旅游目的地")
            String prompt) throws NacosException {

        log.info("============");
        log.info("工具方法：行程规划智能体...正在调用中");
        log.info("============");


        A2aAgent agent = A2aAgent.builder()
                .name("TripPlannerAgent")
                .agentCardResolver(
                        //创建 Nacos 的 AgentCardResolver
                        new NacosAgentCardResolver(NacosUtil.getNacosClient()))
                .build();


        log.info("============");
        log.info("获取到的远程Agent描述："+agent.getDescription());
        log.info("============");


        log.info("============");
        log.info("这个工具方法传入的参数："+ prompt);
        log.info("============");


        prompt = "制定精彩旅游行程："+prompt+", 并预估费用";

        //组装Prompt
        PromptUtils promptUtils =  new PromptUtils();
        Msg userMsg = promptUtils.getPrompt(prompt);

        //远程Agent运行
        Msg remoteAgentResponse = null;
        try {

            log.info("============");
            log.info("远程 "+ agent.getName()+" 开始执行任务....");
            log.info("============");


            //阻塞运行
            remoteAgentResponse = agent.call(userMsg).block();
            System.out.println(remoteAgentResponse.getContent());
            String response = remoteAgentResponse.getTextContent();

            log.info("======= 远程Agent返回 ========");
            log.info(response);
            log.info("======================");

            //判断任务是否完成
            GenerateReason reason = remoteAgentResponse.getGenerateReason();

            log.info("============================");
            switch (reason) {
                case MODEL_STOP:
                    // 任务正常完成
                    log.info("此轮任务正常完成");
                    break;
                case INTERRUPTED:
                    // 任务被中断
                    log.info("此轮任务被中断");
                    break;
            }

            log.info("============================");

            return response;

        }catch (Exception e) {
            log.error("===================");
            log.error("Agent执行任务出错了！！");
            log.error("错误："+e.getMessage());
            log.error("===================");

            return "Agent执行出错了";
        }

    }


}
```

**讲人话（A2A 远程调用的核心）**：

这个类有**两个带 `@Tool` 的方法**，它们就是主管能用的两个工具：
- `callRouteMakingAgent`：调路线专家；`callTripPlannerAgent`：调行程专家。
- `@Tool(description="...")`：这段描述很重要——**主管的大模型靠它判断“该用哪个工具”**。所以描述写得准，模型才派对人。
- 方法体里做了什么：
  1. 用 `A2aAgent.builder().name("RouteMakingAgent")...` 创建一个“**远程 Agent 的本地代理**”，并通过 `NacosAgentCardResolver`（连第3.8节的 `NacosUtil`）去 Nacos 电话簿里**按名字查到真实地址**。
  2. 把传入的 `prompt` 加工一下（如“制定最优驾车路线：xxx，并预估费用”）。
  3. `agent.call(userMsg).block()`：**阻塞式发起远程调用**——等专家干完返回。
  4. 检查 `GenerateReason`（MODEL_STOP=正常完成 / INTERRUPTED=被中断），打印结果并返回。
  5. 出错则捕获返回“Agent执行出错了”，保证主流程不崩。

> 一句话：**这个类把“网络上另一台 Agent 服务”包装成了主管手里的一个普通方法/工具**。主管调它时感觉就像调本地函数，底层却是跨进程 A2A 通信。

### 6.7 ⭐ ManagerAgent.java —— 主管本体

路径：`manager_agent/src/main/java/com/imooc/managerAgent/agents/ManagerAgent.java`

```java
package com.imooc.managerAgent.agents;

import com.imooc.commons.data.ResponseSchema;
import com.imooc.commons.utils.LangFuseUtils;
import com.imooc.managerAgent.tool.RemoteAgentTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.model.StructuredOutputReminder;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.tool.Toolkit;
import com.imooc.managerAgent.hook.planHook;
import com.imooc.managerAgent.plan.TripPlan;
import io.agentscope.core.tracing.TracerRegistry;
import io.agentscope.core.tracing.telemetry.TelemetryTracer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import com.imooc.commons.utils.AgentUtils;
import com.imooc.commons.utils.ToolUtils;
import com.imooc.commons.utils.PromptUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * author: Imooc
 * description: 主管Agent
 * date: 2026
 */

@Component
@Slf4j
public class ManagerAgent {

    @Resource
    private AgentUtils agentUtils;

    @Resource
    private LangFuseUtils langFuseUtils;

    private ReActAgent agent;

    /**
     * author: Imooc
     * description: Agent 创建
     * @param :
     * @return null
     */
    public ReActAgent getManagerAgent() {


        //PlanNotebook
        TripPlan plan = new TripPlan();
        //Toolkit
        ToolUtils toolUtils = new ToolUtils();
        //将远程Agent封装为工具的封装注册到工具包
        Toolkit toolkit = toolUtils.getToolkit(new RemoteAgentTool());

        // 打印所有工具信息
        toolUtils.getTools();

        //计划对象
        PlanNotebook planNotebook = plan.getPlan();


        agent = agentUtils.getReActAgentBuilder(
                "ManagerAgent",
                "负责用户需求的解决方案和执行计划制定, 以及任务分发"
        )
                .sysPrompt("""
                    你是一个旅游管理主管。
                    当用户要求你规划旅游行程时，
                    请先创建一个详细的计划，
                    以及执行计划步骤,
                    并对每个计划步骤,要列出擅长执行这个步骤任务的Agent
                    然后按计划逐步执行。
                    """)
                /* **********************
                 *
                 * ReActAgent 能自主分解复杂任务, 并且会自动生成计划步骤：
                 * 1. .enablePlan()
                 *    1.1 但enablePlan方法不需要传递任何参数，也就是说无法对智能体的计划做自定义的设置
                 * 2. .planNotebook()
                 *
                 * .enablePlan() 内部调用了 PlanNotebook的Builder 构造方法
                 * 是采用默认的 PlanNotebook 的属性
                 *
                 * .planNotebook() 它是传入 PlanNotebook的 实例,
                 * 可以对 PlanNotebook 进行自定义
                 *
                 *
                 * PlanNotebook对象 是Agent能自主分解任务和步骤执行的核心
                 *
                 * PlanNotebook整个流程：
                 * 1. 复杂任务分解
                 * 2. 生成执行步骤
                 * 3. 状态跟踪
                 * 4. 动态调整
                 * 5. 任务完成
                 *
                 * PlanNotebook对象：自主规划 (PlanAct) + 自主决策 (ReAct)
                 *
                 *
                 *
                 *
                 * *********************/

                //自定义配置执行计划
                .planNotebook(planNotebook)
                //拦截器
                .hook(new planHook(planNotebook))
                //工具包
                .toolkit(toolkit)
                //结构化输出 (TOOL_CHOICE: 强制走工具调用产出结构化JSON, 适配qwen等支持工具调用的模型, 比PROMPT稳定)
                .structuredOutputReminder(StructuredOutputReminder.TOOL_CHOICE)
                .build();


        return agent;


    }

    /**
     * author: Imooc
     * description: Agent 运行
     * @param :
     * @return void
     */
    public ResponseSchema run(String prompt) {
//        String prompt = """
//        帮我制定2026年元旦，
//        深圳到惠州3日游自驾游计划，
//        请包含吃住行，天气，酒店，餐饮美食。
//
//        你可以调用以下Agent处理子任务：
//        - routeMaking Agent: 擅长处理自驾游路线制定
//        - tripPlanner Agent: 擅长处理景点行程规划
//
//        - 每个子任务要注明调用的Agent
//        """;

        //构建Prompt
        PromptUtils promptUtils = new PromptUtils();

        //阻塞调用, 跑完整个 ReAct + 计划(planNotebook)流程, 拿到最终回复消息
        Msg reply = agent
                .call(List.of(promptUtils.getPrompt(prompt)))
                .block();

        //ResponseSchema 只有一个 response 字符串字段, 直接取最终回复文本即可。
        //(本版本 SDK 下 planNotebook 计划流程不会产出 _structured_output,
        // 故不走 getStructuredData, 直接用最终文本, 结果等价)
        ResponseSchema result = new ResponseSchema();
        result.response = (reply == null) ? "Agent未返回任何结果" : reply.getTextContent();
        return result;

    }

}
```

**讲人话（把前面所有零件拼起来）**：

`getManagerAgent()` 组装主管：
1. `new TripPlan()` → 拿到自定义计划本（6.4 节）。
2. `toolUtils.getToolkit(new RemoteAgentTool())` → 把 6.6 节那个含两个 `@Tool` 方法的类塞进工具包，于是主管有了“调路线专家”“调行程专家”两个工具。
3. 用工厂建半成品，再加上：
   - `.sysPrompt(...)`：设定它是“旅游管理主管”，要**先做计划、再列出每步该用哪个 Agent、然后逐步执行**。
   - `.planNotebook(planNotebook)`：装上自定义计划本（对比注释里说的 `.enablePlan()` 是用默认配置，这里要自定义所以用 `.planNotebook()`）。
   - `.hook(new planHook(planNotebook))`：装上 6.5 节的拦截器，运行时能看到思考和计划状态。
   - `.toolkit(toolkit)`：装上两个远程调用工具。
   - `.structuredOutputReminder(StructuredOutputReminder.TOOL_CHOICE)`：让模型**强制走工具调用产出结构化结果**，对千问这类支持工具调用的模型比纯 PROMPT 提示更稳定。

`run(prompt)`：真正执行。`agent.call(...).block()` **阻塞**跑完整个 ReAct + 计划流程，拿到最终回复，塞进 `ResponseSchema` 返回。

> 提示：顶部被注释的那段 prompt、和注释里关于 `enablePlan` vs `planNotebook` 的对比，都是原项目保留的教学注释，**照抄即可**，它们能帮你理解设计取舍。

### 6.8 ManagerAgentController.java —— 用户入口

路径：`manager_agent/src/main/java/com/imooc/managerAgent/controller/ManagerAgentController.java`

```java
package com.imooc.managerAgent.controller;

import com.imooc.commons.data.ResponseSchema;
import com.imooc.commons.data.PromptSchema;
import io.agentscope.core.ReActAgent;
import jakarta.annotation.Resource;
import com.imooc.managerAgent.agents.ManagerAgent;
import org.springframework.web.bind.annotation.*;

/**
 * author: Imooc
 * description: 用户和Agent互动的Api 接口
 * date: 2026
 */

@RestController
public class ManagerAgentController {

    @Resource
    private ManagerAgent managerAgent;

    // 用户提交旅游规划的Prompt
    @RequestMapping(
            value = "/trip",
            produces = "application/json;charset=UTF-8",
            method = RequestMethod.POST)
    public ResponseSchema tripPlan(@RequestBody PromptSchema input) {

        ReActAgent manager = managerAgent.getManagerAgent();
        ResponseSchema response = managerAgent.run(input.getPrompt());

        return response;
    }
}
```

**讲人话**：暴露 `POST /trip` 接口。收到用户 JSON（`PromptSchema`）后：先 `getManagerAgent()` 创建主管，再 `run(...)` 执行，返回 `ResponseSchema`。`produces = "application/json;charset=UTF-8"` 保证返回的中文不乱码。

### 6.9 application.yml 和 logback.xml

**`manager_agent/src/main/resources/application.yml`**（端口 **8081**，注意**没有** `agentscope.a2a.server` 那段）：

```yaml
server:
  port: 8081

spring:
  config:
    import: optional:file:.env[.properties]
  ai:
    # LangFuse Agent数据观测
    observation:
      langfuse:
        # LangFuse 服务地址
        endpoint: ${LANGFUSE_ADDR}
        # 私钥
        secret-key: ${LANGFUSE_SECRET_KEY}
        # 公钥
        public-key: ${LANGFUSE_PUBLIC_KEY}
        # 采样率（生产环境建议0.1-0.5）
        sampling-rate: 1.0

agent:
  model_name: ${MODEL_NAME}
  alibaba_dashscope_key: ${ALIBABA_DASHCOPE_KEY}

mcp:
  baidu_map_addr: ${BAIDU_MAP_ADDR}
```

> 为什么主管的 yml 没有 `agentscope.a2a.server`？因为主管是**调用方**，不需要把自己注册成可被调用的服务端。它通过代码里的 `NacosUtil` 主动去 Nacos 查别人。

**`manager_agent/src/main/resources/logback.xml`**（和前面两个模块完全一样）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration  scan="true" scanPeriod="10 seconds">
    <contextName>logback</contextName>


    <!-- 输出到控制台-->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>info</level>
        </filter>
        <encoder>
            <Pattern>%date{yyyy} [%level] %msg%n</Pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>


    <root level="info">
        <appender-ref ref="CONSOLE" />
    </root>

</configuration>
```

### ✅ 第 6 章验证（让主管真的把任务拆成计划，并选对专家派活）

主管自带 `/trip` 接口，不用写临时代码。本章的功能验证目标：**主管收到请求后，真的会用 PlanNotebook 拆出有序计划步骤，并自己判断该调哪个专家**——这是主管区别于普通 Agent 的核心能力。完整的最终成品放第 7 章，这里聚焦「拆解 + 决策」这个内部过程。

**第 1 步**：确保 Nacos 在线，且**第4、5章两个专家正在运行**（主管要远程调它们）。启动 `ManagerAgentApplication`，日志里应出现 `========= 已加载的工具 ==========` 后跟 `callRouteMakingAgent`、`callTripPlannerAgent` 两个工具，以及 `Tomcat started on port 8081`。

**第 2 步**：发一个请求触发主管干活：

```powershell
$body = @{ prompt = "帮我规划深圳到惠州2日自驾游，先查路线再排行程" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/trip" -Method Post -ContentType "application/json; charset=utf-8" -Body $body
```

**第 3 步**：盯着**主管控制台**看，功能达标的标志是这几样**真的出现**：
- `==ManagerAgent 第 N 轮思考：==` —— 主管真的在思考（`planHook` 打印的内心戏）。
- `已生成本轮思考后的执行计划` 后面跟着**带 ⏸/▶️/✅ 图标的子任务列表** —— 证明 **PlanNotebook 真的把任务拆成了有序步骤**。
- `ManagerAgent 准备调用工具：callRouteMakingAgent`（或 `callTripPlannerAgent`） —— 证明主管**自己判断出该派哪个专家**，工具选择功能生效。

✅ 只要看到「思考 → 拆出计划步骤 → 选择并调用某个 `callXxxAgent` 工具」，第 6 章功能就达标了——主管的「拆解 + 决策派活」核心能力已验证。
❌ 若没拆计划/不调工具 → 检查第 6.7 节 `.planNotebook(...)`、`.toolkit(...)`、`.hook(...)` 是否都挂上了。

> 此时你应同时有 3 个程序在跑：8081（主管）、8082（路线）、8085（行程），外加 Nacos（8848）。这一步可能已经开始真正联调了——下一章我们用更完整的请求看端到端成品。

---

## 第 7 章：全流程联调（真正跑通一次完整规划）

### 7.1 严格的启动顺序

顺序很重要，**错了就连不上**：

```
1. 启动 Nacos               （电话簿先开门）
2. 启动 routeMaking_agent   （8082，专家先上线登记）
3. 启动 tripPlanner_agent   （8085，专家先上线登记）
   —— 等这两个在 Nacos 里都能看到 ——
4. 启动 manager_agent       （8081，主管最后上线）
```

> 为什么？主管 `run` 时会通过 Nacos 找两个专家并远程调用它们；如果专家没先启动注册，主管就会“打电话找不到人”而报错。

启动前再确认：
- **务必删除前面章节的临时验证文件**：`QuickModelTest.java`（第3章）、`RouteFunctionTest.java`（第4章）、`TripFunctionTest.java`（第5章）。否则路线/行程专家一启动，那些 `CommandLineRunner` 会自动各跑一次验证调用，既浪费额度又干扰联调日志。删干净后，代码才和原项目一模一样。
- `.env` 里通义千问 Key、百度地图 ak **已填真实有效值**。
- `mvn -pl commons -am clean install -DskipTests` 已执行（commons 的最新代码在本地仓库）。

### 7.2 发起请求

主管起来后（看到 `Tomcat started on port 8081`），用下面任一方式发请求。

**方式 A：PowerShell（Windows 推荐）**

```powershell
$body = @{ prompt = "帮我制定2026年元旦，深圳到惠州3日自驾游计划，包含吃住行、酒店、餐饮美食" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/trip" -Method Post -ContentType "application/json; charset=utf-8" -Body $body
```

**方式 B：curl.exe（注意是 curl.exe 不是 PowerShell 的 curl 别名）**

```powershell
curl.exe -X POST "http://localhost:8081/trip" -H "Content-Type: application/json" -d "{\"prompt\":\"帮我制定深圳到惠州3日自驾游计划，包含吃住行和美食\"}"
```

**方式 C：Postman / Apifox**
- Method：`POST`
- URL：`http://localhost:8081/trip`
- Body：`raw` → `JSON`：
  ```json
  { "prompt": "帮我制定2026年元旦，深圳到惠州3日自驾游计划，包含吃住行、酒店、餐饮美食" }
  ```

> ⏳ 这个请求会**跑比较久**（几十秒到几分钟），因为主管要思考、拆计划、两次远程调用专家、专家又各自调大模型/地图。耐心等，别以为卡死了。

### 7.3 一边等，一边看四个窗口的日志

这是最能验证“系统真的在协作”的环节。分别观察四个控制台：

**主管（8081）窗口** 应该能看到：
- `==ManagerAgent 第 N 轮思考：==` —— 主管在想怎么拆任务。
- `已生成本轮思考后的执行计划` + 子任务列表（带 ⏸/▶️/✅ 图标）—— PlanNotebook 拆出来的步骤。
- `ManagerAgent 准备调用工具：callRouteMakingAgent` —— 决定调路线专家。
- `工具方法：路线制定智能体...正在调用中` / `远程 RouteMakingAgent 开始执行任务....`
- 之后再 `准备调用工具：callTripPlannerAgent` 调行程专家。
- 最后汇总返回。

**路线专家（8082）窗口**：收到主管的远程调用后，开始调百度地图 MCP 查路线、算高速费，返回结果。

**行程专家（8085）窗口**：收到调用后，内部再调景点子专家（`SuggestSightAgent`），可能尝试用技能制表（Windows 无 bash 时这步会报错但不影响返回文字结果）。

**Nacos（8848）**：服务列表里 `RouteMakingAgent`、`TripPlannerAgent` 一直在线。

### 7.4 预期最终结果

PowerShell/Postman 最终会收到类似：

```json
{
  "response": "好的，为您规划深圳到惠州3日自驾游如下：\n第一天：驾车路线（约xx公里，高速费约xx元）...\n景点推荐：...\n住宿：...\n美食：..."
}
```

只要 `response` 字段里是一份**像模像样、包含路线和行程**的旅游计划，就说明**整个多智能体系统打通了** 🎉。

### ✅ 第 7 章验证清单

- [ ] 四个进程（Nacos + 三个 Agent）全部在线，无报错退出。
- [ ] 主管日志里出现“思考 → 生成计划 → 调用 callRouteMakingAgent → 调用 callTripPlannerAgent”的完整链路。
- [ ] 两个专家窗口都打印了“开始执行任务”和返回内容。
- [ ] HTTP 响应 `response` 字段是一份完整旅游计划。

全部打勾 = 你已经 1:1 复刻并跑通了这个项目。

---

## 第 8 章：常见问题 + 知识回顾

### 8.1 常见报错速查

| 现象 | 原因 | 解决 |
|------|------|------|
| 编译报错、找不到 `var`/文本块/switch 模式 | JDK 不是 21 | IDEA `Project Structure` 把 SDK 和语言级别都设为 21 |
| 找不到 `io.agentscope.*` / `commons` | 父 pom 没装、commons 没 install | 先 `mvn -N validate`，再 `mvn -pl commons -am install` |
| 主管调用专家报“找不到 Agent/超时” | 启动顺序错，或专家没注册成功 | 严格按 7.1 顺序；去 Nacos 确认专家在线 |
| 模型 401 / 鉴权失败 | 通义千问 Key 无效或没额度 | 换有效 Key；确认 `.env` 的 `ALIBABA_DASHCOPE_KEY` 拼写 |
| 百度 MCP 初始化失败 / SSE 错误 | `ak` 无效或网络问题 | 检查 `BAIDU_MAP_ADDR` 的 `ak`，确认网络可达 |
| 制表 `table.sh` 报错（找不到 bash/sudo） | Windows 没有 bash/sudo | 装 Git Bash 或 WSL；或忽略（不影响主流程） |
| 中文返回乱码 | 编码问题 | 已用 UTF-8 配置；确认终端编码、请求头 charset |
| 端口被占用 | 8081/8082/8085 被占 | 改对应 yml 的 `server.port`，或关掉占用进程 |

### 8.2 知识点回顾（你这一路学到了什么）

1. **Maven 多模块**：父 pom 管版本和公共依赖，子模块只声明差异 → 这就是为什么 commons 的 pom 几乎空、主管只依赖 commons。
2. **ReActAgent**：会“想-做-看-再想”的智能体；`AgentUtils` 把公共的大脑配置（模型/超时/思考模式）统一封装成工厂。
3. **`toolExecutionConfig`**（你最初的问题）：控制工具执行的超时（120秒）和重试次数（1次）。
4. **PlanNotebook**：让主管把复杂任务自动拆成有序子任务并跟踪状态；`TripPlan` 自定义了不要用户确认、最多 5 步。
5. **Hook 拦截器**：在思考/调用工具前后插钩子，把 Agent 的“内心戏”和计划状态打印出来。
6. **A2A + Nacos**：专家用 A2A starter 把自己注册到 Nacos；主管用 `NacosAgentCardResolver` 按名字找到它们并远程 `call`。`RemoteAgentTool` 把“远程 Agent”包装成主管手里的本地工具。
7. **MCP**：用标准协议给模型外接百度地图，免去手写 HTTP。
8. **父子 Agent 嵌套**：行程专家把景点子专家当工具挂载。
9. **Skill 技能**：用 `.md` 手册 + 脚本指导 Agent 完成特定任务（如制表）。
10. **结构化输出**：`StructuredOutputReminder.TOOL_CHOICE` 让千问稳定产出结构化结果。

### 8.3 最终文件对照清单（确认一个不漏）

> 先确认**临时验证文件已全部删除**：`QuickModelTest.java`、`RouteFunctionTest.java`、`TripFunctionTest.java`。它们是验证功能用的脚手架，不属于原项目，删掉后才能 1:1 一致。

完成后，对照下表逐个打勾，确保和原项目一模一样：

**根 + commons**
- [ ] `pom.xml`（父）
- [ ] `.env`
- [ ] `commons/pom.xml`
- [ ] `commons/.../conf/Properties.java`
- [ ] `commons/.../data/PromptSchema.java`
- [ ] `commons/.../data/ResponseSchema.java`
- [ ] `commons/.../utils/AgentUtils.java`
- [ ] `commons/.../utils/ToolUtils.java`
- [ ] `commons/.../utils/PromptUtils.java`
- [ ] `commons/.../utils/NacosUtil.java`
- [ ] `commons/.../utils/LangFuseUtils.java`

**routeMaking_agent**
- [ ] `pom.xml`
- [ ] `.../RouteMakingAgentApplication.java`
- [ ] `.../agents/RouteMakingAgent.java`
- [ ] `.../mcp/BaiduMapMCP.java`
- [ ] `resources/application.yml`、`resources/logback.xml`

**tripPlanner_agent**
- [ ] `pom.xml`
- [ ] `.../TripPlannerAgentApplication.java`
- [ ] `.../agents/TripPlannerAgent.java`
- [ ] `.../agents/SuggestSightAgent.java`
- [ ] `resources/skills/Suggest-Sights/SKILL.md`
- [ ] `resources/skills/Make-Table/SKILL.md`
- [ ] `resources/skills/Make-Table/scripts/table.sh`
- [ ] `resources/application.yml`、`resources/logback.xml`

**manager_agent**
- [ ] `pom.xml`
- [ ] `.../ManagerAgentApplication.java`
- [ ] `.../controller/ManagerAgentController.java`
- [ ] `.../agents/ManagerAgent.java`
- [ ] `.../tool/RemoteAgentTool.java`
- [ ] `.../plan/TripPlan.java`
- [ ] `.../hook/planHook.java`
- [ ] `resources/application.yml`、`resources/logback.xml`

---

> 🎓 **恭喜！** 走完这 8 章，你不仅 1:1 复刻了项目，更重要的是搞懂了一个**生产级多智能体系统**是怎么用「主管拆解 + 专家协作 + A2A 通信 + MCP 外接工具 + 技能增强」搭起来的。回头再看最开始那段 `ReActAgent.builder()...toolExecutionConfig(...)`，是不是已经一点都不晦涩了？

*（本教程基于现有项目逆向整理，代码与项目当前状态保持一致。密钥请务必替换为自己的，且不要提交到公开仓库。）*
