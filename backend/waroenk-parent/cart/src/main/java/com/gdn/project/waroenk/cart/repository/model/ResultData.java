package com.gdn.project.waroenk.cart.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * Result data wrapper for paginated queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultData<DATA> {
    private Iterable<DATA> dataList;
    private Long total;
    private Optional<DATA> offset;
}











