package com.gdn.project.waroenk.member.entity.converter;

import com.gdn.project.waroenk.member.constant.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter to map Gender enum to CHAR(1) in database.
 * MALE -> 'M', FEMALE -> 'F', OTHER -> 'O'
 */
@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        if (gender == null) {
            return null;
        }
        return gender.getGender();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        
        return switch (dbData.trim().toUpperCase()) {
            case "M" -> Gender.MALE;
            case "F" -> Gender.FEMALE;
            case "O" -> Gender.OTHER;
            default -> throw new IllegalArgumentException("Unknown gender code: " + dbData);
        };
    }
}

