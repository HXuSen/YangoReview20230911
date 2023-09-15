-- 优惠券ID
local voucherId = ARGV[1]
-- 用户ID
local userId = ARGV[2]
-- 订单ID
local orderId = ARGV[3]
-- 优惠券库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 订单key
local orderKey = 'seckill:order:' .. voucherId
-- 判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end
-- 判断用户是否购买过
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end
-- 减库存
redis.call('incrby',stockKey,-1)
-- 增加订单
redis.call('sadd',orderKey,userId)
-- 发送消息
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',orderId)
return 0