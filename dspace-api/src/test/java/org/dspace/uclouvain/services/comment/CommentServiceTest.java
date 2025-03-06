/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.comment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.CommentService;
import org.junit.Before;
import org.junit.Test;

public class CommentServiceTest extends AbstractIntegrationTestWithDatabase {

    private static final List<Pair<String, String>> requiredConfigProperties = Arrays.asList(
        Pair.of("comments.bitstream.bundle.location", "COMMENT"),
        Pair.of("comments.bitstream.name.location", "comment bitstream")
    );

    private final CommentService commentService = UCLouvainServiceFactory.getInstance().getCommentService();
    private Item item;

    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();
    private static final ConfigurationService configService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    // BEFORE & AFTER ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** Save initial stored configuration properties to restore them at the end */
    @Before
    public void initProperties() {
        requiredConfigProperties.forEach(property -> {
            configService.setProperty(property.getLeft(), property.getRight());
        });
        eventService.reloadConfiguration();
    }

    /** Code ran before test execution */
    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).withName("coll").build();
        item = ItemBuilder.createItem(context, collection).withEntityType("MasterThesis").withTitle("comments").build();
        context.restoreAuthSystemState();
    }


    // TESTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Test
    public void testCommentService() throws Exception {
        String commentBundleName = configService.getProperty("comments.bitstream.bundle.location");
        // 1) Check that the newly created item doesn't have any stored comment
        //    As the item is fresh, it doesn't contain any comment nor COMMENT bundle
        assertTrue(CollectionUtils.isEmpty(commentService.getComments(context, item)));
        assertTrue(CollectionUtils.isEmpty(item.getBundles(commentBundleName)));

        // 2) Add a new comment
        //    The newly created comment is a "normal" comment created by the current logged user.
        //    The comment creationDate and lastModified attributes must be the same as the comment was never updated
        //    DEV: Sleep 1s to ensure correct comment timestamp for next comments.
        Comment comment1 = commentService.addComment(context, item, eperson, "First comment");
        assertThat(commentService.getComments(context, item).size(), equalTo(1));
        comment1 = commentService.getComment(context, item, comment1.getId());
        assertEquals(comment1.getCreationDate(), comment1.getModifiedDate());
        Thread.sleep(1000);
        // 3) Add 3 additional comments
        //    Add a second "normal" comment.
        //    Add a third "system" comment (not linked to any authority/user)
        //    Add a fourth "normal" comment linked to admin user.
        //    DEV: Sleep 1s between each comment creation to ensure correct comment timestamp.
        Comment comment2 = commentService.addComment(context, item, "Jean Gloutitou", "Second comment");
        comment2 = commentService.getComment(context, item, comment2.getId());  // reload comment
        assertThat(comment2.getAuthorName(), equalTo("Jean Gloutitou"));
        assertNull(comment2.getAuthorAuthority());
        Thread.sleep(1000);

        Comment comment3 = commentService.addSystemComment(context, item, "System comment manually created");
        comment3 = commentService.getComment(context, item, comment3.getId());
        assertThat(comment3.getAuthorName(), equalTo("system"));
        Thread.sleep(1000);

        Comment comment4 = commentService.addComment(context, item, admin, "Admin comment");
        comment4 = commentService.getComment(context, item, comment4.getId());
        assertThat(comment4.getAuthorAuthority(), equalTo(admin.getID()));
        assertThat(commentService.getComments(context, item).size(), equalTo(4));

        // 4) Update comment#1
        //    Update comment content with new content.
        //    After reload, check if the content is correct and if lastModified attribute is correctly set.
        commentService.updateComment(context, item, comment1.getId(), "First updated comment");
        assertThat(commentService.getComments(context, item).size(), equalTo(4));
        comment1 = commentService.getComment(context, item, comment1.getId());
        assertThat(comment1.getContent(), equalTo("First updated comment"));
        assertNotEquals(comment1.getModifiedDate(), comment1.getCreationDate());

        // 5) Delete comment#1
        commentService.deleteComment(context, item, comment1.getId());
        comment1 = commentService.getComment(context, item, comment1.getId());
        assertNull(comment1);
        assertThat(commentService.getComments(context, item).size(), equalTo(3));

        // 6) Delete all comments
        //    a) Delete all comments in a single step.
        //    b) Add a new comment; Check it exists; delete it.
        //    for both cases, after operations, the bundle and the bitstream must not yet exist.
        commentService.deleteAllComment(context, item);
        assertTrue(CollectionUtils.isEmpty(commentService.getComments(context, item)));
        assertTrue(CollectionUtils.isEmpty(item.getBundles(commentBundleName)));

        Comment comment5 = commentService.addSystemComment(context, item, "dummy system comment");
        assertThat(commentService.getComments(context, item).size(), equalTo(1));
        assertTrue(CollectionUtils.isNotEmpty(item.getBundles(commentBundleName)));
        commentService.deleteComment(context, item, comment5.getId());
        assertTrue(CollectionUtils.isEmpty(commentService.getComments(context, item)));
        assertTrue(CollectionUtils.isEmpty(item.getBundles(commentBundleName)));
    }

    @Test
    public void testLegacyComments() throws Exception {
        String commentBundleName = configService.getProperty("comments.bitstream.bundle.location");

        // 1) Check that the newly created item doesn't have any stored comment
        //    As the item is fresh, it doesn't contain any comment nor COMMENT bundle
        assertTrue(CollectionUtils.isEmpty(commentService.getComments(context, item)));
        assertTrue(CollectionUtils.isEmpty(item.getBundles(commentBundleName)));

        // 2) load legacy comments from a Fedora datastream.
        String legacyCommentContent = "<comments>" +
            "  <comment writer=\"author_name\" timestamp=\"2024-03-29T10:53:03+01:00\">first content</comment>" +
            "</comments>";
        context.turnOffAuthorisationSystem();
        InputStream legacyIS = IOUtils.toInputStream(legacyCommentContent, CharEncoding.UTF_8);
        BitstreamBuilder
            .createBitstream(context, item, legacyIS, commentBundleName)
            .withName("custom bitstream name")  // to check custom name vs. defined property name
            .build();
        context.restoreAuthSystemState();

        // 3) Check comment could be found
        List<Comment> comments = commentService.getComments(context, item);
        assertThat(comments.size(), equalTo(1));
        Comment legacyComment = commentService.getComment(context, item, comments.get(0).getId());
        assertNotNull(legacyComment);
        assertThat(legacyComment.getAuthorName(), equalTo("author_name"));
        assertEquals(legacyComment.getCreationDate(), legacyComment.getModifiedDate());

        // 4) Update comment
        commentService.updateComment(context, item, legacyComment.getId(), "updated content");
        legacyComment = commentService.getComment(context, item, legacyComment.getId());
        assertThat(legacyComment.getContent(), equalTo("updated content"));
        assertThat(legacyComment.getAuthorName(), equalTo("author_name"));
        assertNotEquals(legacyComment.getCreationDate(), legacyComment.getModifiedDate());
        assertThat(commentService.getComments(context, item).size(), equalTo(1));

        // 5) Reset fixture (ensure future tests compatibility)
        commentService.deleteAllComment(context, item);
        assertTrue(CollectionUtils.isEmpty(commentService.getComments(context, item)));
        assertTrue(CollectionUtils.isEmpty(item.getBundles(commentBundleName)));
    }
}
