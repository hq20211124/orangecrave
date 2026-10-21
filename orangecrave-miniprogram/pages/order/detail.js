const request = require('../../utils/request.js');
const config = require('../../utils/config.js');

Page({
  data: { order: null, statusText: '', logs: [] },

  onLoad(query) {
    this.id = query.id;
    getApp().ensureLogin().then(() => this.load());
  },

  load() {
    request.get('/client-api/oms/order/' + this.id).then(order => {
      this.setData({
        order,
        statusText: (config.ORDER_STATUS[order.status] || {}).text || '未知'
      });
    });
    request.get('/client-api/oms/order/' + this.id + '/timeline').then(logs => {
      const list = (logs || []).map(l => ({
        text: (l.fromStatus == null ? '创建订单' : (config.ORDER_STATUS[l.fromStatus] || {}).text)
          + ' → ' + ((config.ORDER_STATUS[l.toStatus] || {}).text),
        time: l.createTime
      }));
      this.setData({ logs: list });
    });
  },

  pay() {
    request.post('/client-api/oms/order/' + this.id + '/pay').then(() => {
      wx.showToast({ title: '支付成功（演示）' });
      this.load();
    });
  },

  goAftersale() {
    wx.navigateTo({ url: '/pages/aftersale/apply?orderId=' + this.id });
  }
});
