package com.compute.rental.common.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(0, "成功", HttpStatus.OK),
    BAD_REQUEST(400, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "无权限访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "资源不存在", HttpStatus.NOT_FOUND),
    VALIDATION_FAILED(422, "参数校验失败", HttpStatus.UNPROCESSABLE_ENTITY),
    SYSTEM_ERROR(500, "系统异常，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),

    BUSINESS_ERROR(10000, "业务处理失败", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(10001, "余额不足", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_CONFLICT(10002, "重复操作", HttpStatus.CONFLICT),
    TOO_MANY_REQUESTS(10003, "请求过于频繁", HttpStatus.TOO_MANY_REQUESTS),
    REQUEST_BODY_INVALID(10004, "请求体格式错误", HttpStatus.BAD_REQUEST),
    REQUEST_PARAMETER_MISSING(10005, "缺少必填参数", HttpStatus.BAD_REQUEST),
    REQUEST_PARAMETER_TYPE_MISMATCH(10006, "请求参数格式错误", HttpStatus.BAD_REQUEST),

    WALLET_NOT_FOUND(11001, "钱包不存在", HttpStatus.NOT_FOUND),
    WALLET_DISABLED(11002, "钱包已禁用", HttpStatus.FORBIDDEN),
    INVALID_AMOUNT(11003, "金额无效", HttpStatus.BAD_REQUEST),
    CONCURRENT_UPDATE_FAILED(11004, "数据状态已变化，请刷新后重试", HttpStatus.CONFLICT),
    WALLET_BUSINESS_REFERENCE_REQUIRED(11005, "钱包业务类型和业务订单号不能为空", HttpStatus.BAD_REQUEST),
    WALLET_IDEMPOTENCY_KEY_DUPLICATE(11006, "钱包幂等键重复", HttpStatus.CONFLICT),

    EMAIL_ALREADY_REGISTERED(20001, "邮箱已注册", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_REGISTERED(20002, "邮箱未注册", HttpStatus.BAD_REQUEST),
    INVALID_LOGIN_CREDENTIALS(20003, "邮箱或密码错误", HttpStatus.UNAUTHORIZED),
    USER_DISABLED(20004, "用户已禁用", HttpStatus.FORBIDDEN),
    EMAIL_CODE_INVALID_OR_EXPIRED(20005, "邮箱验证码无效或已过期", HttpStatus.BAD_REQUEST),
    EMAIL_CODE_SEND_TOO_FREQUENTLY(20006, "邮箱验证码发送过于频繁", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_CODE_ATTEMPTS_EXCEEDED(20007, "邮箱验证码尝试次数过多", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_CODE_RATE_LIMIT_UNAVAILABLE(20008, "邮箱验证码限流不可用", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_CODE_ATTEMPT_LIMIT_UNAVAILABLE(20009, "邮箱验证码尝试限制不可用", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INVITE_CODE(20010, "邀请码无效", HttpStatus.BAD_REQUEST),
    LOGIN_TOKEN_INVALID(20011, "登录令牌无效", HttpStatus.UNAUTHORIZED),
    LOGIN_REQUIRED(20012, "请先登录", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND(20013, "用户不存在", HttpStatus.NOT_FOUND),

    REGION_NOT_FOUND(30001, "地区不存在", HttpStatus.NOT_FOUND),
    GPU_MODEL_NOT_FOUND(30002, "GPU 型号不存在", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(30003, "产品不存在", HttpStatus.NOT_FOUND),
    AI_MODEL_NOT_FOUND(30004, "AI 模型不存在", HttpStatus.NOT_FOUND),
    RENTAL_CYCLE_RULE_NOT_FOUND(30005, "租赁周期规则不存在", HttpStatus.NOT_FOUND),
    PRODUCT_REGION_DISABLED(30006, "产品所属地区不可用", HttpStatus.BAD_REQUEST),
    PRODUCT_GPU_DISABLED(30007, "产品 GPU 型号不可用", HttpStatus.BAD_REQUEST),

    BLOG_POST_NOT_FOUND(31001, "博客文章不存在", HttpStatus.NOT_FOUND),
    BLOG_CATEGORY_NOT_FOUND(31002, "博客分类不存在", HttpStatus.NOT_FOUND),
    BLOG_TAG_NOT_FOUND(31003, "博客标签不存在", HttpStatus.NOT_FOUND),

    DOC_CATEGORY_HAS_CHILDREN(32001, "文档分类存在子分类，不能删除", HttpStatus.BAD_REQUEST),
    DOC_CATEGORY_HAS_ARTICLES(32002, "文档分类存在文章，不能删除", HttpStatus.BAD_REQUEST),
    DOC_ARTICLE_NOT_FOUND(32003, "文档文章不存在", HttpStatus.NOT_FOUND),
    DOC_CATEGORY_NOT_FOUND(32004, "文档分类不存在", HttpStatus.NOT_FOUND),
    DOC_CATEGORY_PARENT_CYCLE(32005, "文档分类父级不能形成循环", HttpStatus.BAD_REQUEST),
    DOC_CATEGORY_CODE_EXISTS(32006, "文档分类编码已存在", HttpStatus.CONFLICT),
    DOC_ARTICLE_SLUG_EXISTS(32007, "文档文章 slug 已存在", HttpStatus.CONFLICT),
    DOC_STATUS_INVALID(32008, "状态无效", HttpStatus.BAD_REQUEST),
    DOC_PUBLISH_STATUS_INVALID(32009, "发布状态无效", HttpStatus.BAD_REQUEST),
    DOC_FIELD_REQUIRED(32010, "缺少必填字段", HttpStatus.BAD_REQUEST),

    RENTAL_ORDER_NOT_FOUND(40001, "租赁订单不存在", HttpStatus.NOT_FOUND),
    RENTAL_ORDER_NOT_PAYABLE(40002, "仅待支付租赁订单可支付", HttpStatus.BAD_REQUEST),
    RENTAL_ORDER_NOT_CANCELABLE(40003, "当前租赁订单状态不可取消", HttpStatus.BAD_REQUEST),
    RENTAL_ORDER_DEPLOY_FEE_NOT_PAYABLE(40004, "仅待激活租赁订单可支付部署费用", HttpStatus.BAD_REQUEST),
    RENTAL_ORDER_NOT_STARTABLE(40005, "仅已暂停租赁订单可启动", HttpStatus.BAD_REQUEST),
    RENTAL_ORDER_NOT_SETTLEABLE(40006, "仅运行中或已暂停租赁订单可提前结算", HttpStatus.BAD_REQUEST),
    RENTAL_ORDER_PROCESSING(40007, "租赁订单正在处理，请稍后重试", HttpStatus.CONFLICT),

    API_CREDENTIAL_NOT_FOUND(41001, "API 凭证不存在", HttpStatus.NOT_FOUND),
    API_CREDENTIAL_NOT_GENERATED(41002, "仅已生成的 API 凭证可激活", HttpStatus.BAD_REQUEST),
    API_CREDENTIAL_NOT_PAUSED(41003, "仅已暂停 API 凭证可启动", HttpStatus.BAD_REQUEST),
    API_CREDENTIAL_NOT_ACTIVE(41004, "API 凭证未处于激活中", HttpStatus.BAD_REQUEST),
    API_CREDENTIAL_ALREADY_EXISTS(41005, "API 凭证已存在", HttpStatus.CONFLICT),
    API_DEPLOY_ORDER_NOT_FOUND(41101, "API 部署订单不存在", HttpStatus.NOT_FOUND),
    API_DEPLOY_ORDER_ALREADY_EXISTS(41102, "API 部署订单已存在", HttpStatus.CONFLICT),
    API_DEPLOY_ORDER_STATUS_CHANGED(41103, "API 部署订单状态已变化", HttpStatus.CONFLICT),
    API_CREDENTIAL_STATUS_CHANGED(41104, "API 凭证状态已变化", HttpStatus.CONFLICT),
    API_TOKEN_PLAINTEXT_REQUIRED(41201, "API Token 明文不能为空", HttpStatus.BAD_REQUEST),
    API_TOKEN_ENCRYPT_FAILED(41202, "API Token 加密失败", HttpStatus.INTERNAL_SERVER_ERROR),
    API_TOKEN_SECRET_NOT_CONFIGURED(41203, "API Token 加密密钥未配置", HttpStatus.INTERNAL_SERVER_ERROR),

    PROFIT_RECORD_NOT_FOUND(42001, "收益记录不存在", HttpStatus.NOT_FOUND),
    SETTLEMENT_ORDER_NOT_FOUND(43001, "结算订单不存在", HttpStatus.NOT_FOUND),
    COMMISSION_RECORD_NOT_FOUND(44001, "佣金记录不存在", HttpStatus.NOT_FOUND),

    RECHARGE_CHANNEL_CODE_EXISTS(60001, "充值渠道编码重复", HttpStatus.CONFLICT),
    RECHARGE_CHANNEL_IN_USE(60002, "充值渠道已有订单引用，不能删除", HttpStatus.BAD_REQUEST),
    RECHARGE_AMOUNT_INVALID(60003, "充值金额必须大于 0", HttpStatus.BAD_REQUEST),
    RECHARGE_AMOUNT_BELOW_MIN(60004, "充值金额低于最低金额", HttpStatus.BAD_REQUEST),
    RECHARGE_ORDER_DUPLICATE(60005, "充值订单或外部交易号重复", HttpStatus.CONFLICT),
    RECHARGE_ORDER_NOT_CANCELABLE(60006, "仅已提交充值订单可取消", HttpStatus.BAD_REQUEST),
    RECHARGE_ORDER_NOT_APPROVABLE(60007, "仅已提交充值订单可审核通过", HttpStatus.BAD_REQUEST),
    RECHARGE_ORDER_NOT_REJECTABLE(60008, "仅已提交充值订单可驳回", HttpStatus.BAD_REQUEST),
    RECHARGE_ORDER_STATUS_CHANGED(60009, "充值订单状态已变化", HttpStatus.CONFLICT),
    RECHARGE_CHANNEL_DISABLED(60010, "充值渠道不可用", HttpStatus.BAD_REQUEST),
    RECHARGE_CHANNEL_NOT_FOUND(60011, "充值渠道不存在", HttpStatus.NOT_FOUND),
    RECHARGE_EXTERNAL_TX_NO_EXISTS(60012, "外部交易号重复", HttpStatus.CONFLICT),
    RECHARGE_ORDER_NOT_FOUND(60013, "充值订单不存在", HttpStatus.NOT_FOUND),
    RECHARGE_AMOUNT_RANGE_INVALID(60014, "最小充值金额不能大于最大充值金额", HttpStatus.BAD_REQUEST),
    RECHARGE_ORDER_PROCESSING(60015, "充值订单正在处理，请稍后重试", HttpStatus.CONFLICT),

    WITHDRAW_ADDRESS_INVALID(70001, "提现地址无效", HttpStatus.BAD_REQUEST),
    WITHDRAW_AMOUNT_BELOW_MIN(70002, "提现金额低于最低金额", HttpStatus.BAD_REQUEST),
    WITHDRAW_DAILY_LIMIT_EXCEEDED(70003, "已超过每日提现限额", HttpStatus.BAD_REQUEST),
    WITHDRAW_ORDER_NOT_CANCELABLE(70004, "仅待处理提现订单可取消", HttpStatus.BAD_REQUEST),
    WITHDRAW_ORDER_NOT_APPROVABLE(70005, "仅待处理提现订单可审核通过", HttpStatus.BAD_REQUEST),
    WITHDRAW_ORDER_NOT_REJECTABLE(70006, "仅待处理或已审核提现订单可驳回", HttpStatus.BAD_REQUEST),
    WITHDRAW_ORDER_NOT_PAYABLE(70007, "仅已审核提现订单可标记打款", HttpStatus.BAD_REQUEST),
    WITHDRAW_ORDER_STATUS_CHANGED(70008, "提现订单状态已变化", HttpStatus.CONFLICT),
    WITHDRAW_ORDER_NOT_FOUND(70009, "提现订单不存在", HttpStatus.NOT_FOUND),
    WITHDRAW_ORDER_PROCESSING(70010, "提现订单正在处理，请稍后重试", HttpStatus.CONFLICT),

    ADMIN_BAD_CREDENTIALS(80001, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ADMIN_DISABLED(80002, "管理员账号已禁用", HttpStatus.FORBIDDEN),
    ADMIN_USERNAME_EXISTS(80003, "用户名已存在", HttpStatus.BAD_REQUEST),
    ADMIN_NOT_FOUND(80004, "管理员不存在", HttpStatus.UNAUTHORIZED),
    ADMIN_TOKEN_REQUIRED(80005, "需要管理员令牌", HttpStatus.FORBIDDEN),
    SYS_CONFIG_MISSING(80010, "缺少系统配置", HttpStatus.INTERNAL_SERVER_ERROR),
    SYS_CONFIG_NOT_FOUND(80011, "系统配置不存在", HttpStatus.NOT_FOUND),
    SYS_CONFIG_UPDATE_FAILED(80012, "系统配置更新失败", HttpStatus.CONFLICT),
    ADMIN_LOG_NOT_FOUND(80013, "管理员日志不存在", HttpStatus.NOT_FOUND),
    NOTIFICATION_NOT_FOUND(80020, "通知不存在", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
