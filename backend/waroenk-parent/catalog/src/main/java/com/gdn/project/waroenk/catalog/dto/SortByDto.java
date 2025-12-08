package com.gdn.project.waroenk.catalog.dto;

public record SortByDto(String field, String direction) {
  public static class SortByBuilder {
    private String field = "id";
    private String direction = "asc";

    public SortByBuilder field(String field) {
      this.field = field;
      return this;
    }

    public SortByBuilder direction(String direction) {
      this.direction = direction;
      return this;
    }

    public SortByDto build() {
      return new SortByDto(field, direction);
    }
  }
}










