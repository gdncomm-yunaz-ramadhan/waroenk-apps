package com.gdn.project.waroenk.catalog.model;

import java.util.List;

public class CategoryTreeNodeDto {
    private String id;
    private String name;
    private String iconUrl;
    private String slug;
    private List<CategoryTreeNodeDto> children;
    
    public CategoryTreeNodeDto() {}
    
    public CategoryTreeNodeDto(String id, String name, String iconUrl, String slug, List<CategoryTreeNodeDto> children) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.slug = slug;
        this.children = children;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public List<CategoryTreeNodeDto> getChildren() { return children; }
    public void setChildren(List<CategoryTreeNodeDto> children) { this.children = children; }
}




