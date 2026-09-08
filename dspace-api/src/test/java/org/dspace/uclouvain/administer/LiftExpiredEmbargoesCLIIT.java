/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link LiftExpiredEmbargoesCLI} and the service methods behind it.
 */
public class LiftExpiredEmbargoesCLIIT extends AbstractIntegrationTestWithDatabase {

    private static final Date PAST = Date.from(Instant.parse("2000-01-01T00:00:00Z"));
    private static final Date FUTURE = Date.from(Instant.parse("2100-01-01T00:00:00Z"));

    private final ResourcePolicyService resourcePolicyService =
        AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    private final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private UCLouvainResourcePolicyService uclouvainResourcePolicyService;
    private Group anonymous;
    private Bitstream bitstream;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        uclouvainResourcePolicyService = UCLouvainServiceFactory.getInstance().getResourcePolicyService();
        context.turnOffAuthorisationSystem();
        anonymous = EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS);
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Col").build();
        Item item = ItemBuilder.createItem(context, collection).withTitle("Publication").build();
        bitstream = BitstreamBuilder.createBitstream(context, item, toInputStream("content", UTF_8))
            .withName("file.pdf").build();
        // A submitted file only carries the custom policy chosen in the upload step: the install step strips the
        // inherited ones as soon as a custom policy exists (see ItemServiceImpl.adjustBitstreamPolicies).
        authorizeService.removeAllPolicies(context, bitstream);
        context.restoreAuthSystemState();
    }

    private int readPolicy(Group group, String name, Date startDate) throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicy policy = ResourcePolicyBuilder.createResourcePolicy(context, null, group)
            .withDspaceObject(bitstream)
            .withAction(Constants.READ)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withName(name)
            .withStartDate(startDate)
            .build();
        context.restoreAuthSystemState();
        return policy.getID();
    }

    private ResourcePolicy reload(int policyId) throws Exception {
        context.commit();
        return resourcePolicyService.find(context, policyId);
    }

    @Test
    public void testExpiredEmbargoBecomesOpenAccess() throws Exception {
        int policyId = readPolicy(anonymous, "embargo", PAST);
        assertThat(expiredEmbargoIds(), contains(policyId));

        assertEquals(1, new LiftExpiredEmbargoesCLI().run(context, false));

        ResourcePolicy policy = reload(policyId);
        assertNull(policy.getStartDate());
        assertEquals("openaccess", policy.getRpName());
        assertEquals(anonymous.getID(), policy.getGroup().getID());
        assertEquals(Constants.READ, policy.getAction());
        assertEquals(bitstream.getID(), policy.getdSpaceObject().getID());
        assertThat(uclouvainResourcePolicyService.findExpiredEmbargoes(context), empty());
    }

    @Test
    public void testAnonymousReadAccessSurvivesTheLift() throws Exception {
        // The expired embargo policy is the only READ grant on the bitstream: deleting it instead of renaming it
        // would leave the file readable by administrators only.
        readPolicy(anonymous, "embargo", PAST);
        assertThat(resourcePolicyService.find(context, bitstream, Constants.READ), hasSize(1));
        assertTrue(anonymousCanRead());

        new LiftExpiredEmbargoesCLI().run(context, false);
        context.commit();

        assertTrue(anonymousCanRead());
    }

    @Test
    public void testDryRunChangesNothing() throws Exception {
        int policyId = readPolicy(anonymous, "embargo", PAST);

        assertEquals(1, new LiftExpiredEmbargoesCLI().run(context, true));

        ResourcePolicy policy = reload(policyId);
        assertNotNull(policy.getStartDate());
        assertEquals("embargo", policy.getRpName());
    }

    @Test
    public void testRunningEmbargoIsNotExpired() throws Exception {
        int policyId = readPolicy(anonymous, "embargo", FUTURE);

        assertEquals(0, new LiftExpiredEmbargoesCLI().run(context, false));

        assertEquals("embargo", reload(policyId).getRpName());
    }

    @Test
    public void testOnlyAnonymousEmbargoesAreLifted() throws Exception {
        context.turnOffAuthorisationSystem();
        Group network = GroupBuilder.createGroup(context).withName("UCLouvain network").build();
        context.restoreAuthSystemState();
        int policyId = readPolicy(network, "embargo", PAST);

        assertEquals(0, new LiftExpiredEmbargoesCLI().run(context, false));

        ResourcePolicy policy = reload(policyId);
        assertEquals("embargo", policy.getRpName());
        assertNotNull(policy.getStartDate());
    }

    @Test
    public void testDatedPoliciesWithAnotherNameAreLeftAlone() throws Exception {
        int policyId = readPolicy(anonymous, "lease", PAST);

        assertEquals(0, new LiftExpiredEmbargoesCLI().run(context, false));

        assertEquals("lease", reload(policyId).getRpName());
    }

    @Test
    public void testOrphanEmbargoPoliciesAreIgnored() throws Exception {
        // Deleting a bitstream can leave its policies behind with a null dspace_object (1009 such rows in a local
        // DIAL.pr database): nothing to lift, and the CLI must not crash on them.
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymous)
            .withAction(Constants.READ)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withName("embargo")
            .withStartDate(PAST)
            .build();
        context.restoreAuthSystemState();

        assertEquals(0, new LiftExpiredEmbargoesCLI().run(context, false));
    }

    @Test
    public void testLiftingANonEmbargoPolicyIsRefused() throws Exception {
        int policyId = readPolicy(anonymous, "restricted", null);
        ResourcePolicy policy = resourcePolicyService.find(context, policyId);

        assertThrows(IllegalArgumentException.class,
            () -> uclouvainResourcePolicyService.liftEmbargo(context, policy));
        assertEquals("restricted", reload(policyId).getRpName());
    }

    private boolean anonymousCanRead() throws Exception {
        context.restoreAuthSystemState();
        context.setCurrentUser(null);
        return authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ);
    }

    private List<Integer> expiredEmbargoIds() throws Exception {
        return uclouvainResourcePolicyService.findExpiredEmbargoes(context).stream()
            .map(ResourcePolicy::getID).toList();
    }
}
