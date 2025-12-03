/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.model.fnrs.FnrsCategoryRest;
import org.dspace.app.rest.model.fnrs.FnrsRest;
import org.dspace.app.rest.model.fnrs.FnrsRuleRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.validation.fnrs.Category;
import org.dspace.uclouvain.validation.fnrs.FNRSValidator;
import org.dspace.uclouvain.validation.fnrs.rules.ValidationRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This REST controller declare endpoints for FNRS validation about an item
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@RestController
@RequestMapping("/api/" + FnrsRest.CATEGORY + "/" + FnrsRest.PLURAL_NAME + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
public class FnrsRestController {

    @Autowired
    FNRSValidator fnrsValidator;
    @Autowired
    ItemService itemService;

    /**
     * Endpoint to determine if an `Item` is valid in regard to FNRS validation rules.
     * This is only a simple response without any explanations
     * @param uuid the item uuid
     * @param response the current response
     * @param request the current request
     * @return the JSON response including the FNRS validation status for this item
     * @throws SQLException if any database exception
     */
    @PreAuthorize("hasPermission(#uuid, 'ITEM','WRITE')")
    @GetMapping(produces = "application/json", path = "/valid")
    public ResponseEntity validate(
            @PathVariable UUID uuid,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws SQLException {
        Context context = ContextUtil.obtainCurrentRequestContext();
        Item item = itemService.find(context, uuid);
        if (item == null) {
            throw new ResourceNotFoundException(uuid.toString());
        }
        FnrsRest model = new FnrsRest();
        model.setUuid(uuid);
        model.setRelevant(fnrsValidator.isRelevant(item));
        model.setValid(fnrsValidator.isValid(item));
        return ResponseEntity.ok().body(model);
    }

    /**
     * Endpoint to explain if an `Item` is valid in regard to FNRS validation rules.
     * The response contains explanations about FNRS categories and why the item is valid or not
     * @param uuid the item uuid
     * @param response the current response
     * @param request the current request
     * @return the JSON response including the FNRS validation status and all explanations about this status
     * @throws SQLException if any database exception
     */
    @PreAuthorize("hasPermission(#uuid, 'ITEM','WRITE')")
    @GetMapping(produces = "application/json", path = "/explain")
    public ResponseEntity explain(
            @PathVariable UUID uuid,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws SQLException {
        Context context = ContextUtil.obtainCurrentRequestContext();
        Item item = itemService.find(context, uuid);
        if (item == null) {
            throw new ResourceNotFoundException(uuid.toString());
        }
        FnrsRest model = new FnrsRest();
        model.setUuid(uuid);

        // Iterate on each FNRS category.
        // For each one, determine if it's applicable for this item
        // If the category is applicable, test all rules defined in this category
        for (Category category : fnrsValidator.getCategories()) {
            FnrsCategoryRest categoryRest = new FnrsCategoryRest();
            categoryRest.setName(category.getName());
            categoryRest.setDescription(category.getDescription());
            categoryRest.setApplicable(category.isApplicable(item));
            if (categoryRest.isApplicable()) {
                for (ValidationRule rule : category.getRules()) {
                    FnrsRuleRest ruleRest = new FnrsRuleRest();
                    ruleRest.setName(rule.getName());
                    ruleRest.setDescription(rule.getDescription());
                    ruleRest.setValid(rule.validate(item));
                    categoryRest.addRule(ruleRest);
                }
            }
            model.addExplanation(categoryRest);
        }
        model.setRelevant(model.getExplanations().stream().anyMatch(FnrsCategoryRest::isApplicable));
        // DEV NOTE: Why not simply use `FnrsValidator.isValid(item)` ?
        //    calling the global `isValid` method will check a second time all categories/rules, but we already tested
        //    them previously and stored pertinent result into the REST model.
        //    So just iterate on the model, and we can determine the global validity
        // DEV NOTE: Why use "complex" expression in `anyMatch` ?
        //    `isValid` could return null value. This is usefully because we won't include 'valid' key into the response
        //    if the category isn't applicable. But `anyMatch` function raised error on 'null' value.
        //    Using this notation, all works fine.
        model.setValid(model
            .getExplanations()
            .stream()
            .anyMatch(c -> Boolean.TRUE.equals(c.isValid()))
        );

        return ResponseEntity.ok().body(model);
    }
}
