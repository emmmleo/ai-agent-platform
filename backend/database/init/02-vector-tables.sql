-- ============================================
-- PGVector 向量存储表结构创建脚本
-- 数据库: PostgreSQL + pgvector 扩展
-- ============================================

-- 启用 pgvector 扩展（需要超级用户权限）
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- 知识库分块向量表 (kb_chunks)
-- ============================================
CREATE TABLE IF NOT EXISTS kb_chunks (
    id BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(768) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引：按知识库ID查询
CREATE INDEX IF NOT EXISTS idx_kb_chunks_kb_id ON kb_chunks (knowledge_base_id);

-- 索引：按文档ID查询
CREATE INDEX IF NOT EXISTS idx_kb_chunks_doc_id ON kb_chunks (document_id);

-- 向量索引：使用 IVFFlat 加速相似度检索
-- lists 参数建议为 sqrt(行数)，这里预设 100
CREATE INDEX IF NOT EXISTS idx_kb_chunks_embedding ON kb_chunks 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ============================================
-- 说明
-- ============================================
-- 1. embedding 字段使用 768 维度，匹配 gemini-embedding-001 模型
-- 2. 使用余弦相似度 (vector_cosine_ops) 进行检索
-- 3. 检索时使用 <=> 操作符计算距离，距离越小越相似
-- 4. 相似度 = 1 - 距离
-- ============================================
