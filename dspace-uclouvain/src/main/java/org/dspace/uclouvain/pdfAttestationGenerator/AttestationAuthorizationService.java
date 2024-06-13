package org.dspace.uclouvain.pdfAttestationGenerator;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.configuration.PDFAttestationGeneratorConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

// Service to check permission for attestation generation and download.
public class AttestationAuthorizationService {

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PDFAttestationGeneratorConfiguration pdfAttestationGeneratorConfiguration;

    @Autowired
    private AuthorizationUtils authUtils;

    @Autowired
    private ItemUtils itemUtils;

    /**
     * Check if the item is valid for the current attestation configuration.
     * @param item: The item to check.
     * @param context: The current DSpace context.
     * @return True if the item is valid for at least one of the configured attestation.
     * @throws SQLException
     */
    public boolean isItemValidForAttestation(Item item, Context context) throws SQLException {
        // Check that the item is not in the workspace
        return !this.itemUtils.isWorkspace(context, item)
            && this.pdfAttestationGeneratorConfiguration
                .getAllHandledTypes()
                .contains(
                    this.itemService.getMetadataFirstValue(
                        item, 
                        "dspace", 
                        "entity", 
                        "type", 
                        Item.ANY
                    )
                );
    }

    /**
     * Check if the user is authorized to download the attestation.
     * Authorized if:
     * - the item can be handled by one of the available handler AND (
     *     - the user is an admin OR
     *     - the user is the submitter of the item OR
     *     - the user is a manager
     * )
     * @param dsItem: The item to check permission for.
     * @param ctx: The current DSpace context.
     * @return True if the user is validates one of the above conditions.
     * @throws SQLException
     */
    public boolean isUserAuthorized(Item dsItem, Context ctx) throws SQLException {
        EPerson currentUser = ctx.getCurrentUser();
        if (currentUser == null) return false; 
        return (this.authorizeService.isAdmin(ctx, dsItem))
            || (dsItem.getSubmitter() == currentUser)
            || (this.authUtils.isManagerOfItem(ctx, dsItem, currentUser));
    }
}
