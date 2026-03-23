package com.company.common.security.autoconfigure;

import com.company.common.security.config.PasswordPolicyConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "care.security")
public class CareSecurityProperties {

    private boolean enabled = true;

    private Jwt jwt = new Jwt();
    private Login login = new Login();
    private Cors cors = new Cors();
    private PasswordPolicyConfig password = new PasswordPolicyConfig();
    private Ldap ldap = new Ldap();
    private Otp otp = new Otp();
    private Captcha captcha = new Captcha();
    private CitizenCert citizenCert = new CitizenCert();
    private Web web = new Web();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Login getLogin() { return login; }
    public void setLogin(Login login) { this.login = login; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public PasswordPolicyConfig getPassword() { return password; }
    public void setPassword(PasswordPolicyConfig password) { this.password = password; }
    public Ldap getLdap() { return ldap; }
    public void setLdap(Ldap ldap) { this.ldap = ldap; }
    public Otp getOtp() { return otp; }
    public void setOtp(Otp otp) { this.otp = otp; }
    public Captcha getCaptcha() { return captcha; }
    public void setCaptcha(Captcha captcha) { this.captcha = captcha; }
    public CitizenCert getCitizenCert() { return citizenCert; }
    public void setCitizenCert(CitizenCert citizenCert) { this.citizenCert = citizenCert; }
    public Web getWeb() { return web; }
    public void setWeb(Web web) { this.web = web; }

    public static class Jwt {
        private int accessTokenTtlMinutes = 30;
        private int refreshTokenTtlDays = 7;
        private String keystorePath = "./keys/jwt-keys.json";

        public int getAccessTokenTtlMinutes() { return accessTokenTtlMinutes; }
        public void setAccessTokenTtlMinutes(int accessTokenTtlMinutes) { this.accessTokenTtlMinutes = accessTokenTtlMinutes; }
        public int getRefreshTokenTtlDays() { return refreshTokenTtlDays; }
        public void setRefreshTokenTtlDays(int refreshTokenTtlDays) { this.refreshTokenTtlDays = refreshTokenTtlDays; }
        public String getKeystorePath() { return keystorePath; }
        public void setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; }
    }

    public static class Login {
        private int maxAttempts = 5;
        private int lockDurationMinutes = 30;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public int getLockDurationMinutes() { return lockDurationMinutes; }
        public void setLockDurationMinutes(int lockDurationMinutes) { this.lockDurationMinutes = lockDurationMinutes; }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Ldap {
        private boolean enabled = false;
        private String url = "ldap://localhost:389";
        private String baseDn = "dc=example,dc=com";
        private String userSearchFilter = "(sAMAccountName={0})";
        private String bindDn = "cn=admin,dc=example,dc=com";
        private String bindPassword = "";
        private String displayNameAttr = "displayName";
        private String emailAttr = "mail";
        private java.util.List<String> defaultRoles = java.util.List.of("ROLE_USER");

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getBaseDn() { return baseDn; }
        public void setBaseDn(String baseDn) { this.baseDn = baseDn; }
        public String getUserSearchFilter() { return userSearchFilter; }
        public void setUserSearchFilter(String userSearchFilter) { this.userSearchFilter = userSearchFilter; }
        public String getBindDn() { return bindDn; }
        public void setBindDn(String bindDn) { this.bindDn = bindDn; }
        public String getBindPassword() { return bindPassword; }
        public void setBindPassword(String bindPassword) { this.bindPassword = bindPassword; }
        public String getDisplayNameAttr() { return displayNameAttr; }
        public void setDisplayNameAttr(String displayNameAttr) { this.displayNameAttr = displayNameAttr; }
        public String getEmailAttr() { return emailAttr; }
        public void setEmailAttr(String emailAttr) { this.emailAttr = emailAttr; }
        public java.util.List<String> getDefaultRoles() { return defaultRoles; }
        public void setDefaultRoles(java.util.List<String> defaultRoles) { this.defaultRoles = defaultRoles; }
    }

    public static class Otp {
        private boolean enabled = false;
        private String issuer = "CareSecuritySystem";
        private int allowedSkew = 1;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public int getAllowedSkew() { return allowedSkew; }
        public void setAllowedSkew(int allowedSkew) { this.allowedSkew = allowedSkew; }
    }

    public static class Captcha {
        private static final String DIGITS_ONLY = "0123456789";
        /** 排除易混淆的 I/O */
        private static final String DIGITS_AND_LETTERS = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";

        private boolean enabled = false;
        private int length = 4;
        private int expireSeconds = 300;
        /** 驗證碼字元集，預設依 includeLetters 自動決定；手動設定 chars 會覆蓋 */
        private String chars;
        /** 是否包含英文字母（預設 false = 純數字） */
        private boolean includeLetters = false;
        /** 圖片寬度（px） */
        private int width = 160;
        /** 圖片高度（px） */
        private int height = 50;
        /** 字型大小（px） */
        private int fontSize = 32;
        /** 是否啟用音訊驗證碼（無障礙） */
        private boolean audioEnabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        public int getExpireSeconds() { return expireSeconds; }
        public void setExpireSeconds(int expireSeconds) { this.expireSeconds = expireSeconds; }
        public String getChars() {
            if (chars != null) {
                return chars;
            }
            return includeLetters ? DIGITS_AND_LETTERS : DIGITS_ONLY;
        }
        public void setChars(String chars) { this.chars = chars; }
        public boolean isIncludeLetters() { return includeLetters; }
        public void setIncludeLetters(boolean includeLetters) { this.includeLetters = includeLetters; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        public boolean isAudioEnabled() { return audioEnabled; }
        public void setAudioEnabled(boolean audioEnabled) { this.audioEnabled = audioEnabled; }
    }

    public static class Web {
        private java.util.List<String> publicEndpoints = java.util.List.of(
                "/api/auth/login",
                "/api/auth/captcha", "/api/auth/captcha/audio/**",
                "/api/auth/refresh",
                "/api/auth/otp/verify",
                "/api/auth/cert/challenge", "/api/auth/cert/login", "/api/auth/cert/login-token"
        );

        public java.util.List<String> getPublicEndpoints() { return publicEndpoints; }
        public void setPublicEndpoints(java.util.List<String> publicEndpoints) { this.publicEndpoints = publicEndpoints; }
    }

    public static class CitizenCert {
        private boolean enabled = false;
        private int challengeExpireSeconds = 300;
        private boolean autoCreateUser = true;
        private java.util.List<String> defaultRoles = java.util.List.of("ROLE_USER");
        private java.util.List<String> intermediateCertPaths = java.util.List.of(
                "classpath:moica/MOICA2.cer", "classpath:moica/MOICA3.cer");
        private java.util.List<String> localCrlPaths = java.util.List.of(
                "classpath:moica/MOICA2.crl", "classpath:moica/MOICA3.crl");
        private boolean ocspEnabled = true;
        private boolean crlEnabled = true;
        private int crlCacheTtlHours = 1;
        /** OCSP 連線超時（毫秒） */
        private int ocspConnectTimeoutMs = 5000;
        /** OCSP 讀取超時（毫秒） */
        private int ocspReadTimeoutMs = 10000;
        /** CRL 連線超時（毫秒） */
        private int crlConnectTimeoutMs = 5000;
        /** CRL 讀取超時（毫秒） */
        private int crlReadTimeoutMs = 15000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getChallengeExpireSeconds() { return challengeExpireSeconds; }
        public void setChallengeExpireSeconds(int challengeExpireSeconds) { this.challengeExpireSeconds = challengeExpireSeconds; }
        public boolean isAutoCreateUser() { return autoCreateUser; }
        public void setAutoCreateUser(boolean autoCreateUser) { this.autoCreateUser = autoCreateUser; }
        public java.util.List<String> getDefaultRoles() { return defaultRoles; }
        public void setDefaultRoles(java.util.List<String> defaultRoles) { this.defaultRoles = defaultRoles; }
        public java.util.List<String> getIntermediateCertPaths() { return intermediateCertPaths; }
        public void setIntermediateCertPaths(java.util.List<String> intermediateCertPaths) { this.intermediateCertPaths = intermediateCertPaths; }
        public java.util.List<String> getLocalCrlPaths() { return localCrlPaths; }
        public void setLocalCrlPaths(java.util.List<String> localCrlPaths) { this.localCrlPaths = localCrlPaths; }
        public boolean isOcspEnabled() { return ocspEnabled; }
        public void setOcspEnabled(boolean ocspEnabled) { this.ocspEnabled = ocspEnabled; }
        public boolean isCrlEnabled() { return crlEnabled; }
        public void setCrlEnabled(boolean crlEnabled) { this.crlEnabled = crlEnabled; }
        public int getCrlCacheTtlHours() { return crlCacheTtlHours; }
        public void setCrlCacheTtlHours(int crlCacheTtlHours) { this.crlCacheTtlHours = crlCacheTtlHours; }
        public int getOcspConnectTimeoutMs() { return ocspConnectTimeoutMs; }
        public void setOcspConnectTimeoutMs(int ocspConnectTimeoutMs) { this.ocspConnectTimeoutMs = ocspConnectTimeoutMs; }
        public int getOcspReadTimeoutMs() { return ocspReadTimeoutMs; }
        public void setOcspReadTimeoutMs(int ocspReadTimeoutMs) { this.ocspReadTimeoutMs = ocspReadTimeoutMs; }
        public int getCrlConnectTimeoutMs() { return crlConnectTimeoutMs; }
        public void setCrlConnectTimeoutMs(int crlConnectTimeoutMs) { this.crlConnectTimeoutMs = crlConnectTimeoutMs; }
        public int getCrlReadTimeoutMs() { return crlReadTimeoutMs; }
        public void setCrlReadTimeoutMs(int crlReadTimeoutMs) { this.crlReadTimeoutMs = crlReadTimeoutMs; }
    }
}
