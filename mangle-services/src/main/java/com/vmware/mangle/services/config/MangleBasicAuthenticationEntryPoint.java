/*
 * Copyright (c) 2016-2019 VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with separate copyright notices
 * and license terms. Your use of these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package com.vmware.mangle.services.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.vmware.mangle.model.response.ErrorDetails;
import com.vmware.mangle.utils.clients.restclient.RestTemplateWrapper;
import com.vmware.mangle.utils.constants.ErrorConstants;
import com.vmware.mangle.utils.exceptions.handler.ErrorCode;

/**
 *
 * MangleBasicAuthenticationEntryPoint for invalid auth
 *
 * @author ranjans
 */

@Log4j2
@Component
public class MangleBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authEx)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write(RestTemplateWrapper.objectToJson(getMangleErrorDetails(request, authEx)));
    }

    @Override
    public void afterPropertiesSet() {
        setRealmName("MANGLE REALM");
        super.afterPropertiesSet();
    }

    private ErrorDetails getMangleErrorDetails(HttpServletRequest httpServletRequest, AuthenticationException authEx) {
        String errorDescription;
        if (authEx instanceof LockedException) {
            errorDescription = authEx.getMessage();
        } else if (authEx instanceof InsufficientAuthenticationException) {
            errorDescription = authEx.getMessage();
        } else {
            errorDescription = ErrorConstants.AUTHENTICATION_FAILED_ERROR_MSG;
        }
        log.error("Authentication for the user {} failed with the error: {}", extractUserName(httpServletRequest),
                authEx.getMessage());
        return new ErrorDetails(new Date(), ErrorCode.LOGIN_EXCEPTION.getCode(), errorDescription,
                httpServletRequest.getRequestURI());
    }

    private String extractUserName(HttpServletRequest request) {
        final String authorization = request.getHeader("Authorization");
        String username = null;
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            final String[] values = credentials.split(":", 2);
            username = values[0];
        }
        return username;
    }
}
