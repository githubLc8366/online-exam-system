package com.hbnu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hbnu.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按学号查询用户（含已逻辑删除的记录，绕过 @TableLogic），找不到返回 null。
     * 唯一键 uk_user_no 覆盖所有行，批量导入据此判定：在用则跳过、已软删除则复活、不存在则新增。
     */
    @Select("SELECT id, is_deleted FROM sys_user WHERE user_no = #{userNo} LIMIT 1")
    User findByUserNoIncludeDeleted(@Param("userNo") String userNo);

    /**
     * 复活此前被软删除的同学号记录：按 id 覆盖资料并置 is_deleted=0（绕过 @TableLogic）。
     * 沿用原 id，保留考试记录等历史外键引用。
     */
    @Update("UPDATE sys_user SET password=#{password}, nickname=#{nickname}, gender=#{gender}, " +
            "phone=#{phone}, email=#{email}, dept_id=#{deptId}, class_id=#{classId}, " +
            "enrollment_year=#{enrollmentYear}, status=#{status}, remark=#{remark}, " +
            "is_deleted=0, update_time=NOW() WHERE id=#{id}")
    int reviveStudent(User user);
}