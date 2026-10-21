const request = require('../../utils/request.js');

Page({
  data: { orderId: null, type: 1, reason: '', amount: '' },

  onLoad(query) {
    this.setData({ orderId: Number(query.orderId) });
  },

  onType(e) {
    this.setData({ type: Number(e.detail.value) });
  },
  onReason(e) {
    this.setData({ reason: e.detail.value });
  },
  onAmount(e) {
    this.setData({ amount: e.detail.value });
  },

  submit() {
    const payload = { orderId: this.data.orderId, type: this.data.type, reason: this.data.reason };
    if (this.data.amount) payload.amount = Number(this.data.amount);
    request.post('/client-api/oms/aftersale/apply', payload).then(() => {
      wx.showToast({ title: '已提交售后' });
      setTimeout(() => wx.navigateBack(), 800);
    });
  }
});
