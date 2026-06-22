package com.hbnu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hbnu.common.Result;
import com.hbnu.mapper.ClazzMapper;
import com.hbnu.mapper.DepartmentMapper;
import com.hbnu.mapper.UserMapper;
import com.hbnu.mapper.UserRoleMapper;
import com.hbnu.pojo.Clazz;
import com.hbnu.pojo.Department;
import com.hbnu.pojo.User;
import com.hbnu.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生管理 Service 实现类
 * 桥接前端字段名（studentNo/name/className/major）与后端实体（userNo/nickname/classId/deptId）
 */
@Service
public class StudentServiceImpl implements StudentService {

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private ClazzMapper clazzMapper;

  @Autowired
  private DepartmentMapper departmentMapper;

  @Autowired
  private UserRoleMapper userRoleMapper;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Override
  @Transactional
  public Result<?> batchResetPassword(List<Long> ids, String prefix, Boolean useSuffix) {
    int count = 0;
    for (Long id : ids) {
      User user = userMapper.selectById(id);
      if (user == null || user.getIsDeleted() == 1) continue;

      String newPassword = "";
      // 前缀
      if (prefix != null && !prefix.isEmpty()) {
        newPassword += prefix;
      }
      // 学号后6位
      if (useSuffix && user.getUserNo() != null && user.getUserNo().length() >= 6) {
        newPassword += user.getUserNo().substring(user.getUserNo().length() - 6);
      }

      if (newPassword.isEmpty()) {
        newPassword = "123456";
      }

      user.setPassword(passwordEncoder.encode(newPassword));
      user.setUpdateTime(LocalDateTime.now());
      userMapper.updateById(user);
      count++;
    }
    return Result.success("成功重置 " + count + " 名学生密码");
  }

  @Override
  @Transactional
  public Result<?> resetPassword(Long id, String newPassword) {
    User user = userMapper.selectById(id);
    if (user == null || user.getIsDeleted() == 1) {
      return Result.error("学生不存在");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);

    return Result.success("密码重置成功");
  }

  @Override
  public Result<Map<String, Object>> getList(Map<String, Object> params) {
    int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
    int pageSize = Integer.parseInt(params.getOrDefault("pageSize", "10").toString());

    QueryWrapper<User> wrapper = new QueryWrapper<>();
    wrapper.eq("is_deleted", 0);
    wrapper.apply("id IN (SELECT user_id FROM sys_user_role WHERE role_id = 3)");

    // 【新增】教师权限过滤：如果有 teacherId，只查本院系学生
    if (params.containsKey("teacherId") && params.get("teacherId") != null) {
      Long teacherId = Long.valueOf(params.get("teacherId").toString());
      User teacher = userMapper.selectById(teacherId);
      if (teacher != null && teacher.getDeptId() != null) {
        // 教师所属院系下的所有班级
        QueryWrapper<Clazz> clazzWrapper = new QueryWrapper<>();
        clazzWrapper.eq("dept_id", teacher.getDeptId())
                .eq("is_deleted", 0);
        List<Clazz> teacherClasses = clazzMapper.selectList(clazzWrapper);
        List<Long> classIds = teacherClasses.stream()
                .map(Clazz::getId)
                .collect(Collectors.toList());
        if (!classIds.isEmpty()) {
          wrapper.in("class_id", classIds);
        } else {
          wrapper.eq("class_id", -1L); // 没有班级，返回空
        }
      }
    }
    // 搜索条件：映射前端字段名到后端
    if (params.containsKey("studentNo") && !params.get("studentNo").toString().isEmpty()) {
      wrapper.like("user_no", params.get("studentNo"));
    }
    if (params.containsKey("name") && !params.get("name").toString().isEmpty()) {
      wrapper.like("nickname", params.get("name"));
    }
    if (params.containsKey("className") && !params.get("className").toString().isEmpty()) {
      // 按班级名称搜索
      wrapper.apply("class_id IN (SELECT id FROM sys_class WHERE class_name LIKE {0})",
          "%" + params.get("className") + "%");
    }
    if (params.containsKey("classId") && params.get("classId") != null && !params.get("classId").toString().isEmpty()) {
      wrapper.eq("class_id", Long.parseLong(params.get("classId").toString()));
    }
    if (params.containsKey("major") && !params.get("major").toString().isEmpty()) {
      // 按专业（院系名称）搜索
      wrapper.apply("dept_id IN (SELECT id FROM sys_dept WHERE dept_name LIKE {0})",
          "%" + params.get("major") + "%");
    }
    if (params.containsKey("status") && params.get("status") != null && !params.get("status").toString().isEmpty()) {
      wrapper.eq("status", Integer.parseInt(params.get("status").toString()));
    }
    wrapper.orderByDesc("create_time");

    IPage<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

    // 查询所有班级和院系的名称映射
    Map<Long, String> classNameMap = new HashMap<>();
    Map<Long, String> deptNameMap = new HashMap<>();
    List<Clazz> allClasses = clazzMapper.selectList(new QueryWrapper<>());
    List<Department> allDepts = departmentMapper.selectList(new QueryWrapper<>());
    for (Clazz c : allClasses) {
      classNameMap.put(c.getId(), c.getClassName());
    }
    for (Department d : allDepts) {
      deptNameMap.put(d.getId(), d.getDeptName());
    }

    // 转换为前端友好的字段名
    List<Map<String, Object>> list = userPage.getRecords().stream().map(u -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", u.getId());
      item.put("studentNo", u.getUserNo()); // userNo → studentNo
      item.put("name", u.getNickname()); // nickname → name
      item.put("gender", u.getGender());
      item.put("className", classNameMap.getOrDefault(u.getClassId(), "")); // classId → className
      item.put("major", deptNameMap.getOrDefault(u.getDeptId(), "")); // deptId → major
      item.put("phone", u.getPhone());
      item.put("email", u.getEmail());
      item.put("enrollmentYear", u.getEnrollmentYear());
      item.put("status", u.getStatus());
      item.put("remark", u.getRemark());
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
    // 学号唯一性校验
    String userNo = (String) data.get("studentNo");
    QueryWrapper<User> checkWrapper = new QueryWrapper<>();
    checkWrapper.eq("user_no", userNo);
    if (userMapper.selectCount(checkWrapper) > 0) {
      return Result.error("学号已存在");
    }

    User user = new User();
    user.setUserNo(userNo);
    // 默认密码：学号后6位
    if (userNo != null && userNo.length() >= 6) {
      user.setPassword(passwordEncoder.encode(userNo.substring(userNo.length() - 6)));
    } else {
      user.setPassword(passwordEncoder.encode("123456"));
    }
    user.setNickname((String) data.get("name"));
    user.setGender(data.get("gender") != null ? Integer.parseInt(data.get("gender").toString()) : 1);
    user.setPhone((String) data.get("phone"));
    user.setEmail((String) data.get("email"));
    user.setEnrollmentYear((String) data.get("enrollmentYear"));
    user.setStatus(data.get("status") != null ? Integer.parseInt(data.get("status").toString()) : 1);
    user.setRemark((String) data.get("remark"));

    // 优先使用 classId 直传；没传再回退到 className 查找
    Clazz selectedClazz = null;
    if (data.get("classId") != null && !data.get("classId").toString().isEmpty()) {
      Long classId = Long.valueOf(data.get("classId").toString());
      user.setClassId(classId);
      selectedClazz = clazzMapper.selectById(classId);
    } else if (data.containsKey("className") && data.get("className") != null
        && !data.get("className").toString().isEmpty()) {
      String className = data.get("className").toString();
      QueryWrapper<Clazz> clazzWrapper = new QueryWrapper<>();
      clazzWrapper.eq("class_name", className);
      selectedClazz = clazzMapper.selectOne(clazzWrapper);
      if (selectedClazz != null) {
        user.setClassId(selectedClazz.getId());
      }
    }
    // 从班级自动获取院系ID（专业）
    if (selectedClazz != null && selectedClazz.getDeptId() != null) {
      user.setDeptId(selectedClazz.getDeptId());
    }

    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(LocalDateTime.now());

    userMapper.insert(user);

    // 插入学生角色关联 (role_id = 3)
    userRoleMapper.insertUserRole(user.getId(), 3L);

    return Result.success("新增成功");
  }

  @Override
  @Transactional
  public Result<?> update(Map<String, Object> data) {
    Long id = Long.valueOf(data.get("id").toString());
    User user = userMapper.selectById(id);
    if (user == null) {
      return Result.error("学生不存在");
    }

    // 学号唯一性校验（排除自己）
    String userNo = (String) data.get("studentNo");
    if (userNo != null && !userNo.equals(user.getUserNo())) {
      QueryWrapper<User> checkWrapper = new QueryWrapper<>();
      checkWrapper.eq("user_no", userNo).ne("id", id);
      if (userMapper.selectCount(checkWrapper) > 0) {
        return Result.error("学号已存在");
      }
      user.setUserNo(userNo);
    }

    if (data.containsKey("name"))
      user.setNickname((String) data.get("name"));
    if (data.containsKey("gender"))
      user.setGender(Integer.parseInt(data.get("gender").toString()));
    if (data.containsKey("phone"))
      user.setPhone((String) data.get("phone"));
    if (data.containsKey("email"))
      user.setEmail((String) data.get("email"));
    if (data.containsKey("enrollmentYear"))
      user.setEnrollmentYear((String) data.get("enrollmentYear"));
    if (data.containsKey("status"))
      user.setStatus(Integer.parseInt(data.get("status").toString()));
    if (data.containsKey("remark"))
      user.setRemark((String) data.get("remark"));

    // 根据班级名称查找 classId 和 deptId（专业）
    if (data.containsKey("className") && data.get("className") != null
        && !data.get("className").toString().isEmpty()) {
      String className = data.get("className").toString();
      QueryWrapper<Clazz> clazzWrapper = new QueryWrapper<>();
      clazzWrapper.eq("class_name", className);
      Clazz clazz = clazzMapper.selectOne(clazzWrapper);
      if (clazz != null) {
        user.setClassId(clazz.getId());
        if (clazz.getDeptId() != null) {
          user.setDeptId(clazz.getDeptId());
        }
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
      return Result.error("学生不存在");
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
  public Result<?> batchImport(List<Map<String, Object>> students) {
    int success = 0;
    int skip = 0;
    for (Map<String, Object> item : students) {
      // 兼容中英文列名
      String userNo = safeString(item.get("学号"));
      if (userNo.isEmpty()) userNo = safeString(item.get("studentNo"));
      if (userNo.isEmpty()) {
        skip++;
        continue;
      }

      // 查重含软删除：uk_user_no 唯一键覆盖所有行。
      // 在用账号跳过；已软删除的同学号走复活分支；不存在则新增。
      User existing = userMapper.findByUserNoIncludeDeleted(userNo);
      if (existing != null && (existing.getIsDeleted() == null || existing.getIsDeleted() == 0)) {
        skip++;
        continue;
      }

      User user = new User();
      user.setUserNo(userNo);
      if (userNo.length() >= 6) {
        user.setPassword(passwordEncoder.encode(userNo.substring(userNo.length() - 6)));
      } else {
        user.setPassword(passwordEncoder.encode("123456"));
      }

      // 姓名
      String name = safeString(item.get("姓名"));
      if (name.isEmpty()) name = safeString(item.get("name"));
      user.setNickname(name);

      // 性别
      String gender = safeString(item.get("性别"));
      if (gender.isEmpty()) gender = safeString(item.get("gender"));
      user.setGender(gender.equals("女") || gender.equals("2") ? 2 : 1);

      user.setPhone(safeString(item.get("联系电话")));
      user.setEmail(safeString(item.get("邮箱")));
      user.setEnrollmentYear(safeString(item.get("入学年份")));
      user.setStatus(1);
      user.setRemark(safeString(item.get("备注")));

      // 班级 → classId，同时自动获取 deptId
      String className = safeString(item.get("班级"));
      if (!className.isEmpty()) {
        QueryWrapper<Clazz> cq = new QueryWrapper<>();
        cq.eq("class_name", className);
        Clazz clazz = clazzMapper.selectOne(cq);
        if (clazz != null) {
          user.setClassId(clazz.getId());
          if (clazz.getDeptId() != null) {
            user.setDeptId(clazz.getDeptId());
          }
        }
      }
      // 如果班级没匹配到，尝试按专业名称查院系
      if (user.getDeptId() == null) {
        String majorName = safeString(item.get("专业"));
        if (!majorName.isEmpty()) {
          QueryWrapper<Department> dq = new QueryWrapper<>();
          dq.eq("dept_name", majorName);
          Department dept = departmentMapper.selectOne(dq);
          if (dept != null) {
            user.setDeptId(dept.getId());
          }
        }
      }

      if (existing == null) {
        // 新增学生
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        userRoleMapper.insertUserRole(user.getId(), 3L);
      } else {
        // 复活此前被软删除的同学号记录：沿用原 id 保留历史，绕过 @TableLogic 置 is_deleted=0
        user.setId(existing.getId());
        userMapper.reviveStudent(user);
        userRoleMapper.insertUserRoleIfAbsent(existing.getId(), 3L);
      }
      success++;
    }
    Map<String, Object> result = new HashMap<>();
    result.put("success", success);
    result.put("skip", skip);
    return Result.success(String.format("成功导入 %d 名学生，跳过 %d 条", success, skip), result);
  }

  @Override
  @Transactional
  public Result<?> updateStatus(Long id, Integer status) {
    User user = userMapper.selectById(id);
    if (user == null) {
      return Result.error("学生不存在");
    }
    user.setStatus(status);
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
    return Result.success("状态更新成功");
  }

  private String safeString(Object obj) {
    return obj == null ? "" : obj.toString().trim();
  }
}