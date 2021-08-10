/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;

import java.util.ArrayList;
import java.util.List;

@LinksRest(links = {
        @LinkRest(
                name = SubscriptionRest.DSPACE_OBJECT,
                method = "getDSpaceObject"
        ),
        @LinkRest(
                name = SubscriptionRest.EPERSON,
                method = "getEPerson"
        )
})
public class SubscriptionRest extends BaseObjectRest<Integer>{
    public static final String NAME = "subscription";
    public static final String NAME_PLURAL = "subscriptions";
    public static final String CATEGORY = "core";
    public static final String DSPACE_OBJECT = "dSpaceObject";
    public static final String EPERSON = "ePerson";

    private Integer id;
    private String type;
    private List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<SubscriptionParameter> getSubscriptionParameterList() {
        return subscriptionParameterList;
    }

    public void setSubscriptionParameterList(List<SubscriptionParameter> subscriptionParameterList) {
        this.subscriptionParameterList = subscriptionParameterList;
    }
    public String getSubscriptionType() {
        return this.type;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }
}
