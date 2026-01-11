-- =====================================================
-- Migrazione inv_id da VARCHAR(50) a SERIAL
-- =====================================================
-- Converte la colonna inv_id da VARCHAR a INTEGER auto-incrementale
--
-- IMPORTANTE: Eseguire questo script PRIMA di modificare backend/Android
--
-- Esecuzione:
-- PGPASSWORD='iniAD16Z77oS' psql -h 57.129.5.234 -p 5432 -U rfidmanager -d rfid_db -f backend/src/db/migrate_inv_id_to_serial.sql
-- =====================================================

-- 1. Backup dati esistenti (tabella temporanea)
CREATE TEMP TABLE inventories_backup AS SELECT * FROM "Inventories";
CREATE TEMP TABLE inventory_items_backup AS SELECT * FROM "Inventory_Items";

-- 2. Eliminare FK constraint da Inventory_Items
ALTER TABLE "Inventory_Items" DROP CONSTRAINT IF EXISTS fk_invitem_inv;

-- 3. Creare sequence se non esiste
CREATE SEQUENCE IF NOT EXISTS inventories_inv_id_seq;

-- 4. Modificare tipo colonna inv_id in Inventories
ALTER TABLE "Inventories" DROP CONSTRAINT IF EXISTS "Inventories_pkey";

-- Convertire colonna in INTEGER
-- Se esistono valori VARCHAR che sono numeri, li converte
-- Altrimenti usa il prossimo valore della sequence
ALTER TABLE "Inventories" ALTER COLUMN inv_id TYPE INTEGER USING (
  CASE
    WHEN inv_id ~ '^[0-9]+$' THEN inv_id::INTEGER
    ELSE nextval('inventories_inv_id_seq'::regclass)
  END
);

-- Impostare default con sequence
ALTER TABLE "Inventories" ALTER COLUMN inv_id SET DEFAULT nextval('inventories_inv_id_seq');

-- Ricreare PRIMARY KEY
ALTER TABLE "Inventories" ADD PRIMARY KEY (inv_id);

-- 5. Modificare tipo colonna inventory_id in Inventory_Items
ALTER TABLE "Inventory_Items" ALTER COLUMN inventory_id TYPE INTEGER USING (
  CASE
    WHEN inventory_id ~ '^[0-9]+$' THEN inventory_id::INTEGER
    ELSE NULL
  END
);

-- 6. Ricreare FK constraint
ALTER TABLE "Inventory_Items" ADD CONSTRAINT fk_invitem_inv
  FOREIGN KEY (inventory_id) REFERENCES "Inventories"(inv_id) ON DELETE CASCADE;

-- 7. Aggiornare sequence con valore corrente massimo
SELECT setval('inventories_inv_id_seq',
  COALESCE((SELECT MAX(inv_id) FROM "Inventories"), 0) + 1,
  false
);

-- 8. Verifica risultato
SELECT
  column_name,
  data_type,
  column_default,
  is_nullable
FROM information_schema.columns
WHERE table_name = 'Inventories' AND column_name = 'inv_id';

-- Output atteso:
-- column_name | data_type | column_default                            | is_nullable
-- inv_id      | integer   | nextval('inventories_inv_id_seq'::regclass) | NO

COMMIT;

-- =====================================================
-- Fine Migrazione
-- =====================================================
