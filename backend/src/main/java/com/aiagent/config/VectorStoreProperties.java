package com.aiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PGVector 向量存储配置
 */
@ConfigurationProperties(prefix = "vectorstore")
public class VectorStoreProperties {

    /**
     * 是否启用向量存储
     */
    private boolean enabled = true;

    /**
     * PostgreSQL JDBC 连接串
     */
    private String url;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 向量表名
     */
    private String table = "kb_chunks";

    /**
     * 向量维度（gemini-embedding-001 为 768）
     */
    private int dimension = 768;

    /**
     * 检索默认 topK
     */
    private int topK = 5;

    /**
     * 最小相似度阈值
     */
    private double minScore = 0.0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }
}
