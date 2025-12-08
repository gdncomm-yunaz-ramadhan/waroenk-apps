package com.gdn.project.waroenk.catalog.properties;

import lombok.Data;

@Data
public class TypeSenseModel {
  private String protocol;
  private String host;
  private Integer port;
  private Integer timeout;
  private Integer embeddingDimension;
  private String apiKey;
  private boolean initializeSchema;
  private String collectionName;
  private int connectionRetries = 3;
  private int retryIntervalInSeconds = 5;
}
