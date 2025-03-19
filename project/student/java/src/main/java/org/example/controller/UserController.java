package org.example.controller;

import org.example.model.Student;
import org.example.utils.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @GetMapping("/GetUser")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String sex) {

        Map<String, Object> response = new HashMap<>();
        try {
            // 参数校验
            if (page < 1 || pageSize < 1 || pageSize > 100) {
                response.put("code", 400);
                response.put("message", "分页参数无效");
                return ResponseEntity.badRequest().body(response);
            }

            // 构建动态SQL
            StringBuilder dataSql = new StringBuilder("SELECT * FROM user WHERE 1=1");
            List<Object> params = new ArrayList<>();

            // ID精确查询
            if (id != null) {
                dataSql.append(" AND id = ?");
                params.add(id);
            }

            // 姓名模糊查询
            if (StringUtils.hasText(name)) {
                dataSql.append(" AND name LIKE ?");
                params.add("%" + name + "%");
            }

            // 电话精确查询
            if (StringUtils.hasText(phone)) {
                dataSql.append(" AND phone = ?");
                params.add(phone);
            }

            // 性别精确查询
            if (StringUtils.hasText(sex)) {
                dataSql.append(" AND sex = ?");
                params.add(sex);
            }

            // 添加分页
            dataSql.append(" ORDER BY id ASC LIMIT ? OFFSET ?");
            int offset = (page - 1) * pageSize;
            params.add(pageSize);
            params.add(offset);

            // 执行数据查询
            List<Student> users = jdbcTemplate.query(
                    dataSql.toString(),
                    params.toArray(),
                    new BeanPropertyRowMapper<>(Student.class)
            );

            // 构建总数查询（移除分页部分）
            String countSql = dataSql.toString()
                    .replaceFirst("LIMIT \\? OFFSET \\?$", "")
                    .replaceFirst("SELECT \\*", "SELECT COUNT(*)");

            // 执行总数查询
            Integer total = jdbcTemplate.queryForObject(
                    countSql,
                    params.subList(0, params.size()-2).toArray(),
                    Integer.class
            );

            // 构建响应
            response.put("code", 200);
            response.put("data", users);
            response.put("total", total);
            response.put("currentPage", page);
            response.put("pageSize", pageSize);
            return ResponseEntity.ok(response);

        } catch (EmptyResultDataAccessException e) {
            response.put("code", 404);
            response.put("message", "未找到匹配的数据");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 新增删除用户功能
    @DeleteMapping("/DelUser")
    public ResponseEntity<Map<String, Object>> DelUser(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 参数校验
            if (!requestBody.containsKey("id")) {
                response.put("code", 400);
                response.put("message", "缺少必要参数: id");
                return ResponseEntity.badRequest().body(response);
            }

            Object idObj = requestBody.get("id");
            int id;

            // 2. 验证是否为整数
            if (idObj instanceof Integer) {
                id = (Integer) idObj;
            } else if (idObj instanceof String) {
                try {
                    id = Integer.parseInt((String) idObj);
                } catch (NumberFormatException e) {
                    response.put("code", 400);
                    response.put("message", "ID必须是有效整数");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                response.put("code", 400);
                response.put("message", "无效的ID格式");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 执行删除操作
            int affectedRows = jdbcTemplate.update(
                    "DELETE FROM user WHERE id = ?",
                    id
            );

            // 4. 处理结果
            if (affectedRows > 0) {
                response.put("code", 200);
                response.put("message", "删除成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("code", 404);
                response.put("message", "指定ID的用户不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @PostMapping("/InsUser")
    public ResponseEntity<Map<String, Object>> insertUser(
            @Valid @RequestBody Student student) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 加密密码
            String encryptedPasswd = MD5Util.encrypt(student.getPasswd());

            // 2. 准备插入SQL
            String sql = """
                INSERT INTO user 
                (name, passwd, birthday, sex, phone, email, addr, education)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            // 3. 执行插入
            int result = jdbcTemplate.update(
                    sql,
                    student.getName(),
                    encryptedPasswd,
                    java.sql.Date.valueOf(student.getBirthday()), // 转换LocalDate
                    student.getSex().name(),
                    student.getPhone(),
                    student.getEmail(),
                    student.getAddr(),
                    student.getEducation()
            );

            // 4. 处理结果
            if (result > 0) {
                response.put("code", 200);
                response.put("message", "用户添加成功");
                return ResponseEntity.ok(response);
            }

        } catch (IllegalArgumentException e) {
            // 处理枚举值不合法的情况
            response.put("code", 400);
            response.put("message", "参数不合法: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            // 处理其他异常
            response.put("code", 500);
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        response.put("code", 500);
        response.put("message", "未知错误");
        return ResponseEntity.internalServerError().body(response);
    }


}