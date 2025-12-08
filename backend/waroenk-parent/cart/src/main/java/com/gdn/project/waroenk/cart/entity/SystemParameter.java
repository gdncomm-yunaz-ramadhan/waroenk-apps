package com.gdn.project.waroenk.cart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * System parameter entity for configurable system parameters.
 * Stored in MongoDB collection "system_parameters".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "system_parameters")
public class SystemParameter {
    @Id
    private String id;

    @Indexed(unique = true)
    private String variable;

    private String data;

    private String description;

    @Builder.Default
    private String type = "STRING"; // STRING, INTEGER, LONG, BOOLEAN, JSON

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Get value as Integer
     */
    public Integer getAsInteger() {
        try {
            return Integer.parseInt(data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get value as Long
     */
    public Long getAsLong() {
        try {
            return Long.parseLong(data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get value as Boolean
     */
    public Boolean getAsBoolean() {
        return Boolean.parseBoolean(data);
    }
}










