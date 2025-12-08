package com.gdn.project.waroenk.catalog.configuration;

import com.gdn.project.waroenk.catalog.properties.TypeSenseModel;
import com.gdn.project.waroenk.catalog.properties.TypeSenseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.typesense.api.Client;
import org.typesense.api.FieldTypes;
import org.typesense.model.CollectionSchema;
import org.typesense.model.Field;
import org.typesense.resources.Node;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.gdn.project.waroenk.catalog.constant.CollectionConstant.MERCHANT_COLLECTION;
import static com.gdn.project.waroenk.catalog.constant.CollectionConstant.PRODUCT_COLLECTION;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TypeSenseConfiguration {
  private final TypeSenseProperties properties;

  @Bean
  @Primary
  @Order(1)
  public Client typesenseClient() throws Exception {
    TypeSenseModel model = properties.getClient();
    List<Node> nodes = new ArrayList<>();
    nodes.add(new Node(model.getProtocol(), model.getHost(), String.valueOf(model.getPort())));
    org.typesense.api.Configuration configuration =
        new org.typesense.api.Configuration(nodes, Duration.ofSeconds(model.getTimeout()), model.getApiKey());

    Client client = new Client(configuration);
    ensureReady(client,
        model.getConnectionRetries(),
        Duration.of(model.getRetryIntervalInSeconds(), ChronoUnit.SECONDS));
    setupTypesenseSchemas(client);
    return client;
  }

  public CompletableFuture<Boolean> ensureReady(Client client, int maxRetry, Duration delay) {
    return CompletableFuture.supplyAsync(() -> {

      int attempt = 0;

      while (attempt < maxRetry) {
        try {
          boolean ok = client.health.retrieve().get("ok").equals(Boolean.TRUE);

          if (ok) {
            log.info("✅ Typesense is healthy and ready!");
            return true;
          }

        } catch (Exception e) {
          log.warn("⚠️ Typesense not ready yet: " + e.getMessage());
        }

        attempt++;
        try {
          TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException ignored) {
        }
      }

      log.warn("❌ Typesense connection failed after {} retries.", attempt);
      return false;
    });
  }

  public void setupTypesenseSchemas(Client client) throws Exception {
    ensureCollectionExists(client, PRODUCT_COLLECTION, productSchema());
    ensureCollectionExists(client, MERCHANT_COLLECTION, merchantSchema());
    log.info("Loading typesense schema task has been completed!");
  }

  private CollectionSchema productSchema() {
    return new CollectionSchema().name(PRODUCT_COLLECTION)
        .fields(List.of(new Field().name("id").type(FieldTypes.STRING).index(true).sort(true),
            new Field().name("type").type(FieldTypes.STRING).facet(true),
            new Field().name("title").type(FieldTypes.STRING).index(true),
            // Enable index for search
            new Field().name("body").type(FieldTypes.STRING).index(true),
            // Enable index for search
            new Field().name("sku").type(FieldTypes.STRING).index(true),
            // Enable index for exact match
            new Field().name("subSku").type(FieldTypes.STRING).index(true),
            // Enable index for exact match
            new Field().name("slug").type(FieldTypes.STRING).index(true),
            new Field().name("category").type(FieldTypes.STRING).facet(true).index(true).optional(true),
            new Field().name("categoryCode").type(FieldTypes.STRING).facet(true).index(true).optional(true),
            new Field().name("categoryNames").type(FieldTypes.STRING_ARRAY).facet(true).optional(true),
            new Field().name("categoryCodes").type(FieldTypes.STRING_ARRAY).facet(true).optional(true),
            new Field().name("brand").type(FieldTypes.STRING).facet(true).index(true).optional(true),
            new Field().name("merchantName").type(FieldTypes.STRING).facet(true).index(true).optional(true),
            new Field().name("merchantCode").type(FieldTypes.STRING).facet(true).index(true).optional(true),
            new Field().name("merchantLocation").type(FieldTypes.STRING).facet(true).optional(true),
            new Field().name("variantKeywords").type(FieldTypes.STRING_ARRAY).facet(true).optional(true),
            new Field().name("attributes").type(FieldTypes.STRING_ARRAY).facet(false).optional(true),
            new Field().name("thumbnail").type(FieldTypes.STRING).optional(true).index(false),
            new Field().name("inStock").type(FieldTypes.BOOL).facet(true).optional(true),
            new Field().name("createdAt").type(FieldTypes.INT64).optional(true),
            new Field().name("price").type(FieldTypes.FLOAT).facet(false).sort(true).optional(true)));
  }

  private CollectionSchema merchantSchema() {
    return new CollectionSchema().name(MERCHANT_COLLECTION)
        .fields(List.of(new Field().name("id").type(FieldTypes.STRING).index(true).sort(true),
            new Field().name("type").type(FieldTypes.STRING).facet(true),
            new Field().name("title").type(FieldTypes.STRING),
            new Field().name("body").type(FieldTypes.STRING),
            new Field().name("code").type(FieldTypes.STRING),
            new Field().name("location").type(FieldTypes.STRING).optional(true),
            new Field().name("iconUrl").type(FieldTypes.STRING).optional(true),
            new Field().name("email").type(FieldTypes.STRING).optional(true),
            new Field().name("phone").type(FieldTypes.STRING).optional(true),
            new Field().name("rating").type(FieldTypes.FLOAT).sort(true).facet(false).optional(true),
            new Field().name("createdAt").type(FieldTypes.INT64).optional(true)));
  }

  private void ensureCollectionExists(Client client, String name, CollectionSchema schema) throws Exception {
    try {
      client.collections(name).retrieve();
      log.warn("✔ Typesense collection already exists: {}", name);
    } catch (Exception e) {
      log.error("⚠ Creating Typesense collection: {}", name);
      client.collections().create(schema);
    }
  }

  @Bean(name = "typesenseExecutor")
  public Executor typesenseExecutor() {
    return Executors.newFixedThreadPool(4);
  }
}
