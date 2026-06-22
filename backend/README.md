# 在线考试系统

面向院校教学场景的一体化在线考试平台，采用 **Spring Boot + Vue3 前后端分离**架构，覆盖管理员、教师、学生三类角色，实现题库建设、智能组卷、考试发布与实时监控、在线考试与防作弊、自动判分与主观题批阅、成绩统计与学情分析、错题本等完整业务闭环。

> 《J2EE 高级开发框架》课程项目

## 技术栈

**后端**　Spring Boot 2.7.18 · Java 1.8 · MyBatis-Plus 3.5.5 · MySQL 8.0 · Druid · Fastjson2 · jjwt（JWT/HS256）· Spring Security Crypto（BCrypt）· Spring AOP

**前端**　Vue 3.4 · Vite 5 · Element Plus · Pinia · Vue Router · ECharts · axios · WindiCSS

## 目录结构

```
.
├── backend/            # Spring Boot 后端（端口 8080）
│   └── src/main/java/com/hbnu/   # 根包：controller / service / mapper / pojo / config / aop ...
├── front/              # Vue3 前端（端口 3000，/api 反向代理至 8080）
│   └── src/            # views（admin/teacher/student）/ router / stores / api ...
├── exam_system_init.sql    # 数据库脚本（建表 + 初始数据）
└── 文档/                # 提交文档（需求/设计/测试/手册/总结/部署）
```

## 快速开始

### 1. 数据库

```sql
CREATE DATABASE exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

```bash
mysql -u root -p exam_system < exam_system_init.sql
```

### 2. 后端

按本机环境修改 `backend/src/main/resources/application.yml` 中的数据源（默认 `root` / `123456`），然后：

```bash
cd backend
mvn spring-boot:run        # 开发模式，监听 :8080
```

### 3. 前端

```bash
cd front
npm install
npm run dev                # 监听 :3000，访问 http://localhost:3000
```

## 测试账号

初始密码均为 `123456`：

| 角色 | 账号 |
|------|------|
| 管理员 | `admin` |
| 教师 | `T001` |
| 学生 | `S001` |

## 核心模块

- **认证鉴权**：JWT 无状态认证 + 后端 URI 前缀角色鉴权 + 前端路由守卫双重校验。
- **题目快照**：组卷时固化题目快照，判分/回看/预览读取快照，隔离题目后续修改。
- **在线考试**：题目/选项乱序、倒计时、心跳保活、10 秒自动保存与断点恢复、切屏监控、超时/强制交卷。
- **判分批阅**：客观题自动判分（单选精确、判断/填空忽略大小写空白、多选集合匹配），主观题人工批阅后总分重算，错题自动归集。
- **学情分析**：成绩统计、题目质量、知识点雷达、班级对比、学生趋势（ECharts）。
- **横切能力**：`@Log` 操作日志 AOP、全局异常处理、审计字段自动填充、逻辑删除。

## 说明

- 系统不依赖 Redis，会话由 JWT 无状态承载、考试断点由前端 localStorage + 服务端记录共同保障。
- 接口文档为手工整理（详见《系统设计文档》接口设计章节），未集成 Swagger。
- 数据库口令与示例账号口令均为本地演示用默认值，正式部署请修改。

---
**作者**　刘博论（学号 20252160A1031，计信2310班）　|　**指导老师**　陈迪凯
