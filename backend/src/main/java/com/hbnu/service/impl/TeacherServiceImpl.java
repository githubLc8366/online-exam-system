package com.hbnu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hbnu.common.Result;
import com.hbnu.mapper.DepartmentMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.mapper.UserRoleMapper;
import com.hbnu.pojo.Department;
import com.hbnu.pojo.User;
import com.hbnu.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师管理 Service 实现类
 * 桥接前端字段名（teacherNo/name/department/title/hireDate）与后端实体（userNo/nickname/deptId/title/hireDate）
 */
@Service
public class TeacherServiceImpl implements TeacherService {

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private DepartmentMapper departmentMapper;

  @Autowired
  private UserRoleMapper userRoleMapper;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Override
  public Result<Map<String, Object>> getList(Map<String, Object> params) {
    int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
    int pageSize = Integer.parseInt(params.getOrDefault("pageSize", "10").toString());

    QueryWrapper<User> wrapper = new QueryWrapper<>();
    wrapper.eq("is_deleted", 0);
    // 只查询教师角色 (role_id = 2) 的用户
    wrapper.apply("id IN (SELECT user_id FROM sys_user_role WHERE role_id = 2)");

    // 搜索条件：映射前端字段名到后端
    if (params.containsKey("teacherNo") && !params.get("teacherNo").toString().isEmpty()) {
      wrapper.like("user_no", params.get("teacherNo"));
    }
    if (params.containsKey("name") && !params.get("name").toString().isEmpty()) {
      wrapper.like("nickname", params.get("name"));
    }
    if (params.containsKey("department") && !params.get("department").toString().isEmpty()) {
      // 按院系名称搜索
      wrapper.apply("dept_id IN (SELECT id FROM sys_dept WHERE dept_name LIKE {0})",
          "%" + params.get("department") + "%");
    }
    if (params.containsKey("title") && !params.get("title").toString().isEmpty()) {
      wrapper.like("title", params.get("title"));
    }
    if (params.containsKey("status") && params.get("status") != null && !params.get("status").toString().isEmpty()) {
      wrapper.eq("status", Integer.parseInt(params.get("status").toString()));
    }
    wrapper.orderByDesc("create_time");

    IPage<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

    // 查询所有院系的名称映射
    Map<Long, String> deptNameMap = new HashMap<>();
    List<Department> allDepts = departmentMapper.selectList(new QueryWrapper<>());
    for (Department d : allDepts) {
      deptNameMap.put(d.getId(), d.getDeptName());
    }

    // 转换为前端友好的字段名
    List<Map<String, Object>> list = userPage.getRecords().stream().map(u -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", u.getId());
      item.put("teacherNo", u.getUserNo()); // userNo → teacherNo
      item.put("name", u.getNickname()); // nickname → name
      item.put("department", deptNameMap.getOrDefault(u.getDeptId(), "")); // deptId → department
      item.put("title", u.getTitle());
      item.put("hireDate", u.getHireDate());
      item.put("status", u.getStatus());
      return item;
    }).collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("list", list);
    result.put("total", userPage.getTotal());

    return Result.success(result);
  }

  @Override
  @Transactional
  public Result<?> add(Map<String, Object> data) {
    // 工号唯一性校验
    String userNo = (String) data.get("teacherNo");
    QueryWrapper<User> checkWrapper = new QueryWrapper<>();
    checkWrapper.eq("user_no", userNo);
    if (userMapper.selectCount(checkWrapper) > 0) {
      return Result.error("工号已存在");
    }

    User user = new User();
    user.setUserNo(userNo);
    // 默认密码：工号后6位
    if (userNo != null && userNo.length() >= 6) {
      user.setPassword(passwordEncoder.encode(userNo.substring(userNo.length() - 6)));
    } else {
      user.setPassword(passwordEncoder.encode("123456"));
    }
    user.setNickname((String) data.get("name"));
    user.setTitle((String) data.get("title"));
    user.setHireDate((String) data.get("hireDate"));
    user.setStatus(data.get("status") != null ? Integer.parseInt(data.get("status").toString()) : 1);
    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(LocalDateTime.now());

    // 根据院系名称查找 deptId
    if (data.containsKey("department") && data.get("department") != null
        && !data.get("department").toString().isEmpty()) {
      String deptName = data.get("department").toString();
      QueryWrapper<Department> deptWrapper = new QueryWrapper<>();
      deptWrapper.eq("dept_name", deptName);
      Department dept = departmentMapper.selectOne(deptWrapper);
      if (dept != null) {
        user.setDeptId(dept.getId());
      }
    }

    userMapper.insert(user);

    // 插入教师角色关联 (role_id = 2)
    userRoleMapper.insertUserRole(user.getId(), 2L);

    return Result.success("新增成功");
  }

  @Override
  @Transactional
  public Result<?> update(Map<String, Object> data) {
    Long id = Long.valueOf(data.get("id").toString());
    User user = userMapper.selectById(id);
    if (user == null) {
      return Result.error("教师不存在");
    }

    // 工号唯一性校验（排除自己）
    String userNo = (String) data.get("teacherNo");
    if (userNo != null && !userNo.equals(user.getUserNo())) {
      QueryWrapper<User> checkWrapper = new QueryWrapper<>();
      checkWrapper.eq("user_no", userNo).ne("id", id);
      if (userMapper.selectCount(checkWrapper) > 0) {
        return Result.error("工号已存在");
      }
      user.setUserNo(userNo);
    }

    if (data.containsKey("name"))
      user.setNickname((String) data.get("name"));
    if (data.containsKey("title"))
      user.setTitle((String) data.get("title"));
    if (data.containsKey("hireDate"))
      user.setHireDate((String) data.get("hireDate"));
    if (data.containsKey("status"))
      user.setStatus(Integer.parseInt(data.get("status").toString()));
    // 根据院系名称查找 deptId
    if (data.containsKey("department") && data.get("department") != null
        && !data.get("department").toString().isEmpty()) {
      String deptName = data.get("department").toString();
      QueryWrapper<Department> deptWrapper = new QueryWrapper<>();
      deptWrapper.eq("dept_name", deptName);
      Department dept = departmentMapper.selectOne(deptWrapper);
      if (dept != null) {
        user.setDeptId(dept.getId());
      }
    }
    user.setUpdateTime(LocalDateTime.now());

    userMapper.updateById(user);

    return Result.success("编辑成功");
  }

  @Override
  @Transactional
  public Result<?> delete(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
      return Result.error("教师不存在");
    }
    // @TableLogic 字段在 updateById 中会被剔除，必须走 deleteById 才能真正逻辑删除
    int affected = userMapper.deleteById(id);
    if (affected == 0) {
      return Result.error("删除失败，记录不存在或已被删除");
    }
    return Result.success("删除成功");
  }

  @Override
  @Transactional
  public Result<?> updateStatus(Long id, Integer status) {
    User user = userMapper.selectById(id);
    if (user == null) {
      return Result.error("教师不存在");
    }
    user.setStatus(status);
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
    return Result.success("状态更新成功");
  }
}