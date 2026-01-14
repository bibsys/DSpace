/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface UCLouvainProfileService {

    /**
     * Find a profile item using an FGS identifier.
     * @param context The DSpace application context.
     * @param fgs     The FGS identifier to use to find the profile.
     * @return Returns the profile that has the given identifier, null otherwise.
     */
    Item findById(Context context, String fgs);

    /**
     * Find a profile item using an email.
     * @param context The DSpace application context.
     * @param email   The email to use to find the profile.
     * @return Returns the profile that has the given email, null otherwise.
     */
    Item findByEmail(Context context, String email);

    /**
     * Find a profile corresponding to the given identifier.
     * @param context The current DSpace application context.
     * @param uuid uuid of the profile item.
     * @param fgs FGS identifier of a profile item.
     * @return The corresponding profile item.
     * @throws IllegalArgumentException If no identifier is provided.
     */
    Item findByIdentifiers(Context context, String uuid, String fgs);

    /**
     * For a given profile, retrieve all the linked publications that use this profile has an author.
     * @param context The DSpace application context.
     * @param profile The profile to find publications for.
     * @return Any publication that references this profile has an author.
     */
    List<Item> findLinkedPublications(Context context, Item profile);

    /**
     * Create an empty profile item with only a FGS identifier.
     * @param context The current DSpace context.
     * @param fgs     The unique FGS identifier to give to the profile item.
     */
    Item createEmptyProfile(Context context, String fgs) throws Exception;

    /**
     * Create an empty profile item with only a FGS identifier.
     * @param context The current DSpace context.
     * @param fgs     The unique FGS identifier to give to the profile item.
     * @param addDefaultInstitution Wether or not to add a default institution to the created profile.
     */
    Item createEmptyProfile(Context context, String fgs, boolean addDefaultInstitution) throws Exception;

    /**
     * Create a fresh new profile for a specific user.
     * Use the metadata present in the given 'currentUser' object to fill the metadata with the profile.
     * @param context The current DSpace context.
     * @param person The user to create a profile for.
     * @return The created profile
     */
    Item createNewProfile(Context context, EPerson person) throws Exception;
}
