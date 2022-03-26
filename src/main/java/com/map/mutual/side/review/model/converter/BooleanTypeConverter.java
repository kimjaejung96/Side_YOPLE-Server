package com.map.mutual.side.review.model.converter;

import com.map.mutual.side.common.enumerate.ApiStatusCode;
import com.map.mutual.side.common.enumerate.BooleanType;
import com.map.mutual.side.common.exception.YOPLEServiceException;
import com.map.mutual.side.review.model.enumeration.EmojiType;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Objects;

@Converter(autoApply = true)
public class BooleanTypeConverter implements AttributeConverter<BooleanType, String> {
    @Override
    public String convertToDatabaseColumn(BooleanType attribute) {
        if (Objects.isNull(attribute)) {
            throw new YOPLEServiceException(ApiStatusCode.SYSTEM_ERROR);
        }
        return attribute.getStatus();
    }

    @Override
    public BooleanType convertToEntityAttribute(String dbData) {
        if (StringUtils.isBlank(dbData)) {
            throw new YOPLEServiceException(ApiStatusCode.SYSTEM_ERROR);
        }
        return BooleanType.find(dbData);
    }

}
