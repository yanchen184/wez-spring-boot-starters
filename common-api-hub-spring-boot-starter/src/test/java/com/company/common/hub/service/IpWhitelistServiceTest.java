package com.company.common.hub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IpWhitelistService 單元測試（純邏輯，不需 Spring Context）。
 */
@DisplayName("IpWhitelistService 測試")
class IpWhitelistServiceTest {

    private IpWhitelistService service;

    @BeforeEach
    void setUp() {
        service = new IpWhitelistService(true);
    }

    @Nested
    @DisplayName("null/空白白名單")
    class NullOrEmptyWhitelist {

        @Test
        @DisplayName("白名單為 null 時應允許所有 IP")
        void shouldAllowAll_whenWhitelistIsNull() {
            assertThat(service.isAllowed("192.168.1.10", null)).isTrue();
        }

        @Test
        @DisplayName("白名單為空字串時應允許所有 IP")
        void shouldAllowAll_whenWhitelistIsEmpty() {
            assertThat(service.isAllowed("192.168.1.10", "")).isTrue();
        }

        @Test
        @DisplayName("白名單為空白時應允許所有 IP")
        void shouldAllowAll_whenWhitelistIsBlank() {
            assertThat(service.isAllowed("192.168.1.10", "   ")).isTrue();
        }
    }

    @Nested
    @DisplayName("單一 IP 精確匹配")
    class ExactMatch {

        @Test
        @DisplayName("IP 完全匹配時應允許")
        void shouldAllow_whenExactIpMatch() {
            assertThat(service.isAllowed("192.168.1.10", "192.168.1.10")).isTrue();
        }

        @Test
        @DisplayName("IP 不匹配時應拒絕")
        void shouldReject_whenIpNotMatch() {
            assertThat(service.isAllowed("192.168.1.11", "192.168.1.10")).isFalse();
        }
    }

    @Nested
    @DisplayName("CIDR 子網路匹配")
    class CidrMatch {

        @Test
        @DisplayName("IP 在 CIDR 範圍內時應允許")
        void shouldAllow_whenIpInCidrRange() {
            assertThat(service.isAllowed("192.168.1.50", "192.168.1.0/24")).isTrue();
        }

        @Test
        @DisplayName("IP 在 CIDR 範圍外時應拒絕")
        void shouldReject_whenIpOutsideCidr() {
            assertThat(service.isAllowed("192.168.2.1", "192.168.1.0/24")).isFalse();
        }

        @Test
        @DisplayName("CIDR 格式錯誤時應拒絕且不爆錯")
        void shouldReject_whenCidrMalformed() {
            assertThat(service.isAllowed("192.168.1.1", "invalid/cidr")).isFalse();
        }
    }

    @Nested
    @DisplayName("範圍匹配（最後一段）")
    class RangeMatch {

        @Test
        @DisplayName("IP 在範圍內時應允許")
        void shouldAllow_whenIpInRange() {
            assertThat(service.isAllowed("192.168.1.15", "192.168.1.10-20")).isTrue();
        }

        @Test
        @DisplayName("IP 在範圍邊界（起始）時應允許")
        void shouldAllow_whenIpAtRangeStart() {
            assertThat(service.isAllowed("192.168.1.10", "192.168.1.10-20")).isTrue();
        }

        @Test
        @DisplayName("IP 在範圍邊界（結束）時應允許")
        void shouldAllow_whenIpAtRangeEnd() {
            assertThat(service.isAllowed("192.168.1.20", "192.168.1.10-20")).isTrue();
        }

        @Test
        @DisplayName("IP 在範圍外時應拒絕")
        void shouldReject_whenIpOutsideRange() {
            assertThat(service.isAllowed("192.168.1.25", "192.168.1.10-20")).isFalse();
        }

        @Test
        @DisplayName("prefix 不同時應拒絕")
        void shouldReject_whenPrefixDiffers() {
            assertThat(service.isAllowed("10.0.0.15", "192.168.1.10-20")).isFalse();
        }
    }

    @Nested
    @DisplayName("多行混合")
    class MultiLine {

        @Test
        @DisplayName("匹配多行中任一行時應允許")
        void shouldAllow_whenMatchesAnyLine() {
            String whitelist = "192.168.1.10\n10.0.0.0/8\n172.16.0.1-50";
            assertThat(service.isAllowed("10.0.0.5", whitelist)).isTrue();
        }

        @Test
        @DisplayName("不匹配任何行時應拒絕")
        void shouldReject_whenMatchesNoLine() {
            String whitelist = "192.168.1.10\n10.0.0.0/8";
            assertThat(service.isAllowed("172.16.0.1", whitelist)).isFalse();
        }

        @Test
        @DisplayName("含空白行時應正確處理")
        void shouldHandleBlankLines_whenWhitelistHasEmptyLines() {
            String whitelist = "192.168.1.10\n\n  \n10.0.0.1";
            assertThat(service.isAllowed("10.0.0.1", whitelist)).isTrue();
        }
    }

    @Nested
    @DisplayName("localhost 處理")
    class Localhost {

        @Test
        @DisplayName("allowLocal=true 時 127.0.0.1 應允許")
        void shouldAllowIpv4Localhost_whenAllowLocalTrue() {
            assertThat(service.isAllowed("127.0.0.1", "192.168.1.10")).isTrue();
        }

        @Test
        @DisplayName("allowLocal=true 時 IPv6 localhost 應允許")
        void shouldAllowIpv6Localhost_whenAllowLocalTrue() {
            assertThat(service.isAllowed("0:0:0:0:0:0:0:1", "192.168.1.10")).isTrue();
        }

        @Test
        @DisplayName("allowLocal=false 時 127.0.0.1 應按白名單判斷")
        void shouldCheckWhitelist_whenAllowLocalFalse() {
            IpWhitelistService strictService = new IpWhitelistService(false);
            assertThat(strictService.isAllowed("127.0.0.1", "192.168.1.10")).isFalse();
        }

        @Test
        @DisplayName("allowLocal=false 但白名單含 127.0.0.1 時應允許")
        void shouldAllowLocalhost_whenInWhitelistAndAllowLocalFalse() {
            IpWhitelistService strictService = new IpWhitelistService(false);
            assertThat(strictService.isAllowed("127.0.0.1", "127.0.0.1")).isTrue();
        }
    }

    @Nested
    @DisplayName("格式錯誤處理")
    class MalformedInput {

        @Test
        @DisplayName("格式錯誤的白名單不應爆錯")
        void shouldNotThrow_whenMalformedWhitelist() {
            assertThat(service.isAllowed("192.168.1.1", "garbage-data")).isFalse();
        }

        @Test
        @DisplayName("範圍格式不完整時不應爆錯")
        void shouldNotThrow_whenIncompleteRange() {
            assertThat(service.isAllowed("192.168.1.1", "192.168.1.10-")).isFalse();
        }
    }
}
