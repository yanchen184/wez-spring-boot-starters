package com.company.common.hub.service;

import com.company.common.hub.entity.HubLog;
import com.company.common.hub.repository.HubLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HubLogService 單元測試（Mock HubLogRepository）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HubLogService 測試")
class HubLogServiceTest {

    @Mock
    private HubLogRepository hubLogRepository;

    private HubLogService logService;

    @BeforeEach
    void setUp() {
        logService = new HubLogService(
                hubLogRepository,
                List.of("password", "passcode", "secret", "token")
        );
    }

    @Nested
    @DisplayName("敏感欄位脫敏")
    class MaskSensitiveFields {

        @Test
        @DisplayName("應將 password 欄位值替換為 ***")
        void shouldMaskPassword_whenPresent() {
            String input = "{\"username\":\"admin\",\"password\":\"mySecret\"}";

            String result = logService.maskSensitiveFields(input);

            assertThat(result).contains("\"username\":\"admin\"");
            assertThat(result).contains("\"password\":\"***\"");
            assertThat(result).doesNotContain("mySecret");
        }

        @Test
        @DisplayName("應將多個敏感欄位全部脫敏")
        void shouldMaskMultipleFields_whenPresent() {
            String input = "{\"password\":\"a\",\"passcode\":\"b\",\"secret\":\"c\",\"name\":\"ok\"}";

            String result = logService.maskSensitiveFields(input);

            assertThat(result).contains("\"name\":\"ok\"");
            assertThat(result).doesNotContain("\"a\"");
            assertThat(result).doesNotContain("\"b\"");
            assertThat(result).doesNotContain("\"c\"");
        }

        @Test
        @DisplayName("應脫敏 token 欄位")
        void shouldMaskTokenField_whenPresent() {
            String input = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9.xxx\",\"data\":\"hello\"}";

            String result = logService.maskSensitiveFields(input);

            assertThat(result).contains("\"token\":\"***\"");
            assertThat(result).contains("\"data\":\"hello\"");
        }

        @Test
        @DisplayName("大小寫不敏感")
        void shouldMaskCaseInsensitive_whenMixedCase() {
            String input = "{\"Password\":\"secret123\"}";

            String result = logService.maskSensitiveFields(input);

            assertThat(result).doesNotContain("secret123");
        }

        @Test
        @DisplayName("null 輸入應回傳 null")
        void shouldReturnNull_whenInputIsNull() {
            assertThat(logService.maskSensitiveFields(null)).isNull();
        }

        @Test
        @DisplayName("空字串輸入應回傳空字串")
        void shouldReturnEmpty_whenInputIsEmpty() {
            assertThat(logService.maskSensitiveFields("")).isEmpty();
        }

        @Test
        @DisplayName("無敏感欄位時應原樣回傳")
        void shouldReturnOriginal_whenNoSensitiveFields() {
            String input = "{\"username\":\"admin\",\"data\":\"hello\"}";

            String result = logService.maskSensitiveFields(input);

            assertThat(result).isEqualTo(input);
        }

        @Test
        @DisplayName("冒號前後有空格時仍應正確脫敏")
        void shouldMask_whenSpacesAroundColon() {
            String input = "{\"password\" : \"mySecret\"}";

            String result = logService.maskSensitiveFields(input);

            assertThat(result).doesNotContain("mySecret");
            assertThat(result).contains("\"***\"");
        }
    }

    @Nested
    @DisplayName("日誌記錄")
    class LogMethod {

        @Test
        @DisplayName("應建立 HubLog 並存入 DB")
        void shouldSaveHubLog_whenLogCalled() {
            when(hubLogRepository.save(any(HubLog.class))).thenAnswer(inv -> inv.getArgument(0));

            logService.log(
                    null, "POST", "/api/hub/token",
                    "{\"username\":\"admin\",\"password\":\"secret\"}",
                    "192.168.1.10", true, "200001",
                    "{\"code\":\"200001\"}", null, 150L
            );

            ArgumentCaptor<HubLog> captor = ArgumentCaptor.forClass(HubLog.class);
            verify(hubLogRepository).save(captor.capture());

            HubLog saved = captor.getValue();
            assertThat(saved.getRequestMethod()).isEqualTo("POST");
            assertThat(saved.getRequestUri()).isEqualTo("/api/hub/token");
            assertThat(saved.getRequestParams()).contains("\"password\":\"***\"");
            assertThat(saved.getRequestParams()).doesNotContain("secret");
            assertThat(saved.getIp()).isEqualTo("192.168.1.10");
            assertThat(saved.getSuccess()).isTrue();
            assertThat(saved.getElapsedMs()).isEqualTo(150L);
        }

        @Test
        @DisplayName("requestParams 為 null 時不應爆錯")
        void shouldHandleNullParams_whenParamsIsNull() {
            when(hubLogRepository.save(any(HubLog.class))).thenAnswer(inv -> inv.getArgument(0));

            logService.log(
                    null, "GET", "/api/users",
                    null, "10.0.0.1", true, "200001",
                    null, null, 50L
            );

            ArgumentCaptor<HubLog> captor = ArgumentCaptor.forClass(HubLog.class);
            verify(hubLogRepository).save(captor.capture());

            assertThat(captor.getValue().getRequestParams()).isNull();
        }

        @Test
        @DisplayName("hubUserSet 為 null 時不應爆錯")
        void shouldHandleNullUserSet_whenAuthFailed() {
            when(hubLogRepository.save(any(HubLog.class))).thenAnswer(inv -> inv.getArgument(0));

            logService.log(
                    null, "POST", "/api/hub/token",
                    null, "10.0.0.1", false, "401002",
                    null, "帳號不存在", 30L
            );

            ArgumentCaptor<HubLog> captor = ArgumentCaptor.forClass(HubLog.class);
            verify(hubLogRepository).save(captor.capture());

            assertThat(captor.getValue().getHubUserSet()).isNull();
            assertThat(captor.getValue().getSuccess()).isFalse();
        }
    }
}
