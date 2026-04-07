let currentSessionId = null;
let currentUserId = null;
let currentAgentId = null;
let currentStatusFilter = 1;
let ws = null;
let currentUser = null;

function init() {
    if (!checkLoginStatus()) {
        return;
    }
    currentAgentId = currentUser.id;
    loadSessions();
    connectWebSocket();
}

function checkLoginStatus() {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    
    if (!token || !userStr) {
        window.location.href = '/login';
        return false;
    }
    
    currentUser = JSON.parse(userStr);
    
    if (currentUser.role < 2) {
        alert('您没有客服权限');
        window.location.href = '/';
        return false;
    }
    
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

function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = protocol + '//' + window.location.host + '/ws/chat?sessionId=agent_' + currentAgentId + '&agentId=' + currentAgentId;
    console.log('Agent connecting to WebSocket:', wsUrl);
    ws = new WebSocket(wsUrl);
    
    ws.onopen = () => console.log('Agent WebSocket connected');
    ws.onmessage = (event) => {
        console.log('Agent received:', event.data);
        const data = JSON.parse(event.data);
        
        if (data.type === 'new_ticket') {
            showNewTicketNotification(data);
            return;
        }
        
        if (data.sessionId === currentSessionId) {
            appendMessageToChat(data);
        }
        loadSessions();
    };
    ws.onerror = (err) => console.error('Agent WebSocket error', err);
    ws.onclose = () => {
        console.log('Agent WebSocket closed, reconnecting...');
        setTimeout(connectWebSocket, 3000);
    };
}

function showNewTicketNotification(data) {
    const notification = document.createElement('div');
    notification.className = 'ticket-notification';
    notification.innerHTML = `
        <div class="notification-content">
            <div class="notification-icon">🎫</div>
            <div class="notification-text">
                <div class="notification-title">您有新的工单，请处理</div>
                <div class="notification-info">工单 #${data.ticketId}</div>
                <div class="notification-message">"${escapeHtml(data.userMessage || '')}"</div>
            </div>
            <button class="notification-close" onclick="this.parentElement.parentElement.remove()">×</button>
        </div>
        <div class="notification-actions">
            <button class="notification-btn primary" onclick="handleTicketNotification(${data.ticketId}, '${data.sessionId}')">立即处理</button>
            <button class="notification-btn secondary" onclick="this.parentElement.parentElement.remove()">稍后处理</button>
        </div>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        if (notification.parentElement) {
            notification.remove();
        }
    }, 30000);
    
    playNotificationSound();
}

function handleTicketNotification(ticketId, sessionId) {
    document.querySelector('.ticket-notification')?.remove();
    
    currentSessionId = sessionId;
    loadMessages(sessionId);
    loadSessions();
    
    window.open('/ticket/list', '_blank');
}

function playNotificationSound() {
    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        oscillator.frequency.value = 800;
        oscillator.type = 'sine';
        gainNode.gain.value = 0.3;
        
        oscillator.start();
        setTimeout(() => {
            oscillator.stop();
        }, 200);
    } catch (e) {
        console.log('Audio not supported');
    }
}

function appendMessageToChat(msg) {
    hideWelcomeScreen();
    
    const container = document.getElementById('messagesContainer');
    const sender = msg.senderType === 1 ? 'ai' : 'user';
    const time = msg.createTime ? new Date(msg.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '';
    
    const messageEl = document.createElement('div');
    messageEl.className = `message ${sender}`;
    
    const avatarClass = sender === 'user' ? 'style="background: linear-gradient(135deg, #0071e3 0%, #0077ed 100%);"' : '';
    const avatarText = sender === 'user' ? '我' : '客';
    
    messageEl.innerHTML = `
        <div class="message-avatar">
            <div class="avatar" ${avatarClass}>${avatarText}</div>
        </div>
        <div>
            <div class="message-content">${escapeHtml(msg.content)}</div>
            <div class="message-time">${time}</div>
        </div>
    `;
    container.appendChild(messageEl);
    container.scrollTop = container.scrollHeight;
}

function hideWelcomeScreen() {
    const welcome = document.getElementById('welcomeScreen');
    if (welcome) {
        welcome.style.display = 'none';
    }
}

function loadSessions() {
    fetch(`/api/chat/agent/sessions?agentId=${currentAgentId}`)
        .then(res => res.json())
        .then(res => {
            if (res.code === 200) {
                renderSessionList(res.data.list);
                updateTabCounts();
            }
        })
        .catch(err => console.error('加载会话列表失败', err));
}

function updateTabCounts() {
    Promise.all([
        fetch(`/api/chat/agent/sessions?agentId=${currentAgentId}`).then(r => r.json())
    ]).then(([sessions]) => {
        const list = sessions.data?.list || [];
        const pending = list.filter(s => s.status === 1).length;
        const active = list.filter(s => s.status === 2).length;
        const done = list.filter(s => s.status === 3).length;
        
        const tabs = document.querySelectorAll('.sidebar-tab');
        tabs[0].querySelector('.count').textContent = pending;
        tabs[1].querySelector('.count').textContent = active;
    });
}

function renderSessionList(sessions) {
    const container = document.querySelector('.session-list');
    container.innerHTML = '';

    if (!sessions || sessions.length === 0) {
        container.innerHTML = '<div class="session-item" style="justify-content: center; color: var(--text-tertiary);">暂无会话</div>';
        return;
    }

    sessions.forEach(session => {
        const item = document.createElement('div');
        item.className = 'session-item' + (session.sessionId === currentSessionId ? ' active' : '');
        item.onclick = () => selectSession(session);
        
        const statusTag = getStatusTag(session.status);
        const time = formatTime(session.updateTime);
        
        item.innerHTML = `
            <div class="session-header">
                <span class="session-user">${session.nickname || ('用户 #' + session.userId) || '匿名'}</span>
                <span class="session-time">${time}</span>
            </div>
            <div class="session-preview">会话ID: ${session.sessionId.substring(0, 20)}...</div>
            <div class="session-tags">
                <span class="session-tag">${statusTag}</span>
            </div>
        `;
        container.appendChild(item);
    });
}

function getStatusTag(status) {
    const map = { 1: '待处理', 2: '进行中', 3: '已完成' };
    return map[status] || '未知';
}

function formatTime(timeStr) {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    return date.toLocaleDateString('zh-CN');
}

async function selectSession(session) {
    currentSessionId = session.sessionId;
    currentUserId = session.userId;
    
    document.querySelectorAll('.session-item').forEach(el => el.classList.remove('active'));
    event.currentTarget.classList.add('active');
    
    document.getElementById('chatUserName').textContent = session.nickname || ('用户 #' + session.userId) || '匿名';
    document.getElementById('chatUserSource').textContent = `会话ID: ${session.sessionId.substring(0, 15)}...`;
    
    if (session.status === 1) {
        await fetch(`/api/chat/agent/accept/${session.sessionId}?agentId=${currentAgentId}`, { method: 'POST' });
        loadSessions();
    }
    
    loadMessages(session.sessionId);
    updateUserInfo(session);
}

function loadMessages(sessionId) {
    fetch(`/api/chat/message/list?sessionId=${sessionId}`)
        .then(res => res.json())
        .then(res => {
            if (res.code === 200) {
                renderMessages(res.data.list);
            }
        })
        .catch(err => console.error('加载消息失败', err));
}

function renderMessages(messages) {
    const container = document.getElementById('messagesContainer');
    container.innerHTML = '';

    if (!messages || messages.length === 0) {
        container.innerHTML = '<div style="text-align: center; color: var(--text-tertiary); padding: 20px;">暂无消息</div>';
        return;
    }

    hideWelcomeScreen();
    messages.forEach(msg => appendMessageToChat(msg));
}

function updateUserInfo(session) {
    const infoValue = document.querySelectorAll('.info-value');
    infoValue[0].textContent = session.nickname || ('用户 #' + session.userId) || '匿名';
    infoValue[1].textContent = session.createTime ? new Date(session.createTime).toLocaleDateString('zh-CN') : '-';
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !currentSessionId) return;
    
    fetch('/api/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sessionId: currentSessionId,
            content: content,
            senderType: 2,
            senderId: currentAgentId
        })
    })
    .then(res => res.json())
    .then(res => {
        if (res.code === 200) {
            input.value = '';
        }
    })
    .catch(err => console.error('发送消息失败', err));
}

function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

function createTicket() {
    if (!currentSessionId) {
        alert('请先选择一个会话');
        return;
    }
    window.open('/ticket/list?sessionId=' + currentSessionId + '&userId=' + (currentUserId || ''), '_blank');
}

function transferSession() {
    alert('转接功能开发中...');
}

function closeSession() {
    if (!currentSessionId) {
        alert('请先选择一个会话');
        return;
    }
    
    if (!confirm('确定要结束这个会话吗？')) return;
    
    fetch(`/api/chat/agent/close/${currentSessionId}`, { method: 'POST' })
        .then(res => res.json())
        .then(res => {
            if (res.code === 200) {
                currentSessionId = null;
                document.getElementById('messagesContainer').innerHTML = '';
                loadSessions();
            }
        })
        .catch(err => console.error('结束会话失败', err));
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.querySelectorAll('.sidebar-tab').forEach((tab, index) => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.sidebar-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        currentStatusFilter = index === 0 ? 1 : index === 1 ? 2 : 3;
        loadSessions();
    });
});

document.addEventListener('DOMContentLoaded', init);
