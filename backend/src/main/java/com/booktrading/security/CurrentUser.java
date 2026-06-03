package com.booktrading.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
public class CurrentUser {

    public Long getId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object userId = request.getAttribute("currentUserId");
        return userId != null ? (Long) userId : null;
    }

    public String getRole() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object role = request.getAttribute("currentUserRole");
        return role != null ? (String) role : null;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }
}
