package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.catalog.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.catalog.entity.SystemParameter;

public interface SystemParameterService {
  SystemParameter findOneSystemParameter(String variable);
  SystemParameter upsertSystemParameter(SystemParameter parameter);
  boolean deleteSystemParameter(String variable);
  MultipleSystemParameterResponse findAllSystemParameters(MultipleSystemParameterRequest request);
  String getVariableData(String variable);
  String getVariableData(String variable, String defaultValue);
}










