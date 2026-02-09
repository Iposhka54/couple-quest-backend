package ru.iposhka.model.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.iposhka.model.Gender;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, Short> {

    @Override
    public Short convertToDatabaseColumn(Gender attribute) {
        return (short) attribute.ordinal();
    }

    @Override
    public Gender convertToEntityAttribute(Short dbData) {
        return Gender.values()[dbData];
    }
}