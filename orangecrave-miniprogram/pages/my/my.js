const app = getApp();

Page({
  data: { nickname: '', userId: null },

  onShow() {
    app.ensureLogin().then(data => {
      this.setData({ nickname: data.nickname, userId: data.userId });
    });
  },

  goOrders() {
    wx.navigateTo({ url: '/pages/order/list' });
  }
});
