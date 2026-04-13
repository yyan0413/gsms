-- RustFS 迁移验证脚本
-- 此脚本用于验证 RustFS 迁移是否成功

-- 1. 查看附件存储类型分布
SELECT
    storage_type AS '存储类型',
    COUNT(*) AS '数量',
    SUM(file_size) / 1024 / 1024 AS '总大小(MB)'
FROM gsms_attachment
WHERE is_deleted = 0
GROUP BY storage_type;

-- 2. 检查是否有本地存储的附件（迁移前）
SELECT
    id,
    file_name AS '文件名',
    file_path AS '文件路径',
    file_size AS '文件大小',
    create_time AS '创建时间'
FROM gsms_attachment
WHERE storage_type = 'local'
  AND is_deleted = 0
ORDER BY create_time DESC;

-- 3. 检查迁移后的 RustFS 附件
SELECT
    id,
    file_name AS '文件名',
    file_path AS '文件路径',
    file_size AS '文件大小',
    create_time AS '创建时间'
FROM gsms_attachment
WHERE storage_type = 'rustfs'
  AND is_deleted = 0
ORDER BY create_time DESC
LIMIT 20;

-- 4. 统计各类型附件的存储分布
SELECT
    file_type AS '文件类型',
    storage_type AS '存储类型',
    COUNT(*) AS '数量'
FROM gsms_attachment
WHERE is_deleted = 0
GROUP BY file_type, storage_type
ORDER BY file_type, storage_type;

-- 5. 检查是否有异常的存储类型
SELECT
    id,
    file_name,
    storage_type
FROM gsms_attachment
WHERE storage_type NOT IN ('local', 'rustfs')
  AND is_deleted = 0;
