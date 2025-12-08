package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.FilterBrandRequest;
import com.gdn.project.waroenk.catalog.MultipleBrandResponse;
import com.gdn.project.waroenk.catalog.entity.Brand;

public interface BrandService {
  Brand createBrand(Brand brand);
  Brand updateBrand(String id, Brand brand);
  Brand findBrandById(String id);
  Brand findBrandBySlug(String slug);
  boolean deleteBrand(String id);
  MultipleBrandResponse filterBrands(FilterBrandRequest request);
}










