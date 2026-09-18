package com.brex.demo.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stores money as an exact BIGINT count of minor units (cents) so SQLite's
 * lack of a real fixed-precision decimal type can't introduce floating-point
 * drift. Java code everywhere else still works in BigDecimal.
 */
@Converter
public class MoneyCentsConverter implements AttributeConverter<BigDecimal, Long> {

    @Override
    public Long convertToDatabaseColumn(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long cents) {
        return cents == null ? null : BigDecimal.valueOf(cents, 2);
    }
}
