package com.post.hub.iamservice.utils;

import com.post.hub.iamservice.service.model.IamServiceUserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserRoleTypeConverter implements AttributeConverter<IamServiceUserRole, String> {

    @Override
    public String convertToDatabaseColumn(IamServiceUserRole iamServiceUserRole) {
        return iamServiceUserRole.name();
    }

    @Override
    public IamServiceUserRole convertToEntityAttribute(String s) {
        return IamServiceUserRole.fromName(s);
    }

}
