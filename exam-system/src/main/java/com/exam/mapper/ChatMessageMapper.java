package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 按会话 ID 列表分组统计未读消息数（SQL GROUP BY，避免将所有未读消息加载到 JVM）。
     * <p>
     * 查询条件：{@code receiver_id = userId AND is_read = 0 AND conversation_id IN (convIds)}
     * <p>
     * 返回：List of {@code {convId: Long, cnt: Long}}，只包含有未读的会话。
     * 利用现有索引 {@code idx_receiver_read(receiver_id, is_read)} + {@code idx_conv_time}
     * 可以完全走索引，无需回表。
     *
     * @param convIds 会话 ID 集合（必须非空，否则不应调用此方法）
     * @param userId  接收方用户 ID
     * @return 未读统计结果列表
     */
    @Select({
            "<script>",
            "SELECT conversation_id AS convId, COUNT(*) AS cnt",
            "FROM chat_message",
            // L3：不计入已软删的消息
            "WHERE receiver_id = #{userId} AND is_read = 0 AND deleted_at IS NULL",
            "AND conversation_id IN",
            "<foreach collection='convIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "GROUP BY conversation_id",
            "</script>"
    })
    List<Map<String, Object>> countUnreadByConversations(@Param("convIds") Collection<Long> convIds,
                                                         @Param("userId") Long userId);
}
