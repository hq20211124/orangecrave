const request = require('../../utils/request.js');
const config = require('../../utils/config.js');

Page({
  data: { list: [] },

  onShow() {
    getApp().ensureLogin().then(() => {
      request.get('/client-api/oms/order/mine', { pageNum: 1, pageSize: 20 }).then(page => {
        const list = ((page && page.list) || []).map(o => Object.assign({}, o, {
          statusText: (config.ORDER_STATUS[o.status] || {}).text || '未知'
        }));
        this.setData({ list });
      });
    });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/order/detail?id=' + e.currentTarget.dataset.id });
  }
});
