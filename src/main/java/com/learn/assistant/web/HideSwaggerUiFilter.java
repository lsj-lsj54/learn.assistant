package com.learn.assistant.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class HideSwaggerUiFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("/swagger-ui.html".equals(path) || path.startsWith("/swagger-ui/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
