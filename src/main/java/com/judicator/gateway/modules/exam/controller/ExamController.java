package com.judicator.gateway.modules.exam.controller;

import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.common.response.ApiResponse;
import com.judicator.gateway.common.security.annotation.RateLimit;
import com.judicator.gateway.modules.exam.dto.request.CreateExamRequest;
import com.judicator.gateway.modules.exam.dto.request.UpdateExamRequest;
import com.judicator.gateway.modules.exam.dto.response.ExamResponse;
import com.judicator.gateway.modules.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "Exam Management", description = "Core CRUD APIs for exams")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class ExamController {

  ExamService examService;

  @Operation(summary = "Tạo mới bài thi")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tạo bài thi thành công",
            content = @Content(schema = @Schema(implementation = ExamResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Vượt giới hạn tần suất theo user_id",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @RateLimit(limit = 30, duration = 60)
  @PostMapping
  public ResponseEntity<ApiResponse<ExamResponse>> createExam(
      @Valid @RequestBody CreateExamRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<ExamResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Tạo bài thi thành công")
            .result(examService.createExam(request))
            .timestamp(Instant.now())
            .path("/api/v1/exams")
            .build());
  }

  @Operation(summary = "Lấy danh sách bài thi trong tenant hiện tại")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lấy danh sách bài thi thành công",
            content = @Content(schema = @Schema(implementation = ExamResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Vượt giới hạn tần suất theo user_id",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @RateLimit(limit = 120, duration = 60)
  @GetMapping
  public ResponseEntity<ApiResponse<List<ExamResponse>>> getAllExams() {
    return ResponseEntity.ok(
        ApiResponse.<List<ExamResponse>>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy danh sách bài thi thành công")
            .result(examService.getAllExams())
            .timestamp(Instant.now())
            .path("/api/v1/exams")
            .build());
  }

  @Operation(summary = "Lấy chi tiết bài thi")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lấy chi tiết bài thi thành công",
            content = @Content(schema = @Schema(implementation = ExamResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Không tìm thấy bài thi trong tenant hiện tại",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Vượt giới hạn tần suất theo user_id",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @RateLimit(limit = 120, duration = 60)
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ExamResponse>> getExam(@PathVariable UUID id) {
    return ResponseEntity.ok(
        ApiResponse.<ExamResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy chi tiết bài thi thành công")
            .result(examService.getExamById(id))
            .timestamp(Instant.now())
            .path("/api/v1/exams/" + id)
            .build());
  }

  @Operation(summary = "Cập nhật bài thi")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Cập nhật bài thi thành công",
            content = @Content(schema = @Schema(implementation = ExamResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Không tìm thấy bài thi trong tenant hiện tại",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Vượt giới hạn tần suất theo user_id",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @RateLimit(limit = 60, duration = 60)
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ExamResponse>> updateExam(
      @PathVariable UUID id, @Valid @RequestBody UpdateExamRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<ExamResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Cập nhật bài thi thành công")
            .result(examService.updateExam(id, request))
            .timestamp(Instant.now())
            .path("/api/v1/exams/" + id)
            .build());
  }

  @Operation(summary = "Xóa mềm bài thi")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Xóa bài thi thành công",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Không tìm thấy bài thi trong tenant hiện tại",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Vượt giới hạn tần suất theo user_id",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @RateLimit(limit = 30, duration = 60)
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable UUID id) {
    examService.deleteExam(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Xóa bài thi thành công")
            .timestamp(Instant.now())
            .path("/api/v1/exams/" + id)
            .build());
  }
}
