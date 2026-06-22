package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper 接口
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

  /**
   * 根据用户ID查询角色编码（通过 sys_user_role 联表）
   */
  @Select("SELECT r.role_code FROM sys_role r " +
      "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
      "WHERE ur.user_id = #{userId} LIMIT 1")
  String findRoleCodeByUserId(Long userId);

  /**
   * 根据用户ID查询所有角色编码
   */
  @Select("SELECT r.role_code FROM sys_role r " +
          "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
          "WHERE ur.user_id = #{userId}")
  List<String> findRoleCodesByUserId(Long userId);
}
