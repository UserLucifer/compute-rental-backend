package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.compute.rental.common.enums.DocPublishStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.modules.commission.service.TeamService;
import com.compute.rental.modules.docs.entity.DocArticle;
import com.compute.rental.modules.docs.mapper.DocArticleMapper;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.service.ProfitService;
import com.compute.rental.modules.order.service.RentalOrderService;
import com.compute.rental.modules.system.dto.DashboardSearchItemResponse;
import com.compute.rental.modules.system.dto.DashboardSearchResponse;
import com.compute.rental.modules.system.dto.UserDashboardOverviewResponse;
import com.compute.rental.modules.system.dto.UserDashboardProfitResponse;
import com.compute.rental.modules.system.dto.UserDashboardRentalResponse;
import com.compute.rental.modules.system.entity.SysNotification;
import com.compute.rental.modules.system.mapper.SysNotificationMapper;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.mapper.WalletTransactionMapper;
import com.compute.rental.modules.wallet.service.WalletService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDashboardService {

    private static final int RECENT_ORDER_LIMIT = 5;
    private static final int SEARCH_LIMIT = 10;
    private static final int KEYWORD_MAX_LENGTH = 64;

    private final WalletService walletService;
    private final RentalOrderService rentalOrderService;
    private final ProfitService profitService;
    private final TeamService teamService;
    private final RentalOrderMapper rentalOrderMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final SysNotificationMapper notificationMapper;
    private final DocArticleMapper docArticleMapper;
    private final LanguageResolver languageResolver;

    public UserDashboardService(
            WalletService walletService,
            RentalOrderService rentalOrderService,
            ProfitService profitService,
            TeamService teamService,
            RentalOrderMapper rentalOrderMapper,
            WalletTransactionMapper walletTransactionMapper,
            SysNotificationMapper notificationMapper,
            DocArticleMapper docArticleMapper,
            LanguageResolver languageResolver
    ) {
        this.walletService = walletService;
        this.rentalOrderService = rentalOrderService;
        this.profitService = profitService;
        this.teamService = teamService;
        this.rentalOrderMapper = rentalOrderMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.notificationMapper = notificationMapper;
        this.docArticleMapper = docArticleMapper;
        this.languageResolver = languageResolver;
    }

    public UserDashboardOverviewResponse overview(Long userId) {
        return new UserDashboardOverviewResponse(
                walletService.getCurrentUserWallet(userId),
                new UserDashboardRentalResponse(
                        rentalOrderService.countUserOrdersByStatus(userId, RentalOrderStatus.RUNNING),
                        rentalOrderService.countUserOrdersByStatus(userId, RentalOrderStatus.PENDING_PAY),
                        rentalOrderService.recentUserOrders(userId, RECENT_ORDER_LIMIT)),
                new UserDashboardProfitResponse(profitService.summary(userId)),
                teamService.summary(userId)
        );
    }

    public DashboardSearchResponse search(Long userId, String keyword, String scope, String acceptLanguage) {
        var normalizedKeyword = normalizeKeyword(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return new DashboardSearchResponse(List.of());
        }
        var searchScope = SearchScope.from(scope);
        var records = new ArrayList<DashboardSearchItemResponse>(SEARCH_LIMIT);
        if (searchScope == SearchScope.DASHBOARD || searchScope == SearchScope.ORDER) {
            records.addAll(searchOrders(userId, normalizedKeyword, remaining(records)));
        }
        if (searchScope == SearchScope.DASHBOARD || searchScope == SearchScope.WALLET_TX) {
            records.addAll(searchWalletTransactions(userId, normalizedKeyword, remaining(records)));
        }
        if (searchScope == SearchScope.DASHBOARD || searchScope == SearchScope.NOTIFICATION) {
            records.addAll(searchNotifications(userId, normalizedKeyword, remaining(records)));
        }
        if (searchScope == SearchScope.DASHBOARD || searchScope == SearchScope.DOC) {
            var language = languageResolver.resolve(null, acceptLanguage);
            records.addAll(searchDocs(language, normalizedKeyword, remaining(records)));
        }
        return new DashboardSearchResponse(records);
    }

    private List<DashboardSearchItemResponse> searchOrders(Long userId, String keyword, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return rentalOrderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                        .eq(RentalOrder::getUserId, userId)
                        .and(wrapper -> wrapper
                                .likeRight(RentalOrder::getOrderNo, keyword)
                                .or()
                                .likeRight(RentalOrder::getProductNameSnapshot, keyword)
                                .or()
                                .likeRight(RentalOrder::getAiModelNameSnapshot, keyword))
                        .orderByDesc(RentalOrder::getId)
                        .last("LIMIT " + limit))
                .stream()
                .map(order -> new DashboardSearchItemResponse(
                        SearchScope.ORDER.name(),
                        firstText(order.getProductNameSnapshot(), order.getAiModelNameSnapshot(), order.getOrderNo()),
                        "订单号 " + order.getOrderNo(),
                        "/dashboard/orders?orderNo=" + encode(order.getOrderNo())))
                .toList();
    }

    private List<DashboardSearchItemResponse> searchWalletTransactions(Long userId, String keyword, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return walletTransactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getUserId, userId)
                        .and(wrapper -> wrapper
                                .likeRight(WalletTransaction::getTxNo, keyword)
                                .or()
                                .likeRight(WalletTransaction::getBizOrderNo, keyword))
                        .orderByDesc(WalletTransaction::getId)
                        .last("LIMIT " + limit))
                .stream()
                .map(tx -> new DashboardSearchItemResponse(
                        SearchScope.WALLET_TX.name(),
                        tx.getTxNo(),
                        StringUtils.hasText(tx.getBizOrderNo()) ? "业务单号 " + tx.getBizOrderNo() : tx.getBizType(),
                        "/dashboard/wallet?txNo=" + encode(tx.getTxNo())))
                .toList();
    }

    private List<DashboardSearchItemResponse> searchNotifications(Long userId, String keyword, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return notificationMapper.selectList(new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .likeRight(SysNotification::getTitle, keyword)
                        .orderByDesc(SysNotification::getId)
                        .last("LIMIT " + limit))
                .stream()
                .map(notification -> new DashboardSearchItemResponse(
                        SearchScope.NOTIFICATION.name(),
                        notification.getTitle(),
                        abbreviate(notification.getContent()),
                        "/dashboard/notifications"))
                .toList();
    }

    private List<DashboardSearchItemResponse> searchDocs(String language, String keyword, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return docArticleMapper.selectList(new LambdaQueryWrapper<DocArticle>()
                        .eq(DocArticle::getLanguage, language)
                        .eq(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                        .likeRight(DocArticle::getTitle, keyword)
                        .orderByAsc(DocArticle::getSortNo)
                        .orderByDesc(DocArticle::getPublishedAt)
                        .orderByDesc(DocArticle::getId)
                        .last("LIMIT " + limit))
                .stream()
                .map(article -> new DashboardSearchItemResponse(
                        SearchScope.DOC.name(),
                        article.getTitle(),
                        abbreviate(article.getSummary()),
                        "/docs/" + encode(article.getSlug())))
                .toList();
    }

    private int remaining(List<DashboardSearchItemResponse> records) {
        return Math.max(0, SEARCH_LIMIT - records.size());
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        var normalized = keyword.trim();
        return normalized.length() > KEYWORD_MAX_LENGTH ? normalized.substring(0, KEYWORD_MAX_LENGTH) : normalized;
    }

    private String firstText(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return fallback;
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        var normalized = value.trim();
        return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private enum SearchScope {
        DASHBOARD,
        ORDER,
        WALLET_TX,
        NOTIFICATION,
        DOC;

        private static SearchScope from(String value) {
            if (!StringUtils.hasText(value)) {
                return DASHBOARD;
            }
            try {
                return SearchScope.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "scope 不支持: " + value);
            }
        }
    }
}
