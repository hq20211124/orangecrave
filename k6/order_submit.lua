// wrk Lua 脚本：下单接口压测
// 用法：wrk -t4 -c50 -d60s -s k6/order_submit.lua http://localhost:8080/client-api/oms/order/submit
// 需要先替换 TOKEN_PLACEHOLDER

local counter = 0

request = function()
  counter = counter + 1
  local body = string.format([[{
    "items": [{"skuId": 1, "quantity": 1}],
    "addressId": 1,
    "remark": "wrk 压测"
  }]])
  return wrk.format("POST", nil, {
    ["Content-Type"] = "application/json",
    ["Authorization"] = "Bearer TOKEN_PLACEHOLDER",
    ["Idempotency-Key"] = "wrk-" .. counter
  }, body)
end
