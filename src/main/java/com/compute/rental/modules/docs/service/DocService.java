package com.compute.rental.modules.docs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.DocPublishStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.docs.dto.DocArticleRequest;
import com.compute.rental.modules.docs.dto.DocArticleResponse;
import com.compute.rental.modules.docs.dto.DocCategoryRequest;
import com.compute.rental.modules.docs.dto.DocCategoryResponse;
import com.compute.rental.modules.docs.entity.DocArticle;
import com.compute.rental.modules.docs.entity.DocCategory;
import com.compute.rental.modules.docs.mapper.DocArticleMapper;
import com.compute.rental.modules.docs.mapper.DocCategoryMapper;
import com.compute.rental.modules.system.service.AdminLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DocService {

    private final DocCategoryMapper categoryMapper;
    private final DocArticleMapper articleMapper;
    private final AdminLogService adminLogService;

    public DocService(
            DocCategoryMapper categoryMapper,
            DocArticleMapper articleMapper,
            AdminLogService adminLogService
    ) {
        this.categoryMapper = categoryMapper;
        this.articleMapper = articleMapper;
        this.adminLogService = adminLogService;
    }

    public List<DocCategoryResponse> publicCategories() {
        var categories = categoryMapper.selectList(new LambdaQueryWrapper<DocCategory>()
                .eq(DocCategory::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(DocCategory::getSortNo)
                .orderByAsc(DocCategory::getId));
        var childrenByParentId = new HashMap<Long, List<DocCategory>>();
        for (var category : categories) {
            childrenByParentId.computeIfAbsent(category.getParentId(), ignored -> new ArrayList<>()).add(category);
        }
        return childrenByParentId.getOrDefault(null, Collections.emptyList()).stream()
                .map(category -> categoryResponse(category, childrenByParentId, new HashSet<>()))
                .toList();
    }

    public PageResult<DocArticleResponse> publicArticles(long pageNo, long pageSize, Long categoryId, String keyword) {
        var categoryIds = enabledCategoryIds();
        if (categoryIds.isEmpty() || (categoryId != null && !categoryIds.contains(categoryId))) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        return pageArticles(pageNo, pageSize, categoryId, DocPublishStatus.PUBLISHED.value(), keyword,
                null, null, categoryIds);
    }

    public PageResult<DocArticleResponse> publicSearch(long pageNo, long pageSize, String keyword) {
        return publicArticles(pageNo, pageSize, null, keyword);
    }

    public DocArticleResponse publicArticle(Long id) {
        var article = articleMapper.selectOne(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getId, id)
                .eq(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                .last("LIMIT 1"));
        return publicArticleResponse(article);
    }

    public DocArticleResponse publicArticleBySlug(String slug) {
        var article = articleMapper.selectOne(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getSlug, normalizeRequired(slug, "slug"))
                .eq(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                .last("LIMIT 1"));
        return publicArticleResponse(article);
    }

    public PageResult<DocCategoryResponse> adminCategories(long pageNo, long pageSize, Long parentId, Integer status) {
        validateCommonStatus(status);
        var page = new Page<DocCategory>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<DocCategory>()
                .eq(parentId != null, DocCategory::getParentId, parentId)
                .eq(status != null, DocCategory::getStatus, status)
                .orderByAsc(DocCategory::getSortNo)
                .orderByAsc(DocCategory::getId);
        var result = categoryMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::categoryResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public DocCategoryResponse createCategory(DocCategoryRequest request, Long adminId, String ip) {
        validateCommonStatus(request.status());
        validateParent(null, request.parentId());
        var categoryCode = normalizeRequired(request.categoryCode(), "categoryCode");
        validateCategoryCodeUnique(categoryCode, null);
        var now = DateTimeUtils.now();
        var category = new DocCategory();
        category.setParentId(request.parentId());
        category.setCategoryCode(categoryCode);
        category.setCategoryName(normalizeRequired(request.categoryName(), "categoryName"));
        category.setIcon(normalizeNullable(request.icon()));
        category.setSortNo(defaultSortNo(request.sortNo()));
        category.setStatus(request.status() == null ? CommonStatus.ENABLED.value() : request.status());
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        categoryMapper.insert(category);
        log(adminId, "CREATE_DOC_CATEGORY", "doc_category", category.getId(), category.getCategoryName(), ip);
        return categoryResponse(category);
    }

    @Transactional
    public DocCategoryResponse updateCategory(Long id, DocCategoryRequest request, Long adminId, String ip) {
        var existing = requireCategory(id);
        validateCommonStatus(request.status());
        validateParent(id, request.parentId());
        var categoryCode = normalizeRequired(request.categoryCode(), "categoryCode");
        validateCategoryCodeUnique(categoryCode, id);
        var status = request.status() == null ? existing.getStatus() : request.status();
        var now = DateTimeUtils.now();
        categoryMapper.update(null, new LambdaUpdateWrapper<DocCategory>()
                .eq(DocCategory::getId, id)
                .set(DocCategory::getParentId, request.parentId())
                .set(DocCategory::getCategoryCode, categoryCode)
                .set(DocCategory::getCategoryName, normalizeRequired(request.categoryName(), "categoryName"))
                .set(DocCategory::getIcon, normalizeNullable(request.icon()))
                .set(DocCategory::getSortNo, defaultSortNo(request.sortNo()))
                .set(DocCategory::getStatus, status)
                .set(DocCategory::getUpdatedAt, now));
        log(adminId, "UPDATE_DOC_CATEGORY", "doc_category", id, categoryCode, ip);
        return categoryResponse(requireCategory(id));
    }

    @Transactional
    public DocCategoryResponse setCategoryStatus(Long id, Integer status, Long adminId, String ip) {
        validateCommonStatus(status);
        requireCategory(id);
        categoryMapper.update(null, new LambdaUpdateWrapper<DocCategory>()
                .eq(DocCategory::getId, id)
                .set(DocCategory::getStatus, status)
                .set(DocCategory::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("DOC_CATEGORY", status), "doc_category", id, "status=" + status, ip);
        return categoryResponse(requireCategory(id));
    }

    @Transactional
    public void deleteCategory(Long id, Long adminId, String ip) {
        var category = requireCategory(id);
        var childCount = categoryMapper.selectCount(new LambdaQueryWrapper<DocCategory>()
                .eq(DocCategory::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_HAS_CHILDREN);
        }
        var articleCount = articleMapper.selectCount(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getCategoryId, id));
        if (articleCount > 0) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_HAS_ARTICLES);
        }
        categoryMapper.deleteById(id);
        log(adminId, "DELETE_DOC_CATEGORY", "doc_category", id, category.getCategoryName(), ip);
    }

    public PageResult<DocArticleResponse> adminArticles(long pageNo, long pageSize, Long categoryId,
                                                       Integer publishStatus, String keyword,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        validatePublishStatus(publishStatus);
        return pageArticles(pageNo, pageSize, categoryId, publishStatus, keyword, startTime, endTime, null);
    }

    public DocArticleResponse adminArticle(Long id) {
        return articleResponse(requireArticle(id));
    }

    @Transactional
    public DocArticleResponse createArticle(DocArticleRequest request, Long adminId, String ip) {
        validatePublishStatus(request.publishStatus());
        requireCategory(request.categoryId());
        var slug = normalizeRequired(request.slug(), "slug");
        validateSlugUnique(slug, null);
        var now = DateTimeUtils.now();
        var article = new DocArticle();
        article.setCategoryId(request.categoryId());
        article.setTitle(normalizeRequired(request.title(), "title"));
        article.setSlug(slug);
        article.setSummary(normalizeNullable(request.summary()));
        article.setContentMarkdown(normalizeRequired(request.contentMarkdown(), "contentMarkdown"));
        article.setPublishStatus(request.publishStatus() == null ? DocPublishStatus.DRAFT.value() : request.publishStatus());
        article.setPublishedAt(Integer.valueOf(DocPublishStatus.PUBLISHED.value()).equals(article.getPublishStatus()) ? now : null);
        article.setSortNo(defaultSortNo(request.sortNo()));
        article.setViewCount(0L);
        article.setCreatedBy(adminId);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        articleMapper.insert(article);
        log(adminId, "CREATE_DOC_ARTICLE", "doc_article", article.getId(), article.getTitle(), ip);
        return articleResponse(requireArticle(article.getId()));
    }

    @Transactional
    public DocArticleResponse updateArticle(Long id, DocArticleRequest request, Long adminId, String ip) {
        var existing = requireArticle(id);
        validatePublishStatus(request.publishStatus());
        requireCategory(request.categoryId());
        var slug = normalizeRequired(request.slug(), "slug");
        validateSlugUnique(slug, id);
        var publishStatus = request.publishStatus() == null ? existing.getPublishStatus() : request.publishStatus();
        var publishedAt = existing.getPublishedAt();
        if (Integer.valueOf(DocPublishStatus.PUBLISHED.value()).equals(publishStatus)
                && !Integer.valueOf(DocPublishStatus.PUBLISHED.value()).equals(existing.getPublishStatus())) {
            publishedAt = DateTimeUtils.now();
        }
        var now = DateTimeUtils.now();
        articleMapper.update(null, new LambdaUpdateWrapper<DocArticle>()
                .eq(DocArticle::getId, id)
                .set(DocArticle::getCategoryId, request.categoryId())
                .set(DocArticle::getTitle, normalizeRequired(request.title(), "title"))
                .set(DocArticle::getSlug, slug)
                .set(DocArticle::getSummary, normalizeNullable(request.summary()))
                .set(DocArticle::getContentMarkdown, normalizeRequired(request.contentMarkdown(), "contentMarkdown"))
                .set(DocArticle::getPublishStatus, publishStatus)
                .set(DocArticle::getPublishedAt, publishedAt)
                .set(DocArticle::getSortNo, defaultSortNo(request.sortNo()))
                .set(DocArticle::getUpdatedAt, now));
        log(adminId, "UPDATE_DOC_ARTICLE", "doc_article", id, slug, ip);
        return articleResponse(requireArticle(id));
    }

    @Transactional
    public DocArticleResponse publishArticle(Long id, Long adminId, String ip) {
        requireArticle(id);
        var now = DateTimeUtils.now();
        articleMapper.update(null, new LambdaUpdateWrapper<DocArticle>()
                .eq(DocArticle::getId, id)
                .set(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                .set(DocArticle::getPublishedAt, now)
                .set(DocArticle::getUpdatedAt, now));
        log(adminId, "PUBLISH_DOC_ARTICLE", "doc_article", id, "published", ip);
        return articleResponse(requireArticle(id));
    }

    @Transactional
    public DocArticleResponse unpublishArticle(Long id, Long adminId, String ip) {
        requireArticle(id);
        articleMapper.update(null, new LambdaUpdateWrapper<DocArticle>()
                .eq(DocArticle::getId, id)
                .set(DocArticle::getPublishStatus, DocPublishStatus.OFFLINE.value())
                .set(DocArticle::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, "UNPUBLISH_DOC_ARTICLE", "doc_article", id, "offline", ip);
        return articleResponse(requireArticle(id));
    }

    @Transactional
    public void deleteArticle(Long id, Long adminId, String ip) {
        var article = requireArticle(id);
        articleMapper.deleteById(id);
        log(adminId, "DELETE_DOC_ARTICLE", "doc_article", id, article.getTitle(), ip);
    }

    private PageResult<DocArticleResponse> pageArticles(long pageNo, long pageSize, Long categoryId,
                                                       Integer publishStatus, String keyword,
                                                       LocalDateTime startTime, LocalDateTime endTime,
                                                       List<Long> allowedCategoryIds) {
        var normalizedKeyword = normalizeNullable(keyword);
        var page = new Page<DocArticle>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<DocArticle>()
                .eq(categoryId != null, DocArticle::getCategoryId, categoryId)
                .in(allowedCategoryIds != null, DocArticle::getCategoryId,
                        allowedCategoryIds == null ? Collections.emptyList() : allowedCategoryIds)
                .eq(publishStatus != null, DocArticle::getPublishStatus, publishStatus)
                .ge(startTime != null, DocArticle::getPublishedAt, startTime)
                .le(endTime != null, DocArticle::getPublishedAt, endTime)
                .and(StringUtils.hasText(normalizedKeyword), query -> query
                        .like(DocArticle::getTitle, normalizedKeyword)
                        .or()
                        .like(DocArticle::getSummary, normalizedKeyword)
                        .or()
                        .like(DocArticle::getContentMarkdown, normalizedKeyword))
                .orderByAsc(DocArticle::getSortNo)
                .orderByDesc(DocArticle::getPublishedAt)
                .orderByDesc(DocArticle::getId);
        var result = articleMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::articleResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private DocArticleResponse publicArticleResponse(DocArticle article) {
        if (article == null || !isCategoryPathEnabled(article.getCategoryId())) {
            throw new BusinessException(ErrorCode.DOC_ARTICLE_NOT_FOUND);
        }
        articleMapper.update(null, new LambdaUpdateWrapper<DocArticle>()
                .eq(DocArticle::getId, article.getId())
                .setSql("view_count = view_count + 1"));
        article.setViewCount(article.getViewCount() == null ? 1L : article.getViewCount() + 1);
        return articleResponse(article);
    }

    private List<Long> enabledCategoryIds() {
        return categoryMapper.selectList(new LambdaQueryWrapper<DocCategory>()
                        .eq(DocCategory::getStatus, CommonStatus.ENABLED.value()))
                .stream()
                .map(DocCategory::getId)
                .filter(this::isCategoryPathEnabled)
                .toList();
    }

    private boolean isCategoryPathEnabled(Long categoryId) {
        var currentId = categoryId;
        var visited = new HashSet<Long>();
        while (currentId != null) {
            if (!visited.add(currentId)) {
                return false;
            }
            var category = categoryMapper.selectById(currentId);
            if (category == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(category.getStatus())) {
                return false;
            }
            currentId = category.getParentId();
        }
        return true;
    }

    private DocCategoryResponse categoryResponse(DocCategory category) {
        return new DocCategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getCategoryCode(),
                category.getCategoryName(),
                category.getIcon(),
                category.getSortNo(),
                category.getStatus(),
                Collections.emptyList(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private DocCategoryResponse categoryResponse(DocCategory category, Map<Long, List<DocCategory>> childrenByParentId,
                                                 Set<Long> visited) {
        if (!visited.add(category.getId())) {
            return categoryResponse(category);
        }
        var children = childrenByParentId.getOrDefault(category.getId(), Collections.emptyList()).stream()
                .map(child -> categoryResponse(child, childrenByParentId, new HashSet<>(visited)))
                .toList();
        return new DocCategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getCategoryCode(),
                category.getCategoryName(),
                category.getIcon(),
                category.getSortNo(),
                category.getStatus(),
                children,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private DocArticleResponse articleResponse(DocArticle article) {
        return new DocArticleResponse(
                article.getId(),
                article.getCategoryId(),
                categoryName(article.getCategoryId()),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getContentMarkdown(),
                article.getPublishStatus(),
                article.getPublishedAt(),
                article.getSortNo(),
                article.getViewCount(),
                article.getCreatedBy(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private String categoryName(Long categoryId) {
        var category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getCategoryName();
    }

    private DocCategory requireCategory(Long id) {
        var category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private DocArticle requireArticle(Long id) {
        var article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.DOC_ARTICLE_NOT_FOUND);
        }
        return article;
    }

    private void validateParent(Long id, Long parentId) {
        if (parentId == null) {
            return;
        }
        requireCategory(parentId);
        if (id != null && createsCycle(id, parentId)) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_PARENT_CYCLE);
        }
    }

    private boolean createsCycle(Long id, Long parentId) {
        var currentId = parentId;
        var visited = new HashSet<Long>();
        while (currentId != null) {
            if (currentId.equals(id) || !visited.add(currentId)) {
                return true;
            }
            var current = categoryMapper.selectById(currentId);
            if (current == null) {
                return false;
            }
            currentId = current.getParentId();
        }
        return false;
    }

    private void validateCategoryCodeUnique(String categoryCode, Long excludeId) {
        var count = categoryMapper.selectCount(new LambdaQueryWrapper<DocCategory>()
                .eq(DocCategory::getCategoryCode, categoryCode)
                .ne(excludeId != null, DocCategory::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_CODE_EXISTS);
        }
    }

    private void validateSlugUnique(String slug, Long excludeId) {
        var count = articleMapper.selectCount(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getSlug, slug)
                .ne(excludeId != null, DocArticle::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DOC_ARTICLE_SLUG_EXISTS);
        }
    }

    private void validateCommonStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (!Integer.valueOf(CommonStatus.ENABLED.value()).equals(status)
                && !Integer.valueOf(CommonStatus.DISABLED.value()).equals(status)) {
            throw new BusinessException(ErrorCode.DOC_STATUS_INVALID);
        }
    }

    private void validatePublishStatus(Integer publishStatus) {
        if (publishStatus == null) {
            return;
        }
        if (!Integer.valueOf(DocPublishStatus.DRAFT.value()).equals(publishStatus)
                && !Integer.valueOf(DocPublishStatus.PUBLISHED.value()).equals(publishStatus)
                && !Integer.valueOf(DocPublishStatus.OFFLINE.value()).equals(publishStatus)) {
            throw new BusinessException(ErrorCode.DOC_PUBLISH_STATUS_INVALID);
        }
    }

    private Integer defaultSortNo(Integer sortNo) {
        return sortNo == null ? 0 : sortNo;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.DOC_FIELD_REQUIRED, fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String statusAction(String subject, Integer status) {
        return Integer.valueOf(CommonStatus.ENABLED.value()).equals(status) ? "ENABLE_" + subject : "DISABLE_" + subject;
    }

    private void log(Long adminId, String action, String table, Long targetId, String remark, String ip) {
        adminLogService.log(adminId, action, table, targetId, null, null,
                StringUtils.hasText(remark) ? remark : action, ip);
    }
}
