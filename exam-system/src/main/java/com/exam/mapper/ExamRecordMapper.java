package com.exam.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.ExamRecord;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
    @Update("UPDATE exam_record SET switch_count = IFNULL(switch_count, 0) + 1 WHERE id = #{id}")
    int incrementSwitchCount(Long id);

    /**
     * CAS 提交：仅在 status=1（答题中）时更新为指定状态。
     * 避免学生手动交卷与 ExamEndTask 自动交卷并发时互相覆盖。
     * 返回受影响行数：0 表示记录已被其它路径处理。
     */
    @Update("UPDATE exam_record SET status = #{newStatus}, " +
            "submit_time = #{submitTime}, " +
            "objective_score = #{objectiveScore}, " +
            "subjective_score = #{subjectiveScore}, " +
            "total_score = #{totalScore} " +
            "WHERE id = #{id} AND status = 1")
    int casSubmitFromInProgress(@Param("id") Long id,
                                @Param("newStatus") Integer newStatus,
                                @Param("submitTime") LocalDateTime submitTime,
                                @Param("objectiveScore") BigDecimal objectiveScore,
                                @Param("subjectiveScore") BigDecimal subjectiveScore,
                                @Param("totalScore") BigDecimal totalScore);
}
