--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- CREATE comment table
-----------------------------------------------------------------------------------

CREATE TABLE comment
(
    uuid UUID NOT NULL,
    owner_id UUID NOT NULL,
    author_name VARCHAR(200) NOT NULL,
    author_id UUID,
    content CLOB NOT NULL,
    created TIMESTAMP NOT NULL,
    modified TIMESTAMP,

    CONSTRAINT comment_pkey PRIMARY KEY (uuid),
    CONSTRAINT comment_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES item (uuid),
    CONSTRAINT comments_author_id_fkey FOREIGN KEY (author_id) REFERENCES eperson (uuid) ON DELETE SET NULL
);

CREATE INDEX comment_owner_id_index on comment(owner_id);
CREATE INDEX idx_comments_created ON comment(created);
CREATE INDEX idx_comments_modified ON comment(modified);
