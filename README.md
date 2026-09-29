# 学习助手

本地学习助手。DeepSeek 负责对话，Ollama 的 `bge-m3` 把 PDF 做成向量，PostgreSQL 的 pgvector 保存资料和对话记忆。

## 启动

1. 准备 PostgreSQL，并安装 pgvector。把连接写进项目根目录的 `.env`：

```
DATASOURCE_URL=jdbc:postgresql://localhost:5432/learning_assistant
DEEPSEEK_API_KEY=你的密钥
```

数据库用户名和密码默认是 `learning` / `learning`，可以在 `src/main/resources/application.yml` 里改。

2. 启动 Ollama，并拉取嵌入模型：

```
ollama serve
ollama pull bge-m3
```

3. 在项目根目录启动应用：

```
mvn spring-boot:run
```

4. 打开 http://localhost:8080 。

查资料会检索 PDF，不调用下载和文件工具。办事情会调用工具，不强制检索。输入框上方可以切换。

接口说明在 http://localhost:8080/doc.html 。
