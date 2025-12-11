package com.aiagent.knowledgebase.util;

import com.aiagent.config.VectorStoreProperties;
import com.pgvector.PGvector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量存储服务
 * 基于 PostgreSQL + pgvector 实现向量的写入和检索
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorStoreProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final GeminiEmbeddingClient embeddingClient;

    public VectorStoreService(VectorStoreProperties properties,
                              @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
                              GeminiEmbeddingClient embeddingClient) {
        this.properties = properties;
        this.jdbcTemplate = vectorJdbcTemplate;
        this.embeddingClient = embeddingClient;
    }

    /**
     * 检查向量存储是否可用
     */
    public boolean isEnabled() {
        return properties.isEnabled() 
                && properties.getUrl() != null 
                && !properties.getUrl().isBlank();
    }

    /**
     * 将文档分块写入向量库
     *
     * @param knowledgeBaseId 知识库ID
     * @param documentId      文档ID
     * @param chunks          文本分块列表
     * @return 写入的分块数量
     */
    public int saveChunks(Long knowledgeBaseId, Long documentId, List<String> chunks) {
        if (!isEnabled() || CollectionUtils.isEmpty(chunks)) {
            return 0;
        }

        log.info("开始向量化：kbId={}, docId={}, chunks={}", knowledgeBaseId, documentId, chunks.size());

        // 生成向量
        List<float[]> vectors = embeddingClient.embedAll(chunks);
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("向量数量与分块数量不一致");
        }

        String table = properties.getTable();
        String sql = "INSERT INTO " + table + 
                " (knowledge_base_id, document_id, chunk_index, content, embedding) " +
                "VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, knowledgeBaseId);
                ps.setLong(2, documentId);
                ps.setInt(3, i);
                ps.setString(4, chunks.get(i));
                ps.setObject(5, new PGvector(vectors.get(i)));
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });

        log.info("向量化完成：写入 {} 条记录", chunks.size());
        return chunks.size();
    }

    /**
     * 删除文档的所有向量
     */
    public void deleteByDocumentId(Long documentId) {
        if (!isEnabled()) {
            return;
        }
        String sql = "DELETE FROM " + properties.getTable() + " WHERE document_id = ?";
        int deleted = jdbcTemplate.update(sql, documentId);
        log.debug("删除文档向量：docId={}, deleted={}", documentId, deleted);
    }

    /**
     * 删除知识库的所有向量
     */
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        if (!isEnabled()) {
            return;
        }
        String sql = "DELETE FROM " + properties.getTable() + " WHERE knowledge_base_id = ?";
        int deleted = jdbcTemplate.update(sql, knowledgeBaseId);
        log.debug("删除知识库向量：kbId={}, deleted={}", knowledgeBaseId, deleted);
    }

    /**
     * 按知识库ID列表检索相关分块
     *
     * @param knowledgeBaseIds 知识库ID列表
     * @param query            查询文本
     * @param topK             返回数量（可选）
     * @return 检索结果列表
     */
    public List<ChunkSearchResult> search(List<Long> knowledgeBaseIds, String query, Integer topK) {
        if (!isEnabled()) {
            log.warn("向量存储未启用");
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(knowledgeBaseIds)) {
            log.warn("知识库ID列表为空");
            return Collections.emptyList();
        }
        if (query == null || query.isBlank()) {
            log.warn("查询文本为空");
            return Collections.emptyList();
        }

        log.info("开始RAG检索: knowledgeBaseIds={}, query={}", knowledgeBaseIds, query);

        // 生成查询向量
        float[] queryVector = embeddingClient.embed(query);
        if (queryVector == null || queryVector.length == 0) {
            log.error("查询向量生成失败: query={}", query);
            return Collections.emptyList();
        }
        log.info("查询向量生成成功: vectorLength={}", queryVector.length);
        
        int k = (topK != null && topK > 0) ? topK : properties.getTopK();
        String table = properties.getTable();
        log.debug("使用参数: table={}, topK={}, minScore={}", table, k, properties.getMinScore());

        // 构建 IN 子句
        String placeholders = knowledgeBaseIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT knowledge_base_id, document_id, chunk_index, content, " +
                "(1 - (embedding <=> ?)) AS score " +
                "FROM " + table + " " +
                "WHERE knowledge_base_id IN (" + placeholders + ") " +
                "ORDER BY embedding <=> ? ASC " +
                "LIMIT ?";

        log.debug("执行SQL查询: {}", sql);
        log.debug("查询参数: knowledgeBaseIds={}, topK={}", knowledgeBaseIds, k);

        List<ChunkSearchResult> results = jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            int idx = 1;
            ps.setObject(idx++, new PGvector(queryVector));
            for (Long id : knowledgeBaseIds) {
                ps.setLong(idx++, id);
            }
            ps.setObject(idx++, new PGvector(queryVector));
            ps.setInt(idx, k);
            return ps;
        }, (rs, rowNum) -> new ChunkSearchResult(
                rs.getLong("knowledge_base_id"),
                rs.getLong("document_id"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getDouble("score")
        ));

        log.info("SQL查询返回 {} 条原始结果", results != null ? results.size() : 0);
        
        if (results == null || results.isEmpty()) {
            log.warn("数据库查询未返回任何结果，可能原因：1) 数据库中没有对应知识库的数据 2) 向量表为空 3) 知识库ID不匹配");
            return Collections.emptyList();
        }

        // 记录原始结果的分数分布
        if (!results.isEmpty()) {
            double maxScore = results.stream().mapToDouble(ChunkSearchResult::score).max().orElse(0.0);
            double minScoreInResults = results.stream().mapToDouble(ChunkSearchResult::score).min().orElse(0.0);
            double avgScore = results.stream().mapToDouble(ChunkSearchResult::score).average().orElse(0.0);
            log.info("查询结果分数分布: 最高={}, 最低={}, 平均={}", 
                    String.format("%.4f", maxScore), 
                    String.format("%.4f", minScoreInResults), 
                    String.format("%.4f", avgScore));
        }

        // 过滤低分结果
        double minScore = properties.getMinScore();
        int originalSize = results.size();
        if (minScore > 0) {
            results = results.stream()
                    .filter(r -> r.score() >= minScore)
                    .collect(Collectors.toList());
            log.info("应用最小分数过滤 (minScore={}): 过滤前={}, 过滤后={}", minScore, originalSize, results.size());
        }

        if (results.isEmpty()) {
            log.info("最终返回 0 条检索结果");
            return Collections.emptyList();
        }
        // 获取分数最高的那个结果（如果有多个最高则取第一个）
        ChunkSearchResult topResult = results.stream()
                .max((a, b) -> Double.compare(a.score(), b.score()))
                .orElse(null);
        if (topResult != null) {
            log.info("最终返回分数最高的1条检索结果: score={}, docId={}, kbId={}", 
                String.format("%.4f", topResult.score()), 
                topResult.documentId(), 
                topResult.knowledgeBaseId());
            return Collections.singletonList(topResult);
        } else {
            log.info("最终返回 0 条检索结果");
            return Collections.emptyList();
        }
    }

    /**
     * 诊断方法：检查数据库中是否有指定知识库的向量数据
     * 
     * @param knowledgeBaseIds 知识库ID列表
     * @return 诊断信息
     */
    public String diagnose(List<Long> knowledgeBaseIds) {
        if (!isEnabled()) {
            return "向量存储未启用";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("=== 向量存储诊断信息 ===\n");
        result.append("表名: ").append(properties.getTable()).append("\n");
        result.append("数据库URL: ").append(properties.getUrl()).append("\n");
        result.append("最小分数阈值: ").append(properties.getMinScore()).append("\n");
        result.append("TopK: ").append(properties.getTopK()).append("\n\n");
        
        try {
            // 检查表是否存在且有数据
            String countSql = "SELECT COUNT(*) FROM " + properties.getTable();
            Integer totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            result.append("向量表总记录数: ").append(totalCount).append("\n");
            
            if (totalCount == 0) {
                result.append("⚠️ 警告：向量表为空，请先上传并处理知识库文档\n");
                return result.toString();
            }
            
            // 检查每个知识库的数据
            if (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty()) {
                result.append("\n按知识库统计:\n");
                for (Long kbId : knowledgeBaseIds) {
                    String kbCountSql = "SELECT COUNT(*) FROM " + properties.getTable() + 
                                       " WHERE knowledge_base_id = ?";
                    Integer kbCount = jdbcTemplate.queryForObject(kbCountSql, Integer.class, kbId);
                    result.append("  知识库ID ").append(kbId).append(": ").append(kbCount).append(" 条向量\n");
                    
                    if (kbCount == 0) {
                        result.append("    ⚠️ 该知识库没有向量数据\n");
                    }
                }
            }
            
            // 获取一条示例数据
            String sampleSql = "SELECT knowledge_base_id, document_id, chunk_index, " +
                              "LENGTH(content) as content_length FROM " + properties.getTable() + 
                              " LIMIT 1";
            jdbcTemplate.query(sampleSql, rs -> {
                result.append("\n示例数据:\n");
                result.append("  知识库ID: ").append(rs.getLong("knowledge_base_id")).append("\n");
                result.append("  文档ID: ").append(rs.getLong("document_id")).append("\n");
                result.append("  分块索引: ").append(rs.getInt("chunk_index")).append("\n");
                result.append("  内容长度: ").append(rs.getInt("content_length")).append(" 字符\n");
            });
            
        } catch (Exception e) {
            result.append("❌ 诊断过程出错: ").append(e.getMessage()).append("\n");
            log.error("向量存储诊断失败", e);
        }
        
        return result.toString();
    }

    /**
     * 检索结果记录
     */
    public record ChunkSearchResult(
            Long knowledgeBaseId,
            Long documentId,
            Integer chunkIndex,
            String content,
            Double score
    ) {
        public ChunkSearchResult {
            score = (score == null) ? 0.0 : score;
        }
    }
}
