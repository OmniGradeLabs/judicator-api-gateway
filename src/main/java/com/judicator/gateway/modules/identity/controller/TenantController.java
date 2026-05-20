package com.judicator.gateway.modules.identity.controller;

import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.common.response.ApiResponse;
import com.judicator.gateway.modules.identity.dto.tenant.request.TenantCreationRequest;
import com.judicator.gateway.modules.identity.dto.tenant.response.TenantResponse;
import com.judicator.gateway.modules.identity.service.tenant.TenantService;
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
@RequestMapping("/tenants")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(
    name = "Tenant Management",
    description = "Quản lý khách hàng (Tenant) - Dành cho System Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class TenantController {

  TenantService tenantService;

  @Operation(summary = "Lấy danh sách tất cả khách hàng (Tenants)")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lấy danh sách thành công",
            content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Access token thiếu hoặc hết hạn",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "User hiện tại không có role SYSTEM_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @GetMapping
  public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants() {
    return ResponseEntity.ok(
        ApiResponse.<List<TenantResponse>>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy danh sách thành công")
            .result(tenantService.getAllTenants())
            .timestamp(Instant.now())
            .path("/tenants")
            .build());
  }

  @Operation(
      summary = "Tạo mới khách hàng (Tenant)",
      description =
          "Tạo một không gian làm việc (Tenant) mới trên hệ thống SaaS. Cần cung cấp slug"
              + " (để làm subdomain).")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tạo Tenant thành công",
            content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Trùng Slug",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @PostMapping
  public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
      @Valid @RequestBody TenantCreationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<TenantResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Tạo khách hàng thành công")
            .result(tenantService.createTenant(request))
            .timestamp(Instant.now())
            .path("/tenants")
            .build());
  }

  @Operation(summary = "Lấy thông tin chi tiết của Tenant")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lấy thông tin thành công",
            content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Không tìm thấy Tenant",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable UUID id) {
    return ResponseEntity.ok(
        ApiResponse.<TenantResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy thông tin thành công")
            .result(tenantService.getTenantById(id))
            .timestamp(Instant.now())
            .path("/tenants/" + id)
            .build());
  }

  @Operation(
      summary = "Disable Tenant (Suspend)",
      description =
          "Tạm dừng hoạt động của Tenant. Hệ thống sẽ tự động kick toàn bộ User thuộc"
              + " Tenant này khỏi Redis.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Đình chỉ thành công",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Không tìm thấy Tenant",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @PutMapping("/{id}/suspend")
  public ResponseEntity<ApiResponse<Void>> suspendTenant(@PathVariable UUID id) {
    tenantService.suspendTenant(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Đã đình chỉ hoạt động khách hàng")
            .timestamp(Instant.now())
            .path("/tenants/" + id + "/suspend")
            .build());
  }

  @Operation(summary = "Xóa mềm Tenant")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Xóa thành công",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Không tìm thấy Tenant",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable UUID id) {
    tenantService.deleteTenant(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Đã xóa khách hàng")
            .timestamp(Instant.now())
            .path("/tenants/" + id)
            .build());
  }
}
