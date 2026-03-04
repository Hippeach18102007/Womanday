-- Chạy script này 1 lần duy nhất trong SQL Server Management Studio (SSMS)
-- hoặc dùng sqlcmd trước khi start app

USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'women_day_db')
BEGIN
    CREATE DATABASE women_day_db;
    PRINT 'Database women_day_db da duoc tao thanh cong!';
END
ELSE
BEGIN
    PRINT 'Database women_day_db da ton tai.';
END
GO

-- Spring JPA se tu dong tao bang greeting_config khi app khoi dong
