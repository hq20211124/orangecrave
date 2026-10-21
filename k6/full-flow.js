// 全链路压测脚本：40% 浏览 / 30% 查订单 / 20% 下单 / 10% 秒杀
// 用法：k6 run k6/full-flow.js --env BASE_URL=http://localhost:8080 --env TOKEN=<your_token>

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const orderLatency = new Trend('order_submit_latency');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

export const options = {
  stages: [
    { duration: '2m', target: 50 },
    { duration: '5m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '5m', target: 100 },
    { duration: '2m', target: 200 },
    { duration: '5m', target: 200 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(99)<500'],
    errors: ['rate<0.01'],
  },
};

export function setup() {
  if (TOKEN) return { token: TOKEN };
  const res = http.post(`${BASE_URL}/admin-api/auth/login`, JSON.stringify({
    username: 'admin',
    password: 'admin123'
  }), { headers: { 'Content-Type': 'application/json' } });
  const body = JSON.parse(res.body);
  return { token: body.data ? body.data.token : '' };
}

export default function(data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  const rand = Math.random();

  // 40% 浏览商品
  if (rand < 0.4) {
    const res = http.get(`${BASE_URL}/client-api/pms/spu/list?page=1&pageSize=10`, { headers });
    check(res, { '商品列表 200': (r) => r.status === 200 });
  }
  // 30% 查订单
  else if (rand < 0.7) {
    const res = http.get(`${BASE_URL}/admin-api/oms/order/list?page=1&pageSize=20`, { headers });
    check(res, { '订单列表 200': (r) => r.status === 200 });
  }
  // 20% 下单
  else if (rand < 0.9) {
    const res = http.post(`${BASE_URL}/client-api/oms/order/submit`, JSON.stringify({
      items: [{ skuId: 1, quantity: 1 }],
      addressId: 1,
      remark: 'k6 压测'
    }), {
      headers: { ...headers, 'Idempotency-Key': `k6-${__VU}-${__ITER}` }
    });
    check(res, { '下单 200': (r) => r.status === 200 });
    orderLatency.add(res.timings.duration);
  }
  // 10% 秒杀
  else {
    const res = http.post(`${BASE_URL}/client-api/oms/order/seckill`, JSON.stringify({
      seckillId: 1,
      skuId: 1,
      quantity: 1
    }), {
      headers: { ...headers, 'Idempotency-Key': `k6-seckill-${__VU}-${__ITER}` }
    });
    check(res, {
      '秒杀响应': (r) => r.status === 200 || r.status === 429
    });
  }

  sleep(1);
}
