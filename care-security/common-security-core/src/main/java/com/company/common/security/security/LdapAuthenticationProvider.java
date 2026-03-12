package com.company.common.security.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;
import java.util.Optional;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * LDAP authentication provider that performs bind authentication against an LDAP server.
 * Returns user attributes (display name, email) on successful authentication.
 */
public class LdapAuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(LdapAuthenticationProvider.class);

    private final String url;
    private final String baseDn;
    private final String userSearchFilter;
    private final String bindDn;
    private final String bindPassword;
    private final String displayNameAttr;
    private final String emailAttr;

    public LdapAuthenticationProvider(String url,
                                      String baseDn,
                                      String userSearchFilter,
                                      String bindDn,
                                      String bindPassword,
                                      String displayNameAttr,
                                      String emailAttr) {
        this.url = url;
        this.baseDn = baseDn;
        this.userSearchFilter = userSearchFilter;
        this.bindDn = bindDn;
        this.bindPassword = bindPassword;
        this.displayNameAttr = displayNameAttr;
        this.emailAttr = emailAttr;
    }

    /**
     * Authenticate user against LDAP.
     * 1. Bind with service account to search for the user DN.
     * 2. Bind with the found user DN + password to verify credentials.
     *
     * @return LdapUserInfo if authentication succeeds, empty if user not found or bad credentials.
     */
    public Optional<LdapUserInfo> authenticate(String username, String password) {
        DirContext serviceCtx = null;
        DirContext userCtx = null;
        try {
            // Step 1: Bind with service account
            serviceCtx = createContext(bindDn, bindPassword);

            // Step 2: Search for the user
            String filter = userSearchFilter.replace("{0}", escapeFilter(username));
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[]{displayNameAttr, emailAttr});

            NamingEnumeration<SearchResult> results = serviceCtx.search(baseDn, filter, controls);
            if (!results.hasMore()) {
                log.debug("LDAP user not found: {}", username);
                return Optional.empty();
            }

            SearchResult result = results.next();
            String userDn = result.getNameInNamespace();
            Attributes attrs = result.getAttributes();

            String displayName = getAttrValue(attrs, displayNameAttr);
            String email = getAttrValue(attrs, emailAttr);

            // Step 3: Bind with user credentials to verify password
            userCtx = createContext(userDn, password);

            log.info("LDAP authentication successful for user: {}", username);
            return Optional.of(new LdapUserInfo(username, displayName, email));

        } catch (AuthenticationException e) {
            log.debug("LDAP authentication failed for user: {}", username);
            return Optional.empty();
        } catch (NamingException e) {
            log.error("LDAP error during authentication for user: {}", username, e);
            return Optional.empty();
        } finally {
            closeContext(serviceCtx);
            closeContext(userCtx);
        }
    }

    /**
     * Test LDAP connection with service account.
     */
    public boolean testConnection() {
        DirContext ctx = null;
        try {
            ctx = createContext(bindDn, bindPassword);
            return true;
        } catch (NamingException e) {
            log.error("LDAP connection test failed: {}", e.getMessage());
            return false;
        } finally {
            closeContext(ctx);
        }
    }

    private DirContext createContext(String principal, String credentials) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);

        if (url.startsWith("ldaps://")) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        // Connection timeout
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "10000");

        return new InitialDirContext(env);
    }

    private String getAttrValue(Attributes attrs, String attrName) throws NamingException {
        if (attrs.get(attrName) != null) {
            return (String) attrs.get(attrName).get();
        }
        return null;
    }

    private String escapeFilter(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void closeContext(DirContext ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException ignored) {
                // ignore
            }
        }
    }

    public record LdapUserInfo(String username, String displayName, String email) {}
}
