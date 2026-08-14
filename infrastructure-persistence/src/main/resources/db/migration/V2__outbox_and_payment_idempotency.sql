-- El pago es la operacion no reintentable del sistema: la unicidad de la clave de
-- idempotencia la garantiza la base de datos, no el codigo de aplicacion.
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(128);
UPDATE payments SET idempotency_key = id::text WHERE idempotency_key IS NULL;
ALTER TABLE payments ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE payments ADD CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key);

-- El relay busca siempre las filas pendientes mas antiguas.
CREATE INDEX idx_outbox_unpublished ON outbox_events (occurred_on) WHERE published = FALSE;
