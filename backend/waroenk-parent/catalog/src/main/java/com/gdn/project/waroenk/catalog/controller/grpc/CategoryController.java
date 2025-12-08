package com.gdn.project.waroenk.catalog.controller.grpc;

import com.gdn.project.waroenk.catalog.CategoryData;
import com.gdn.project.waroenk.catalog.CategoryServiceGrpc;
import com.gdn.project.waroenk.catalog.CategoryTreeResponse;
import com.gdn.project.waroenk.catalog.CreateCategoryRequest;
import com.gdn.project.waroenk.catalog.FilterCategoryRequest;
import com.gdn.project.waroenk.catalog.FindCategoryBySlugRequest;
import com.gdn.project.waroenk.catalog.MultipleCategoryResponse;
import com.gdn.project.waroenk.catalog.UpdateCategoryRequest;
import com.gdn.project.waroenk.catalog.entity.Category;
import com.gdn.project.waroenk.catalog.mapper.CategoryMapper;
import com.gdn.project.waroenk.catalog.service.CategoryService;
import com.gdn.project.waroenk.common.Basic;
import com.gdn.project.waroenk.common.Empty;
import com.gdn.project.waroenk.common.Id;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CategoryController extends CategoryServiceGrpc.CategoryServiceImplBase {

  private static final CategoryMapper mapper = CategoryMapper.INSTANCE;
  private final CategoryService categoryService;

  @Override
  public void createCategory(CreateCategoryRequest request, StreamObserver<CategoryData> responseObserver) {
    Category category = mapper.toEntity(request);
    CategoryData response = mapper.toResponseGrpc(categoryService.createCategory(category));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateCategory(UpdateCategoryRequest request, StreamObserver<CategoryData> responseObserver) {
    Category category =
        Category.builder().name(request.getName()).slug(request.getSlug()).parentId(request.getParentId()).build();
    CategoryData response = mapper.toResponseGrpc(categoryService.updateCategory(request.getId(), category));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteCategory(Id request, StreamObserver<Basic> responseObserver) {
    boolean result = categoryService.deleteCategory(request.getValue());
    Basic response = Basic.newBuilder().setStatus(result).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findCategoryById(Id request, StreamObserver<CategoryData> responseObserver) {
    CategoryData response = mapper.toResponseGrpc(categoryService.findCategoryById(request.getValue()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void findCategoryBySlug(FindCategoryBySlugRequest request, StreamObserver<CategoryData> responseObserver) {
    CategoryData response = mapper.toResponseGrpc(categoryService.findCategoryBySlug(request.getSlug()));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void filterCategory(FilterCategoryRequest request, StreamObserver<MultipleCategoryResponse> responseObserver) {
    MultipleCategoryResponse response = categoryService.filterCategories(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getCategoryTree(Empty request, StreamObserver<CategoryTreeResponse> responseObserver) {
    CategoryTreeResponse response = categoryService.getCategoryTree();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}












