package com.judicator.gateway.modules.exam.mapper;

import com.judicator.gateway.modules.exam.dto.request.CreateExamRequest;
import com.judicator.gateway.modules.exam.dto.request.UpdateExamRequest;
import com.judicator.gateway.modules.exam.dto.response.ExamResponse;
import com.judicator.gateway.modules.exam.entity.Exam;
import com.judicator.gateway.modules.exam.entity.ExamRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExamMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "createdByTeacherId", ignore = true)
  @Mapping(target = "isDeleted", ignore = true)
  Exam toEntity(CreateExamRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "exam", ignore = true)
  ExamRule toRuleEntity(CreateExamRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "createdByTeacherId", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  void updateEntity(UpdateExamRequest request, @MappingTarget Exam exam);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "exam", ignore = true)
  void updateRule(UpdateExamRequest request, @MappingTarget ExamRule rule);

  @Mapping(target = "id", source = "exam.id")
  @Mapping(target = "createdAt", source = "exam.createdAt")
  @Mapping(target = "updatedAt", source = "exam.updatedAt")
  @Mapping(target = "rulePayload", source = "rule.rulePayload")
  @Mapping(target = "playwrightZipUrl", source = "rule.playwrightZipUrl")
  @Mapping(target = "createdByTeacherName", source = "createdByTeacherName")
  ExamResponse toResponse(Exam exam, ExamRule rule, String createdByTeacherName);
}
