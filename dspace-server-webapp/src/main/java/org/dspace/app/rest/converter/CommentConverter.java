/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CommentRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.uclouvain.content.Comment;
import org.springframework.stereotype.Component;

/**
 * Converter to translate item comments to a HAL rest response
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class CommentConverter implements DSpaceConverter<Comment, CommentRest> {

    @Override
    public CommentRest convert(Comment comment, Projection projection) {
        CommentRest restModel = buildRestModel(comment);
        restModel.setProjection(projection);
        return restModel;
    }

    @Override
    public Class<Comment> getModelClass() {
        return Comment.class;
    }

    private CommentRest buildRestModel(Comment comment) {
        CommentRest model = new CommentRest();
        model.setId(comment.getID());
        model.setOwner(comment.getOwner().getID());
        model.setAuthorName(comment.getAuthorName());
        if (comment.getAuthorAuthority() != null) {
            model.setAuthorAuthority(comment.getAuthorAuthority().getID());
        }
        model.setCreated(comment.getCreationDate());
        if (comment.getModifiedDate() != comment.getCreationDate()) {
            model.setModified(comment.getModifiedDate());
        }
        model.setContent(comment.getContent());
        return model;
    }
}
