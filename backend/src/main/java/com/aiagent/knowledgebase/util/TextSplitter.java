package com.aiagent.knowledgebase.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文本分块器
 * 将长文本按固定大小分块，支持重叠以保持上下文连续性
 */
@Component
public class TextSplitter {

    /**
     * 默认分块大小（字符数，约 600 tokens）
     */
    private static final int DEFAULT_CHUNK_SIZE = 2000;

    /**
     * 默认重叠大小（字符数）
     */
    private static final int DEFAULT_OVERLAP = 200;

    /**
     * 使用默认参数分块
     */
    public List<String> split(String text) {
        return split(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 自定义参数分块
     *
     * @param text      待分块文本
     * @param chunkSize 分块大小（字符数）
     * @param overlap   重叠大小（字符数）
     * @return 分块列表
     */
    public List<String> split(String text, int chunkSize, int overlap) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        chunkSize = Math.max(100, chunkSize);
        overlap = Math.max(0, Math.min(overlap, chunkSize / 2));

        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(length, start + chunkSize);

            // 尝试在句子边界处截断
            if (end < length) {
                int preferredBreak = findBreakPoint(text, start, end);
                if (preferredBreak > start) {
                    end = preferredBreak;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }

            if (end >= length) {
                break;
            }

            // 下一块从 overlap 位置开始
            start = Math.max(end - overlap, start + 1);
        }

        return chunks;
    }

    /**
     * 在指定范围内寻找合适的断点（句号、换行等）
     */
    private int findBreakPoint(String text, int start, int end) {
        // 在末尾 200 字符范围内寻找断点
        int searchStart = Math.max(start, end - 200);
        String window = text.substring(searchStart, end);

        // 优先级：换行 > 句号 > 其他标点
        int[] breaks = {
                window.lastIndexOf('\n'),
                window.lastIndexOf('。'),
                window.lastIndexOf('！'),
                window.lastIndexOf('？'),
                window.lastIndexOf('.'),
                window.lastIndexOf('!'),
                window.lastIndexOf('?'),
                window.lastIndexOf('；'),
                window.lastIndexOf(';')
        };

        int bestBreak = -1;
        for (int br : breaks) {
            if (br > bestBreak) {
                bestBreak = br;
                break; // 按优先级取第一个找到的
            }
        }

        if (bestBreak <= 0) {
            return end;
        }

        return searchStart + bestBreak + 1;
    }
}
