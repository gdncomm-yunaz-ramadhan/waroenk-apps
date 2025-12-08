package com.gdn.project.waroenk.catalog.mapper;

import com.gdn.project.waroenk.catalog.CategoryData;
import com.gdn.project.waroenk.catalog.CategoryNode;
import com.gdn.project.waroenk.catalog.CreateCategoryRequest;
import com.gdn.project.waroenk.catalog.dto.category.CategoryTreeNodeDto;
import com.gdn.project.waroenk.catalog.dto.category.CreateCategoryRequestDto;
import com.gdn.project.waroenk.catalog.entity.Category;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CategoryMapper extends GenericMapper {

  CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);


  default CategoryData toResponseGrpc(Category entity) {
    CategoryData.Builder builder = CategoryData.newBuilder();
    builder.setId(entity.getId());
    builder.setName(entity.getName());
    builder.setSlug(entity.getSlug());
    if (entity.getIconUrl() != null) {
      builder.setIconUrl(entity.getIconUrl());
    }
    if (entity.getParentId() != null) {
      builder.setParentId(entity.getParentId());
    }
    if (entity.getCreatedAt() != null) {
      builder.setCreatedAt(toTimestamp(entity.getCreatedAt()));
    }
    if (entity.getUpdatedAt() != null) {
      builder.setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
    }
    return builder.build();
  }

  default CategoryNode toCategoryNodeGrpc(CategoryTreeNodeDto nodeDto) {
    CategoryNode.Builder builder = CategoryNode.newBuilder();
    builder.setId(nodeDto.getId());
    builder.setName(nodeDto.getName());
    builder.setSlug(nodeDto.getSlug());
    if (nodeDto.getIconUrl() != null) {
      builder.setIconUrl(nodeDto.getIconUrl());
    }
    if (ObjectUtils.isNotEmpty(nodeDto.getChildren())) {
      builder.addAllChildren(nodeDto.getChildren().stream().map(this::toCategoryNodeGrpc).toList());
    }
    return builder.build();
  }

  default Category toEntity(CreateCategoryRequest request) {
    return Category.builder()
        .name(request.getName())
        .slug(request.getSlug())
        .iconUrl(request.getIconUrl())
        .parentId(request.getParentId())
        .build();
  }

  default Category toEntity(CreateCategoryRequestDto dto) {
    return Category.builder().name(dto.name()).slug(dto.slug()).iconUrl(dto.iconUrl()).parentId(dto.parentId()).build();
  }

  default CategoryTreeNodeDto toTreeNodeDto(CategoryNode node) {
    return CategoryTreeNodeDto.builder()
        .id(node.getId())
        .name(node.getName())
        .iconUrl(node.getIconUrl())
        .slug(node.getSlug())
        .children(node.getChildrenList().stream().map(this::toTreeNodeDto).toList())
        .build();
  }
}



