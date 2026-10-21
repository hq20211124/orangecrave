const request = require('./utils/request.js');

App({
  globalData: {
    channelId: null,
    userId: null,
    nickname: ''
  },

  onLaunch() {
    this.loginPromise = this.ensureLogin();
  },

  // 首次进入上报渠道（见 07 篇 §4.3）
  visitChannel() {
    request.post('/client-api/channel/visit', { channelCode: 'xhs' })
      .then(data => { this.globalData.channelId = data.channelId; })
      .catch(() => {});
  },

  // 登录：wx.login 拿 code → 换 token（见 07 篇 §3.1）
  ensureLogin() {
    if (this._loginPromise) return this._loginPromise;
    this._loginPromise = new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          request.post('/client-api/auth/wx-login', { code: res.code })
            .then(data => {
              wx.setStorageSync('token', data.token);
              this.globalData.userId = data.userId;
              this.globalData.nickname = data.nickname;
              resolve(data);
            })
            .catch(reject);
        },
        fail: reject
      });
    });
    return this._loginPromise;
  }
});
