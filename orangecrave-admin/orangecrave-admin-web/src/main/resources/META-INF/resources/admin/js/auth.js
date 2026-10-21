// token 管理 + 权限（见 06 篇 §3.2）。前端隐藏只是体验优化，不是安全边界。
function getToken() {
  return sessionStorage.getItem('token');
}

function setAuth(data) {
  sessionStorage.setItem('token', data.token);
  sessionStorage.setItem('perms', JSON.stringify(data.perms || []));
  sessionStorage.setItem('nickname', data.nickname || '');
}

function hasPerm(key) {
  return JSON.parse(sessionStorage.getItem('perms') || '[]').includes(key);
}

// 隐藏无权限的按钮
function applyPerms(root) {
  (root || document).querySelectorAll('[data-perm]').forEach(el => {
    if (!hasPerm(el.dataset.perm)) el.remove();
  });
}

async function logout() {
  try { await http.post('/admin-api/auth/logout'); } catch (e) { /* ignore */ }
  sessionStorage.clear();
  location.href = 'index.html';
}

// 加载当前登录者信息并缓存权限
async function loadUserInfo() {
  const info = await http.get('/admin-api/auth/info');
  sessionStorage.setItem('perms', JSON.stringify(info.perms || []));
  sessionStorage.setItem('nickname', info.nickname || info.username || '');
  return info;
}
