package com.gdn.project.waroenk.catalog.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for category tree node.
 * NOTE: This is a regular class (not a record) to ensure Jackson's NON_FINAL
 * default typing adds the @class property for Redis cache serialization.
 * Java records are implicitly final, which causes NON_FINAL typing to skip them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNodeDto {
    private String id;
    private String name;
    private String iconUrl;
    private String slug;
    private List<CategoryTreeNodeDto> children;
}
