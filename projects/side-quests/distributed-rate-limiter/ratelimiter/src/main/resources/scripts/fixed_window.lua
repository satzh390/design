-- fixed_window.lua
--
-- KEYS[1] = key
-- ARGV[1] = limit
-- ARGV[2] = window_seconds
--
-- Returns:
-- [1] = allowed (1 or 0)
-- [2] = remaining quota
-- [3] = retry-after milliseconds

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_seconds = tonumber(ARGV[2])

local now = redis.call('TIME')
local now_seconds = tonumber(now[1])
local window_start = now_seconds - (now_seconds % window_seconds)
local bucket_key = key .. ':' .. window_start

local count = redis.call('INCR', bucket_key)
if count == 1 then
    redis.call('EXPIRE', bucket_key, window_seconds)
end

local allowed = 0
local retry_after_ms = 0
if count <= limit then
    allowed = 1
end

if allowed == 0 then
    retry_after_ms = (window_seconds - (now_seconds % window_seconds)) * 1000
end

local remaining = math.max(0, limit - count)
return { allowed, remaining, retry_after_ms }
