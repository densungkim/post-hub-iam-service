package com.post.hub.iamservice.mapper;

import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.dto.comment.CommentSearchDTO;
import com.post.hub.iamservice.model.entities.Comment;
import com.post.hub.iamservice.model.entities.Post;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.model.request.comment.UpdateCommentRequest;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {DateTimeUtils.class, Object.class}
)
public interface CommentMapper {

    @Mapping(source = "user.id", target = "owner.id")
    @Mapping(source = "user.username", target = "owner.username")
    @Mapping(source = "user.email", target = "owner.email")
    @Mapping(source = "post.id", target = "postId")
    CommentDTO toDTO(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "post", source = "post")
    @Mapping(target = "createdBy", source = "user.email")
    Comment createComment(CommentRequest commentRequest, User user, Post post);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateComment(@MappingTarget Comment comment, UpdateCommentRequest request);

    @Mapping(source = "user.id", target = "owner.id")
    @Mapping(source = "user.username", target = "owner.username")
    @Mapping(source = "user.email", target = "owner.email")
    @Mapping(source = "post.id", target = "postId")
    @Mapping(source = "deleted", target = "isDeleted")
    CommentSearchDTO toCommentSearchDTO(Comment comment);

}
