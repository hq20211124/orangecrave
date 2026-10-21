const request = require('../../utils/request.js');

Page({
  data: { list: [] },

  onLoad() {
    getApp().ensureLogin().then(() => {
      request.get('/client-api/pms/spu/page', { pageNum: 1, pageSize: 20 })
        .then(page => this.setData({ list: (page && page.list) || [] }));
    });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/goods/detail?id=' + e.currentTarget.dataset.id });
  }
});
