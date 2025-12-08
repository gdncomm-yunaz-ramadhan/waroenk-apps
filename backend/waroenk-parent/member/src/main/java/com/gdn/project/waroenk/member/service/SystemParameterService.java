package com.gdn.project.waroenk.member.service;

import com.gdn.project.waroenk.member.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.member.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.member.entity.SystemParameter;

public interface SystemParameterService {
  SystemParameter findOneSystemParameter(String variable);

  SystemParameter upsertSystemParameter(SystemParameter parameter);

  boolean deleteSystemParameter(String variable);

  MultipleSystemParameterResponse findAllSystemParameters(MultipleSystemParameterRequest request);

  String getVariableData(String variable);

  String getVariableData(String variable, String defaultValue);
}
