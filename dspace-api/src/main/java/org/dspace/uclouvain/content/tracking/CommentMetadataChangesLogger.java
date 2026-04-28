/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.tracking;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of logger class to store metadata changes as a {@link org.dspace.uclouvain.content.Comment}
 * on a {@link Item}
 * TODO :: Should implement a `MetadataChangesLogger` interface (and maybe more implements of this interface)
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class CommentMetadataChangesLogger {

    @Autowired
    CommentService commentService;

    public void logChanges(Context context, Item item, Changes<MetadataValueSnapshot> changes) throws SQLException {
        if (context.ignoreAutomaticCommentCreation()) {
            return;
        }
        String content = Stream.of(
            changes.added().stream().map(this::convertAddMetadataSnapshot),
            changes.removed().stream().map(this::convertRemoveMetadataSnapshot),
            changes.updated().stream().map(c -> convertUpdateMetadataSnapshot(c.getLeft(), c.getRight()))
        ).flatMap(s -> s).collect(Collectors.joining());
        commentService.create(context, item, context.getCurrentUser(), content);
    }

    // PRIVATE METHODS =================================================================================================
    private String convertAddMetadataSnapshot(MetadataValueSnapshot snapshot) {
        String msg = "@add@@%s[%d]\n".formatted(snapshot.getFieldName(), snapshot.place());
        if (snapshot.authority() != null) {
            msg += "[s]authority:(%d)%s\n".formatted(snapshot.confidence(), snapshot.authority());
        }
        if (snapshot.securityLevel() != 0) {
            msg += "[s]security:%d\n".formatted(snapshot.securityLevel());
        }
        msg += "[+]%s\n".formatted(snapshot.value().trim());
        return msg;
    }

    private String convertRemoveMetadataSnapshot(MetadataValueSnapshot snapshot) {
        return "@remove@@%s[%d]\n[-]%s\n".formatted(snapshot.getFieldName(), snapshot.place(), snapshot.value().trim());
    }

    private String convertUpdateMetadataSnapshot(MetadataValueSnapshot oldS, MetadataValueSnapshot newS) {
        String msg = "@update@@%s[%d]\n".formatted(oldS.getFieldName(), oldS.place());
        if (!Objects.equals(oldS.authority(), newS.authority())) {
            msg += "[s]authority:(%d)%s <> (%d)%s\n".formatted(
                oldS.confidence(), oldS.authority(),
                newS.confidence(), newS.authority()
            );
        }
        if (oldS.securityLevel() != newS.securityLevel()) {
            msg += "[s]security:%d <> %d\n".formatted(oldS.securityLevel(), newS.securityLevel());
        }
        msg += "[~-]%s\n".formatted(oldS.value().trim());
        msg += "[~+]%s\n".formatted(newS.value().trim());
        return msg;
    }

}
