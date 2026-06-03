--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Migration script to update uclouvain_item_authority_metadata_enhancement 
-- to match the new ItemToEnhance Hibernate entity.
--
-- @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>

-- NOTE: In h2, multiple DROPs in one ALTER is not allowed.
-- Drop existing constraints.
ALTER TABLE uclouvain_item_authority_metadata_enhancement 
    DROP CONSTRAINT IF EXISTS item_enhancement_source_fkey;
ALTER TABLE uclouvain_item_authority_metadata_enhancement 
    DROP CONSTRAINT IF EXISTS item_enhancement_target_fkey;
ALTER TABLE uclouvain_item_authority_metadata_enhancement 
    DROP CONSTRAINT IF EXISTS unique_enhancement;
-- Rename the column.
ALTER TABLE uclouvain_item_authority_metadata_enhancement 
    RENAME COLUMN source_uuid TO item_uuid;
-- Remove the unused column.
ALTER TABLE uclouvain_item_authority_metadata_enhancement 
    DROP COLUMN target_uuid;
-- Add the new 'entity_type' column.
ALTER TABLE uclouvain_item_authority_metadata_enhancement 
    ADD COLUMN entity_type VARCHAR(255) NOT NULL;
-- Define the new Primary Key (which also ensures uniqueness).
ALTER TABLE uclouvain_item_authority_metadata_enhancement 
    ADD CONSTRAINT uclouvain_item_authority_metadata_enhancement_pkey PRIMARY KEY (item_uuid);