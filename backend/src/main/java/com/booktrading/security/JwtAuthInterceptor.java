package com.booktrading.security;

import com.booktrading.entity.User;
import com.booktrading.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    // Paths that allow unauthenticated access but still benefit from user identification
    private static final List<String> OPTIONAL_AUTH_PATHS = Arrays.asList(
            "/api/book/detail/",
            "/api/book/list",
            "/api/book/recommend",
            "/api/book/search",
            "/api/category/list",
            "/api/comment/book/",
            "/api/banner/list",
            "/api/file/"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestPath = request.getRequestURI();
        boolean optionalAuth = OPTIONAL_AUTH_PATHS.stream().anyMatch(requestPath::startsWith);

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserId(token);
                    String role = jwtTokenProvider.getRole(token);

                    // Check user status - banned users are blocked immediately
                    User user = userService.getById(userId);
                    if (user == null || !"ACTIVE".equals(user.getStatus())) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":401,\"message\":\"账号已被禁用或不存在\"}");
                        return false;
                    }

                    request.setAttribute("currentUserId", userId);
                    request.setAttribute("currentUserRole", role);

                    // Admin role check for admin endpoints
                    if (requestPath.startsWith("/api/admin/")) {
                        if (!"ADMIN".equals(role)) {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"无管理员权限\"}");
                            return false;
                        }
                    }

                    return true;
                }
            } catch (Exception e) {
                log.warn("JWT验证失败: {}", e.getMessage());
            }
        }

        // No valid token — allow optional-auth paths through
        if (optionalAuth) {
            return true;
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
        return false;
    }
}
