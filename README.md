# 学习助手

## 演示视频

https://github.com/lsj-lsj54/learn.assistant/raw/master/docs/demo.mp4

本地学习助手。DeepSeek 负责对话，Ollama 的 `bge-m3` 把 PDF 做成向量，PostgreSQL 的 pgvector 保存资料和对话记忆。页面在 `front/`，由应用直接提供。

这是给一个人在本机用的。接口没有登录，CORS 允许任意来源。不要把 8080 暴露到公网。

## 两种模式

输入框上方可以切换。两种模式共用同一段对话记忆，但挂上的能力不同。

| 模式 | 做什么 | 不做什么 |
| --- | --- | --- |
| 查资料 | 检索已导入的 PDF，回答带来源文件和页码 | 不调用搜索、下载和文件工具 |
| 办事情 | 调用搜索、抓取、下载、写 PDF、读写文件 | 不强制检索资料库 |

切换发生在 `LearningChatClient`：查资料只注册检索顾问，办事情只注册工具。

## 查资料

检索顾问在 `rag.advisor`，分成检索前、检索时、检索后。

检索前是一条装饰器链，顺序是压缩、翻译、重写。都走 DeepSeek，温度是 0。不满足条件就原样交给下一层，避免每次提问都多打几次模型：

- 有对话历史，并且问题超过 24 个字，才压缩历史。
- 问题里有拉丁字母，并且超过 24 个字，才翻译成中文。
- 问题超过 24 个字，才改写成更适合向量检索的问法。

检索时按相似度阈值和 `topK` 查向量库。没有检索到资料时，仍把用户原话交给模型，不用一段固定回复盖掉对话记忆。资料和对话里都没有答案时，提示词要求说不知道。

导入在 `DocumentIngestService`。读取、切段、写入向量库分成 ETL 的三段，各自用策略路由器接上具体实现。现在真正接进整库导入的是 PDF。文本、Markdown、HTML、JSON 的读取类已经按同样的方式写好，还没有注册成导入来源。

导入可以重复执行。同一份 PDF 内容没变就跳过；内容变了会按文件名替换旧切片。侧边栏可以上传、列出页数和切片数、替换或删除单份资料。文件的保存和删除在 `library`，不放在 `rag` 里。当前只有 PDF 这一种文件策略。

对应测试：`QueryRewritesTest`、`ConversationAwareQueryAugmenterTest`、`DocumentIngestServiceTest`。

切片长度按 OpenAI 的 CL100K 分词器计算，嵌入模型是 `bge-m3`。两套分词器不一致，配置里的 500 并不等于 bge-m3 的 500 个 token。这是有意留下的近似，没有为嵌入模型单独换分词器。

## 办事情

工具在 `tool.concretetool`。包括搜索、抓取网页、下载、写 PDF，以及列出、读取、写入、删除文件。`tool.config` 负责收集这些工具；调用记录在 `tool.log`，回答里看到的步骤在 `tool.activity`。

文件工具只能访问 `pdf/` 和 `res/`。PDF 进 pdf 目录，其他文件进 res。路径必须以此开头，不能读写源码和配置。下载和写 PDF 使用同一套目录。

流式回答里，状态行显示具体工具名。工具结束后，回答下面附上工具名、参数摘要和结果。

对应测试：`FileOperationToolTest`、`ToolCallLogTest`。

## 启动

1. 准备带 pgvector 的 PostgreSQL。项目根目录有 `docker-compose.yml`，可以：

```
docker compose up -d
```

2. 在项目根目录写 `.env`。这个文件不要提交。

```
DATASOURCE_URL=jdbc:postgresql://localhost:5432/learning_assistant
DEEPSEEK_API_KEY=你的密钥
```

数据库用户名和密码默认是 `learning` / `learning`，可以在 `src/main/resources/application.yml` 里改。要用网页搜索，再加 `BOCHA_API_KEY`。

3. 启动 Ollama，并拉取嵌入模型：

```
ollama serve
ollama pull bge-m3
```

4. 在项目根目录启动应用：

```
mvn spring-boot:run
```

5. 打开 http://localhost:8080 。

写 PDF 时会寻找系统里的中文字体。也可以在 `.env` 里指定 `LEARN_TOOLS_PDF_FONT`，指向一个 `.ttf`、`.otf` 或 `.ttc` 文件。

接口说明在 http://localhost:8080/doc.html 。
