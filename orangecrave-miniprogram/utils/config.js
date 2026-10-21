// 环境配置（见 07 篇 §5）：演示版只连自建后台
const ENV = {
  dev: { apiBase: 'http://localhost:8080' },
  test: { apiBase: 'http://localhost:8080' }
};

const current = 'dev';

module.exports = {
  apiBase: ENV[current].apiBase,
  // 订单状态枚举（见 07 篇 §3.3）——★ 为后台特有状态
  ORDER_STATUS: {
    0: { text: '待付款', color: '#FF9500' },
    1: { text: '待成团', color: '#FF9500' },
    2: { text: '待审核', color: '#007AFF' },
    3: { text: '待生产', color: '#007AFF' },
    4: { text: '生产中', color: '#007AFF' },
    5: { text: '质检中', color: '#007AFF' },
    6: { text: '待发货', color: '#007AFF' },
    7: { text: '已发货', color: '#34C759' },
    8: { text: '已完成', color: '#8E8E93' },
    9: { text: '已取消', color: '#8E8E93' },
    10: { text: '售后中', color: '#FF9500' },
    11: { text: '已退款', color: '#8E8E93' },
    13: { text: '已取消', color: '#8E8E93' }
  }
};
