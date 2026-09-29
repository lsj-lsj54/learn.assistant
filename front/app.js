const apiBase = location.port === "8080" ? "" : "http://localhost:8080";
const storageKey = "learn-assistant-chats";

const chatList = document.querySelector("#chatList");
const messagesEl = document.querySelector("#messages");
const welcome = document.querySelector("#welcome");
const composer = document.querySelector("#composer");
const input = document.querySelector("#input");
const send = document.querySelector("#send");
const hint = document.querySelector("#hint");
const sidebar = document.querySelector(".sidebar");
const stage = document.querySelector("#stage");

let chats = loadChats();
let currentId = chats[0]?.id ?? null;
let sending = false;

document.querySelector("#newChat").addEventListener("click", () => {
    currentId = null;
    sidebar.classList.remove("open");
    render();
    input.focus();
});

document.querySelector("#menu").addEventListener("click", () => {
    sidebar.classList.toggle("open");
});

document.querySelector("#importPdf").addEventListener("click", importPdf);
document.querySelector("#clearVectors").addEventListener("click", clearVectors);

composer.addEventListener("submit", (event) => {
    event.preventDefault();
    submit();
});

input.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        submit();
    }
});

input.addEventListener("input", () => {
    input.style.height = "auto";
    input.style.height = Math.min(input.scrollHeight, 160) + "px";
    send.disabled = sending || input.value.trim() === "";
});

render();
loadSettings();

function loadChats() {
    try {
        const saved = JSON.parse(localStorage.getItem(storageKey));
        return Array.isArray(saved) ? saved : [];
    } catch {
        return [];
    }
}

function saveChats() {
    localStorage.setItem(storageKey, JSON.stringify(chats));
}

function render() {
    const chat = chats.find((item) => item.id === currentId);
    const hasMessages = Boolean(chat?.messages?.length);
    document.body.classList.toggle("empty", !hasMessages);
    welcome.hidden = hasMessages;
    messagesEl.hidden = !hasMessages;
    messagesEl.innerHTML = "";
    if (chat?.messages) {
        for (const message of chat.messages) {
            const item = document.createElement("div");
            item.className = "message " + message.role + (message.pending ? " pending" : "");
            item.textContent = message.content;
            messagesEl.appendChild(item);
        }
        scrollToLatest();
    }
    chatList.innerHTML = "";
    for (const item of chats) {
        const row = document.createElement("div");
        row.className = "chat-row";
        const button = document.createElement("button");
        button.type = "button";
        button.className = "chat-item" + (item.id === currentId ? " active" : "");
        button.textContent = item.title;
        button.addEventListener("click", () => {
            currentId = item.id;
            sidebar.classList.remove("open");
            render();
        });
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "chat-delete";
        remove.setAttribute("aria-label", "删除对话");
        remove.textContent = "×";
        remove.addEventListener("click", () => deleteChat(item.id));
        row.append(button, remove);
        chatList.appendChild(row);
    }
    send.disabled = sending || input.value.trim() === "";
}

function scrollToLatest() {
    const snap = () => {
        const latest = messagesEl.lastElementChild;
        if (latest) {
            latest.scrollIntoView({ block: "end" });
        }
        stage.scrollTop = stage.scrollHeight;
    };
    snap();
    requestAnimationFrame(() => {
        snap();
        requestAnimationFrame(snap);
    });
}

async function submit() {
    const text = input.value.trim();
    if (!text || sending) {
        return;
    }
    if (!currentId) {
        currentId = crypto.randomUUID();
        chats.unshift({ id: currentId, title: text.slice(0, 18), messages: [] });
    }
    const chat = chats.find((item) => item.id === currentId);
    chat.messages.push({ role: "user", content: text });
    chat.messages.push({ role: "assistant", content: "正在思考", pending: true });
    input.value = "";
    input.style.height = "auto";
    sending = true;
    setHint("");
    render();
    let accumulated = "";
    try {
        const response = await fetch(apiBase + "/api/chat/stream", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "text/event-stream"
            },
            body: JSON.stringify({ message: text, conversationId: currentId })
        });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.message || body.error || "请求失败");
        }
        if (!response.body) {
            throw new Error("请求失败");
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        while (true) {
            const chunk = await reader.read();
            if (chunk.done) {
                buffer += decoder.decode();
                break;
            }
            buffer += decoder.decode(chunk.value, { stream: true });
            const parsed = consumeSse(buffer);
            buffer = parsed.rest;
            accumulated = applyStreamEvents(chat, accumulated, parsed.events);
        }
        const tail = consumeSse(buffer.trim() ? buffer + "\n\n" : buffer);
        accumulated = applyStreamEvents(chat, accumulated, tail.events);
        const pending = chat.messages[chat.messages.length - 1];
        pending.content = accumulated;
        pending.pending = false;
    } catch (error) {
        const pending = chat.messages[chat.messages.length - 1];
        const kept = pending && pending.role === "assistant" && pending.content !== "正在思考";
        if (!kept) {
            chat.messages.pop();
        } else {
            pending.pending = false;
        }
        setHint(error.message || "无法连接后端", true);
    } finally {
        sending = false;
        saveChats();
        render();
    }
}

function applyStreamEvents(chat, accumulated, events) {
    for (const event of events) {
        if (event.name === "error") {
            const failure = new Error(event.data || "请求失败");
            throw failure;
        }
        accumulated += event.data;
        showAssistant(chat, accumulated);
    }
    return accumulated;
}

function showAssistant(chat, content) {
    const message = chat.messages[chat.messages.length - 1];
    message.content = content;
    message.pending = false;
    if (currentId !== chat.id) {
        return;
    }
    const node = messagesEl.lastElementChild;
    if (!node || !node.classList.contains("assistant")) {
        render();
        return;
    }
    node.textContent = content;
    node.classList.remove("pending");
    scrollToLatest();
}

function consumeSse(buffer) {
    const events = [];
    const normalized = buffer.replace(/\r\n/g, "\n");
    let rest = normalized;
    let boundary = rest.indexOf("\n\n");
    while (boundary >= 0) {
        const block = rest.slice(0, boundary);
        rest = rest.slice(boundary + 2);
        let name = "message";
        const dataLines = [];
        for (const line of block.split("\n")) {
            if (line.startsWith("event:")) {
                name = line.slice(6).trim();
            } else if (line.startsWith("data:")) {
                dataLines.push(line.slice(5).replace(/^ /, ""));
            }
        }
        if (dataLines.length > 0) {
            events.push({ name, data: dataLines.join("\n") });
        }
        boundary = rest.indexOf("\n\n");
    }
    return { events, rest };
}

async function importPdf() {
    setHint("正在导入 PDF");
    try {
        const response = await fetch(apiBase + "/api/documents", { method: "POST" });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.message || body.error || "导入失败");
        }
        const added = body.addedCount ?? 0;
        const skipped = body.skippedCount ?? 0;
        if (added === 0 && skipped === 0) {
            setHint("没有可导入的文档");
        } else if (added === 0) {
            setHint("没有新片段，跳过 " + skipped + " 段重复");
        } else if (skipped === 0) {
            setHint("已导入 " + added + " 段");
        } else {
            setHint("已导入 " + added + " 段，跳过 " + skipped + " 段重复");
        }
    } catch (error) {
        setHint(error.message || "无法连接后端", true);
    }
}

async function clearVectors() {
    if (!window.confirm("只清空已导入的资料，历史对话会保留。继续吗？")) {
        return;
    }
    setHint("正在清空资料库");
    try {
        const response = await fetch(apiBase + "/api/documents", { method: "DELETE" });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.message || body.error || "清空失败");
        }
        setHint("已清空 " + (body.deletedCount ?? 0) + " 条资料");
    } catch (error) {
        setHint(error.message || "无法连接后端", true);
    }
}

async function deleteChat(id) {
    const chat = chats.find((item) => item.id === id);
    if (!chat || !window.confirm("删除「" + chat.title + "」？")) {
        return;
    }
    chats = chats.filter((item) => item.id !== id);
    if (currentId === id) {
        currentId = chats[0]?.id ?? null;
    }
    saveChats();
    render();
    try {
        const response = await fetch(apiBase + "/api/chat/" + encodeURIComponent(id), { method: "DELETE" });
        if (!response.ok) {
            throw new Error();
        }
    } catch {
        setHint("对话已从列表删除，服务器记录未能清除", true);
    }
}

async function loadSettings() {
    try {
        const response = await fetch(apiBase + "/api/settings");
        if (!response.ok) {
            throw new Error();
        }
        const body = await response.json();
        document.querySelector("#scrapeMax").textContent = body.scrapeMaxChars + " 字";
        document.querySelector("#readMax").textContent = body.readMaxChars + " 字";
        document.querySelector("#memoryMax").textContent = body.maxMemoryMessages + " 条";
        document.querySelector("#emptyContext").textContent = body.allowEmptyContext ? "允许" : "不允许";
        setHint("");
    } catch {
        document.querySelector("#scrapeMax").textContent = "未连接";
        document.querySelector("#readMax").textContent = "未连接";
        document.querySelector("#memoryMax").textContent = "未连接";
        document.querySelector("#emptyContext").textContent = "未连接";
        setHint("无法连接后端", true);
    }
}

function setHint(text, isError) {
    hint.textContent = text;
    hint.classList.toggle("error", Boolean(isError));
}
