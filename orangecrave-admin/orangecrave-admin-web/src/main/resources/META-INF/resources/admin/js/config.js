// 全局配置与常量
const API_PREFIX = ''; // 与后台同源

const ORDER_STATUS = {
  0: { text: '待付款', cls: 'tag-orange' },
  1: { text: '待成团', cls: 'tag-orange' },
  2: { text: '待审核', cls: 'tag-blue' },
  3: { text: '待生产', cls: 'tag-blue' },
  4: { text: '生产中', cls: 'tag-blue' },
  5: { text: '质检中', cls: 'tag-blue' },
  6: { text: '待发货', cls: 'tag-blue' },
  7: { text: '已发货', cls: 'tag-green' },
  8: { text: '已完成', cls: 'tag-gray' },
  9: { text: '已取消', cls: 'tag-gray' },
  10: { text: '售后中', cls: 'tag-orange' },
  11: { text: '已退款', cls: 'tag-gray' },
  13: { text: '已取消', cls: 'tag-gray' }
};

const WORK_STATUS = {
  1: { text: '正常', cls: 'tag-green' },
  2: { text: '待审核', cls: 'tag-blue' },
  3: { text: '待修改', cls: 'tag-orange' },
  5: { text: '商品下架', cls: 'tag-gray' },
  6: { text: '作品过期', cls: 'tag-gray' },
  7: { text: '作品违规', cls: 'tag-red' }
};

// 拼查询串
function qs(params) {
  if (!params) return '';
  const parts = [];
  Object.keys(params).forEach(k => {
    const v = params[k];
    if (v !== undefined && v !== null && v !== '') {
      parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v));
    }
  });
  return parts.length ? '?' + parts.join('&') : '';
}
