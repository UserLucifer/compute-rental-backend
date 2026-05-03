package com.compute.rental.modules.order.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("api_credential")
public class ApiCredential {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("credential_no")
    private String credentialNo;

    @TableField("user_id")
    private Long userId;

    @TableField("rental_order_id")
    private Long rentalOrderId;

    @TableField("api_name")
    private String apiName;

    @TableField("api_base_url")
    private String apiBaseUrl;

    @TableField("token_ciphertext")
    private String tokenCiphertext;

    @TableField("token_masked")
    private String tokenMasked;

    @TableField("model_name_snapshot")
    private String modelNameSnapshot;

    @TableField("deploy_fee_snapshot")
    private BigDecimal deployFeeSnapshot;

    @TableField("token_status")
    private String tokenStatus;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    @TableField("activation_paid_at")
    private LocalDateTime activationPaidAt;

    @TableField("activated_at")
    private LocalDateTime activatedAt;

    @TableField("auto_pause_at")
    private LocalDateTime autoPauseAt;

    @TableField("paused_at")
    private LocalDateTime pausedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("expired_at")
    private LocalDateTime expiredAt;

    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("mock_request_count")
    private Long mockRequestCount;

    @TableField("mock_token_display")
    private Long mockTokenDisplay;

    @TableField("mock_last_refresh_at")
    private LocalDateTime mockLastRefreshAt;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
