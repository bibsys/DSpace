--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- CREATE uclouvain_item_enrichment TABLE: one row per attempt to enrich an item
-- from an external source (CrossRef, PubMed, arXiv...) for a given identifier.
-----------------------------------------------------------------------------------

CREATE SEQUENCE uclouvain_item_enrichment_id_seq;

CREATE TABLE uclouvain_item_enrichment
(
    id INTEGER NOT NULL,
    item_uuid UUID NOT NULL REFERENCES item(uuid) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    identifier_field VARCHAR(64) NOT NULL,
    identifier_value VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(1024),
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uclouvain_item_enrichment_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_uclouvain_item_enrichment_lookup
    ON uclouvain_item_enrichment(item_uuid, provider, identifier_value);
