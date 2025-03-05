/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.junit.Before;
import org.junit.Test;

public class CommentServiceTest extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final CommentService commentService = UCLouvainServiceFactory.getInstance().getCommentService();

    private Item item;

    /** Code ran before test execution */
    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).withName("coll").build();
        item = ItemBuilder.createItem(context, collection).withEntityType("MasterThesis").withTitle("C#1").build();
        context.restoreAuthSystemState();
    }


    @Test
    public void testCommentServiceCRUDOperations() throws Exception {
        // Starting tests, ensure that no comment exists into the database.
        assertTrue(CollectionUtils.isEmpty(commentService.findAll(context)));

        // CREATE SOME COMMENTS.
        //   * Create a new comment and check it is saved into the database, and we can retrieve it.
        //   * Create a system comment and try to retrieve it.
        //   * Clean all created comments before next step.
        Comment comment = commentService.create(context, item, admin, "Comment#1");
        assertNotNull(comment);
        assertEquals(commentService.findAll(context).size(), 1);
        assertEquals(commentService.countCommentByItem(context, item), 1);

        commentService.create(context, item, "Jean Gloutitou", "Comment#2");
        assertEquals(commentService.countCommentByItem(context, item), 2);

        String systemCommentContent = RandomStringUtils.random(10, true, true);
        commentService.createSystemComment(context, item, systemCommentContent);
        comment = commentService.findByItem(context, item).stream()
            .filter(Comment::isSystemComment)
            .findFirst().orElse(null);
        assertNotNull(comment);
        assertNotNull(comment.getCreationDate());
        assertEquals(comment.getContent(), systemCommentContent);

        Comment twinComment = commentService.find(context, comment.getID());
        assertNotNull(twinComment);
        assertEquals(comment.getID(), twinComment.getID());
        commentService.deleteAllItemComments(context, item);
        assertEquals(commentService.countCommentByItem(context, item), 0);

        // DELETE AN ITEM WITH RELATED COMMENTS
        //   * Create a new comments on item.
        //   * Check when the item is deleted, all related comments must be deleted automatically.
        commentService.create(context, item, admin, "Comment#3");
        commentService.create(context, item, "Jean Gloutitou", "Comment#4");
        assertEquals(commentService.countCommentByItem(context, item), 2);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, item);
        context.restoreAuthSystemState();

        assertTrue(CollectionUtils.isEmpty(commentService.findByItem(context, item)));
        assertTrue(CollectionUtils.isEmpty(commentService.findAll(context)));
    }
}
