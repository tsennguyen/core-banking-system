# Core Banking System

Hệ thống quản lý tài khoản ngân hàng và giao dịch tài chính (Core Banking REST API) phục vụ quản lý khách hàng, tài khoản thanh toán/tiết kiệm, nạp/rút/chuyển tiền an toàn đa luồng, kiểm soát hạn mức, tính phí giao dịch, bảo mật JWT/RBAC, phát hiện giao dịch bất thường và xuất báo cáo đối soát.

## 1. Công nghệ sử dụng
- **Backend:** Java 21 LTS, Spring Boot 3.5.0
- **Cơ sở dữ liệu:** PostgreSQL 17 (Flyway migration)
- **Caching & Locking:** Caffeine Cache / Redis, Pessimistic Locking (\`SELECT FOR UPDATE\`)
- **Tài liệu API:** OpenAPI 3 / Swagger (\`/swagger-ui.html\`)
- **Bảo mật:** Spring Security, Stateless JWT, BCrypt, Rate Limiting
- **Chất lượng mã nguồn:** JaCoCo (Coverage ≥ 80%), Spotless, Checkstyle
- **Testing:** JUnit 5, Mockito, Testcontainers

## 2. Cấu trúc dự án
- \`backend/\`: Mã nguồn Spring Boot REST API
- \`frontend/\`: Giao diện quản trị Next.js (Dashboard)
- \`docs/\`: Tài liệu kiến trúc, sơ đồ thực thể liên kết (ERD) và Architectural Decision Records (ADR)

## 3. Hướng dẫn chạy thử nghiệm
### Yêu cầu môi trường
- Java 21
- Maven 3.9+ (hoặc dùng \`./mvnw\` có sẵn trong thư mục \`backend\`)
- PostgreSQL 17 (hoặc Docker Compose)

\`\`\`bash
# Khởi động dịch vụ backend
cd backend
./mvnw clean spring-boot:run
\`\`\`

Chi tiết các API và tài liệu nghiệp vụ được cung cấp tại \`/swagger-ui.html\` sau khi khởi động.
