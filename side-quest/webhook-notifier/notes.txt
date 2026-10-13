FR:
Register, update, and remove webhook endpoints for an Account.
Enable or disable an endpoint without deleting its configuration.
Subscribe to selected event types, such as:
task.created
task.updated, optionally filtered to changes in startDate
task.deleted
Deliver matching events to the Account's HTTPS endpoint.
Retry transient failures and expose permanently failing deliveries for investigation and replay.
Let customers verify endpoint ownership and send a test event.
Show delivery health and history so customers can tell whether a webhook is configured correctly and continues to work.
Order of event matters, task updated without task created is not helpful at-all

NFR:
Task writes must not wait for customer HTTP endpoints to respond.
Do not lose an event after the corresponding task change is committed.
Isolate slow or failing customer endpoints from one another.
Bound connection time, response time, retries, and queued work.
Support horizontal scaling of event publishers and delivery workers.
Protect endpoint secrets and authenticate webhook payloads.
Make delivery attempts, failures, backlog, and recovery observable.


Scale:
100 accounts,
2000 projects,
200000 tasks

50% of task change every hour
100k events per hours, approx 28 events per second.


Account identity is derived from the authenticated caller, not trusted from an arbitrary request body.

POST   /v1/webhooks
GET    /v1/webhooks
GET    /v1/webhooks/{webhookId}
PATCH  /v1/webhooks/{webhookId}          # endpoint, subscriptions, enabled
DELETE /v1/webhooks/{webhookId}
POST   /v1/webhooks/{webhookId}/test
GET    /v1/webhooks/{webhookId}/deliveries
POST   /v1/webhooks/{webhookId}/deliveries/{deliveryId}/replay
Example registration:

{
  "url": "https://hooks.example.com/task-events",
  "enabled": true,
  "subscriptions": [
    { "eventType": "task.created" },
    { "eventType": "task.updated", "changedFields": ["startDate"] }
  ]
}

Current scale is less, lets design for 10x business growth
1000 accounts
20000 projects
2M tasks

1M events per hour, approx 350 events/second


BEGIN;

WITH candidate AS (
    SELECT j.id
    FROM jobs j
    WHERE (
        (
            j.status = 'PENDING'
            AND (j.retry_at IS NULL OR j.retry_at <= NOW())
        )
        OR
        (
            j.status = 'PROCESSING'
            AND (j.locked_until IS NULL OR j.locked_until <= NOW())
        )
    )
    AND NOT EXISTS (
        SELECT 1
        FROM jobs earlier
        WHERE earlier.webhook_id = j.webhook_id
          AND (earlier.created_at, earlier.id)
              < (j.created_at, j.id)
          AND earlier.status <> 'SUCCEEDED'
    )
    ORDER BY j.created_at ASC, j.id ASC
    LIMIT 1
    FOR UPDATE OF j SKIP LOCKED
)
UPDATE jobs j
SET status = 'PROCESSING',
    locked_until = NOW() + INTERVAL '60 seconds',
    attempts = attempts + 1
FROM candidate c
WHERE j.id = c.id
RETURNING j.*;

COMMIT;

BEGIN;

-- 1. Lock one dispatch row and its current job.
SELECT
    d.webhook_id,
    d.next_job_id
FROM job_dispatch d
JOIN jobs j
    ON j.id = d.next_job_id
   AND j.webhook_id = d.webhook_id
WHERE
    (
        d.status = 'PENDING'
        AND (d.retry_at IS NULL OR d.retry_at <= NOW())
    )
    OR
    (
        d.status = 'PROCESSING'
        AND d.locked_until <= NOW()
    )
LIMIT 1
FOR UPDATE OF d SKIP LOCKED;

-- 2. Using the webhook_id and next_job_id returned above,
--    update both records within this same transaction.

UPDATE jobs
SET status = 'PROCESSING',
    attempts = attempts + 1,
    locked_until = NOW() + INTERVAL '60 seconds'
WHERE id = :next_job_id
  AND (
      (status = 'PENDING'
       AND (retry_at IS NULL OR retry_at <= NOW()))
      OR
      (status = 'PROCESSING'
       AND locked_until <= NOW())
  )
RETURNING *;

-- Only if exactly one job was claimed:
UPDATE job_dispatch
SET status = 'PROCESSING',
    locked_until = NOW() + INTERVAL '60 seconds'
WHERE webhook_id = :webhook_id;

COMMIT;


create arch diagram

    1. Webhook service
    2. Event services(Task service...)
    3. Fanout service
    4. webhook delivery worker
    5. Archival worker
    6. Account DB
    7. Webhook job DB


    Webhook services - 
        Manage webhook table/DB, CRUD webhook for each account.
        (Webhook Table/DB is read heavy)
        Will use account DB with webhook table, 
        assuming account DB dont have any table that read heavy table and i dont want another DB for it.
        Adding things always complicate design.
    
    Event services - 
        Multiple resources in system can have thier own service based
        on the scale it need. Each service manage any resources and it is the
        one send events for webhook. For example our case task service, which
        sent task.created, task.updated, task.deleted events.

        it achieve reliable event delivery through transactional outbox pattern 
        or change data capture. it doesnt mean atmost one delivery to broker.
        it is atleast one delivery so consumer should be ideompotent by using
        event id.

    Fanout services -
        Consume events from kafka broker[partition by taskId]. Why partition
        by taskId because order matters only per resource here it is task.
        TaskId should be unique across accounts. it helps to acheive higher 
        spread/distribution than partition by accountId.

        Fanout service find webhooks for each account through webservice / directly.
        webservices means two network hop fanoutservice -> webhookservice -> redis(optional)
        -> webhook db/table.

        For this requirement i need fanout service extremly fast, so i directly connect to Account DB.
        Fanout service get webhooks for account in event and insert job per webhook to jobs table.

        Use seperate webhook-job DB to isolate load/scale.

    Webhook delivery workers -
        Each worker can run above query, to find job and get locked.
        locked_until - used like timeout, other worker can pick same job after locked_until. 
                       locked_until can be used to break circuit if webhook is slow.
        retry_after - retry with exponential backoff

    Archival workers - 
        Worker can run during less load time each day and move completed jobs to read-only/append-log database.
        Deletion will be costly in primary DB. Not focusing here as it is out of scope.

    
    Bottlenecks:
        1. Read DB for webhook in fanout service is costlier.
        2. Above query is costlier for picking job.
        3. Webhook job DB can't able to handle on extreme load
    

    1. Read DB for webhook in fanout service is costlier.
        -> Use redis cache for faster retrival 
            version webhook config and webhook service write to database and update redis cache. 
            version helps to overwrite config to outdated one. we also set ttl for cache.
            Another reliable way is to use transactional outbox or CDC to update redis. but it still have below cons.

            Cons: stale config in redis.

            if stale data is not acceptable, 
                we may have to use source DB with read replica and quoram based approach. but it will affect availablity[CAP theorm]
        
        -> Above query is costlier for picking job
            Create webhook-job-dispatch table, row for each webhook id with next_job_id, locked_until, retry_after.
            Worker pick job from dispatch table, work on it and update next job id to dispatch table with columnns reset.
            
        -> Webhook job DB can't able to handle on extreme load
            Still for heavy loaded system, will partition/shard postgres into multiple db with its own set of replicas.
            Shard based on webhook id so jobs for an account spread accross db. will use hash based partitioning to spread much better,
            if webhook id is regular serial number or even if UUID hashing again may spread better.

            Now problem is worker need to aware of DB it should work. I thought of multiple thing like static db node assigned to worker,
            coordinator assign db for each worker based on db load etc.

            I prefer below
                1. Use some dedicated poller, which work on particular db node. pull elgible job and push to message broker.
                2. Worker can independetly work on job and mark it completed or update status properly. so poller can pick in next cycle properly.
                3. To avoid polling continously, i will poll like batch and wait for sometime.

        
            







        



