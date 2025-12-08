package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.entity.Merchant;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class MerchantSearchIndexMapper implements SearchIndexMapper<Merchant> {

  @Override
  public String id(Merchant entity) {
    return entity.getCode();
  }

  @Override
  public String type() {
    return "merchants";
  }

  @Override
  public String title(Merchant entity) {
    return entity.getName();
  }

  @Override
  public String body(Merchant entity) {
    StringBuilder body = new StringBuilder();
    body.append(entity.getName()).append(" ");
    if (entity.getCode() != null) {
      body.append(entity.getCode()).append(" ");
    }
    if (entity.getLocation() != null) {
      body.append(entity.getLocation());
    }
    return body.toString().trim();
  }

  @Override
  public Map<String, Object> extraFields(Merchant entity) {
    Map<String, Object> fields = new HashMap<>();
    fields.put("code", entity.getCode());
    fields.put("location", entity.getLocation());
    fields.put("iconUrl", entity.getIconUrl());
    fields.put("rating", entity.getRating());
    if (entity.getContact() != null) {
      fields.put("email", entity.getContact().getEmail());
      fields.put("phone", entity.getContact().getPhone());
    }
    return fields;
  }

  @Override
  public Set<String> queryAbleFields() {
    return Set.of("title", "body", "code", "location");
  }
}
