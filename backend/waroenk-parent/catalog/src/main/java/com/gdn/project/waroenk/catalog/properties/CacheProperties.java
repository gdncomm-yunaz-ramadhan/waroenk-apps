package com.gdn.project.waroenk.catalog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Data
@Primary
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
  private Long defaultExpiryTimeInSeconds = 60L;
}
