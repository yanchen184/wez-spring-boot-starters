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
        private boolean enabled = false;
        private int length = 4;
        private int expireSeconds = 300;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        public int getExpireSeconds() { return expireSeconds; }
        public void setExpireSeconds(int expireSeconds) { this.expireSeconds = expireSeconds; }
    }

    public static class CitizenCert {
        private boolean enabled = false;
        private int challengeExpireSeconds = 300;
        private boolean autoCreateUser = true;
        private java.util.List<String> defaultRoles = java.util.List.of("ROLE_USER");
        private java.util.List<String> intermediateCertPaths = java.util.List.of(
                "classpath:moica/MOICA2.cer", "classpath:moica/MOICA3.cer");
        private java.util.List<String> localCrlPaths = java.util.List.of(
                "file:/opt/moica/MOICA2-complete.crl", "file:/opt/moica/MOICA3-complete.crl");
        private boolean ocspEnabled = true;
        private boolean crlEnabled = true;
        private int crlCacheTtlHours = 1;

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
    }
}
