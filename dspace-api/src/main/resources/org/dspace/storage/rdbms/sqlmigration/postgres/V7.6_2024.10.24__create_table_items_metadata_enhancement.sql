--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- This table is used to store queued enhancement for items. This is part of the authority based metadata enhancement system.
--
-- @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
CREATE TABLE uclouvain_item_authority_metadata_enhancement
(
	source_uuid UUID NOT NULL,
	target_uuid UUID NOT NULL,
	date_queued TIMESTAMP NOT NULL,
	CONSTRAINT item_enhancement_source_fkey FOREIGN KEY (source_uuid) REFERENCES item (uuid),
	CONSTRAINT item_enhancement_target_fkey FOREIGN KEY (target_uuid) REFERENCES item (uuid),
	CONSTRAINT unique_enhancement UNIQUE(source_uuid, target_uuid)
);
CREATE INDEX idx_enhancement_date_queued ON uclouvain_item_authority_metadata_enhancement(date_queued);
