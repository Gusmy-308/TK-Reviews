--优惠券ID
local voucherId = ARGV[1]
--用户ID
local userId = ARGV[2]
--订单Id
local orderId = ARGV[3]

local stockKey = 'seckill:stock:'.. voucherId
local orderKey='seckill:order:'.. voucherId
--判断库存是否足够
if (tonumber(redis.call("get", stockKey)) <= 0 )then
    return 1
end
--判断用户是否已购买
if(redis.call('sismember', orderKey, userId) == 1) then
    return 2
end
--扣库存
redis.call("incrby", stockKey, -1)
--添加到订单列表
redis.call("sadd", orderKey, userId)
--发送消息到队列中
redis.call("XADD", "stream.orders", "*", "voucherId", voucherId, "userId", userId, "id", orderId)
return 0




