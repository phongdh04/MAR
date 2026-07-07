package vn.mar.common.validation;

import vn.mar.common.exception.ValidationException;
import vn.mar.common.search.SearchText;

public final class EnumParser {

    private EnumParser() {
    }

    public static <E extends Enum<E>> E optionalEnum(
            Class<E> enumType,
            String value,
            String field,
            String code,
            String message) {
        String normalized = SearchText.upperOrNull(value);
        if (normalized == null) {
            return null;
        }
        return parse(enumType, normalized, field, code, message);
    }

    public static <E extends Enum<E>> E requiredEnum(
            Class<E> enumType,
            String value,
            String field,
            String code,
            String message) {
        String normalized = SearchText.upperOrNull(value);
        if (normalized == null) {
            throw ValidationException.of(field, code, message);
        }
        return parse(enumType, normalized, field, code, message);
    }

    private static <E extends Enum<E>> E parse(
            Class<E> enumType,
            String normalized,
            String field,
            String code,
            String message) {
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of(field, code, message);
        }
    }
}
