package ru.iposhka.model.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.iposhka.model.CoupleStatus;

@Converter(autoApply = true)
public class CoupleStatusConverter implements AttributeConverter<CoupleStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(CoupleStatus status) {
        if (status == null)
            return null;
        return (short) status.ordinal();
    }

    @Override
    public CoupleStatus convertToEntityAttribute(Short dbData) {
        if (dbData == null) return null;
        return CoupleStatus.values()[dbData];
    }
}