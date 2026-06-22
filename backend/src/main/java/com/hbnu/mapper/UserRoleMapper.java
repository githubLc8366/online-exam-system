package com.hbnu.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联 Mapper 接口
 * 操作 sys_user_role 表
 */
@Mapper
public interface UserRoleMapper {

  /**
   * 插入用户角色关联
   */
  @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
  int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

  /**
   * 幂等插入用户角色关联：已存在（命中 uk_user_role）则不报错。
   * 用于复活学生时补建角色，因软删除不会清理 sys_user_role，原关联通常仍在。
   */
  @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId}) " +
          "ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)")
  int insertUserRoleIfAbsent(@Param("userId") Long userId, @Param("roleId") Long roleId);
}