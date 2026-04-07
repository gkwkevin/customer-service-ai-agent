let ws = null;
let sessionId = null;
let conversations = [];
let isManualClose = false;
let shouldReconnectImmediately = false;
let currentUser = null;

async function init() {
    if (!checkLoginStatus()) {
        return;
    }
    await createSession();
    loadConversationList();
    if (sessionId) {
        connectWebSocket();
    }
}

function checkLoginStatus() {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    
    if (!token || !userStr) {
        window.location.href = '/login';
        return false;
    }
    
    currentUser = JSON.parse(userStr);
    
    const userNameEl = document.getElementById('userName');
    
    if (userNameEl) {
        userNameEl.textContent = currentUser.nickname || currentUser.username;
    }
    
    updateNavigation();
    return true;
}

function updateNavigation() {
    const nav = document.getElementById('mainNav');
    if (!nav || !currentUser) return;
    
    let navHtml = '<a href="/">对话</a>';
    
    if (currentUser.role >= 2) {
        navHtml += '<a href="/agent">客服</a>';
        navHtml += '<a href="/ticket/list">工单</a>';
    }
    
    if (currentUser.role === 3) {
        navHtml += '<a href="/admin">管理</a>';
    }
    
    nav.innerHTML = navHtml;
}

function toggleDropdown() {
    const dropdownMenu = document.getElementById('dropdownMenu');
    const userAvatar = document.getElementById('userAvatar');
    
    dropdownMenu.classList.toggle('show');
    userAvatar.classList.toggle('active');
}

document.addEventListener('click', function(e) {
    const dropdown = document.querySelector('.user-dropdown');
    const dropdownMenu = document.getElementById('dropdownMenu');
    const userAvatar = document.getElementById('userAvatar');
    
    if (dropdown && !dropdown.contains(e.target)) {
        dropdownMenu.classList.remove('show');
        userAvatar.classList.remove('active');
    }
});

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    currentUser = null;
    window.location.href = '/login';
}

function reconnectWebSocket() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        isManualClose = true;
        ws.onclose = function() {
            isManualClose = false;
            connectWebSocket();
        };
        ws.close();
    } else {
        connectWebSocket();
    }
}

async function createSession() {
    try {
        const response = await fetch('/api/chat/session', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.id })
        });
        const res = await response.json();
        if (res.code === 200) {
            sessionId = res.data.sessionId;
            console.log("真实会话ID：", sessionId);
        }
    } catch (err) {
        console.error("创建会话失败", err);
    }
}

async function loadConversationList() {
    try {
        const response = await fetch(`/api/chat/session/list?userId=${currentUser.id}`);
        const res = await response.json();
        if (res.code === 200) {
            conversations = res.data.list || [];
            renderConversationList();
        }
    } catch (err) {
        console.error("加载对话列表失败", err);
    }
}

function renderConversationList() {
    const container = document.getElementById('conversationList');
    container.innerHTML = '';
    
    conversations.forEach(conv => {
        const item = document.createElement('div');
        item.className = 'conversation-item' + (conv.sessionId === sessionId ? ' active' : '');
        
        const time = conv.updateTime ? formatTime(conv.updateTime) : formatTime(conv.createTime);
        item.innerHTML = `
            <div class="conversation-content" onclick="switchConversation('${conv.sessionId}')">
                <div class="conversation-title">会话 #${conv.sessionId.split('_')[2] || conv.id}</div>
                <div class="conversation-time">${time}</div>
            </div>
            <button class="delete-btn" onclick="event.stopPropagation(); deleteConversation('${conv.sessionId}')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"></path>
                </svg>
            </button>
        `;
        container.appendChild(item);
    });
}

async function deleteConversation(targetSessionId) {
    if (!confirm('确定要删除这个对话吗？')) return;
    
    try {
        const response = await fetch(`/api/chat/session/${targetSessionId}`, {
            method: 'DELETE'
        });
        const res = await response.json();
        if (res.code === 200) {
            if (targetSessionId === sessionId) {
                await newChat();
            }
            loadConversationList();
        }
    } catch (err) {
        console.error("删除对话失败", err);
    }
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 86400000) {
        return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } else if (diff < 172800000) {
        return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } else {
        return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
    }
}

function switchConversation(targetSessionId) {
    sessionId = targetSessionId;
    renderConversationList();
    loadMessages(targetSessionId);
    reconnectWebSocket();
}

async function loadMessages(sid) {
    try {
        const response = await fetch(`/api/chat/message/list?sessionId=${sid}`);
        const res = await response.json();
        if (res.code === 200) {
            const container = document.getElementById('messagesContainer');
            container.innerHTML = '';
            
            const messages = res.data.list || [];
            messages.forEach(msg => {
                const message = {
                    content: msg.content,
                    sender: msg.senderType === 1 ? 'user' : 'ai',
                    time: new Date(msg.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
                };
                appendMessage(message);
            });
        }
    } catch (err) {
        console.error("加载消息失败", err);
    }
}

async function newChat() {
    await createSession();
    loadConversationList();
    const container = document.getElementById('messagesContainer');
    container.innerHTML = `
        <div class="welcome-screen" id="welcomeScreen">
            <div class="welcome-icon">💬</div>
            <h1 class="welcome-title">有什么可以帮您？</h1>
            <p class="welcome-desc">我是 AI 智能客服，可以为您解答产品问题、处理售后咨询等。</p>
            <div class="suggestions">
                <button class="suggestion-btn" onclick="sendSuggestion('产品功能介绍')">
                    产品功能介绍
                    <span>了解我们的核心功能</span>
                </button>
                <button class="suggestion-btn" onclick="sendSuggestion('如何申请退款')">
                    如何申请退款
                    <span>退款流程和注意事项</span>
                </button>
                <button class="suggestion-btn" onclick="sendSuggestion('联系人工客服')">
                    联系人工客服
                    <span>转接人工服务</span>
                </button>
            </div>
        </div>
    `;
}

// ---------------------- 以下代码不变 ----------------------
function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    let wsUrl = protocol + '//' + window.location.host + '/ws/chat';
    if (sessionId) {
        wsUrl += '?sessionId=' + sessionId;
    }
    console.log('Connecting to WebSocket:', wsUrl);
    ws = new WebSocket(wsUrl);

    ws.onopen = function() {
        console.log('WebSocket connected successfully');
        // WebSocket连接成功后，加载历史消息（确保能收到AI欢迎消息）
        if (sessionId) {
            setTimeout(() => loadMessages(sessionId), 100);
        }
    };

    ws.onmessage = function(event) {
        console.log('Received message:', event.data);
        const data = JSON.parse(event.data);
        handleIncomingMessage(data);
    };

    ws.onclose = function() {
        console.log('WebSocket disconnected');
        if (shouldReconnectImmediately) {
            shouldReconnectImmediately = false;
            connectWebSocket();
        } else if (!isManualClose) {
            setTimeout(connectWebSocket, 3000);
        }
    };

    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
    };
}

function handleIncomingMessage(data) {
    hideWelcomeScreen();

    if (data.type === 'typing') {
        showTypingIndicator();
        return;
    }

    hideTypingIndicator();

    // 忽略自己发送的消息（senderType === 1 是用户）
    if (data.senderType === 1) {
        console.log('Ignoring own message');
        return;
    }

    const time = data.createTime ? new Date(data.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

    const message = {
        content: data.content,
        sender: 'ai',
        time: time
    };

    appendMessage(message);
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();

    if (!content) return;
    if (!sessionId) { alert("请先创建会话"); return; }

    hideWelcomeScreen();

    const message = {
        content: content,
        sender: 'user',
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    };

    appendMessage(message);
    input.value = '';

    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({
            sessionId: sessionId,
            content: content,
            type: 'message',
            senderId: currentUser ? currentUser.id : null
        }));
        loadConversationList();
    } else {
        sendMessageToServer(content);
    }
}

function sendMessageToServer(content) {
    fetch('/api/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sessionId: sessionId,
            content: content
        })
    })
        .then(response => response.json())
        .then(data => {
            const message = {
                content: data.data.content,
                sender: 'ai',
                time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
            };
            appendMessage(message);
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

function sendSuggestion(text) {
    document.getElementById('messageInput').value = text;
    sendMessage();
}

function appendMessage(message) {
    const container = document.getElementById('messagesContainer');

    const messageEl = document.createElement('div');
    messageEl.className = `message ${message.sender}`;

    const avatarClass = message.sender === 'user' ? '' : 'style="background: linear-gradient(135deg, #34c759 0%, #30d158 100%);"';
    const avatarText = message.sender === 'user' ? '我' : '客服';

    messageEl.innerHTML = `
        <div class="message-avatar">
            <div class="avatar" ${avatarClass}>${avatarText}</div>
        </div>
        <div>
            <div class="message-content">${escapeHtml(message.content)}</div>
            <div class="message-time">${message.time}</div>
        </div>
    `;

    container.appendChild(messageEl);
    container.scrollTop = container.scrollHeight;
}

function showTypingIndicator() {
    const container = document.getElementById('messagesContainer');
    let indicator = document.getElementById('typingIndicator');

    if (!indicator) {
        indicator = document.createElement('div');
        indicator.id = 'typingIndicator';
        indicator.className = 'message ai';
        indicator.innerHTML = `
            <div class="message-avatar">
                <div class="avatar" style="background: linear-gradient(135deg, #34c759 0%, #30d158 100%);">AI</div>
            </div>
            <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
            </div>
        `;
        container.appendChild(indicator);
        container.scrollTop = container.scrollHeight;
    }
}

function hideTypingIndicator() {
    const indicator = document.getElementById('typingIndicator');
    if (indicator) {
        indicator.remove();
    }
}

function hideWelcomeScreen() {
    const welcome = document.getElementById('welcomeScreen');
    if (welcome) {
        welcome.style.display = 'none';
    }
}

function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', init);