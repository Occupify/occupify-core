-- =========================================================================
-- V5: Alter contracts table and create cancellation_contracts table
-- =========================================================================

-- 1. Alter contracts table to add contract terms and job details
ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS terms_file_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS job_description TEXT,
    ADD COLUMN IF NOT EXISTS salary_due_date VARCHAR(100);

-- 2. Create cancellation_contracts table
CREATE TABLE IF NOT EXISTS cancellation_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    requested_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    evidence_file_key VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACCEPTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Indexes for cancellation_contracts
CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_contract_id ON cancellation_contracts(contract_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_requested_by_user_id ON cancellation_contracts(requested_by_user_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_status ON cancellation_contracts(status);
