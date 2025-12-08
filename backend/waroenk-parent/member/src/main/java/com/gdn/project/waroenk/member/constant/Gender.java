package com.gdn.project.waroenk.member.constant;

public enum Gender {
  MALE("M"), FEMALE("F"), OTHER("O");

  private final String gender;

  Gender(String option) {
    this.gender = option;
  }

  public String getGender(){
    return this.gender;
  }
}
