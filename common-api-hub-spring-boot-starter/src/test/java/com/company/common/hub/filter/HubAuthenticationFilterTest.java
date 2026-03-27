package com.company.common.hub.filter;

import com.company.common.hub.dto.HubAuthResult;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import com.company.common.hub.exception.HubAuthException;
import com.company.common.hub.exception.HubTokenException;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.service.HubAuthService;
import com.company.common.hub.service.HubLogService;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HubAuthenticationFilter 單元測試。
 *
 * <p>使用 MockHttpServletRequest/Response/FilterChain 模擬 HTTP 請求。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HubAuthenticationFilter 測試")
class HubAuthenticationFilterTest {

    @Mock
    private HubAuthService hubAuthService;

    @Mock
    private HubLogService hubLogService;

    @Mock
    private HubSetRepository hubSetRepository;

    @Mock
    private FilterChain filterChain;

    private HubAuthenticationFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new HubAuthenticationFilter(
                hubAuthService, hubLogService, hubSetRepository, objectMapper
        );
    }

    private HubSet createEnabledHubSet(String uri) {
        HubSet hubSet = new HubSet();
        hubSet.setId(1L);
        hubSet.setName("Test API");
        hubSet.setUri(uri);
        hubSet.setEnabled(true);
        hubSet.setJwtTokenAging(3600);
        return hubSet;
    }

    @Test
    @DisplayName("URI 不在管控範圍時應直接放行")
    void shouldPassThrough_whenUriNotManaged() throws ServletException, IOException {
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(hubAuthService, never()).authenticate(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("缺少 X-Hub-Token header 時應回傳 401")
    void shouldReturn401_whenTokenHeaderMissing() throws ServletException, IOException {
        when(hubSetRepository.findByEnabledTrue())
                .thenReturn(List.of(createEnabledHubSet("/api/users/**")));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains(HubResponseCode.AUTH_FAILED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Token 認證成功時應放行並記日誌")
    void shouldPassThrough_whenTokenValid() throws ServletException, IOException {
        HubSet hubSet = createEnabledHubSet("/api/users/**");
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of(hubSet));

        HubUser hubUser = new HubUser();
        hubUser.setId(1L);
        hubUser.setUsername("admin");

        HubUserSet hubUserSet = new HubUserSet();
        hubUserSet.setId(1L);

        HubAuthResult authResult = new HubAuthResult(hubUser, hubSet, hubUserSet);
        when(hubAuthService.authenticate(eq("/api/users/1"), eq("GET"),
                isNull(), isNull(), eq("valid-token"), anyString()))
                .thenReturn(authResult);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        request.addHeader(HubAuthenticationFilter.TOKEN_HEADER, "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(hubLogService).log(eq(hubUserSet), eq("GET"), eq("/api/users/1"),
                isNull(), anyString(), eq(true), eq(HubResponseCode.SUCCESS),
                isNull(), isNull(), anyLong());
    }

    @Test
    @DisplayName("認證失敗（HubAuthException）時應回傳錯誤並記日誌")
    void shouldReturnError_whenAuthFails() throws ServletException, IOException {
        when(hubSetRepository.findByEnabledTrue())
                .thenReturn(List.of(createEnabledHubSet("/api/users/**")));
        when(hubAuthService.authenticate(any(), any(), any(), any(), any(), any()))
                .thenThrow(new HubAuthException(HubResponseCode.IP_DENIED, "IP 不在白名單"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        request.addHeader(HubAuthenticationFilter.TOKEN_HEADER, "some-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains(HubResponseCode.IP_DENIED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Token 過期（HubTokenException）時應回傳 422 錯誤")
    void shouldReturn422_whenTokenExpired() throws ServletException, IOException {
        when(hubSetRepository.findByEnabledTrue())
                .thenReturn(List.of(createEnabledHubSet("/api/users/**")));
        when(hubAuthService.authenticate(any(), any(), any(), any(), any(), any()))
                .thenThrow(new HubTokenException(HubResponseCode.TOKEN_EXPIRED, "Token 已過期"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        request.addHeader(HubAuthenticationFilter.TOKEN_HEADER, "expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(response.getContentAsString()).contains(HubResponseCode.TOKEN_EXPIRED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("X-Hub-Token 為空白字串時應回傳 401")
    void shouldReturn401_whenTokenHeaderBlank() throws ServletException, IOException {
        when(hubSetRepository.findByEnabledTrue())
                .thenReturn(List.of(createEnabledHubSet("/api/users/**")));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        request.addHeader(HubAuthenticationFilter.TOKEN_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Hub 管理端點 /api/hub/** 不在 HubSet 中時應放行")
    void shouldPassThrough_whenHubAdminEndpoint() throws ServletException, IOException {
        when(hubSetRepository.findByEnabledTrue())
                .thenReturn(List.of(createEnabledHubSet("/api/users/**")));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/hub/token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
