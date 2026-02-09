package ru.iposhka.model.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.iposhka.model.InviteStatus;

@Converter(autoApply = true)
public class InviteStatusConverter implements AttributeConverter<InviteStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(InviteStatus attribute) {
        return (short) attribute.ordinal();
    }

    @Override
    public InviteStatus convertToEntityAttribute(Short dbData) {
        return InviteStatus.values()[dbData];
    }
}