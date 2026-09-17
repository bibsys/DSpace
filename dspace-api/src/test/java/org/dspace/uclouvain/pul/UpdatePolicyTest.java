/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.dspace.services.ConfigurationService;
import org.dspace.uclouvain.pul.UpdatePolicy.Behaviour;
import org.junit.Test;

/**
 * {@link UpdatePolicy} reads the three field lists and answers for any field.
 */
public class UpdatePolicyTest {

    @Test
    public void behaviourComesFromTheListTheFieldIsIn() {
        ConfigurationService configuration = mock(ConfigurationService.class);
        when(configuration.getArrayProperty(UpdatePolicy.REPLACE_PROPERTY, new String[0]))
            .thenReturn(new String[] {"dc.title", " dc.description.abstract "});
        when(configuration.getArrayProperty(UpdatePolicy.MERGE_PROPERTY, new String[0]))
            .thenReturn(new String[] {"dc.subject"});
        when(configuration.getArrayProperty(UpdatePolicy.ADD_IF_EMPTY_PROPERTY, new String[0]))
            .thenReturn(new String[] {"dc.date.issued", ""});

        UpdatePolicy policy = UpdatePolicy.fromConfiguration(configuration);

        assertEquals(Behaviour.REPLACE, policy.behaviourFor("dc.title"));
        assertEquals(Behaviour.REPLACE, policy.behaviourFor("dc.description.abstract"));
        assertEquals(Behaviour.MERGE, policy.behaviourFor("dc.subject"));
        assertEquals(Behaviour.ADD_IF_EMPTY, policy.behaviourFor("dc.date.issued"));
        assertEquals(Behaviour.IGNORE, policy.behaviourFor("dc.type.subtype"));
        assertEquals(Behaviour.IGNORE, policy.behaviourFor(""));
    }
}
