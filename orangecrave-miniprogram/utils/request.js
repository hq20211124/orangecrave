// 统一请求封装（见 07 篇 §3）：Authorization: Bearer <token>，{code,msg,data}，401 重新登录
const config = require('./config.js');

// 幂等键：同 method+url+body 得到同键，服务端据此去重（见 02 篇 §1.5）
function idemKey(method, path, body) {
  const text = method + '|' + path + '|' + JSON.stringify(body || {});
  let h = 2166136261 >>> 0;
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i);
    h = Math.imul(h, 16777619) >>> 0;
  }
  return 'idem-' + h.toString(16) + '-' + text.length.toString(16);
}

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const method = options.method || 'GET';
    const header = Object.assign({
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }, options.header || {});
    if (method !== 'GET') {
      header['Idempotency-Key'] = idemKey(method, path, options.data);
    }
    wx.request({
      url: config.apiBase + path,
      method: method,
      data: options.data,
      header: header,
      success(res) {
        const body = res.data;
        if (body.code === 401) {
          wx.removeStorageSync('token');
          reject(new Error('未登录'));
          return;
        }
        if (body.code !== 0) {
          wx.showToast({ title: body.msg || '请求失败', icon: 'none' });
          reject(new Error(body.msg || '请求失败'));
          return;
        }
        resolve(body.data);
      },
      fail(err) {
        wx.showToast({ title: '网络异常', icon: 'none' });
        reject(err);
      }
    });
  });
}

function upload(filePath) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    wx.uploadFile({
      url: config.apiBase + '/client-api/file/upload',
      filePath,
      name: 'file',
      header: { 'Authorization': token ? 'Bearer ' + token : '' },
      success(res) {
        const body = JSON.parse(res.data);
        if (body.code !== 0) {
          reject(new Error(body.msg || '上传失败'));
          return;
        }
        resolve(body.data);
      },
      fail: reject
    });
  });
}

module.exports = {
  get: (path, data) => request(path, { method: 'GET', data }),
  post: (path, data) => request(path, { method: 'POST', data }),
  put: (path, data) => request(path, { method: 'PUT', data }),
  upload
};
