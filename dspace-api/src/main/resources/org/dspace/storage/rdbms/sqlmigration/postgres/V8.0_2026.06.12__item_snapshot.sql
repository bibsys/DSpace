--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- CREATE uclouvain_item_snapshot TABLE
-----------------------------------------------------------------------------------

CREATE TABLE uclouvain_item_snapshot
(
    uuid UUID NOT NULL REFERENCES item(uuid) ON DELETE CASCADE,
    timestamp TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    content TEXT NOT NULL,

    CONSTRAINT uclouvain_item_snapshot_pkey PRIMARY KEY (uuid)
);
CREATE INDEX idx_uclouvain_item_snapshot_timestamp ON uclouvain_item_snapshot(timestamp);
