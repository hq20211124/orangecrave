const request = require('../../utils/request.js');

Page({
  data: { spu: null, skus: [] },

  onLoad(query) {
    this.spuId = query.id;
    getApp().ensureLogin().then(() => {
      request.get('/client-api/pms/spu/' + this.spuId).then(spu => this.setData({ spu }));
      request.get('/client-api/pms/spu/' + this.spuId + '/skus').then(skus => this.setData({ skus: skus || [] }));
    });
  },

  startCustom(e) {
    const skuId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/order/confirm?spuId=' + this.spuId + '&skuId=' + skuId });
  }
});
