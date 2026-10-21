// 统一请求封装（见 06 篇 §3.1）。与后台接口规范严格对应。
// 幂等键按 method+url+body 生成（FNV-1a）：同一份提交的重复点击得到同一个键，服务端据此去重。
function idemKey(method, url, body) {
  const text = method + '|' + url + '|' + (typeof body === 'string' ? body : JSON.stringify(body || ''));
  let h = 2166136261 >>> 0;
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i);
    h = Math.imul(h, 16777619) >>> 0;
  }
  return 'idem-' + h.toString(16).padStart(8, '0') + '-' + text.length.toString(16);
}

async function request(url, options = {}) {
  const token = sessionStorage.getItem('token');
  const method = (options.method || 'GET').toUpperCase();
  const headers = Object.assign({
    'Content-Type': 'application/json',
    'Authorization': token ? 'Bearer ' + token : ''
  }, options.headers || {});
  // 写操作携带幂等键（见 02 篇 §1.5）
  if (method !== 'GET') {
    headers['Idempotency-Key'] = idemKey(method, url, options.body);
  }

  const res = await fetch(API_PREFIX + url, Object.assign({}, options, { headers }));
  let body;
  try { body = await res.json(); } catch (e) { body = { code: 500, msg: '响应解析失败' }; }

  if (body.code === 401) {
    sessionStorage.clear();
    if (window.top) { window.top.location.href = 'index.html'; } else { location.href = 'index.html'; }
    throw new Error('未登录');
  }
  if (body.code !== 0) {
    Dialog.toast(body.msg || '请求失败');
    throw new Error(body.msg || '请求失败');
  }
  return body.data;
}

const http = {
  get: (url, params) => request(url + qs(params)),
  post: (url, data) => request(url, { method: 'POST', body: JSON.stringify(data || {}) }),
  put: (url, data) => request(url, { method: 'PUT', body: JSON.stringify(data || {}) }),
  del: (url) => request(url, { method: 'DELETE' })
};
