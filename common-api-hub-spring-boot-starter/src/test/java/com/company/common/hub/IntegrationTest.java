package com.company.common.hub;

import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubLog;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import com.company.common.hub.filter.HubAuthenticationFilter;
import com.company.common.hub.repository.HubLogRepository;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.repository.HubUserRepository;
import com.company.common.hub.repository.HubUserSetRepository;
import com.company.common.hub.service.HubLogService;
import com.company.common.hub.service.HubTokenService;
import com.company.common.hub.service.IpWhitelistService;
import com.company.common.hub.service.HubAuthService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 9: 端對端整合測試。
 *
 * <p>使用真實 H2 資料庫跑完整流程：
 * <ol>
 *   <li>插入測試資料</li>
 *   <li>帳密換 Token</li>
 *   <li>帶 Token 呼叫被保護的 API</li>
 *   <li>不帶 Token 被擋</li>
 *   <li>驗證日誌</li>
 * </ol>
 */
@SpringBootTest(classes = {TestJpaConfig.class, IntegrationTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 9: 端對端整合測試")
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HubSetRepository hubSetRepository;

    @Autowired
    private HubUserRepository hubUserRepository;

    @Autowired
    private HubUserSetRepository hubUserSetRepository;

    @Autowired
    private HubLogRepository hubLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        hubLogRepository.deleteAll();
        hubUserSetRepository.deleteAll();
        hubUserRepository.deleteAll();
        hubSetRepository.deleteAll();
    }

    private void insertTestData() {
        insertTestData(null); // 無 IP 限制
    }

    private void insertTestData(String verifyIp) {
        HubSet hubSet = new HubSet();
        hubSet.setName("使用者查詢 API");
        hubSet.setUri("/api/users/**");
        hubSet.setJwtTokenAging(3600);
        hubSet.setEnabled(true);
        hubSet = hubSetRepository.save(hubSet);

        HubUser hubUser = new HubUser();
        hubUser.setUsername("testuser");
        hubUser.setPassword(ENCODER.encode("test1234"));
        hubUser.setEnabled(true);
        hubUser.setVerifyIp(verifyIp);
        hubUser = hubUserRepository.save(hubUser);

        HubUserSet hubUserSet = new HubUserSet();
        hubUserSet.setHubSet(hubSet);
        hubUserSet.setHubUser(hubUser);
        hubUserSet.setEnabled(true);
        hubUserSet.setUserVerify(true);
        hubUserSet.setJwtTokenVerify(true);
        hubUserSet.setVerifyDts(LocalDate.now().minusDays(30));
        hubUserSet.setVerifyDte(LocalDate.now().plusDays(30));
        hubUserSetRepository.save(hubUserSet);
    }

    @Test
    @Order(1)
    @DisplayName("帳密換 Token 應成功")
    void shouldIssueToken_whenValidCredentials() throws Exception {
        insertTestData();

        String body = "{\"username\":\"testuser\",\"password\":\"test1234\",\"uri\":\"/api/users/**\"}";

        mockMvc.perform(post("/api/hub/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HubResponseCode.TOKEN_ISSUED))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @Order(2)
    @DisplayName("帶 Token 呼叫被保護的 API 應成功（回傳 404 表示 Filter 放行了）")
    void shouldPassFilter_whenTokenValid() throws Exception {
        insertTestData();

        // 先取 Token
        String body = "{\"username\":\"testuser\",\"password\":\"test1234\",\"uri\":\"/api/users/**\"}";
        MvcResult tokenResult = mockMvc.perform(post("/api/hub/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(tokenResult.getResponse().getContentAsString());
        String token = json.get("data").get("token").asText();

        // 帶 Token 打受保護 API（該 API 不存在，所以 Filter 放行後會回 404）
        mockMvc.perform(get("/api/users/1")
                        .header(HubAuthenticationFilter.TOKEN_HEADER, token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    @DisplayName("不帶 Token 呼叫被保護的 API 應回傳 401")
    void shouldReturn401_whenNoToken() throws Exception {
        insertTestData();

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(HubResponseCode.AUTH_FAILED));
    }

    @Test
    @Order(4)
    @DisplayName("IP 不在白名單時應回傳 401003")
    void shouldReturn401003_whenIpDenied() throws Exception {
        // 設定白名單為只允許 10.0.0.0/8，但 MockMvc 預設 IP 是 127.0.0.1
        insertTestData("10.0.0.0/8");

        // 先取 Token（帳密認證也會被 IP 白名單擋住，因為 localhost 不在 10.0.0.0/8）
        String body = "{\"username\":\"testuser\",\"password\":\"test1234\",\"uri\":\"/api/users/**\"}";
        mockMvc.perform(post("/api/hub/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(HubResponseCode.IP_DENIED));
    }

    @Test
    @Order(5)
    @DisplayName("認證成功後應記錄 HubLog")
    void shouldLogAfterAuthentication() throws Exception {
        insertTestData();

        String body = "{\"username\":\"testuser\",\"password\":\"test1234\",\"uri\":\"/api/users/**\"}";
        mockMvc.perform(post("/api/hub/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        List<HubLog> logs = hubLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.stream().anyMatch(l -> Boolean.TRUE.equals(l.getSuccess()))).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("帳密錯誤時應回傳 401 並記日誌")
    void shouldReturn401AndLog_whenWrongPassword() throws Exception {
        insertTestData();

        String body = "{\"username\":\"testuser\",\"password\":\"wrongpass\",\"uri\":\"/api/users/**\"}";
        mockMvc.perform(post("/api/hub/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(HubResponseCode.AUTH_FAILED));

        List<HubLog> logs = hubLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.stream().anyMatch(l -> Boolean.FALSE.equals(l.getSuccess()))).isTrue();
    }
}
