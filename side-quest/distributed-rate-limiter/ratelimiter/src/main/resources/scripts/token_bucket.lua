-- token_bucket.lua
--
-- KEYS[1] = token bucket key
--
-- ARGV[1] = capacity
-- ARGV[2] = refill rate (tokens/sec)
--
-- Returns:
-- [1] = allowed (1 or 0)
-- [2] = remaining tokens
-- [3] = retry-after milliseconds

local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])

-- Get current time from Redis
-- TIME returns:
-- [1] = seconds
-- [2] = microseconds
local redis_time = redis.call('TIME')

local seconds = tonumber(redis_time[1])
local microseconds = tonumber(redis_time[2])

local now_ms =
    (seconds * 1000) +
    math.floor(microseconds / 1000)

-- Read current bucket state
local values = redis.call(
    'HMGET',
    key,
    'token',
    'last_refill_at'
)

local token = tonumber(values[1])
local last_refill_at = tonumber(values[2])

-- First request: initialize bucket
if token == nil then
    token = capacity
    last_refill_at = now_ms
end

-- Protect against clock moving backwards
if now_ms < last_refill_at then
    now_ms = last_refill_at
end

-- Calculate elapsed time
local elapsed_seconds =
    (now_ms - last_refill_at) / 1000

-- Generate new tokens
local generated_tokens =
    elapsed_seconds * refill_rate

-- Refill bucket, capped at capacity
local current_tokens =
    math.min(
        capacity,
        token + generated_tokens
    )

local is_allowed = 0
local retry_after_ms = 0

-- Consume one token if available
if current_tokens >= 1 then

    current_tokens = current_tokens - 1
    is_allowed = 1

else

    -- Time required to generate one token
    retry_after_ms = math.ceil(
        ((1 - current_tokens) / refill_rate) * 1000
    )

end

-- Save state
redis.call(
    'HSET',
    key,
    'token',
    current_tokens,
    'last_refill_at',
    now_ms
)

-- Return:
-- allowed
-- remaining tokens
-- retry-after milliseconds
return {
    is_allowed,
    current_tokens,
    retry_after_ms
}