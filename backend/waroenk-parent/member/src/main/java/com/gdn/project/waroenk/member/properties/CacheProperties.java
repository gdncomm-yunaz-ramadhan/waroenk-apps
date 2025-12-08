package com.gdn.project.waroenk.member.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
  private Long defaultExpiryTimeInSeconds = 60L;
}
