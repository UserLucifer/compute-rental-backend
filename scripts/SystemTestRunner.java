import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SystemTestRunner {

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final String SIGNUP_CODE = "123456";
    private static final String TEST_PASSWORD = "Test123456";
    private static final BigDecimal RECHARGE_AMOUNT = new BigDecimal("1000.00000000");

    private final String baseUrl = env("SYSTEM_TEST_BASE_URL", "http://localhost:8080");
    private final String runId = LocalDateTime.now().toString()
            .replace("-", "")
            .replace(":", "")
            .replace(".", "")
            .substring(0, 14);
    private final List<StepResult> results = new ArrayList<>();
    private Connection connection;
    private String adminToken;
    private TestUser userA;
    private TestUser userB;
    private TestUser userC;
    private Catalog catalog;
    private String reportPath;

    public static void main(String[] args) throws Exception {
        var runner = new SystemTestRunner();
        try {
            runner.run();
        } finally {
            runner.writeReport();
            if (runner.connection != null) {
                runner.connection.close();
            }
        }
    }

    private void run() throws Exception {
        reportPath = Path.of("target", "system-test-report-" + runId + ".html").toAbsolutePath().toString();
        step("Backend health", "GET /api/system/health returns UP", () -> {
            var data = get("/api/system/health", null);
            require("UP".equals(data.path("status").asText()), "health status is not UP: " + data);
            return "status=" + data.path("status").asText();
        });

        step("Connect test database", "application-local.yml datasource can be opened", () -> {
            var cfg = readDatasourceConfig();
            connection = DriverManager.getConnection(cfg.url(), cfg.username(), cfg.password());
            return "database=" + connection.getCatalog();
        });

        step("Validate initialized product data", "Enabled products and AI models have required pricing/token fields", () -> {
            var blankProducts = queryLong("""
                    SELECT COUNT(*)
                    FROM product
                    WHERE status = 1
                      AND (gpu_power_tops IS NULL OR token_output_per_minute IS NULL
                           OR token_output_per_day IS NULL OR rent_price IS NULL)
                    """);
            var blankModels = queryLong("""
                    SELECT COUNT(*)
                    FROM ai_model
                    WHERE status = 1
                      AND (monthly_token_consumption_trillion IS NULL OR token_unit_price IS NULL
                           OR deploy_tech_fee IS NULL OR deploy_tech_fee <= 0)
                    """);
            require(blankProducts == 0, "enabled products with blank fields=" + blankProducts);
            require(blankModels == 0, "enabled AI models with blank fields=" + blankModels);
            return "blankProducts=0, blankModels=0";
        });

        step("Admin login", "Admin account can log in and receive adminAccessToken", () -> {
            var data = post("/api/admin/auth/login", null, mapOf(
                    "userName", "admin",
                    "password", "admin123"
            ));
            adminToken = data.path("adminAccessToken").asText();
            requireText(adminToken, "adminAccessToken");
            return "admin=" + data.at("/admin/userName").asText();
        });

        step("Select rentable catalog", "Pick one enabled product, AI model, and cycle rule from DB", () -> {
            catalog = loadCatalog();
            return catalog.productName + " / " + catalog.aiModelName + " / " + catalog.cycleCode;
        });

        step("Register user A", "Signup creates user, wallet, and invite code", () -> {
            userA = registerUser("A", null);
            return userA.email + ", invite=" + userA.inviteCode;
        });

        step("Register user B through A", "Signup with A invite creates level-1 team relation", () -> {
            userB = registerUser("B", userA.inviteCode);
            var count = queryLong("""
                    SELECT COUNT(*)
                    FROM user_team_relation
                    WHERE ancestor_user_id = ? AND descendant_user_id = ? AND level_depth = 1
                    """, userA.id, userB.id);
            require(count == 1, "A->B team relation count=" + count);
            return userB.email + ", parent=A";
        });

        step("Register user C through B", "Signup with B invite creates B level-1 and A level-2 relations", () -> {
            userC = registerUser("C", userB.inviteCode);
            var bToC = queryLong("""
                    SELECT COUNT(*)
                    FROM user_team_relation
                    WHERE ancestor_user_id = ? AND descendant_user_id = ? AND level_depth = 1
                    """, userB.id, userC.id);
            var aToC = queryLong("""
                    SELECT COUNT(*)
                    FROM user_team_relation
                    WHERE ancestor_user_id = ? AND descendant_user_id = ? AND level_depth = 2
                    """, userA.id, userC.id);
            require(bToC == 1, "B->C team relation count=" + bToC);
            require(aToC == 1, "A->C team relation count=" + aToC);
            return userC.email + ", parent=B, grandparent=A";
        });

        long channelId = createRechargeChannel();
        String rechargeNo = createAndApproveRecharge(channelId);

        step("User wallet after recharge", "C wallet available balance increases by approved amount", () -> {
            var wallet = get("/api/wallet/me", userC.token);
            var balance = decimal(wallet, "availableBalance");
            require(balance.compareTo(new BigDecimal("900")) > 0, "availableBalance too low: " + balance);
            return "rechargeNo=" + rechargeNo + ", availableBalance=" + balance;
        });

        var expireOrderNo = createRunningOrder("expire");
        testAutoPauseAndResume(expireOrderNo);
        testDailyProfitAndCommission(expireOrderNo);
        testExpireSettlement(expireOrderNo);

        var earlyOrderNo = createRunningOrder("early");
        testEarlySettlement(earlyOrderNo);

        var timeoutOrderNo = createPendingActivationOrder();
        testActivationTimeoutCancel(timeoutOrderNo);

        testUserBackofficeViews(expireOrderNo);
        testAdminBackofficeViews(expireOrderNo);
    }

    private TestUser registerUser(String label, String inviteCode) throws Exception {
        var suffix = label.toLowerCase(Locale.ROOT) + "-" + runId;
        var email = "system-test-" + suffix + "@example.com";
        insertSignupCode(email);
        var data = post("/api/auth/signup", null, mapOf(
                "email", email,
                "code", SIGNUP_CODE,
                "userName", "SysTest" + label + "_" + runId,
                "password", TEST_PASSWORD,
                "inviteCode", inviteCode
        ));
        var token = data.path("accessToken").asText();
        var id = data.at("/user/id").asLong();
        require(id > 0, "registered user id is empty");
        requireText(token, "accessToken");
        var walletCount = queryLong("SELECT COUNT(*) FROM user_wallet WHERE user_id = ?", id);
        require(walletCount == 1, "wallet count=" + walletCount);
        var ownInvite = queryString("SELECT invite_code FROM user_referral_relation WHERE user_id = ?", id);
        requireText(ownInvite, "invite_code");
        return new TestUser(id, email, token, ownInvite);
    }

    private void insertSignupCode(String email) throws Exception {
        var normalized = email.trim().toLowerCase(Locale.ROOT);
        var hash = sha256(normalized + ":SIGNUP:" + SIGNUP_CODE);
        update("""
                INSERT INTO email_verify_code(email, scene, code_hash, send_ip, expire_at, status, created_at)
                VALUES (?, 'SIGNUP', ?, '127.0.0.1', DATE_ADD(NOW(), INTERVAL 10 MINUTE), 0, NOW())
                """, normalized, hash);
    }

    private long createRechargeChannel() throws Exception {
        final long[] channelId = new long[1];
        step("Admin create recharge channel", "Admin creates an enabled test-only recharge channel", () -> {
            var data = post("/api/admin/recharge/channels", adminToken, mapOf(
                    "channelCode", "SYS_TEST_" + runId,
                    "channelName", "System Test USDT",
                    "network", "TRC20",
                    "displayUrl", "https://example.invalid/pay/" + runId,
                    "accountName", "System Test",
                    "accountNo", "T" + runId,
                    "minAmount", new BigDecimal("500.00000000"),
                    "maxAmount", new BigDecimal("10000.00000000"),
                    "feeRate", BigDecimal.ZERO,
                    "sortNo", 1,
                    "status", 1
            ));
            channelId[0] = data.path("channelId").asLong();
            require(channelId[0] > 0, "channelId is empty");
            return "channelId=" + channelId[0];
        });

        step("User recharge channel list", "Enabled test channel is visible to authenticated users", () -> {
            var data = get("/api/recharge/channels", userC.token);
            require(containsId(data, "channelId", channelId[0]), "channel not found in list: " + data);
            return "channel visible";
        });
        return channelId[0];
    }

    private String createAndApproveRecharge(long channelId) throws Exception {
        final String[] rechargeNo = new String[1];
        step("User submit recharge order", "C submits a 1000 USDT recharge order", () -> {
            var data = post("/api/recharge/orders", userC.token, mapOf(
                    "channelId", channelId,
                    "applyAmount", RECHARGE_AMOUNT,
                    "externalTxNo", "TX-" + runId,
                    "paymentProofUrl", "https://example.invalid/proof/" + runId,
                    "userRemark", "system test recharge"
            ));
            rechargeNo[0] = data.path("rechargeNo").asText();
            requireText(rechargeNo[0], "rechargeNo");
            require("SUBMITTED".equals(data.path("status").asText()), "recharge status=" + data.path("status").asText());
            return "rechargeNo=" + rechargeNo[0];
        });

        step("Admin approve recharge order", "Admin approval credits C wallet", () -> {
            var data = post("/api/admin/recharge/orders/" + enc(rechargeNo[0]) + "/approve", adminToken, mapOf(
                    "actualAmount", RECHARGE_AMOUNT,
                    "reviewRemark", "system test approved"
            ));
            require("APPROVED".equals(data.path("status").asText()), "recharge status=" + data.path("status").asText());
            requireText(data.path("walletTxNo").asText(), "walletTxNo");
            return "walletTxNo=" + data.path("walletTxNo").asText();
        });
        return rechargeNo[0];
    }

    private String createRunningOrder(String purpose) throws Exception {
        var orderNo = createOrder(purpose);
        step("Pay machine fee for " + purpose + " order", "Order enters PENDING_ACTIVATION and API credential is generated", () -> {
            var data = post("/api/rental/orders/" + enc(orderNo) + "/pay", userC.token, null);
            require("PENDING_ACTIVATION".equals(data.path("orderStatus").asText()),
                    "orderStatus=" + data.path("orderStatus").asText());
            require("GENERATED".equals(data.at("/apiCredential/tokenStatus").asText()),
                    "tokenStatus=" + data.at("/apiCredential/tokenStatus").asText());
            return "orderNo=" + orderNo + ", status=PENDING_ACTIVATION";
        });

        step("Pay deploy fee for " + purpose + " order", "Deploy fee payment activates API and starts rental", () -> {
            var data = post("/api/rental/orders/" + enc(orderNo) + "/deploy/pay", userC.token, null);
            require("PAID".equals(data.path("status").asText()), "deploy status=" + data.path("status").asText());
            return "deployNo=" + data.path("deployNo").asText();
        });

        step("Verify running " + purpose + " order", "Order detail is RUNNING with ACTIVE API credential", () -> {
            var data = get("/api/rental/orders/" + enc(orderNo), userC.token);
            require("RUNNING".equals(data.path("orderStatus").asText()), "orderStatus=" + data.path("orderStatus").asText());
            require("ACTIVE".equals(data.at("/apiCredential/tokenStatus").asText()),
                    "tokenStatus=" + data.at("/apiCredential/tokenStatus").asText());
            return "profitEndAt=" + data.path("profitEndAt").asText();
        });
        return orderNo;
    }

    private String createPendingActivationOrder() throws Exception {
        var orderNo = createOrder("activation-timeout");
        step("Pay machine fee for activation-timeout order", "Order remains PENDING_ACTIVATION before deploy fee", () -> {
            var data = post("/api/rental/orders/" + enc(orderNo) + "/pay", userC.token, null);
            require("PENDING_ACTIVATION".equals(data.path("orderStatus").asText()),
                    "orderStatus=" + data.path("orderStatus").asText());
            return "orderNo=" + orderNo;
        });
        return orderNo;
    }

    private String createOrder(String purpose) throws Exception {
        final String[] orderNo = new String[1];
        step("Estimate " + purpose + " rental order", "Estimate endpoint calculates machine and deploy fee", () -> {
            var data = post("/api/rental/estimate", null, mapOf(
                    "productId", catalog.productId,
                    "aiModelId", catalog.aiModelId,
                    "cycleRuleId", catalog.cycleRuleId,
                    "language", "zh-CN"
            ));
            require(decimal(data, "rentPrice").compareTo(BigDecimal.ZERO) > 0, "rentPrice must be positive");
            require(decimal(data, "deployTechFee").compareTo(BigDecimal.ZERO) > 0, "deployTechFee must be positive");
            return "rentPrice=" + data.path("rentPrice").asText()
                    + ", deployTechFee=" + data.path("deployTechFee").asText();
        });

        step("Create " + purpose + " rental order", "C creates a PENDING_PAY rental order", () -> {
            var data = post("/api/rental/orders", userC.token, mapOf(
                    "productId", catalog.productId,
                    "aiModelId", catalog.aiModelId,
                    "cycleRuleId", catalog.cycleRuleId
            ));
            orderNo[0] = data.path("orderNo").asText();
            requireText(orderNo[0], "orderNo");
            require("PENDING_PAY".equals(data.path("orderStatus").asText()),
                    "orderStatus=" + data.path("orderStatus").asText());
            return "orderNo=" + orderNo[0];
        });
        return orderNo[0];
    }

    private void testAutoPauseAndResume(String orderNo) throws Exception {
        step("Force auto-pause due time", "Set auto_pause_at to the past for this order only", () -> {
            var updated = update("""
                    UPDATE rental_order ro
                    LEFT JOIN api_credential ac ON ac.rental_order_id = ro.id
                    SET ro.auto_pause_at = DATE_SUB(NOW(), INTERVAL 1 MINUTE),
                        ro.updated_at = NOW(),
                        ac.auto_pause_at = DATE_SUB(NOW(), INTERVAL 1 MINUTE),
                        ac.updated_at = NOW()
                    WHERE ro.order_no = ?
                    """, orderNo);
            require(updated >= 1, "updated rows=" + updated);
            return "auto_pause_at moved to past";
        });

        step("Run auto-pause scheduler", "Scheduler pauses due RUNNING order and API token", () -> {
            var data = runScheduler("/api/admin/scheduler/auto-pause/run");
            require(data.path("successCount").asInt() >= 1, "scheduler result=" + data);
            var detail = get("/api/rental/orders/" + enc(orderNo), userC.token);
            require("PAUSED".equals(detail.path("orderStatus").asText()), "orderStatus=" + detail.path("orderStatus").asText());
            require("PAUSED".equals(detail.at("/apiCredential/tokenStatus").asText()),
                    "tokenStatus=" + detail.at("/apiCredential/tokenStatus").asText());
            return "successCount=" + data.path("successCount").asInt();
        });

        step("User resumes paused rental", "Start endpoint moves order back to RUNNING", () -> {
            var data = post("/api/rental/orders/" + enc(orderNo) + "/start", userC.token, null);
            require("RUNNING".equals(data.path("orderStatus").asText()), "orderStatus=" + data.path("orderStatus").asText());
            require("ACTIVE".equals(data.at("/apiCredential/tokenStatus").asText()),
                    "tokenStatus=" + data.at("/apiCredential/tokenStatus").asText());
            update("""
                    UPDATE rental_order ro
                    LEFT JOIN api_credential ac ON ac.rental_order_id = ro.id
                    SET ro.auto_pause_at = DATE_ADD(NOW(), INTERVAL 20 MINUTE),
                        ro.updated_at = NOW(),
                        ac.auto_pause_at = DATE_ADD(NOW(), INTERVAL 20 MINUTE),
                        ac.updated_at = NOW()
                    WHERE ro.order_no = ?
                    """, orderNo);
            return "status=RUNNING";
        });
    }

    private void testDailyProfitAndCommission(String orderNo) throws Exception {
        var orderId = queryLong("SELECT id FROM rental_order WHERE order_no = ?", orderNo);
        step("Run daily-profit scheduler", "A settled profit record exists for today's running order", () -> {
            var before = queryLong("SELECT COUNT(*) FROM rental_profit_record WHERE rental_order_id = ?", orderId);
            var data = runScheduler("/api/admin/scheduler/daily-profit/run");
            var after = queryLong("""
                    SELECT COUNT(*)
                    FROM rental_profit_record
                    WHERE rental_order_id = ? AND status = 'SETTLED'
                    """, orderId);
            require(after >= 1, "settled profit count after run=" + after + ", scheduler=" + data);
            require(after >= before, "profit count decreased");
            return "before=" + before + ", after=" + after + ", successCount=" + data.path("successCount").asInt();
        });

        step("Run daily-profit scheduler again", "Second run does not create duplicate profit for same order/date", () -> {
            var before = queryLong("SELECT COUNT(*) FROM rental_profit_record WHERE rental_order_id = ?", orderId);
            runScheduler("/api/admin/scheduler/daily-profit/run");
            var after = queryLong("SELECT COUNT(*) FROM rental_profit_record WHERE rental_order_id = ?", orderId);
            require(before == after, "before=" + before + ", after=" + after);
            return "profit records remain " + after;
        });

        step("Run commission scheduler", "C's settled profit generates level-1 and level-2 commissions", () -> {
            var data = runScheduler("/api/admin/scheduler/commission-generate/run");
            var commissionCount = queryLong("""
                    SELECT COUNT(*)
                    FROM commission_record cr
                    JOIN rental_profit_record pr ON pr.id = cr.source_profit_id
                    WHERE pr.rental_order_id = ? AND cr.status = 'SETTLED'
                    """, orderId);
            require(commissionCount == 2, "commissionCount=" + commissionCount + ", scheduler=" + data);
            var bCommission = queryBigDecimal("SELECT total_commission FROM user_wallet WHERE user_id = ?", userB.id);
            var aCommission = queryBigDecimal("SELECT total_commission FROM user_wallet WHERE user_id = ?", userA.id);
            require(bCommission.compareTo(BigDecimal.ZERO) > 0, "B commission=" + bCommission);
            require(aCommission.compareTo(BigDecimal.ZERO) > 0, "A commission=" + aCommission);
            return "commissionCount=2, A=" + aCommission + ", B=" + bCommission;
        });
    }

    private void testExpireSettlement(String orderNo) throws Exception {
        step("Force rental expiry", "Set profit_end_at to the past for expire-settlement test", () -> {
            var updated = update("""
                    UPDATE rental_order
                    SET profit_end_at = DATE_SUB(NOW(), INTERVAL 1 MINUTE),
                        auto_pause_at = DATE_ADD(NOW(), INTERVAL 20 MINUTE),
                        updated_at = NOW()
                    WHERE order_no = ? AND order_status = 'RUNNING'
                    """, orderNo);
            require(updated == 1, "updated rows=" + updated);
            return "profit_end_at moved to past";
        });

        step("Run expire-settlement scheduler", "Expired running order is settled and principal is returned", () -> {
            var data = runScheduler("/api/admin/scheduler/expire-settlement/run");
            require(data.path("successCount").asInt() >= 1, "scheduler result=" + data);
            var detail = get("/api/rental/orders/" + enc(orderNo), userC.token);
            require("EXPIRED".equals(detail.path("orderStatus").asText()), "orderStatus=" + detail.path("orderStatus").asText());
            require("SETTLED".equals(detail.path("settlementStatus").asText()),
                    "settlementStatus=" + detail.path("settlementStatus").asText());
            var settlementCount = queryLong("""
                    SELECT COUNT(*)
                    FROM rental_settlement_order so
                    JOIN rental_order ro ON ro.id = so.rental_order_id
                    WHERE ro.order_no = ? AND so.settlement_type = 'EXPIRE' AND so.status = 'SETTLED'
                    """, orderNo);
            require(settlementCount == 1, "settlementCount=" + settlementCount);
            return "orderStatus=EXPIRED, settlementCount=1";
        });
    }

    private void testEarlySettlement(String orderNo) throws Exception {
        step("User early-settles rental order", "RUNNING order becomes EARLY_CLOSED and settlement is settled", () -> {
            var data = post("/api/rental/orders/" + enc(orderNo) + "/settle-early", userC.token, null);
            require("EARLY_TERMINATE".equals(data.path("settlementType").asText()),
                    "settlementType=" + data.path("settlementType").asText());
            require("SETTLED".equals(data.path("status").asText()), "settlement status=" + data.path("status").asText());
            var detail = get("/api/rental/orders/" + enc(orderNo), userC.token);
            require("EARLY_CLOSED".equals(detail.path("orderStatus").asText()),
                    "orderStatus=" + detail.path("orderStatus").asText());
            var tokenStatus = queryString("""
                    SELECT ac.token_status
                    FROM api_credential ac
                    JOIN rental_order ro ON ro.id = ac.rental_order_id
                    WHERE ro.order_no = ?
                    """, orderNo);
            require("REVOKED".equals(tokenStatus), "tokenStatus=" + tokenStatus);
            return "settlementNo=" + data.path("settlementNo").asText();
        });
    }

    private void testActivationTimeoutCancel(String orderNo) throws Exception {
        step("Force activation timeout", "Move api_generated_at beyond default 60 minute timeout", () -> {
            var updated = update("""
                    UPDATE rental_order
                    SET api_generated_at = DATE_SUB(NOW(), INTERVAL 61 MINUTE),
                        updated_at = NOW()
                    WHERE order_no = ? AND order_status = 'PENDING_ACTIVATION'
                    """, orderNo);
            require(updated == 1, "updated rows=" + updated);
            return "api_generated_at moved to past";
        });

        step("Run activation-timeout scheduler", "Pending activation order is canceled and refunded", () -> {
            var data = runScheduler("/api/admin/scheduler/activation-timeout-cancel/run");
            require(data.path("successCount").asInt() >= 1, "scheduler result=" + data);
            var detail = get("/api/rental/orders/" + enc(orderNo), userC.token);
            require("CANCELED".equals(detail.path("orderStatus").asText()),
                    "orderStatus=" + detail.path("orderStatus").asText());
            var tokenStatus = queryString("""
                    SELECT ac.token_status
                    FROM api_credential ac
                    JOIN rental_order ro ON ro.id = ac.rental_order_id
                    WHERE ro.order_no = ?
                    """, orderNo);
            require("REVOKED".equals(tokenStatus), "tokenStatus=" + tokenStatus);
            return "orderStatus=CANCELED, tokenStatus=REVOKED";
        });
    }

    private void testUserBackofficeViews(String orderNo) throws Exception {
        step("User rental and API management views", "C can query orders, deploy info, API list, profits, and settlements", () -> {
            get("/api/rental/orders?current=1&size=10", userC.token);
            get("/api/rental/orders/" + enc(orderNo) + "/profits?current=1&size=10", userC.token);
            get("/api/rental/api-management?current=1&size=10", userC.token);
            get("/api/profit/summary", userC.token);
            get("/api/settlement/orders?current=1&size=10", userC.token);
            return "user rental/profit/settlement views ok";
        });

        step("User team and commission views", "A/B can query team and commission pages after C profit settlement", () -> {
            get("/api/team/summary", userA.token);
            get("/api/team/members?current=1&size=10", userA.token);
            get("/api/commission/summary", userA.token);
            get("/api/commission/records?current=1&size=10", userB.token);
            return "team and commission views ok";
        });
    }

    private void testAdminBackofficeViews(String orderNo) throws Exception {
        step("Admin dashboard and catalog views", "Admin dashboard and catalog read APIs are accessible", () -> {
            get("/api/admin/dashboard/overview", adminToken);
            get("/api/admin/dashboard/finance", adminToken);
            get("/api/admin/products?pageNo=1&pageSize=5", adminToken);
            get("/api/admin/ai-models?pageNo=1&pageSize=5", adminToken);
            get("/api/admin/rental-cycle-rules?pageNo=1&pageSize=5", adminToken);
            return "dashboard/catalog views ok";
        });

        step("Admin business management views", "Admin can query users, wallets, orders, credentials, profits, commissions, settlements, logs", () -> {
            get("/api/admin/users?pageNo=1&pageSize=10&keyword=" + enc(runId), adminToken);
            get("/api/admin/wallets?pageNo=1&pageSize=10&user_id=" + userC.id, adminToken);
            get("/api/admin/wallet-transactions?pageNo=1&pageSize=10&user_id=" + userC.id, adminToken);
            get("/api/admin/rental/orders?pageNo=1&pageSize=10&order_no=" + enc(orderNo), adminToken);
            get("/api/admin/rental/orders/" + enc(orderNo), adminToken);
            get("/api/admin/api-credentials?pageNo=1&pageSize=10&user_id=" + userC.id, adminToken);
            get("/api/admin/api-deploy-orders?pageNo=1&pageSize=10&user_id=" + userC.id, adminToken);
            get("/api/admin/profit/records?pageNo=1&pageSize=10&order_no=" + enc(orderNo), adminToken);
            get("/api/admin/commission/records?pageNo=1&pageSize=10&order_no=" + enc(orderNo), adminToken);
            get("/api/admin/settlement/orders?pageNo=1&pageSize=10&order_no=" + enc(orderNo), adminToken);
            get("/api/admin/scheduler/logs?pageNo=1&pageSize=20", adminToken);
            get("/api/admin/logs?pageNo=1&pageSize=20", adminToken);
            return "admin business views ok";
        });
    }

    private JsonNode runScheduler(String path) throws Exception {
        JsonNode data = null;
        for (int i = 0; i < 3; i++) {
            data = post(path, adminToken, null);
            if (!"SKIPPED".equals(data.path("status").asText())) {
                return data;
            }
            Thread.sleep(1500L);
        }
        return data;
    }

    private Catalog loadCatalog() throws Exception {
        var product = queryOne("""
                SELECT id, product_code, product_name
                FROM product
                WHERE status = 1
                  AND token_output_per_day IS NOT NULL AND token_output_per_day > 0
                  AND token_output_per_minute IS NOT NULL AND token_output_per_minute > 0
                  AND gpu_power_tops IS NOT NULL
                  AND rent_price IS NOT NULL AND rent_price > 0
                ORDER BY rent_price ASC, id ASC
                LIMIT 1
                """);
        var model = queryOne("""
                SELECT id, model_code, model_name, deploy_tech_fee
                FROM ai_model
                WHERE status = 1
                  AND monthly_token_consumption_trillion IS NOT NULL
                  AND token_unit_price IS NOT NULL AND token_unit_price > 0
                  AND deploy_tech_fee IS NOT NULL AND deploy_tech_fee > 0
                ORDER BY deploy_tech_fee ASC, id ASC
                LIMIT 1
                """);
        var cycle = queryOne("""
                SELECT id, cycle_code, cycle_days
                FROM rental_cycle_rule
                WHERE status = 1
                ORDER BY cycle_days ASC, id ASC
                LIMIT 1
                """);
        require(!product.isEmpty(), "no enabled product found");
        require(!model.isEmpty(), "no enabled AI model found");
        require(!cycle.isEmpty(), "no enabled cycle rule found");
        return new Catalog(
                ((Number) product.get("id")).longValue(),
                String.valueOf(product.get("product_code")),
                String.valueOf(product.get("product_name")),
                ((Number) model.get("id")).longValue(),
                String.valueOf(model.get("model_code")),
                String.valueOf(model.get("model_name")),
                (BigDecimal) model.get("deploy_tech_fee"),
                ((Number) cycle.get("id")).longValue(),
                String.valueOf(cycle.get("cycle_code")),
                ((Number) cycle.get("cycle_days")).intValue()
        );
    }

    private JsonNode get(String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .header("Accept", "application/json");
        addToken(builder, token);
        return send(path, builder.build());
    }

    private JsonNode post(String path, String token, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
        addToken(builder, token);
        if (body == null) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8));
        }
        return send(path, builder.build());
    }

    private JsonNode send(String path, HttpRequest request) throws Exception {
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        var body = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(path + " HTTP " + response.statusCode() + ": " + body);
        }
        var root = JSON.readTree(body);
        if (root.path("code").asInt(-1) != 0) {
            throw new IllegalStateException(path + " API code=" + root.path("code").asInt()
                    + ", message=" + root.path("message").asText() + ", body=" + body);
        }
        return root.path("data");
    }

    private void addToken(HttpRequest.Builder builder, String token) {
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private void step(String name, String expected, StepAction action) throws Exception {
        var started = System.currentTimeMillis();
        try {
            var actual = action.run();
            results.add(new StepResult(name, "PASS", expected, actual,
                    System.currentTimeMillis() - started, null));
            System.out.println("[PASS] " + name + " - " + actual);
        } catch (Throwable ex) {
            results.add(new StepResult(name, "FAIL", expected, ex.getMessage(),
                    System.currentTimeMillis() - started, stackTrace(ex)));
            System.err.println("[FAIL] " + name + " - " + ex.getMessage());
            if (ex instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(ex);
        }
    }

    private DatasourceConfig readDatasourceConfig() throws Exception {
        var envUrl = System.getenv("DB_URL");
        var envUser = System.getenv("DB_USERNAME");
        var envPassword = System.getenv("DB_PASSWORD");
        if (hasText(envUrl)) {
            return new DatasourceConfig(envUrl, envUser == null ? "" : envUser, envPassword == null ? "" : envPassword);
        }

        var local = Path.of("application-local.yml");
        require(Files.exists(local), "application-local.yml not found");
        String url = null;
        String username = null;
        String password = null;
        var datasource = false;
        for (var rawLine : Files.readAllLines(local, StandardCharsets.UTF_8)) {
            if (rawLine.trim().equals("datasource:")) {
                datasource = true;
                continue;
            }
            if (datasource && rawLine.startsWith("  ") && !rawLine.startsWith("    ")) {
                datasource = false;
            }
            if (!datasource) {
                continue;
            }
            var line = rawLine.trim();
            if (line.startsWith("url:")) {
                url = line.substring("url:".length()).trim();
            } else if (line.startsWith("username:")) {
                username = line.substring("username:".length()).trim();
            } else if (line.startsWith("password:")) {
                password = line.substring("password:".length()).trim();
            }
        }
        requireText(url, "datasource.url");
        return new DatasourceConfig(url, username == null ? "" : username, password == null ? "" : password);
    }

    private long queryLong(String sql, Object... params) throws Exception {
        try (var statement = prepare(sql, params);
             var rs = statement.executeQuery()) {
            require(rs.next(), "query returned no rows: " + sql);
            return rs.getLong(1);
        }
    }

    private String queryString(String sql, Object... params) throws Exception {
        try (var statement = prepare(sql, params);
             var rs = statement.executeQuery()) {
            require(rs.next(), "query returned no rows: " + sql);
            return rs.getString(1);
        }
    }

    private BigDecimal queryBigDecimal(String sql, Object... params) throws Exception {
        try (var statement = prepare(sql, params);
             var rs = statement.executeQuery()) {
            require(rs.next(), "query returned no rows: " + sql);
            return rs.getBigDecimal(1);
        }
    }

    private Map<String, Object> queryOne(String sql, Object... params) throws Exception {
        try (var statement = prepare(sql, params);
             var rs = statement.executeQuery()) {
            if (!rs.next()) {
                return Map.of();
            }
            var meta = rs.getMetaData();
            var row = new LinkedHashMap<String, Object>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            return row;
        }
    }

    private int update(String sql, Object... params) throws Exception {
        try (var statement = prepare(sql, params)) {
            return statement.executeUpdate();
        }
    }

    private PreparedStatement prepare(String sql, Object... params) throws Exception {
        var statement = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            var value = params[i];
            if (value instanceof LocalDateTime time) {
                statement.setTimestamp(i + 1, Timestamp.valueOf(time));
            } else {
                statement.setObject(i + 1, value);
            }
        }
        return statement;
    }

    private void writeReport() throws Exception {
        if (reportPath == null) {
            return;
        }
        Files.createDirectories(Path.of("target"));
        var passed = results.stream().filter(result -> "PASS".equals(result.status())).count();
        var failed = results.size() - passed;
        var html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>System Test Report</title>
                  <style>
                    body { font-family: Arial, "Microsoft YaHei", sans-serif; margin: 32px; color: #1f2937; }
                    h1 { font-size: 24px; margin: 0 0 8px; }
                    .meta { color: #4b5563; margin-bottom: 20px; line-height: 1.7; }
                    table { border-collapse: collapse; width: 100%; table-layout: fixed; }
                    th, td { border: 1px solid #d1d5db; padding: 10px; vertical-align: top; word-break: break-word; }
                    th { background: #f3f4f6; text-align: left; }
                    .pass { color: #047857; font-weight: 700; }
                    .fail { color: #b91c1c; font-weight: 700; }
                    pre { white-space: pre-wrap; background: #111827; color: #f9fafb; padding: 12px; overflow: auto; }
                  </style>
                </head>
                <body>
                """);
        html.append("<h1>System Test Report</h1>");
        html.append("<div class=\"meta\">");
        html.append("Run ID: ").append(escape(runId)).append("<br>");
        html.append("Base URL: ").append(escape(baseUrl)).append("<br>");
        html.append("Generated At: ").append(escape(LocalDateTime.now().toString())).append("<br>");
        html.append("Summary: ").append(passed).append(" passed, ").append(failed).append(" failed");
        if (catalog != null) {
            html.append("<br>Catalog: ")
                    .append(escape(catalog.productName)).append(" / ")
                    .append(escape(catalog.aiModelName)).append(" / ")
                    .append(escape(catalog.cycleCode));
        }
        html.append("</div>");
        html.append("<table><thead><tr><th style=\"width:18%\">Step</th><th style=\"width:8%\">Status</th>")
                .append("<th style=\"width:28%\">Expected</th><th>Actual</th><th style=\"width:8%\">Time</th></tr></thead><tbody>");
        for (var result : results) {
            html.append("<tr><td>").append(escape(result.name())).append("</td><td class=\"")
                    .append("PASS".equals(result.status()) ? "pass" : "fail")
                    .append("\">").append(result.status()).append("</td><td>")
                    .append(escape(result.expected())).append("</td><td>")
                    .append(escape(result.actual()));
            if (result.error() != null) {
                html.append("<pre>").append(escape(result.error())).append("</pre>");
            }
            html.append("</td><td>").append(result.elapsedMs()).append(" ms</td></tr>");
        }
        html.append("</tbody></table></body></html>");
        Files.writeString(Path.of(reportPath), html.toString(), StandardCharsets.UTF_8);
        System.out.println("Report: " + reportPath);
    }

    private boolean containsId(JsonNode data, String fieldName, long id) {
        if (data == null || !data.isArray()) {
            return false;
        }
        for (var item : data) {
            if (item.path(fieldName).asLong(-1L) == id) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        var value = node.path(fieldName).asText();
        requireText(value, fieldName);
        return new BigDecimal(value);
    }

    private static Map<String, Object> mapOf(Object... values) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            if (values[i + 1] != null) {
                map.put(String.valueOf(values[i]), values[i + 1]);
            }
        }
        return map;
    }

    private static String sha256(String value) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        var bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        var builder = new StringBuilder(bytes.length * 2);
        for (var b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireText(String value, String fieldName) {
        require(hasText(value), fieldName + " is blank");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String env(String key, String fallback) {
        var value = System.getenv(key);
        return hasText(value) ? value : fallback;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String stackTrace(Throwable throwable) {
        var writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private interface StepAction {
        String run() throws Exception;
    }

    private record DatasourceConfig(String url, String username, String password) {
    }

    private record TestUser(long id, String email, String token, String inviteCode) {
    }

    private record Catalog(
            long productId,
            String productCode,
            String productName,
            long aiModelId,
            String aiModelCode,
            String aiModelName,
            BigDecimal deployFee,
            long cycleRuleId,
            String cycleCode,
            int cycleDays
    ) {
    }

    private record StepResult(
            String name,
            String status,
            String expected,
            String actual,
            long elapsedMs,
            String error
    ) {
    }
}
