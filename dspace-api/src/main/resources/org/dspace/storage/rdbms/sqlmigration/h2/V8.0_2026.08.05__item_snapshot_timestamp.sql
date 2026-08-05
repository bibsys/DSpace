--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- MAKE uclouvain_item_snapshot.timestamp BEHAVE LIKE item.last_modified
-----------------------------------------------------------------------------------
-- The snapshot timestamp must always mirror `item.last_modified` of the item state the snapshot
-- captures, so that staleness can be evaluated per item. A `DEFAULT CURRENT_TIMESTAMP` silently
-- replaced that value by the insertion time whenever the column was omitted from the INSERT, which
-- made the very first snapshot of an item carry a meaningless timestamp. The value is now always
-- supplied by the application layer.
--
-- NOTE :: the PostgreSQL counterpart ALSO converts the column to TIMESTAMP WITH TIME ZONE, to match
--         `item.last_modified` there. Nothing similar is needed here: in the H2 schema
--         `item.last_modified` is a plain TIMESTAMP too, so both columns already agree, and no
--         migration of the whole repository uses TIMESTAMP WITH TIME ZONE under H2.
-----------------------------------------------------------------------------------

ALTER TABLE uclouvain_item_snapshot ALTER COLUMN timestamp DROP DEFAULT;
