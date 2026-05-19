# Judicator API Gateway - Tiêu chuẩn API Specification

Tài liệu này đặc tả chi tiết các APIs của 2 module cốt lõi: **Authentication (Đăng nhập & Phiên bản)** và **Tenant Management (Quản lý Khách hàng SaaS)**. Tài liệu dành cho Frontend/Mobile Developers để tích hợp.

---

## 1. Cấu trúc Response chuẩn (Standard Response Format)

Tất cả các API (thành công hoặc thất bại) đều trả về một format JSON duy nhất (`ApiResponse`):

```json
{
  "code": 1000,
  "message": "Success message or error description",
  "result": { ... }, // Dữ liệu trả về (nếu có), null nếu lỗi hoặc không có dữ liệu
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/api/path"
}
```

*   `code`: HTTP Status code hoặc mã lỗi nội bộ (Ví dụ: 1000 = Thành công, 4003 = Lỗi Validate/Logic).
*   `message`: Thông báo cho End-User (Có thể hiển thị trực tiếp lên UI).
*   `result`: Payload thực tế.

---

## 2. Authentication Module (`/auth`)

Module chịu trách nhiệm xác thực người dùng, cấp phát và thu hồi JWT token, quản lý phiên bản (session).

### 2.1. Đăng nhập (Login)
*   **Endpoint:** `POST /auth/login`
*   **Description:** Đăng nhập bằng `username` / `password`. Trả về Access Token trong body và thiết lập Refresh Token vào HttpOnly Cookie.
*   **Rate Limit:** 5 requests / 60 seconds (theo `username`).

**Request Body (`application/json`)**
```json
{
  "username": "system.admin@judicator.local",
  "password": "Password@123"
}
```

**Response (Thành công - 200 OK)**
*   **Headers:** `Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800`
```json
{
  "code": 1000,
  "message": "Đăng nhập thành công",
  "result": {
    "authenticated": true,
    "accessToken": "<ACCESS_TOKEN_JWT>",
    "refreshToken": "<REFRESH_TOKEN_JWT>" // Tùy chọn, vì FE nên dùng Cookie
  },
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/auth/login"
}
```

### 2.2. Làm mới Token (Refresh)
*   **Endpoint:** `POST /auth/refresh`
*   **Description:** Lấy Access Token mới khi token cũ hết hạn. Hệ thống ưu tiên đọc Refresh Token từ Cookie `refresh_token`. Nếu thiết bị không hỗ trợ Cookie (như Mobile App), có thể gửi qua header `X-Refresh-Token`.

**Request Headers**
*   `Cookie`: `refresh_token=<token>` (Ưu tiên)
*   `X-Refresh-Token`: `<token>` (Fallback)

**Response (Thành công - 200 OK)**
*   **Headers:** `Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800` (JTI được xoay vòng)
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "authenticated": true,
    "accessToken": "<ACCESS_TOKEN_JWT_NEW>",
    "refreshToken": "<REFRESH_TOKEN_JWT_NEW>"
  },
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/auth/refresh"
}
```

### 2.3. Lấy thông tin Cá nhân (Get Profile)
*   **Endpoint:** `GET /auth/me`
*   **Description:** Lấy thông tin user hiện tại đang đăng nhập cùng với danh sách Roles và Permissions.
*   **Security:** Yêu cầu Header `Authorization: Bearer <AccessToken>`

**Response (Thành công - 200 OK)**
```json
{
  "code": 1000,
  "message": "Lấy thông tin cá nhân thành công",
  "result": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "tenantId": "123e4567-e89b-12d3-a456-426614174001",
    "username": "system.admin@judicator.local",
    "fullName": "System Administrator",
    "phone": "0900000001",
    "roles": ["SYSTEM_ADMIN"],
    "permissions": ["QUYEN_TREN_APP.HE_THONG.TENANT.CREATE", "..."]
  },
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/auth/me"
}
```

### 2.4. Đăng xuất (Logout)
*   **Endpoint:** `POST /auth/logout`
*   **Description:** Đăng xuất thiết bị hiện tại. Hủy Session trong DB, xóa Cookie.
*   **Security:** Yêu cầu Header `Authorization: Bearer <AccessToken>`

**Response (Thành công - 200 OK)**
*   **Headers:** `Set-Cookie: refresh_token=; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=0` (Xóa Cookie)
```json
{
  "code": 1000,
  "message": "Đăng xuất thành công",
  "result": null,
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/auth/logout"
}
```

### 2.5. Đăng xuất Tất cả Thiết bị (Logout All)
*   **Endpoint:** `POST /auth/logout-all`
*   **Description:** Đăng xuất tài khoản khỏi **TẤT CẢ** các thiết bị đang đăng nhập.
*   **Security:** Yêu cầu Header `Authorization: Bearer <AccessToken>`

**Response (Thành công - 200 OK)**
```json
{
  "code": 1000,
  "message": "Đăng xuất tất cả sessions thành công",
  "result": null,
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/auth/logout-all"
}
```

### 2.6. Admin Kích xuất User (Force Logout)
*   **Endpoint:** `POST /auth/admin/users/{userId}/force-logout`
*   **Description:** Dành cho SYSTEM_ADMIN để đá văng một User bất kỳ khỏi hệ thống.
*   **Security:** Yêu cầu Header `Authorization: Bearer <AccessToken>` (Must have role: `SYSTEM_ADMIN`)

**Response (Thành công - 200 OK)**
```json
{
  "code": 1000,
  "message": "Force logout thành công",
  "result": null,
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/auth/admin/users/{userId}/force-logout"
}
```

---

## 3. Tenant Management Module (`/tenants`)

Module dành cho Quản trị viên hệ thống (SYSTEM_ADMIN) quản lý các khách hàng SaaS (Tenants). Tất cả API dưới đây đều yêu cầu Header `Authorization: Bearer <AccessToken>` và quyền hạn tương ứng.

### 3.1. Tạo mới Tenant
*   **Endpoint:** `POST /tenants`
*   **Description:** Tạo một khách hàng SaaS mới.

**Request Body (`application/json`)**
```json
{
  "name": "Trường Đại học XYZ",
  "slug": "daihoc-xyz",
  "emailContact": "admin@xyz.edu.vn"
}
```
*(Lưu ý: `slug` chỉ được chứa chữ thường, số và dấu gạch ngang `-`)*

**Response (Thành công - 200 OK)**
```json
{
  "code": 1000,
  "message": "Tạo khách hàng thành công",
  "result": {
    "id": "a1b2c3d4-...",
    "name": "Trường Đại học XYZ",
    "slug": "daihoc-xyz",
    "emailContact": "admin@xyz.edu.vn",
    "status": "ACTIVE",
    "createdAt": "2026-05-19T10:00:00"
  },
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/tenants"
}
```

### 3.2. Xem Chi tiết Tenant
*   **Endpoint:** `GET /tenants/{id}`
*   **Description:** Lấy thông tin chi tiết một Tenant theo UUID.

**Response (Thành công - 200 OK)**
```json
{
  "code": 1000,
  "message": "Lấy thông tin thành công",
  "result": {
    "id": "a1b2c3d4-...",
    "name": "Trường Đại học XYZ",
    "slug": "daihoc-xyz",
    "emailContact": "admin@xyz.edu.vn",
    "status": "ACTIVE",
    "createdAt": "2026-05-19T10:00:00"
  },
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/tenants/{id}"
}
```

### 3.3. Đình chỉ (Suspend) Tenant
*   **Endpoint:** `PATCH /tenants/{id}/suspend`
*   **Description:** Tạm dừng hoạt động của Tenant (Ví dụ: hết hạn hợp đồng, chưa thanh toán). Khi gọi API này, toàn bộ Users thuộc Tenant này sẽ bị kick khỏi hệ thống ngay lập tức (Xóa session & blacklist token).

**Response (Thành công - 200 OK)**
```json
{
  "code": 1000,
  "message": "Đã đình chỉ hoạt động khách hàng",
  "result": null,
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/tenants/{id}/suspend"
}
```

### 3.4. Xóa mềm (Delete) Tenant
*   **Endpoint:** `DELETE /tenants/{id}`
*   **Description:** Đánh dấu xóa (Soft delete) một Tenant.

**Response (Thành công - 200 OK)**
```json
{
  "code": 1000,
  "message": "Đã xóa khách hàng",
  "result": null,
  "timestamp": "2026-05-19T10:00:00.000Z",
  "path": "/tenants/{id}"
}
```
