/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.apicatalog.jsonld.StringUtils;
import jakarta.mail.MessagingException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.citations.UCLouvainCitationsService;
import org.dspace.uclouvain.content.dao.ItemSnapshotDAO;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.ItemSnapshotContentSerializer;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.explainer.DiffExplainerFactory;
import org.dspace.uclouvain.content.snapshot.diff.formats.OutputFormat;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;
import org.dspace.uclouvain.core.NotificationType;
import org.dspace.uclouvain.core.mails.Recipient;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.exceptions.SendEmailException;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ItemSnapshotService}, used to deal with {@link ItemSnapshot}
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Service
public class ItemSnapshotServiceImpl implements ItemSnapshotService {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemSnapshotServiceImpl.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemSnapshotDAO itemSnapshotDAO;
    @Autowired
    private AccessStatusService accessStatusService;
    @Autowired
    private ItemSnapshotContentSerializer snapshotSerializer;
    @Autowired
    private DiffExplainerFactory diffExplainerFactory;
    @Autowired
    private ResearcherProfileService researcherProfileService;
    @Autowired
    private UCLouvainCitationsService citationService;

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final List<String> trackedMetadata = Stream
        .of(configService.getArrayProperty("item-snapshot.tracked-metadata", new String[] {}))
        .toList();

    // IMPLEMENTED METHODS =============================================================================================
    @Override
    public ItemSnapshot get(Context context, UUID id, boolean deserialize) throws SQLException {
        ItemSnapshot snapshot = itemSnapshotDAO.findByID(context, ItemSnapshot.class, id);
        if (snapshot == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_item_snapshot", "not_found,item_id=" + id));
            }
            return null;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_item_snapshot", "item_id=" + id));
            }
            if (deserialize) {
                try {
                    snapshotSerializer.deserialize(snapshot);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to deserialize ItemSnapshot#" + id, e);
                }
            }
            return snapshot;
        }
    }
    @Override
    public ItemSnapshot get(Context context, UUID id) throws SQLException {
        return get(context, id, true);
    }

    @Override
    public List<UUID> findItemsToSnapshot(Context context, Date from, int limit) throws SQLException {
        return itemSnapshotDAO.findItemsToSnapshot(context, from, limit);
    }

    @Override
    public ItemSnapshot takeSnapshot(Context context, UUID id) throws SQLException {
        Item item = itemService.find(context, id);
        if (item == null) {
            throw new IllegalArgumentException("Item#" + id + " doesn't exist");
        }
        return this.takeSnapshot(context, item);
    }
    @Override
    public ItemSnapshot takeSnapshot(Context context, Item item) throws SQLException {
        ItemSnapshot snapshot = new ItemSnapshot();
        snapshot.setItem(item);
        snapshot.setTimestamp(new Date());
        snapshot.setSnapshotElements(this.buildSnapshotElements(context, item));
        return snapshot;
    }

    @Override
    public ItemSnapshotDiff compareSnapshot(ItemSnapshot snapshot1, ItemSnapshot snapshot2)
        throws IllegalArgumentException {
        if (snapshot1 == null || snapshot2 == null) {
            throw new IllegalArgumentException("Snapshot cannot be null");
        }
        // DEV NOTE: We use `getItem().getID()` instead of `getId()` because the snapshot could be not yet persisted
        if (!Objects.equals(snapshot1.getItem().getID(), snapshot2.getItem().getID())) {
            throw new IllegalArgumentException(String.format(
                    "We can only compare snapshots for the same related item :: %s<>%s",
                    snapshot1.getId(), snapshot2.getId()
            ));
        }
        ItemSnapshotDiff diff = new ItemSnapshotDiff(snapshot1.getItem());

        // For better performance, we will index the second snapshot using a single key (path + class)
        Map<String, SnapshotElement> mapSnapshot2 = snapshot2
            .getSnapshotElements().stream()
            .collect(Collectors.toMap(
                this::getSnapshotElementKey,
                element -> element,
                (existing, replacement) -> existing // For potential doubles...
            ));
        // Loop on snapshot1 elements to find update/remove elements
        //   If current read element key isn't present into mapSnapshot2: This is a "remove"
        //   If element path exists in both snapshots, but aren't equals: This is an "update"
        for (SnapshotElement element1 : snapshot1.getSnapshotElements()) {
            String key = getSnapshotElementKey(element1);
            SnapshotElement element2 = mapSnapshot2.remove(key);
            if (element2 == null || !element1.equals(element2)) {
                diff.addChange(element1, element2);
            }
        }
        // At the end, elements not removed from the constructed map and "added" element
        for (SnapshotElement element : mapSnapshot2.values()) {
            diff.addChange(null, element);
        }
        return diff;
    }
    private String getSnapshotElementKey(SnapshotElement snapshotElement) {
        return snapshotElement.getPath() + "__" + snapshotElement.getClass().getName();
    }

    @Override
    public ItemSnapshotDiff detectChanges(Context context, UUID id) throws Exception {
        Item item = itemService.find(context, id);
        if (item == null) {
            throw new IllegalArgumentException("Item#" + id + " doesn't exist");
        }
        return this.detectChanges(context, item);
    }
    @Override
    public ItemSnapshotDiff detectChanges(Context context, Item item) throws Exception {
        ItemSnapshot snapshot1 = get(context, item.getID());
        if (snapshot1 == null) {
            throw new IllegalArgumentException("Unable to retrieve snapshot for Item#" + item.getID());
        }
        return compareSnapshot(snapshot1, takeSnapshot(context, item));
    }

    @Override
    public String explainChanges(ItemSnapshotDiff changes, OutputFormat format, Locale locale) {
        Locale finalLocale = (locale != null) ? locale : Locale.getDefault();
        return changes.getChanges().stream()
            .sorted(Comparator.comparing(ItemSnapshotDiff::getChangeSortKey))
            .map(change -> diffExplainerFactory.explain(change.getLeft(), change.getRight(), format, finalLocale))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining());
    }


    @Override
    public void store(Context context, ItemSnapshot snapshot) throws Exception {
        if (snapshot.getItem() == null) {
            throw new IllegalArgumentException("Snapshot must be related to an item");
        }

        // Persist changes into database
        //    If the database doesn't yet store a snapshot for the related item, just "create" the snapshot
        //    If the database already stored a snapshot, we need to retrieve it, update it and "save" it
        if (snapshot.getId() == null) {
            snapshot.setId(snapshot.getItem().getID());
        }
        ItemSnapshot existingSnapshot = itemSnapshotDAO.findByID(context, ItemSnapshot.class, snapshot.getId());
        if (existingSnapshot == null) {
            log.info("Store new snapshot for Item#{}", snapshot.getItem().getID());
            snapshot.setContent(snapshotSerializer.serialize(snapshot));
            snapshot.setTimestamp(snapshot.getItem().getLastModified());
            itemSnapshotDAO.create(context, snapshot);
        } else {
            log.info("Update snapshot for Item#{}", snapshot.getItem().getID());
            existingSnapshot.setContent(snapshotSerializer.serialize(snapshot));
            existingSnapshot.setTimestamp(snapshot.getItem().getLastModified());
            itemSnapshotDAO.save(context, existingSnapshot);
        }

    }

    @Override
    public List<Recipient> getNotifyRecipients(Context context, ItemSnapshotDiff diff, NotificationType method) {
        if (diff == null || diff.getItem() == null) {
            return List.of();
        }
        return Stream.concat(
            getPublicationAuthorRecipients(diff, method),
            getSubmitterRecipient(diff, method)
        ).toList();
    }

    @Override
    public void notifyRecipient(
        Context context,
        Recipient recipient,
        List<ItemSnapshotDiff> changes,
        NotificationType method
    ) throws Exception {
        // TODO :: implements other notification method
        switch (method) {
            case EMAIL:
                notifyRecipientByEmail(context, recipient, changes);
                break;
            default:
                throw new NotImplementedException(method + " is not supported");
        }
    }

    // PRIVATE METHODS =================================================================================================
    /**
     * This method analyze an item to extract any snapshot element useful to create the snapshot
     * @param context the application context
     * @param item the item to analyze
     * @return a list of {@link SnapshotElement} regarding this object
     * @throws SQLException if any database exception occurred
     */
    private List<SnapshotElement> buildSnapshotElements(Context context, Item item) throws SQLException {
        return Stream.concat(
            buildSnapshotMetadataElements(item).stream(),
            buildSnapshotFileElements(context, item).stream()
        ).collect(Collectors.toList());
    }
    private List<SnapshotElement> buildSnapshotMetadataElements(Item item) {
        List<SnapshotElement> elements = new ArrayList<>();
        for (String trackedField : trackedMetadata) {
            List<MetadataValue> mdValues = itemService.getMetadataByMetadataString(item, trackedField);
            for (MetadataValue md : mdValues) {
                String fieldPath = md.getMetadataField().toString('.');
                String mdPath = String.format("%s[%d]", fieldPath, md.getPlace());
                elements.add(new MetadataSnapshotElement(mdPath, md.getValue()));
            }
        }
        return elements;
    }
    private List<SnapshotElement> buildSnapshotFileElements(Context context, Item item) throws SQLException {
        List<SnapshotElement> elements = new ArrayList<>();
        List<Bundle> bundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
        for (Bundle bundle : bundles) {
            for (Bitstream b : bundle.getBitstreams()) {
                elements.add(new FileSnapshotElement(
                    b.getID(),
                    b.getName(),
                    b.getChecksumAlgorithm() + "#" + b.getChecksum(),
                    accessStatusService.getBitstreamAccessStatus(context, b)
                ));
            }
        }
        return elements;
    }

    /**
     * Get all authors recipients for a publication related to a specific notification method
     * @param diff the publications changes to analyze (to get related Publication(item))
     * @param method the notification method (email, phone, telepathy, ...)
     * @return the list of recipient to notify
     */
    private Stream<Recipient> getPublicationAuthorRecipients(ItemSnapshotDiff diff, NotificationType method) {
        try {
            Publication publication = PublicationFactory.build(diff.getItem());
            // Return list of authors with emails. Priority is given to "private email" if available
            return publication.getAuthors()
                .stream()
                .map(a -> {
                    String contactValue = resolveContactValue(a, method);
                    return contactValue != null ? new Recipient(a.getName(), Map.of(method, contactValue)) : null;
                })
                .filter(r -> r != null && StringUtils.isNotBlank(r.get(method)));
        } catch (InvalidModelEntityTypeException e) {
            return Stream.empty();
        }
    }
    private String resolveContactValue(PublicationAuthor author, NotificationType method) {
        // TODO :: add other communication method retrieval if necessary
        return switch (method) {
            case EMAIL -> {
                String email = StringUtils.isNotBlank(author.getPrivateEmail())
                    ? author.getPrivateEmail()
                    : author.getEmail();
                EmailValidator validator = EmailValidator.getInstance();
                yield (email != null && validator.isValid(email)) ? email : null;
            }
            default -> throw new UnsupportedOperationException("unsupported method: " + method);
        };
    }

    /**
     * Get submitter recipient for a publication related to a specific notification method
     * @param diff the publications changes to analyze (to get related Publication(item))
     * @param method the notification method (email, phone, telepathy, ...)
     * @return the submitter recipient (as stream for easy manipulation)
     */
    private Stream<Recipient> getSubmitterRecipient(ItemSnapshotDiff diff, NotificationType method) {
        EPerson submitter = diff.getItem().getSubmitter();
        if (submitter == null) {
            return Stream.empty();
        }
        // TODO :: add other communication method retrieval if necessary
        return switch (method) {
            case EMAIL -> Stream.of(new Recipient(
                submitter.getName(),
                Map.of(NotificationType.EMAIL, submitter.getEmail())
            ));
            default -> throw new UnsupportedOperationException("unsupported method: " + method);
        };
    }

    /**
     * Notify a recipient of changes on its publication by email. Changes will use HTML format to be included into email
     * @param context the application context
     * @param recipient the recipient to notify
     * @param changes all diff changes to notify
     */
    private void notifyRecipientByEmail(Context context, Recipient recipient, List<ItemSnapshotDiff> changes)
        throws SendEmailException {
        try {
            Locale locale = getLocaleForRecipient(context, recipient);
            List<Triple<Item, String, String>> explanations = changes.stream()
                .map(change -> Triple.of(
                    change.getItem(),
                    citationService.getCitationForItemByCrosswalk(context, change.getItem(), "publication-uclouvain"),
                    explainChanges(change, OutputFormat.EMAIL_HTML, locale)
                ))
                .toList();
            Email email = Email.getEmail(I18nUtil.getEmailFilename(locale, "notify_changes"));
            email.setSubject(I18nUtil.getMessage("snapshot.email.subject", locale));
            email.setReplyTo(configService.getProperty("uclouvain.default.mail.reply-to", "noreply@dspace.org"));
            email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
            email.addArgument(recipient);
            email.addArgument(explanations);
            email.addRecipient(recipient.get(NotificationType.EMAIL));
            for (String addr : configService.getArrayProperty("item-snapshot.email-notification.cc-addresses")) {
                email.addCcAddress(addr);
            }
            email.send();
        } catch (IOException | MessagingException e) {
            throw new SendEmailException("Failed to call .send() on the generated email.", e);
        }
    }

    private Locale getLocaleForRecipient(Context context, Recipient recipient) {
        Locale defaultLocale = Locale.getDefault();
        if (recipient == null) {
            return defaultLocale;
        }
        String email = recipient.get(NotificationType.EMAIL);
        if (email == null || email.isBlank()) {
            return defaultLocale;
        }
        Map<String, String> identifiers = Map.of(
            "person.email", email,
            "person.email.official", email
        );
        try {
            ResearcherProfile profile = researcherProfileService.findFirstByIdentifiers(context, identifiers);
            if (profile != null) {
                return profile.getCommunicationLanguage()
                    .filter(lang -> !lang.isBlank())
                    .map(Locale::forLanguageTag)
                    .orElse(defaultLocale);
            }
        } catch (Exception e) {
            log.warn("Error getting recipient locale for email: {}", email, e);
        }
        return defaultLocale;
    }
}
