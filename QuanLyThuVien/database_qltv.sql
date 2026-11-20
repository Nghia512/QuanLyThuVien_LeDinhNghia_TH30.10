USE master;
GO

IF DB_ID(N'quanlythuvien') IS NOT NULL
BEGIN
    ALTER DATABASE quanlythuvien SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP  DATABASE quanlythuvien;
END
GO
CREATE DATABASE quanlythuvien;
GO
USE quanlythuvien;
GO
-- 1. DANH MỤC
CREATE TABLE theloai (
    matheloai  INT IDENTITY(1,1) PRIMARY KEY,
    tentheloai NVARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tacgia (
    matacgia   INT IDENTITY(1,1) PRIMARY KEY,
    tentacgia  NVARCHAR(150) NOT NULL,
    quoctich   NVARCHAR(80)
);

CREATE TABLE nhaxuatban (
    manxb  INT IDENTITY(1,1) PRIMARY KEY,
    tennxb NVARCHAR(150) NOT NULL UNIQUE,
    diachi NVARCHAR(255)
);
GO

-- 2. SÁCH
CREATE TABLE sach (
    masach      INT IDENTITY(1,1) PRIMARY KEY,
    isbn        VARCHAR(20)   NULL,
    tensach     NVARCHAR(255) NOT NULL,
    matheloai   INT NOT NULL,
    manxb       INT NULL,
    namxuatban  INT NULL,
    soluongtong INT NOT NULL    DEFAULT 0 CHECK (soluongtong >= 0),
    soluongcon  INT NOT NULL    DEFAULT 0 CHECK (soluongcon >= 0),
    CONSTRAINT  fk_sach_theloai FOREIGN KEY (matheloai)  REFERENCES theloai(matheloai),
    CONSTRAINT  fk_sach_nxb     FOREIGN KEY (manxb)      REFERENCES nhaxuatban(manxb),
    CONSTRAINT  ck_sach_soluong CHECK (soluongcon <= soluongtong)
);
CREATE UNIQUE INDEX ux_sach_isbn ON sach(isbn) WHERE isbn IS NOT NULL;

-- Quan hệ nhiều-nhiều: 1 sách có nhiều tác giả
CREATE TABLE sach_tacgia (
    masach   INT NOT NULL,
    matacgia INT NOT NULL,
    PRIMARY KEY (masach, matacgia),
    FOREIGN KEY (masach)    REFERENCES sach(masach)     ON DELETE CASCADE,
    FOREIGN KEY (matacgia)  REFERENCES tacgia(matacgia) ON DELETE CASCADE
);
GO

-- 3. NGƯỜI DÙNG
CREATE TABLE docgia (
    madocgia     INT IDENTITY(1,1) PRIMARY KEY,
    hoten        NVARCHAR(100) NOT NULL,
    ngaysinh     DATE          NULL,
    gioitinh     NVARCHAR(10)  NULL         CHECK (gioitinh IN (N'Nam', N'Nu', N'Khac')),
    diachi       NVARCHAR(255) NULL,
    sodienthoai  VARCHAR(15)   NULL,
    email        VARCHAR(100)  NULL,
    ngaydangky   DATE          NOT NULL     DEFAULT CAST(GETDATE() AS DATE),
    ngayhethan   DATE          NOT NULL,
    trangthai    NVARCHAR(20)  NOT NULL     DEFAULT N'HoatDong'
                 CHECK (trangthai IN (N'HoatDong', N'Khoa'))
);
CREATE UNIQUE INDEX ux_docgia_sdt   ON docgia(sodienthoai) WHERE sodienthoai IS NOT NULL;
CREATE UNIQUE INDEX ux_docgia_email ON docgia(email)       WHERE email IS NOT NULL;

CREATE TABLE nhanvien (
    manhanvien   INT IDENTITY(1,1) PRIMARY KEY,
    hoten        NVARCHAR(100) NOT NULL,
    chucvu       NVARCHAR(50)  NULL,
    sodienthoai  VARCHAR(15)   NULL,
    email        VARCHAR(100)  NULL
);
CREATE UNIQUE INDEX ux_nhanvien_email ON nhanvien(email) WHERE email IS NOT NULL;
GO

-- 4. MƯỢN / TRẢ
CREATE TABLE phieumuon (
    maphieu     INT IDENTITY(1,1) PRIMARY KEY,
    madocgia    INT  NOT NULL,
    manhanvien  INT  NOT NULL,
    ngaymuon    DATE NOT NULL     DEFAULT CAST(GETDATE() AS DATE),
    hantra      DATE NOT NULL,
    FOREIGN KEY (madocgia)        REFERENCES docgia(madocgia),
    FOREIGN KEY (manhanvien)      REFERENCES nhanvien(manhanvien),
    CHECK (hantra >= ngaymuon)
);

CREATE TABLE chitietmuon (
    maphieu  INT           NOT NULL,
    masach   INT           NOT NULL,
    soluong  INT           NOT NULL   DEFAULT 1 CHECK (soluong > 0),
    ngaytra  DATE          NULL,
    tienphat DECIMAL(10,0) NOT NULL   DEFAULT 0,
    PRIMARY KEY (maphieu, masach),
    FOREIGN KEY (maphieu)             REFERENCES phieumuon(maphieu) ON DELETE CASCADE,
    FOREIGN KEY (masach)              REFERENCES sach(masach)
);
GO

--5. INDEX
CREATE INDEX idx_sachten        ON sach(tensach);
CREATE INDEX idx_phieudocgia    ON phieumuon(madocgia);
CREATE INDEX idx_phieuhantra    ON phieumuon(hantra);
GO

--6. TRIGGER
-- Khi mượn
CREATE TRIGGER trg_muonsach
ON chitietmuon
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM sach s
        JOIN (SELECT masach, SUM(soluong) AS sl FROM inserted GROUP BY masach) i
          ON i.masach = s.masach
        WHERE s.soluongcon < i.sl
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50001, N'Không đủ sách trong kho để mượn', 1;
    END
    UPDATE s
    SET soluongcon = s.soluongcon - i.sl
    FROM sach s
    JOIN (SELECT masach, SUM(soluong) AS sl FROM inserted GROUP BY masach) i
      ON i.masach = s.masach;
END
GO
-- Khi trả
CREATE TRIGGER trg_trasach
ON chitietmuon
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(ngaytra) RETURN;
    -- Phạt
    UPDATE ct
    SET tienphat = CASE
            WHEN DATEDIFF(DAY, pm.hantra, i.ngaytra) > 0
            THEN DATEDIFF(DAY, pm.hantra, i.ngaytra) * 5000 * i.soluong
            ELSE 0 END
    FROM chitietmuon ct
    JOIN inserted i   ON i.maphieu = ct.maphieu  AND i.masach = ct.masach
    JOIN deleted d    ON d.maphieu = i.maphieu   AND d.masach = i.masach
    JOIN phieumuon pm ON pm.maphieu = i.maphieu
    WHERE d.ngaytra IS NULL AND i.ngaytra IS NOT NULL;
    -- Kho
    UPDATE s
    SET soluongcon = s.soluongcon + x.sl
    FROM sach s
    JOIN (
        SELECT i.masach, SUM(i.soluong) AS sl
        FROM inserted i
        JOIN deleted d  ON d.maphieu = i.maphieu AND d.masach = i.masach
        WHERE d.ngaytra IS NULL AND i.ngaytra IS NOT NULL
        GROUP BY i.masach
    ) x ON x.masach = s.masach;
END
GO

--7. TẠO PHIẾU MƯỢN
CREATE PROCEDURE sp_taophieumuon
    @madocgia     INT,
    @manhanvien   INT,
    @masach       INT,
    @songay       INT = 14
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @songay IS NULL OR @songay < 1
        THROW 50004, N'Số ngày mượn phải lớn hơn 0', 1;

    DECLARE @trangthai NVARCHAR(20), @hethan DATE, @maphieu INT;

    SELECT  @trangthai = trangthai, @hethan = ngayhethan
    FROM docgia WHERE madocgia = @madocgia;

    IF @trangthai IS NULL
        THROW 50002, N'Không tìm thấy độc giả', 1;

    IF @trangthai <> N'HoatDong' OR @hethan < CAST(GETDATE() AS DATE)
        THROW 50003, N'Thẻ độc giả bị khóa hoặc đã hết hạn', 1;

    BEGIN TRANSACTION;

    INSERT INTO phieumuon (madocgia, manhanvien, hantra)
    VALUES (@madocgia, @manhanvien, DATEADD(DAY, @songay, CAST(GETDATE() AS DATE)));

    SET @maphieu = SCOPE_IDENTITY();

    INSERT INTO chitietmuon (maphieu, masach) VALUES (@maphieu, @masach);

    COMMIT TRANSACTION;
END
GO

-- 8. CHI TIET
CREATE VIEW v_sachchitiet AS
SELECT s.masach, s.tensach, tl.tentheloai, nxb.tennxb, s.namxuatban,
       (SELECT STRING_AGG(tg.tentacgia, N', ')
        FROM sach_tacgia st
        JOIN tacgia tg ON tg.matacgia = st.matacgia
        WHERE st.masach = s.masach) AS tacgia,
        s.soluongtong, s.soluongcon
FROM sach s
JOIN theloai tl          ON tl.matheloai = s.matheloai
LEFT JOIN nhaxuatban nxb ON nxb.manxb = s.manxb;
GO

CREATE VIEW v_sachquahan AS
SELECT pm.maphieu, dg.hoten, dg.sodienthoai, s.tensach, pm.hantra,
       DATEDIFF(DAY, pm.hantra, CAST(GETDATE() AS DATE)) AS songaytre
FROM chitietmuon ct
JOIN phieumuon pm  ON pm.maphieu  = ct.maphieu
JOIN docgia dg     ON dg.madocgia = pm.madocgia
JOIN sach s        ON s.masach    = ct.masach
WHERE ct.ngaytra IS NULL AND pm.hantra < CAST(GETDATE() AS DATE);
GO

-- 9. DỮ LIỆU MẪU
INSERT INTO theloai (tentheloai) VALUES
(N'Công nghệ thông tin'), (N'Văn học'), (N'Kinh tế'), (N'Khoa học'),
(N'Lịch sử'), (N'Tâm lý học'), (N'Kỹ năng sống'), (N'Thiếu nhi'),
(N'Giáo dục'), (N'Ngoại ngữ'), (N'Địa lý'), (N'Truyện tranh');

INSERT INTO tacgia (tentacgia, quoctich) VALUES
(N'Nguyễn Nhật Ánh', N'Việt Nam'),
(N'Robert C. Martin', N'Mỹ'),
(N'Thomas H. Cormen', N'Mỹ'),
(N'Charles E. Leiserson', N'Mỹ');

INSERT INTO nhaxuatban (tennxb, diachi) VALUES
(N'NXB Trẻ', N'TP. Hồ Chí Minh'),
(N'NXB Thanh Niên', N'Hà Nội'),
(N'MIT Press', N'Cambridge, MA'),
(N'NXB Kim Đồng', N'Hà Nội'),
(N'NXB Giáo dục Việt Nam', N'Hà Nội'),
(N'NXB Chính trị Quốc gia Sự thật', N'Hà Nội'),
(N'NXB Lao Động', N'Hà Nội'),
(N'NXB Tổng hợp TP.HCM', N'TP. Hồ Chí Minh'),
(N'NXB Thế Giới', N'Hà Nội'),
(N'NXB Đại học Quốc gia Hà Nội', N'Hà Nội'),
(N'Pearson', N'London'),
(N'O''Reilly Media', N'Sebastopol, California');

INSERT INTO sach (isbn, tensach, matheloai, manxb, namxuatban, soluongtong, soluongcon) VALUES
('9786041000011', N'Cho tôi xin một vé đi tuổi thơ', 2, 1, 2008, 5, 5),
('9780132350884', N'Clean Code', 1, 3, 2008, 3, 3),
('9780262033848', N'Introduction to Algorithms', 1, 3, 2009, 4, 4);

INSERT INTO sach_tacgia (masach, matacgia) VALUES (1,1), (2,2), (3,3), (3,4);

INSERT INTO nhanvien (hoten, chucvu, sodienthoai, email) VALUES
(N'Trần Thị Thủ Thư', N'Thủ thư', '0900000001', 'thuthu@thuvien.vn');

INSERT INTO docgia (hoten, ngaysinh, gioitinh, diachi, sodienthoai, email, ngayhethan) VALUES
(N'Nguyễn Văn A', '2005-03-10', N'Nam', N'Hà Nội', '0911111111', 'a@example.com', DATEADD(YEAR, 1, CAST(GETDATE() AS DATE))),
(N'Lê Thị B',     '2004-07-21', N'Nu',  N'Hà Nội', '0922222222', 'b@example.com', DATEADD(YEAR, 1, CAST(GETDATE() AS DATE)));
GO

-- TEST
EXEC sp_taophieumuon @madocgia = 1, @manhanvien = 1, @masach = 2, @songay = 14;
GO

-- 10. TRUY VẤN THƯỜNG DÙNG
-- Tìm sách theo tên
SELECT * FROM v_sachchitiet WHERE tensach LIKE N'%Clean%';

-- Sách đang được mượn
SELECT dg.hoten, s.tensach, pm.ngaymuon, pm.hantra
FROM chitietmuon ct
JOIN phieumuon pm ON pm.maphieu  = ct.maphieu
JOIN docgia dg    ON dg.madocgia = pm.madocgia
JOIN sach s       ON s.masach    = ct.masach
WHERE ct.ngaytra IS NULL;

-- Top 5 sách được mượn nhiều nhất
SELECT TOP 5 s.tensach, COUNT(*) AS solanmuon
FROM chitietmuon ct
JOIN sach s ON s.masach = ct.masach
GROUP BY s.masach, s.tensach
ORDER BY solanmuon DESC;

-- Trả sách (trigger tự cộng kho và tính tiền phạt)
-- UPDATE chitietmuon SET ngaytra = CAST(GETDATE() AS DATE) WHERE maphieu = 1 AND masach = 2;

-- Tổng tiền phạt theo độc giả
SELECT dg.hoten, SUM(ct.tienphat) AS tongphat
FROM chitietmuon ct
JOIN phieumuon pm ON pm.maphieu = ct.maphieu
JOIN docgia dg    ON dg.madocgia = pm.madocgia
GROUP BY dg.madocgia, dg.hoten
HAVING SUM(ct.tienphat) > 0;
GO
