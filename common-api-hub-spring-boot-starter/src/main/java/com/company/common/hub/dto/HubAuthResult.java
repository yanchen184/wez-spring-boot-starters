package com.company.common.hub.dto;

import com.company.common.hub.entity.HubSet;
import com.company.common.hub.entity.HubUser;
import com.company.common.hub.entity.HubUserSet;
import lombok.Getter;

/**
 * 認證結果（不可變物件）。
 *
 * <p>認證成功後封裝 hubUser、hubSet、hubUserSet，
 * 供 Token 簽發及日誌記錄使用。
 */
@Getter
public class HubAuthResult {

    private final HubUser hubUser;
    private final HubSet hubSet;
    private final HubUserSet hubUserSet;

    public HubAuthResult(HubUser hubUser, HubSet hubSet, HubUserSet hubUserSet) {
        this.hubUser = hubUser;
        this.hubSet = hubSet;
        this.hubUserSet = hubUserSet;
    }
}
