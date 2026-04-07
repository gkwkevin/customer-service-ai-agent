let currentPage = 1;
let currentStatus = null;
let currentPriority = null;
let currentKeyword = '';
let currentUser = null;
let urlSessionId = null;
let urlUserId = null;

function checkLoginStatus() {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    
    if (!token || !userStr) {
        window.location.href = '/login';
        return false;
    }
    
    currentUser = JSON.parse(userStr);
    
    if (currentUser.role < 2) {
        alert('您没有访问权限');
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

async function loadTickets() {
    const params = new URLSearchParams({
        page: currentPage,
        size: 10
    });
    
    if (currentStatus) params.append('status', currentStatus);
    if (currentPriority) params.append('priority', currentPriority);
    if (currentKeyword) params.append('keyword', currentKeyword);

    try {
        const response = await fetch(`/api/ticket/list?${params}`);
        const res = await response.json();
        
        if (res.code === 200) {
            renderTicketList(res.data.list);
            renderPagination(res.data.total, res.data.page, res.data.size);
            updateStats(res.data.total);
        }
    } catch (err) {
        console.error('加载工单失败', err);
    }
}

function renderTicketList(tickets) {
    const container = document.querySelector('.ticket-table');
    const header = container.querySelector('.table-header');
    container.innerHTML = '';
    container.appendChild(header);

    if (!tickets || tickets.length === 0) {
        container.innerHTML += `
            <div class="table-row" style="justify-content: center; padding: 40px;">
                <span style="color: var(--text-tertiary);">暂无工单数据</span>
            </div>
        `;
        return;
    }

    tickets.forEach(ticket => {
        const statusBadge = getStatusBadge(ticket.status);
        const row = document.createElement('div');
        row.className = 'table-row';
        row.innerHTML = `
            <div class="col-ticket">
                <span class="ticket-no">#${ticket.ticketNo}</span>
            </div>
            <div class="col-title">
                <div class="ticket-title">${escapeHtml(ticket.title)}</div>
                <div class="ticket-desc">${escapeHtml(ticket.content || '').substring(0, 50)}${ticket.content && ticket.content.length > 50 ? '...' : ''}</div>
            </div>
            <div class="col-user">
                <div class="user-cell">
                    <div class="avatar">用</div>
                    <span>${ticket.userNickname || ('用户#' + ticket.userId) || '未知'}</span>
                </div>
            </div>
            <div class="col-status">
                <span class="badge ${statusBadge.class}">${statusBadge.text}</span>
            </div>
            <div class="col-assignee">
                <div class="assignee-cell">
                    ${ticket.assigneeId ? `<div class="avatar">客</div><span>${ticket.assigneeNickname || ('客服#' + ticket.assigneeId)}</span>` : '<span style="color: var(--text-tertiary);">-</span>'}
                </div>
            </div>
            <div class="col-action">
                <button class="action-btn" onclick="viewTicket(${ticket.id})">查看</button>
                <button class="action-btn" onclick="deleteTicket(${ticket.id})" style="color: #ff3b30;">删除</button>
            </div>
        `;
        container.appendChild(row);
    });
}

function getStatusBadge(status) {
    const statusMap = {
        1: { text: '待处理', class: 'badge-warning' },
        2: { text: '处理中', class: 'badge-info' },
        3: { text: '已解决', class: 'badge-success' },
        4: { text: '已关闭', class: 'badge-danger' }
    };
    return statusMap[status] || { text: '未知', class: '' };
}

function renderPagination(total, page, size) {
    const totalPages = Math.ceil(total / size);
    const container = document.querySelector('.pagination');
    container.innerHTML = '';

    const prevBtn = document.createElement('button');
    prevBtn.className = 'page-btn';
    prevBtn.disabled = page <= 1;
    prevBtn.innerHTML = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"></polyline></svg>`;
    prevBtn.onclick = () => { if (page > 1) { currentPage--; loadTickets(); } };
    container.appendChild(prevBtn);

    for (let i = 1; i <= Math.min(totalPages, 5); i++) {
        const btn = document.createElement('button');
        btn.className = 'page-btn' + (i === page ? ' active' : '');
        btn.textContent = i;
        btn.onclick = () => { currentPage = i; loadTickets(); };
        container.appendChild(btn);
    }

    const nextBtn = document.createElement('button');
    nextBtn.className = 'page-btn';
    nextBtn.disabled = page >= totalPages;
    nextBtn.innerHTML = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>`;
    nextBtn.onclick = () => { if (page < totalPages) { currentPage++; loadTickets(); } };
    container.appendChild(nextBtn);
}

function updateStats() {
    fetch('/api/ticket/stats')
        .then(res => res.json())
        .then(res => {
            if (res.code === 200) {
                const stats = res.data;
                const values = document.querySelectorAll('.stat-box-value');
                values[0].textContent = stats.total;
                values[1].textContent = stats.pending;
                values[2].textContent = stats.resolved;
                values[3].textContent = stats.resolveRate + '%';
            }
        })
        .catch(err => console.error('加载统计失败', err));
}

function loadAgents() {
    fetch('/api/ticket/agents')
        .then(res => res.json())
        .then(res => {
            if (res.code === 200) {
                const agents = res.data || [];
                const createSelect = document.getElementById('createAssignee');
                const editSelect = document.getElementById('editAssignee');
                
                const options = '<option value="">请选择处理人</option>' + 
                    agents.map(a => `<option value="${a.id}">${a.nickname || a.username}</option>`).join('');
                
                if (createSelect) createSelect.innerHTML = options;
                if (editSelect) editSelect.innerHTML = options;
            }
        })
        .catch(err => console.error('加载客服列表失败', err));
}

function openModal() {
    document.getElementById('modalOverlay').classList.add('active');
}

function closeModal() {
    document.getElementById('modalOverlay').classList.remove('active');
    document.querySelectorAll('.modal input, .modal textarea, .modal select').forEach(el => el.value = '');
}

async function createTicket() {
    const modalBody = document.querySelector('.modal-body');
    const title = modalBody.querySelector('input[type="text"]').value.trim();
    const content = modalBody.querySelector('textarea').value.trim();
    const selects = modalBody.querySelectorAll('select');
    const priority = selects[0].value;
    const assigneeId = selects[1].value;

    if (!title) {
        alert('请输入工单标题');
        return;
    }

    try {
        const response = await fetch('/api/ticket', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title,
                content,
                priority: parseInt(priority),
                assigneeId: assigneeId ? parseInt(assigneeId) : null,
                sessionId: urlSessionId,
                userId: urlUserId ? parseInt(urlUserId) : null
            })
        });
        const res = await response.json();
        
        if (res.code === 200) {
            closeModal();
            loadTickets();
            updateStats();
        } else {
            alert('创建失败: ' + res.message);
        }
    } catch (err) {
        console.error('创建工单失败', err);
        alert('创建失败');
    }
}

async function viewTicket(id) {
    try {
        const response = await fetch(`/api/ticket/${id}`);
        const res = await response.json();
        
        if (res.code === 200) {
            const ticket = res.data;
            document.getElementById('editId').value = ticket.id;
            document.getElementById('editTicketNo').value = '#' + ticket.ticketNo;
            document.getElementById('editTitle').value = ticket.title || '';
            document.getElementById('editContent').value = ticket.content || '';
            document.getElementById('editStatus').value = ticket.status;
            document.getElementById('editPriority').value = ticket.priority;
            document.getElementById('editAssignee').value = ticket.assigneeId || '';
            openEditModal();
        }
    } catch (err) {
        console.error('获取工单详情失败', err);
    }
}

function openEditModal() {
    document.getElementById('editModalOverlay').classList.add('active');
}

function closeEditModal() {
    document.getElementById('editModalOverlay').classList.remove('active');
}

async function submitEdit() {
    const id = document.getElementById('editId').value;
    const title = document.getElementById('editTitle').value.trim();
    const content = document.getElementById('editContent').value.trim();
    const status = document.getElementById('editStatus').value;
    const priority = document.getElementById('editPriority').value;
    const assigneeId = document.getElementById('editAssignee').value;

    if (!title) {
        alert('请输入工单标题');
        return;
    }

    try {
        const response = await fetch(`/api/ticket/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title,
                content,
                status: parseInt(status),
                priority: parseInt(priority),
                assigneeId: assigneeId ? parseInt(assigneeId) : null
            })
        });
        const res = await response.json();
        
        if (res.code === 200) {
            closeEditModal();
            loadTickets();
            updateStats();
        } else {
            alert('保存失败: ' + res.message);
        }
    } catch (err) {
        console.error('保存工单失败', err);
        alert('保存失败');
    }
}

async function deleteTicket(id) {
    if (!confirm('确定要删除这个工单吗？')) return;
    
    try {
        const response = await fetch(`/api/ticket/${id}`, {
            method: 'DELETE'
        });
        const res = await response.json();
        
        if (res.code === 200) {
            loadTickets();
            updateStats();
        } else {
            alert('删除失败: ' + res.message);
        }
    } catch (err) {
        console.error('删除工单失败', err);
        alert('删除失败');
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.getElementById('modalOverlay').addEventListener('click', function(e) {
    if (e.target === this) {
        closeModal();
    }
});

document.getElementById('editModalOverlay').addEventListener('click', function(e) {
    if (e.target === this) {
        closeEditModal();
    }
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeModal();
        closeEditModal();
    }
});

document.querySelector('.search-input')?.addEventListener('input', function(e) {
    currentKeyword = e.target.value.trim();
    currentPage = 1;
    loadTickets();
});

document.querySelectorAll('.filter-select').forEach(select => {
    select.addEventListener('change', function(e) {
        const filterGroup = e.target.closest('.filter-group');
        const label = filterGroup.querySelector('.filter-label').textContent;
        
        if (label === '状态') {
            currentStatus = e.target.value ? parseInt(e.target.value) : null;
        } else if (label === '优先级') {
            currentPriority = e.target.value ? parseInt(e.target.value) : null;
        }
        currentPage = 1;
        loadTickets();
    });
});

document.addEventListener('DOMContentLoaded', function() {
    if (!checkLoginStatus()) {
        return;
    }
    loadTickets();
    updateStats();
    loadAgents();
    
    const urlParams = new URLSearchParams(window.location.search);
    urlSessionId = urlParams.get('sessionId');
    urlUserId = urlParams.get('userId');
    
    if (urlSessionId) {
        openModal();
        const modalBody = document.querySelector('.modal-body');
        const titleInput = modalBody.querySelector('input[type="text"]');
        if (titleInput) {
            titleInput.value = '会话工单: ' + urlSessionId.substring(0, 20);
        }
        const contentTextarea = modalBody.querySelector('textarea');
        if (contentTextarea) {
            contentTextarea.value = '来源会话: ' + urlSessionId;
        }
    }
});
