package com.gdn.project.waroenk.catalog.model;

import org.apache.commons.lang3.StringUtils;

public record SortInfo(String field, String direction) {
    public static SortInfo of(String field, String direction) {
        return new SortInfo(
            StringUtils.isNotBlank(field) ? field : "id",
            StringUtils.isNotBlank(direction) ? direction : "asc"
        );
    }
    
    public static SortInfo defaultSort() {
        return new SortInfo("id", "asc");
    }
}




