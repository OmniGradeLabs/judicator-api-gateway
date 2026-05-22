package com.judicator.gateway.modules.exam.service;

import com.judicator.gateway.modules.exam.dto.request.CreateExamRequest;
import com.judicator.gateway.modules.exam.dto.request.UpdateExamRequest;
import com.judicator.gateway.modules.exam.dto.response.ExamResponse;
import java.util.List;
import java.util.UUID;

public interface ExamService {

  ExamResponse createExam(CreateExamRequest request);

  List<ExamResponse> getAllExams();

  ExamResponse getExamById(UUID id);

  ExamResponse updateExam(UUID id, UpdateExamRequest request);

  void deleteExam(UUID id);
}
