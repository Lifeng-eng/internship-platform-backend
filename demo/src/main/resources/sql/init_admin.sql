-- 初始化管理员账号
-- 密码: admin123 (BCrypt加密)
INSERT INTO `user` (`username`, `password`, `phone`, `role`, `status`) VALUES
('admin', '$2b$10$gO6gjwfsXcEU/huF9U.7yuWstmYijRkxMM7dz5YEeS/lDcbNqOZWS', '13800000000', 'admin', 'normal');

INSERT INTO `admin_info` (`user_id`, `admin_id`, `must_change_password`) VALUES
(1, 'ADMIN001', TRUE);
