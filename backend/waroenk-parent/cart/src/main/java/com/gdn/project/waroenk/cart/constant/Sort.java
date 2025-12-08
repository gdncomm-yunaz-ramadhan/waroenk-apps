package com.gdn.project.waroenk.cart.constant;

public enum Sort {
  ASCENDING("asc"), DESCENDING("desc");
  private final String shortName;

  Sort(String shortName) {
    this.shortName = shortName;
  }

  public String getShortName() {
    return this.shortName;
  }
}
