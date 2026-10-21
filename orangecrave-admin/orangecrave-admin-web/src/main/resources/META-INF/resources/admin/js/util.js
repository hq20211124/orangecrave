// 日期 / 金额格式化
function esc(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function money(value) {
  if (value === null || value === undefined || value === '') return '-';
  const n = Number(value);
  if (isNaN(n)) return value;
  return '¥' + n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function fmtDate(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').substring(0, 19);
}

// 链接目标只接受 http(s) 与站内相对路径，挡掉 javascript: / data: 这类伪协议
function safeUrl(value) {
  const s = String(value === null || value === undefined ? '' : value).trim();
  if (!s) return '';
  return /^(https?:)?\/\//i.test(s) || s.startsWith('/') ? esc(s) : '';
}

function orderStatusTag(status) {
  const s = ORDER_STATUS[status] || { text: '未知', cls: 'tag-gray' };
  return `<span class="tag ${s.cls}">${s.text}</span>`;
}

function workStatusTag(status) {
  const s = WORK_STATUS[status] || { text: '未知', cls: 'tag-gray' };
  return `<span class="tag ${s.cls}">${s.text}</span>`;
}

function truncate(text, len = 40) {
  if (!text) return '';
  const s = String(text);
  // 备注/昵称/地址快照都是用户上传的文本，直接拼进 HTML 就是存储型 XSS
  const body = s.length > len ? esc(s.substring(0, len)) + '…' : esc(s);
  return s.length > len ? `<span title="${esc(s)}">${body}</span>` : body;
}
