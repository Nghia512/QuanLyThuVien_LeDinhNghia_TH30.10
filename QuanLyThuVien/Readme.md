# Đồ án Java: Quản lý thư viện

Ứng dụng desktop Java Swing kết nối SQL Server bằng JDBC. Chức năng hiện có:

- Trang chủ với thanh menu xanh kiểu Hệ thống / Quản lý / Tra cứu / Trợ giúp; mở từng màn hình qua menu.

- Thêm, sửa, xóa và tìm kiếm sách theo tên, thể loại, năm xuất bản, nhà xuất bản hoặc tác giả; hỗ trợ ISBN tùy chọn và không cho xóa sách đã có lịch sử mượn.
- Chọn nhiều tác giả khi thêm/sửa sách; có thể thêm tác giả mới ngay trong form sách.
- Chọn thể loại và nhà xuất bản theo mã/tên khi nhập sách.
- Thêm thể loại và nhà xuất bản trong tab **Thể loại / NXB**; danh sách và lựa chọn được sắp theo mã tăng dần.
- Thêm, sửa, xóa và tìm kiếm độc giả theo tên, số điện thoại hoặc email; có ngày sinh, giới tính, địa chỉ, liên hệ và hạn thẻ.
- Thêm, sửa, xóa nhân viên; mã tự cấp và chức vụ được chọn từ danh sách phù hợp với phần mềm.
- Chọn sách còn trong kho theo mã/tên khi lập phiếu mượn, để tránh nhập nhầm mã.
- Lập phiếu mượn qua stored procedure `sp_taophieumuon` (kiểm tra thẻ, tồn kho; trigger cập nhật kho).
- Ghi nhận trả sách; trigger cập nhật số lượng và tiền phạt.
- Xem danh sách sách quá hạn.

## Công nghệ

- Java 17+
- Apache Maven
- Microsoft SQL Server
- JDBC (`mssql-jdbc`)

## Cài đặt cơ sở dữ liệu

1. Mở `database_qltv.sql` bằng SQL Server Management Studio và chạy script.
2. Lưu ý: đầu script xóa database `quanlythuvien` nếu đã tồn tại rồi tạo lại. Sao lưu dữ liệu trước khi chạy lại script.
3. Script tạo dữ liệu mẫu; tài khoản nhân viên mẫu có `manhanvien = 1`.

## Kết nối

Mặc định ứng dụng dùng **Windows Authentication**, giống cấu hình đăng nhập SSMS của bạn, kết nối tới:

`jdbc:sqlserver://localhost:1433;databaseName=quanlythuvien;encrypt=true;trustServerCertificate=true`

Maven tự chép thư viện xác thực Windows vào `target/native`. Chạy ứng dụng bằng:

```powershell
java '-Djava.library.path=target/native' -jar target/quanlythuvien-1.0.0.jar
```

Tài khoản Windows đang chạy ứng dụng phải có quyền truy cập database `quanlythuvien` trong SQL Server. Nếu muốn dùng SQL Authentication thay vào đó, đặt `QTTV_DB_INTEGRATED=false` cùng `QTTV_DB_USER` và `QTTV_DB_PASSWORD`.

Thiết lập biến môi trường nếu thông tin SQL Server của bạn khác:

- `QTTV_DB_URL` – JDBC URL
- `QTTV_DB_INTEGRATED` – Windows Authentication (mặc định `true`)
- `QTTV_DB_USER` và `QTTV_DB_PASSWORD` – chỉ dùng khi đặt `QTTV_DB_INTEGRATED=false`

## Chạy

Từ thư mục dự án, chạy:

```powershell
mvn clean package
java '-Djava.library.path=target/native' -jar target/quanlythuvien-1.0.0.jar
```

Hoặc chạy lớp `vn.edu.qltv.QuanLyThuVienApp` trong IDE sau khi Maven tải dependency.

Ngày sinh và ngày hết hạn nhập theo định dạng `yyyy-mm-dd` (ví dụ `2005-03-10`). Ở màn hình sách, dùng các bộ lọc thể loại, năm xuất bản, nhà xuất bản và tác giả; mã được hiển thị kèm tên trong danh sách. Khi lập phiếu mượn, chọn độc giả theo tên/số điện thoại, nhân viên và sách từ danh sách; sách hiển thị mã, tên và số lượng còn trong kho.
