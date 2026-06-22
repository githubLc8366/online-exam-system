package com.hbnu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hbnu.common.Result;
import com.hbnu.pojo.WrongBook;

import java.util.List;

/**
 * 错题本 Service 接口
 */
public interface WrongBookService {

  /**
   * 分页查询错题本列表（包含题目详情）
   */
  Result<?> pageList(Integer page, Integer size, Long userId, String subject, Integer isMastered);

  /**
   * 添加错题
   *
   * @param wrongBook 错题信息
   * @return Result
   */
  Result<?> addWrongBook(WrongBook wrongBook);

  /**
   * 更新错题掌握状态
   *
   * @param id         错题ID
   * @param isMastered 是否已掌握
   * @return Result
   */
  Result<?> updateMasteredStatus(Long id, Integer isMastered);

  /**
   * 删除错题
   *
   * @param id 错题ID
   * @return Result
   */
  Result<?> deleteWrongBook(Long id);

  /**
   * 批量删除错题
   *
   * @param ids 错题ID列表
   * @return Result
   */
  Result<?> batchDeleteWrongBook(List<Long> ids);

}