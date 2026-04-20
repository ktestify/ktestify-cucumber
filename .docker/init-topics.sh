#!/bin/bash
# ============================================================
# ktestify — Kafka topic + Avro schema initializer
#
# Runs once inside the 'init' container after broker and
# schema-registry are healthy.
#
# Schema subjects registered (two strategies per schema):
#
#   TopicNameStrategy  : <topicName>-value
#     → used when no schemaName is given in the feature DataTable
#     → AbstractKafkaProducer falls back to topic.getNamespacedTopic() + "-value"
#
#   SchemaNameStrategy : <SchemaName>-value
#     → used when schemaName = "Order" is set in the feature DataTable
#     → AbstractKafkaProducer looks up context.getSchemaName() + "-value"
# ============================================================
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP_SERVERS:-broker:29092}"
SR_URL="${SCHEMA_REGISTRY_URL:-http://schema-registry:8081}"
REPLICATION=1
PARTITIONS=1

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[init]${NC} $*"; }
warn() { echo -e "${YELLOW}[init]${NC} $*"; }

# ── 1. Create Kafka topics ────────────────────────────────────
log "Creating Kafka topics on ${BOOTSTRAP}..."

create_topic() {
  local topic="$1"
  if kafka-topics --bootstrap-server "${BOOTSTRAP}" --list | grep -qx "${topic}"; then
    warn "Topic '${topic}' already exists — skipping."
  else
    kafka-topics --bootstrap-server "${BOOTSTRAP}" \
      --create \
      --topic "${topic}" \
      --partitions "${PARTITIONS}" \
      --replication-factor "${REPLICATION}"
    log "Created topic '${topic}'"
  fi
}

create_topic "ktestify.raw-orders"
create_topic "ktestify.raw-orders-processed"
create_topic "ktestify.avro-orders"
create_topic "ktestify.avro-orders-processed"
# Roundtrip topics for self-contained framework tests
create_topic "ktestify.raw-roundtrip"
create_topic "ktestify.avro-roundtrip"
create_topic "ktestify.raw-batch"

log "All topics created."

# ── 2. Register Avro schemas in Schema Registry ───────────────
log "Registering Avro schemas in ${SR_URL}..."

register_schema() {
  local subject="$1"
  local schema_file="$2"

  if [ ! -f "${schema_file}" ]; then
    warn "Schema file not found: ${schema_file} — skipping."
    return
  fi

  # Escape the schema JSON for the SR REST payload
  local schema_json
  schema_json=$(python3 -c "
import json, sys
with open('${schema_file}') as f:
    content = f.read()
print(json.dumps({'schema': content}))
")

  local http_code
  http_code=$(curl -s -o /tmp/sr_response.json -w "%{http_code}" \
    -X POST "${SR_URL}/subjects/${subject}/versions" \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    -d "${schema_json}")

  if [ "${http_code}" = "200" ]; then
    local schema_id
    schema_id=$(grep -o '"id":[0-9]*' /tmp/sr_response.json | grep -o '[0-9]*')
    log "Registered schema for subject '${subject}' (id=${schema_id})"
  else
    warn "Schema '${subject}' returned HTTP ${http_code}: $(cat /tmp/sr_response.json)"
  fi
}

# Order schema — registered under BOTH subject naming conventions:
#   "Order-value"                    → schemaName = "Order" in the feature DataTable
#   "ktestify.avro-orders-value"     → fallback when no schemaName is set (TopicNameStrategy)
register_schema "Order-value"                          "/schemas/Order.avsc"
register_schema "ktestify.avro-orders-value"           "/schemas/Order.avsc"
register_schema "ktestify.avro-roundtrip-value"        "/schemas/Order.avsc"

# OrderProcessed schema — same dual registration
register_schema "OrderProcessed-value"                 "/schemas/OrderProcessed.avsc"
register_schema "ktestify.avro-orders-processed-value" "/schemas/OrderProcessed.avsc"

log "Schema registration complete."

# ── 3. Seed one raw test message ──────────────────────────────
log "Seeding a raw test message to ktestify.raw-orders..."
echo '{"orderId":"seed-001","amount":99.99,"currency":"EUR","status":"PENDING"}' | \
  kafka-console-producer \
    --bootstrap-server "${BOOTSTRAP}" \
    --topic "ktestify.raw-orders" \
    --property "parse.key=false"

log "Seeding complete."
log "-----------------------------------------------------------"
log "Infrastructure ready!"
log "  Kafka broker    : localhost:9092"
log "  Schema Registry : http://localhost:8081"
log "  Topics          : ktestify.raw-orders"
log "                    ktestify.raw-orders-processed"
log "                    ktestify.avro-orders"
log "                    ktestify.avro-orders-processed"
log "  SR subjects     : Order-value, ktestify.avro-orders-value"
log "                    OrderProcessed-value, ktestify.avro-orders-processed-value"
log "-----------------------------------------------------------"



