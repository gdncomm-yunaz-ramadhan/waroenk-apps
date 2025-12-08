package com.gdn.project.waroenk.member.converter;

import com.gdn.project.waroenk.member.constant.Sort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to allow case-insensitive Sort enum binding from request parameters.
 * Supports: "asc", "ASC", "ASCENDING" -> Sort.ASCENDING
 *           "desc", "DESC", "DESCENDING" -> Sort.DESCENDING
 */
@Component
public class StringToSortConverter implements Converter<String, Sort> {

    @Override
    public Sort convert(String source) {
        if (source == null || source.isBlank()) {
            return Sort.ASCENDING; // default
        }
        
        String normalized = source.trim().toUpperCase();
        
        return switch (normalized) {
            case "ASC", "ASCENDING" -> Sort.ASCENDING;
            case "DESC", "DESCENDING" -> Sort.DESCENDING;
            default -> throw new IllegalArgumentException(
                "Invalid sort direction: '" + source + "'. Use 'asc' or 'desc'");
        };
    }
}














