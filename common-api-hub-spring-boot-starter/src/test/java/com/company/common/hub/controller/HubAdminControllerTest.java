package com.company.common.hub.controller;

import com.company.common.hub.IntegrationTestConfig;
import com.company.common.hub.TestJpaConfig;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.repository.HubLogRepository;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.repository.HubUserRepository;
import com.company.common.hub.repository.HubUserSetRepository;
import com.company.common.hub.service.HubAuthService;
import com.company.common.hub.service.HubLogService;
import com.company.common.hub.service.HubTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HubAdminController 測試。
 */
@SpringBootTest(classes = {TestJpaConfig.class, IntegrationTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HubAdminController 測試")
class HubAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HubSetRepository hubSetRepository;

    @MockitoBean
    private HubUserRepository hubUserRepository;

    @MockitoBean
    private HubUserSetRepository hubUserSetRepository;

    @MockitoBean
    private HubLogRepository hubLogRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private HubAuthService hubAuthService;

    @MockitoBean
    private HubTokenService hubTokenService;

    @MockitoBean
    private HubLogService hubLogService;

    // ========== HubSet ==========

    @Test
    @DisplayName("應回傳所有 API 設定")
    void shouldListAllApiSets() throws Exception {
        HubSet set = new HubSet();
        set.setId(1L);
        set.setName("Test API");
        set.setUri("/api/test/**");
        when(hubSetRepository.findAll()).thenReturn(List.of(set));
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        mockMvc.perform(get("/api/hub/admin/api-sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HubResponseCode.SUCCESS))
                .andExpect(jsonPath("$.data[0].name").value("Test API"));
    }

    @Test
    @DisplayName("應建立 API 設定")
    void shouldCreateApiSet() throws Exception {
        HubSet set = new HubSet();
        set.setId(1L);
        set.setName("New API");
        set.setUri("/api/new/**");
        when(hubSetRepository.save(any(HubSet.class))).thenReturn(set);
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        String body = "{\"name\":\"New API\",\"uri\":\"/api/new/**\",\"jwtTokenAging\":3600}";

        mockMvc.perform(post("/api/hub/admin/api-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New API"));
    }

    @Test
    @DisplayName("更新不存在的 API 設定應回傳 404")
    void shouldReturn404_whenUpdateNonExistentApiSet() throws Exception {
        when(hubSetRepository.findById(999L)).thenReturn(Optional.empty());
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        String body = "{\"name\":\"Updated\",\"uri\":\"/api/updated/**\"}";

        mockMvc.perform(put("/api/hub/admin/api-sets/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ========== HubUser ==========

    @Test
    @DisplayName("建立使用者時密碼應 BCrypt 加密")
    void shouldEncryptPassword_whenCreateUser() throws Exception {
        when(passwordEncoder.encode("plain123")).thenReturn("$2a$10$encoded");
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        HubUser saved = new HubUser();
        saved.setId(1L);
        saved.setUsername("newuser");
        saved.setPassword("$2a$10$encoded");
        when(hubUserRepository.save(any(HubUser.class))).thenReturn(saved);

        String body = "{\"username\":\"newuser\",\"password\":\"plain123\"}";

        mockMvc.perform(post("/api/hub/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    @DisplayName("更新使用者不傳密碼時應保留原密碼")
    void shouldKeepOriginalPassword_whenUpdateWithoutPassword() throws Exception {
        HubUser existing = new HubUser();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setPassword("$2a$10$originalEncoded");
        existing.setEnabled(true);

        when(hubUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(hubUserRepository.save(any(HubUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        String body = "{\"username\":\"admin-updated\",\"enabled\":true}";

        mockMvc.perform(put("/api/hub/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").value("$2a$10$originalEncoded"));
    }

    @Test
    @DisplayName("刪除不存在的使用者應回傳 404")
    void shouldReturn404_whenDeleteNonExistentUser() throws Exception {
        when(hubUserRepository.existsById(999L)).thenReturn(false);
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        mockMvc.perform(delete("/api/hub/admin/users/999"))
                .andExpect(status().isNotFound());
    }

    // ========== HubLog ==========

    @Test
    @DisplayName("應回傳分頁日誌")
    void shouldReturnPagedLogs() throws Exception {
        Page<?> emptyPage = new PageImpl<>(List.of());
        when(hubLogRepository.findAll(any(Pageable.class))).thenReturn((Page) emptyPage);
        when(hubSetRepository.findByEnabledTrue()).thenReturn(List.of());

        mockMvc.perform(get("/api/hub/admin/logs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HubResponseCode.SUCCESS));
    }
}
