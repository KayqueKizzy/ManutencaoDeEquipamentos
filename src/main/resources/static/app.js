const state = { equipments: [], tickets: [] };
const $ = (selector) => document.querySelector(selector);
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const encoded = (value) => encodeURIComponent(value);

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
    });
    if (!response.ok) {
        let message = 'Não foi possível concluir a operação.';
        try { message = (await response.json()).message || message; } catch (_) { }
        throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
}

function showToast(message, isError = false) {
    const toast = $('#toast');
    toast.textContent = message;
    toast.classList.toggle('error', isError);
    toast.classList.remove('hidden');
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.add('hidden'), 3200);
}

function statusClass(status) {
    if (status === 'Em uso') return 'status-in-use';
    if (status === 'Em manutenção') return 'status-manutencao';
    return 'status-inativo';
}

function ticketStatusClass(status) {
    return status === 'Concluído' ? 'status-concluido' : 'status-aberto';
}

function priorityClass(priority) {
    return `priority-${priority === 'Alta' ? 'alta' : priority === 'Baixa' ? 'baixa' : 'media'}`;
}

function equipmentSymbol(category) {
    const symbols = { 'Computador': '▣', 'Projetor': '▤', 'Impressora': '▧', 'Rede': '⌁', 'Áudio e vídeo': '◖', 'Climatização': '❋' };
    return symbols[category] || '▣';
}

function renderStats() {
    $('#stat-total').textContent = state.equipments.length;
    $('#stat-active').textContent = state.equipments.filter(item => item.status === 'Em uso').length;
    $('#stat-repair').textContent = state.equipments.filter(item => item.status === 'Em manutenção').length;
    const openTickets = state.tickets.filter(item => item.status === 'Aberto').length;
    $('#stat-open').textContent = openTickets;
    $('#ticket-count').textContent = openTickets;
}

function renderEquipments() {
    const list = $('#equipment-list');
    const query = $('#search').value.trim().toLocaleLowerCase('pt-BR');
    const filter = $('#status-filter').value;
    const items = state.equipments.filter(item => {
        const text = [item.name, item.category, item.assetTag, item.location, item.brand, item.model].join(' ').toLocaleLowerCase('pt-BR');
        return (!query || text.includes(query)) && (!filter || item.status === filter);
    });
    list.innerHTML = items.map(item => `
        <article class="equipment-card">
            <div class="equipment-symbol">${equipmentSymbol(item.category)}</div>
            <div class="equipment-info">
                <div class="equipment-title">${escapeHtml(item.name)}</div>
                <div class="equipment-meta">${escapeHtml(item.category)}${item.brand ? ` · ${escapeHtml(item.brand)}` : ''}${item.model ? ` ${escapeHtml(item.model)}` : ''}${item.location ? ` · ${escapeHtml(item.location)}` : ''}${item.assetTag ? ` · ${escapeHtml(item.assetTag)}` : ''}</div>
            </div>
            <div class="equipment-side">
                <span class="status-pill ${statusClass(item.status)}">${escapeHtml(item.status || 'Em uso')}</span>
                <div class="card-actions">
                    <button class="icon-button" data-action="ticket" data-id="${escapeHtml(item.id)}" title="Abrir chamado" aria-label="Abrir chamado">＋</button>
                    <button class="icon-button" data-action="edit" data-id="${escapeHtml(item.id)}" title="Editar" aria-label="Editar">✎</button>
                    <button class="icon-button" data-action="delete" data-id="${escapeHtml(item.id)}" title="Excluir" aria-label="Excluir">⌫</button>
                </div>
            </div>
        </article>`).join('');
    $('#equipment-empty').classList.toggle('hidden', items.length > 0);
}

function renderTickets() {
    const openTickets = state.tickets.filter(item => item.status === 'Aberto');
    const list = $('#ticket-list');
    list.innerHTML = openTickets.slice(0, 8).map(item => `
        <article class="ticket-card">
            <div class="ticket-title">${escapeHtml(item.title)}</div>
            <div class="ticket-device">${escapeHtml(item.equipmentName || 'Equipamento')} · ${escapeHtml(item.description || 'Sem descrição')}</div>
            <div class="ticket-bottom">
                <span class="priority-pill ${priorityClass(item.priority)}">${escapeHtml(item.priority || 'Média')} prioridade</span>
                <button class="complete-button" data-action="complete" data-id="${escapeHtml(item.id)}">Concluir chamado</button>
            </div>
        </article>`).join('');
    $('#ticket-empty').classList.toggle('hidden', openTickets.length > 0);
}

async function refresh() {
    const [equipments, tickets] = await Promise.all([
        api('/api/equipamentos'),
        api('/api/manutencoes')
    ]);
    state.equipments = equipments;
    state.tickets = tickets;
    renderStats();
    renderEquipments();
    renderTickets();
}

function openEquipmentForm(equipment = null) {
    const form = $('#equipment-form');
    form.reset();
    form.elements.id.value = equipment?.id || '';
    for (const field of ['name', 'category', 'brand', 'model', 'assetTag', 'location', 'status', 'description']) {
        if (equipment && equipment[field] != null) form.elements[field].value = equipment[field];
    }
    $('#equipment-modal-title').textContent = equipment ? 'Editar equipamento' : 'Novo equipamento';
    form.querySelector('[type="submit"]').textContent = equipment ? 'Salvar alterações' : 'Salvar equipamento';
    $('#equipment-modal').classList.remove('hidden');
    form.elements.name.focus();
}

function openMaintenanceForm(equipment) {
    const form = $('#maintenance-form');
    form.reset();
    form.elements.equipmentId.value = equipment.id;
    $('#selected-equipment').textContent = `${equipment.name} · ${equipment.location || 'Local não informado'}`;
    $('#maintenance-modal').classList.remove('hidden');
    form.elements.title.focus();
}

function closeModal(id) { $(`#${id}`).classList.add('hidden'); }

document.addEventListener('click', async (event) => {
    const close = event.target.closest('[data-close]');
    if (close) { closeModal(close.dataset.close); return; }
    if (event.target.classList.contains('modal-backdrop')) { event.target.classList.add('hidden'); return; }
    const button = event.target.closest('[data-action]');
    if (!button) return;
    const { action, id } = button.dataset;
    try {
        if (action === 'edit') {
            const equipment = state.equipments.find(item => item.id === id);
            if (equipment) openEquipmentForm(equipment);
        } else if (action === 'ticket') {
            const equipment = state.equipments.find(item => item.id === id);
            if (equipment) openMaintenanceForm(equipment);
        } else if (action === 'delete') {
            const equipment = state.equipments.find(item => item.id === id);
            if (equipment && confirm(`Excluir “${equipment.name}”? O histórico de chamados será preservado.`)) {
                await api(`/api/equipamentos/${encoded(id)}`, { method: 'DELETE' });
                showToast('Equipamento excluído.');
                await refresh();
            }
        } else if (action === 'complete') {
            await api(`/api/manutencoes/${encoded(id)}/concluir`, { method: 'POST' });
            showToast('Chamado concluído.');
            await refresh();
        }
    } catch (error) { showToast(error.message, true); }
});

$('#add-equipment').addEventListener('click', () => openEquipmentForm());
$('#add-equipment-secondary').addEventListener('click', () => openEquipmentForm());
$('#empty-add').addEventListener('click', () => openEquipmentForm());
$('#search').addEventListener('input', renderEquipments);
$('#status-filter').addEventListener('change', renderEquipments);

$('#equipment-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries());
    const id = data.id;
    delete data.id;
    try {
        await api(id ? `/api/equipamentos/${encoded(id)}` : '/api/equipamentos', {
            method: id ? 'PUT' : 'POST', body: JSON.stringify(data)
        });
        closeModal('equipment-modal');
        showToast(id ? 'Equipamento atualizado.' : 'Equipamento cadastrado.');
        await refresh();
    } catch (error) { showToast(error.message, true); }
});

$('#maintenance-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries());
    const id = data.equipmentId;
    delete data.equipmentId;
    try {
        await api(`/api/manutencoes/equipamento/${encoded(id)}`, { method: 'POST', body: JSON.stringify(data) });
        closeModal('maintenance-modal');
        showToast('Chamado aberto e equipamento marcado para manutenção.');
        await refresh();
    } catch (error) { showToast(error.message, true); }
});

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') document.querySelectorAll('.modal-backdrop:not(.hidden)').forEach(modal => modal.classList.add('hidden'));
});

refresh().catch(error => showToast(`Não foi possível carregar os dados. ${error.message}`, true));
