const request = require('../../utils/request.js');

Page({
  data: { series: [], spus: [] },

  onLoad() {
    getApp().ensureLogin().then(() => {
      request.get('/client-api/pms/series/list').then(list => this.setData({ series: list || [] }));
      request.get('/client-api/pms/spu/page', { pageNum: 1, pageSize: 10 })
        .then(page => this.setData({ spus: (page && page.list) || [] }));
    });
  },

  goGoods(e) {
    wx.navigateTo({ url: '/pages/goods/detail?id=' + e.currentTarget.dataset.id });
  },

  goOrders() {
    wx.navigateTo({ url: '/pages/order/list' });
  },

  goMy() {
    wx.navigateTo({ url: '/pages/my/my' });
  }
});
