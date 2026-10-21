const request = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    skuId: null,
    qty: 10,
    imageKey: '',
    imageUrl: '',
    price: null,
    promiseVersion: 'v1.0',
    submitting: false
  },

  onLoad(query) {
    this.setData({ skuId: Number(query.skuId) });
    app.ensureLogin().then(() => this.preview());
  },

  onQty(e) {
    this.setData({ qty: Number(e.detail.value) });
  },

  preview() {
    request.post('/client-api/oms/order/preview', { skuId: this.data.skuId, qty: this.data.qty })
      .then(price => this.setData({ price }))
      .catch(() => {});
  },

  // 定制编辑：选图 → 上传 → 上传即审（含版权承诺，见 07 篇 §4）
  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const filePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中' });
        request.upload(filePath)
          .then(data => request.post('/client-api/aud/image/check', {
            imageKey: data.key,
            promiseVersion: this.data.promiseVersion
          }).then(audit => {
            wx.hideLoading();
            this.setData({ imageKey: data.key, imageUrl: data.url });
            wx.showToast({ title: audit.autoResult === 3 ? '图片违规' : '审核通过', icon: 'none' });
          }))
          .catch(() => wx.hideLoading());
      }
    });
  },

  submit() {
    if (this.data.submitting) return;
    if (!this.data.imageKey) {
      wx.showToast({ title: '请先上传图片', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    const receiver = JSON.stringify({ name: '演示收货人', phone: '13800000000', detail: '演示地址' });
    request.post('/client-api/oms/order/submit', {
      skuId: this.data.skuId,
      qty: this.data.qty,
      channelId: app.globalData.channelId,
      receiverSnapshot: receiver,
      promiseVersion: this.data.promiseVersion,
      images: [{ slotIndex: 0, imageUrl: this.data.imageUrl, copies: this.data.qty }]
    }).then(order => {
      wx.redirectTo({ url: '/pages/order/detail?id=' + order.id });
    }).catch(() => this.setData({ submitting: false }));
  }
});
