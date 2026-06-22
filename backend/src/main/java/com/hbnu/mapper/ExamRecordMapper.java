package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 考试记录 Mapper 接口
 */
@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

  /**
   * 查询最近7天每日新增考试记录数
   */
  @Select("SELECT DATE(create_time) AS dateStr, COUNT(*) AS count " +
      "FROM exam_record " +
      "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
      "GROUP BY DATE(create_time) " +
      "ORDER BY DATE(create_time) ASC")
  List<Map<String, Object>> selectDailyRecordTrend();
}
