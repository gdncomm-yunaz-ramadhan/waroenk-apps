package com.gdn.project.waroenk.contract;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration to enable the embedded gRPC UI.
 * This is automatically picked up by Spring Boot's auto-configuration mechanism.
 */
@AutoConfiguration
@Import(EmbeddedGrpcUiController.class)
public class GrpcUiAutoConfiguration {
}





