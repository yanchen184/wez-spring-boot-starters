package com.company.common.hub.controller;

import com.company.common.hub.dto.HubResponse;
import com.company.common.hub.dto.HubResponseCode;
import com.company.common.hub.entity.HubLog;
import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import com.company.common.hub.repository.HubLogRepository;
import com.company.common.hub.repository.HubSetRepository;
import com.company.common.hub.repository.HubUserRepository;
import com.company.common.hub.repository.HubUserSetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hub 管理端點（CRUD）。
 *
 * <ul>
 *   <li>{@code /api/hub/admin/api-sets} — HubSet CRUD</li>
 *   <li>{@code /api/hub/admin/users} — HubUser CRUD</li>
 *   <li>{@code /api/hub/admin/user-sets} — HubUserSet CRUD</li>
 *   <li>{@code /api/hub/admin/logs} — 分頁查詢日誌</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/hub/admin")
public class HubAdminController {

    private final HubSetRepository hubSetRepository;
    private final HubUserRepository hubUserRepository;
    private final HubUserSetRepository hubUserSetRepository;
    private final HubLogRepository hubLogRepository;
    private final PasswordEncoder passwordEncoder;

    public HubAdminController(HubSetRepository hubSetRepository,
                               HubUserRepository hubUserRepository,
                               HubUserSetRepository hubUserSetRepository,
                               HubLogRepository hubLogRepository,
                               PasswordEncoder passwordEncoder) {
        this.hubSetRepository = hubSetRepository;
        this.hubUserRepository = hubUserRepository;
        this.hubUserSetRepository = hubUserSetRepository;
        this.hubLogRepository = hubLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========== HubSet CRUD ==========

    @GetMapping("/api-sets")
    public ResponseEntity<HubResponse<List<HubSet>>> listApiSets() {
        List<HubSet> sets = hubSetRepository.findAll();
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "查詢成功", sets));
    }

    @PostMapping("/api-sets")
    public ResponseEntity<HubResponse<HubSet>> createApiSet(@RequestBody HubSet hubSet) {
        HubSet saved = hubSetRepository.save(hubSet);
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "建立成功", saved));
    }

    @PutMapping("/api-sets/{id}")
    public ResponseEntity<HubResponse<HubSet>> updateApiSet(@PathVariable Long id,
                                                              @RequestBody HubSet hubSet) {
        return hubSetRepository.findById(id)
                .map(existing -> {
                    existing.setName(hubSet.getName());
                    existing.setUri(hubSet.getUri());
                    existing.setJwtTokenAging(hubSet.getJwtTokenAging());
                    existing.setEnabled(hubSet.getEnabled());
                    existing.setDescription(hubSet.getDescription());
                    HubSet updated = hubSetRepository.save(existing);
                    return ResponseEntity.ok(
                            HubResponse.success(HubResponseCode.SUCCESS, "更新成功", updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api-sets/{id}")
    public ResponseEntity<HubResponse<Void>> deleteApiSet(@PathVariable Long id) {
        if (!hubSetRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hubSetRepository.deleteById(id);
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "刪除成功"));
    }

    // ========== HubUser CRUD ==========

    @GetMapping("/users")
    public ResponseEntity<HubResponse<List<HubUser>>> listUsers() {
        List<HubUser> users = hubUserRepository.findAll();
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "查詢成功", users));
    }

    @PostMapping("/users")
    public ResponseEntity<HubResponse<HubUser>> createUser(@RequestBody HubUser hubUser) {
        hubUser.setPassword(passwordEncoder.encode(hubUser.getPassword()));
        HubUser saved = hubUserRepository.save(hubUser);
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "建立成功", saved));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<HubResponse<HubUser>> updateUser(@PathVariable Long id,
                                                             @RequestBody HubUser hubUser) {
        return hubUserRepository.findById(id)
                .map(existing -> {
                    existing.setUsername(hubUser.getUsername());
                    existing.setOrgId(hubUser.getOrgId());
                    existing.setVerifyIp(hubUser.getVerifyIp());
                    existing.setEnabled(hubUser.getEnabled());
                    // 更新時不傳 password → 保留原密碼
                    if (hubUser.getPassword() != null && !hubUser.getPassword().isBlank()) {
                        existing.setPassword(passwordEncoder.encode(hubUser.getPassword()));
                    }
                    HubUser updated = hubUserRepository.save(existing);
                    return ResponseEntity.ok(
                            HubResponse.success(HubResponseCode.SUCCESS, "更新成功", updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<HubResponse<Void>> deleteUser(@PathVariable Long id) {
        if (!hubUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hubUserRepository.deleteById(id);
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "刪除成功"));
    }

    // ========== HubUserSet CRUD ==========

    @GetMapping("/user-sets")
    public ResponseEntity<HubResponse<List<HubUserSet>>> listUserSets() {
        List<HubUserSet> userSets = hubUserSetRepository.findAll();
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "查詢成功", userSets));
    }

    @PostMapping("/user-sets")
    public ResponseEntity<HubResponse<HubUserSet>> createUserSet(@RequestBody HubUserSet hubUserSet) {
        HubUserSet saved = hubUserSetRepository.save(hubUserSet);
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "建立成功", saved));
    }

    @PutMapping("/user-sets/{id}")
    public ResponseEntity<HubResponse<HubUserSet>> updateUserSet(@PathVariable Long id,
                                                                   @RequestBody HubUserSet hubUserSet) {
        return hubUserSetRepository.findById(id)
                .map(existing -> {
                    existing.setHubSet(hubUserSet.getHubSet());
                    existing.setHubUser(hubUserSet.getHubUser());
                    existing.setVerifyDts(hubUserSet.getVerifyDts());
                    existing.setVerifyDte(hubUserSet.getVerifyDte());
                    existing.setUserVerify(hubUserSet.getUserVerify());
                    existing.setJwtTokenVerify(hubUserSet.getJwtTokenVerify());
                    existing.setEnabled(hubUserSet.getEnabled());
                    HubUserSet updated = hubUserSetRepository.save(existing);
                    return ResponseEntity.ok(
                            HubResponse.success(HubResponseCode.SUCCESS, "更新成功", updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/user-sets/{id}")
    public ResponseEntity<HubResponse<Void>> deleteUserSet(@PathVariable Long id) {
        if (!hubUserSetRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hubUserSetRepository.deleteById(id);
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "刪除成功"));
    }

    // ========== HubLog 查詢 ==========

    @GetMapping("/logs")
    public ResponseEntity<HubResponse<Page<HubLog>>> listLogs(Pageable pageable) {
        Page<HubLog> logs = hubLogRepository.findAll(pageable);
        return ResponseEntity.ok(HubResponse.success(HubResponseCode.SUCCESS, "查詢成功", logs));
    }
}
