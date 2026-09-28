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
    const hasMessages = Boolean(chat?.messages.length);
    document.body.classList.toggle("empty", !hasMessages);
    welcome.hidden = hasMessages;
    messagesEl.hidden = !hasMessages;
    messagesEl.innerHTML = "";
    if (chat) {
        for (const message of chat.messages) {
            const item = document.createElement("div");
            item.className = "message " + message.role + (message.pending ? " pending" : "");
            item.textContent = message.content;
            messagesEl.appendChild(item);
        }
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }
    chatList.innerHTML = "";
    for (const item of chats) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "chat-item" + (item.id === currentId ? " active" : "");
        button.textContent = item.title;
        button.addEventListener("click", () => {
            currentId = item.id;
            sidebar.classList.remove("open");
            render();
        });
        chatList.appendChild(button);
    }
    send.disabled = sending || input.value.trim() === "";
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
    try {
        const response = await fetch(apiBase + "/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: text, conversationId: currentId })
        });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.message || body.error || "请求失败");
        }
        chat.messages[chat.messages.length - 1] = { role: "assistant", content: body.reply ?? "" };
    } catch (error) {
        chat.messages.pop();
        setHint(error.message || "无法连接后端", true);
    } finally {
        sending = false;
        saveChats();
        render();
    }
}

async function importPdf() {
    setHint("正在导入 PDF");
    try {
        const response = await fetch(apiBase + "/api/documents", { method: "POST" });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.message || body.error || "导入失败");
        }
        setHint("已导入，切成 " + (body.chunkCount ?? 0) + " 段");
    } catch (error) {
        setHint(error.message || "无法连接后端", true);
    }
}

function setHint(text, isError) {
    hint.textContent = text;
    hint.classList.toggle("error", Boolean(isError));
}
