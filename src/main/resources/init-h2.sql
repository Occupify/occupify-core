CREATE ALIAS IF NOT EXISTS unaccent FOR "com.occupify.core.util.H2Functions.unaccent";

-- Alter contracts table to add contract terms and job details
ALTER TABLE IF EXISTS contracts ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE IF EXISTS contracts ADD COLUMN IF NOT EXISTS terms_file_key VARCHAR(500);
ALTER TABLE IF EXISTS contracts ADD COLUMN IF NOT EXISTS job_description TEXT;
ALTER TABLE IF EXISTS contracts ADD COLUMN IF NOT EXISTS salary_due_date VARCHAR(100);

-- Create cancellation_contracts table
CREATE TABLE IF NOT EXISTS cancellation_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    reason TEXT NOT NULL,
    evidence_file_key VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACCEPTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_contract_id ON cancellation_contracts(contract_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_requested_by_user_id ON cancellation_contracts(requested_by_user_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_status ON cancellation_contracts(status);

-- Indexes from V5 bids migration
CREATE UNIQUE INDEX IF NOT EXISTS uk_bids_job_freelancer ON bids(job_id, freelancer_id);
CREATE INDEX IF NOT EXISTS idx_bids_freelancer_status ON bids(freelancer_id, status);
