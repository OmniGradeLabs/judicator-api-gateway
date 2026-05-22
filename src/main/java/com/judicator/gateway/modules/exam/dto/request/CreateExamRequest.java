package com.judicator.gateway.modules.exam.dto.request;

import com.judicator.gateway.modules.exam.enumType.ExamStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExamRequest(
    @NotBlank(message = "Tiêu đề bài thi không được để trống")
        @Size(max = 500, message = "Tiêu đề bài thi không được vượt quá 500 ký tự")
        String title,
    @NotBlank(message = "Nội dung markdown không được để trống") String markdown,
    @NotNull(message = "Thời lượng bài thi không được để trống")
        @Min(value = 1, message = "Thời lượng bài thi phải >= 1 phút")
        @Max(value = 1440, message = "Thời lượng bài thi không được vượt quá 1440 phút")
        Integer timeLimitMinutes,
    @Size(max = 255, message = "paceLimits không được vượt quá 255 ký tự") String paceLimits,
    @NotNull(message = "Trạng thái bài thi không được để trống") ExamStatus status,
    @NotBlank(message = "rulePayload không được để trống") String rulePayload,
    @Size(max = 1000, message = "playwrightZipUrl không được vượt quá 1000 ký tự")
        String playwrightZipUrl) {}
