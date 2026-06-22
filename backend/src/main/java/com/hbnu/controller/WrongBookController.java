package com.hbnu.controller;

import com.hbnu.common.JwtUtils;
import com.hbnu.common.Result;
import com.hbnu.service.WrongBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 错题本控制器
 */
@RestController
@RequestMapping("/api/wrong-book")
public class WrongBookController {

    @Autowired
    private WrongBookService wrongBookService;

    /**
     * 获取错题本列表
     * GET /api/wrong-book/list
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer isMastered,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return wrongBookService.pageList(page, size, userId, subject, isMastered);
    }

    /**
     * 更新错题掌握状态
     * PUT /api/wrong-book/mastered/{id}
     */
    @PutMapping("/mastered/{id}")
    public Result<?> updateMastered(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer isMastered = body.get("isMastered") != null ? Integer.parseInt(body.get("isMastered").toString()) : 1;
        return wrongBookService.updateMasteredStatus(id, isMastered);
    }

    /**
     * 删除错题
     * DELETE /api/wrong-book/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return wrongBookService.deleteWrongBook(id);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                return JwtUtils.getUserId(token.replace("Bearer ", ""));
            } catch (Exception e) {
                // ignore
            }
        }
        return 3L;
    }
}