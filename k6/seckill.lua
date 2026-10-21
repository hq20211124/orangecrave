// wrk Lua 脚本：秒杀接口压测
// 用法：wrk -t8 -c500 -d60s -s k6/seckill.lua http://localhost:8080/client-api/oms/order/seckill
// 需要先替换 TOKEN_PLACEHOLDER

local counter = 0

request = function()
  counter = counter + 1
  local body = string.format([[{
    "seckillId": 1,
    "skuId": 1,
    "quantity": 1
  }]])
  return wrk.format("POST", nil, {
    ["Content-Type"] = "application/json",
    ["Authorization"] = "Bearer TOKEN_PLACEHOLDER",
    ["Idempotency-Key"] = "wrk-seckill-" .. counter
  }, body)
end
