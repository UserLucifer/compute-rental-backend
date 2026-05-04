package com.compute.rental.modules.docs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.DocLanguage;
import com.compute.rental.common.enums.DocPublishStatus;
import com.compute.rental.common.enums.DocSection;
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

    public List<DocCategoryResponse> publicCategories(String section, String language) {
        var normalizedLanguage = normalizeLanguageDefault(language);
        var normalizedSection = normalizeSectionOptional(section);
        var categories = categoryMapper.selectList(new LambdaQueryWrapper<DocCategory>()
                .eq(DocCategory::getLanguage, normalizedLanguage)
                .eq(normalizedSection != null, DocCategory::getSection, normalizedSection)
                .eq(DocCategory::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(DocCategory::getSortNo)
                .orderByAsc(DocCategory::getId));
        var childrenByParentId = new HashMap<Long, List<DocCategory>>();
        for (var category : categories) {
            if (isCategoryPathEnabled(category.getId(), normalizedLanguage, normalizedSection)) {
                childrenByParentId.computeIfAbsent(category.getParentId(), ignored -> new ArrayList<>()).add(category);
            }
        }
        return childrenByParentId.getOrDefault(null, Collections.emptyList()).stream()
                .map(category -> categoryResponse(category, childrenByParentId, new HashSet<>()))
                .toList();
    }

    public PageResult<DocArticleResponse> publicArticles(long pageNo, long pageSize, String section, String language,
                                                         Long categoryId, String keyword) {
        var normalizedLanguage = normalizeLanguageDefault(language);
        var normalizedSection = normalizeSectionOptional(section);
        var categoryIds = enabledCategoryIds(normalizedLanguage, normalizedSection);
        if (categoryIds.isEmpty() || (categoryId != null && !categoryIds.contains(categoryId))) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        return pageArticles(pageNo, pageSize, normalizedLanguage, normalizedSection, categoryId, DocPublishStatus.PUBLISHED.value(),
                null, keyword, null, null, categoryIds);
    }

    public PageResult<DocArticleResponse> publicSearch(long pageNo, long pageSize, String section,
                                                       String language, String keyword) {
        return publicArticles(pageNo, pageSize, section, language, null, keyword);
    }

    public DocArticleResponse publicSectionHome(String section, String language) {
        var normalizedLanguage = normalizeLanguageDefault(language);
        var normalizedSection = normalizeSectionRequired(section);
        var article = articleMapper.selectOne(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getLanguage, normalizedLanguage)
                .eq(DocArticle::getSection, normalizedSection)
                .eq(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                .eq(DocArticle::getIsSectionHome, 1)
                .last("LIMIT 1"));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分区首页文档不存在");
        }
        return publicArticleResponse(article);
    }

    public DocArticleResponse publicArticle(Long id, String language) {
        var normalizedLanguage = normalizeLanguageDefault(language);
        var article = articleMapper.selectOne(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getId, id)
                .eq(DocArticle::getLanguage, normalizedLanguage)
                .eq(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                .last("LIMIT 1"));
        return publicArticleResponse(article);
    }

    public DocArticleResponse publicArticleBySlug(String slug, String language) {
        var normalizedLanguage = normalizeLanguageDefault(language);
        var article = articleMapper.selectOne(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getLanguage, normalizedLanguage)
                .eq(DocArticle::getSlug, normalizeRequired(slug, "slug"))
                .eq(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                .last("LIMIT 1"));
        return publicArticleResponse(article);
    }

    public PageResult<DocCategoryResponse> adminCategories(long pageNo, long pageSize, String language, String section,
                                                          Long parentId, Integer status) {
        var normalizedLanguage = normalizeLanguageDefault(language);
        var normalizedSection = normalizeSectionOptional(section);
        validateCommonStatus(status);
        var page = new Page<DocCategory>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<DocCategory>()
                .eq(DocCategory::getLanguage, normalizedLanguage)
                .eq(normalizedSection != null, DocCategory::getSection, normalizedSection)
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
        var language = normalizeLanguageDefault(request.language());
        var section = normalizeSectionRequired(request.section());
        validateCommonStatus(request.status());
        validateParent(null, request.parentId(), language, section);
        var categoryCode = normalizeRequired(request.categoryCode(), "categoryCode");
        validateCategoryCodeUnique(language, section, categoryCode, null);
        var now = DateTimeUtils.now();
        var category = new DocCategory();
        category.setLanguage(language);
        category.setSection(section);
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
        var language = normalizeLanguageDefault(request.language());
        var section = normalizeSectionRequired(request.section());
        validateCommonStatus(request.status());
        validateCategoryScopeChange(existing, language, section);
        validateParent(id, request.parentId(), language, section);
        var categoryCode = normalizeRequired(request.categoryCode(), "categoryCode");
        validateCategoryCodeUnique(language, section, categoryCode, id);
        var status = request.status() == null ? existing.getStatus() : request.status();
        var now = DateTimeUtils.now();
        categoryMapper.update(null, new LambdaUpdateWrapper<DocCategory>()
                .eq(DocCategory::getId, id)
                .set(DocCategory::getLanguage, language)
                .set(DocCategory::getSection, section)
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

    public PageResult<DocArticleResponse> adminArticles(long pageNo, long pageSize, String language, String section,
                                                       Long categoryId, Integer publishStatus,
                                                       Integer isSectionHome, String keyword,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        var normalizedLanguage = normalizeLanguageDefault(language);
        var normalizedSection = normalizeSectionOptional(section);
        validatePublishStatus(publishStatus);
        validateHomeFlag(isSectionHome);
        return pageArticles(pageNo, pageSize, normalizedLanguage, normalizedSection, categoryId, publishStatus, isSectionHome,
                keyword, startTime, endTime, null);
    }

    public DocArticleResponse adminArticle(Long id) {
        return articleResponse(requireArticle(id));
    }

    @Transactional
    public DocArticleResponse createArticle(DocArticleRequest request, Long adminId, String ip) {
        var language = normalizeLanguageDefault(request.language());
        var section = normalizeSectionRequired(request.section());
        validatePublishStatus(request.publishStatus());
        var category = requireCategory(request.categoryId());
        validateArticleCategoryScope(language, section, category);
        var isSectionHome = defaultHomeFlag(request.isSectionHome());
        var publishStatus = request.publishStatus() == null ? DocPublishStatus.DRAFT.value() : request.publishStatus();
        validatePublishedHomeUnique(language, section, null, publishStatus, isSectionHome);
        var slug = normalizeRequired(request.slug(), "slug");
        validateSlugUnique(language, slug, null);
        var now = DateTimeUtils.now();
        var article = new DocArticle();
        article.setLanguage(language);
        article.setSection(section);
        article.setCategoryId(request.categoryId());
        article.setTitle(normalizeRequired(request.title(), "title"));
        article.setSlug(slug);
        article.setSummary(normalizeNullable(request.summary()));
        article.setContentMarkdown(normalizeRequired(request.contentMarkdown(), "contentMarkdown"));
        article.setPublishStatus(publishStatus);
        article.setIsSectionHome(isSectionHome);
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
        var language = normalizeLanguageDefault(request.language());
        var section = normalizeSectionRequired(request.section());
        validatePublishStatus(request.publishStatus());
        var category = requireCategory(request.categoryId());
        validateArticleCategoryScope(language, section, category);
        var isSectionHome = defaultHomeFlag(request.isSectionHome());
        var publishStatus = request.publishStatus() == null ? existing.getPublishStatus() : request.publishStatus();
        validatePublishedHomeUnique(language, section, id, publishStatus, isSectionHome);
        var slug = normalizeRequired(request.slug(), "slug");
        validateSlugUnique(language, slug, id);
        var publishedAt = existing.getPublishedAt();
        if (Integer.valueOf(DocPublishStatus.PUBLISHED.value()).equals(publishStatus)
                && !Integer.valueOf(DocPublishStatus.PUBLISHED.value()).equals(existing.getPublishStatus())) {
            publishedAt = DateTimeUtils.now();
        }
        var now = DateTimeUtils.now();
        articleMapper.update(null, new LambdaUpdateWrapper<DocArticle>()
                .eq(DocArticle::getId, id)
                .set(DocArticle::getLanguage, language)
                .set(DocArticle::getSection, section)
                .set(DocArticle::getCategoryId, request.categoryId())
                .set(DocArticle::getTitle, normalizeRequired(request.title(), "title"))
                .set(DocArticle::getSlug, slug)
                .set(DocArticle::getSummary, normalizeNullable(request.summary()))
                .set(DocArticle::getContentMarkdown, normalizeRequired(request.contentMarkdown(), "contentMarkdown"))
                .set(DocArticle::getPublishStatus, publishStatus)
                .set(DocArticle::getIsSectionHome, isSectionHome)
                .set(DocArticle::getPublishedAt, publishedAt)
                .set(DocArticle::getSortNo, defaultSortNo(request.sortNo()))
                .set(DocArticle::getUpdatedAt, now));
        log(adminId, "UPDATE_DOC_ARTICLE", "doc_article", id, slug, ip);
        return articleResponse(requireArticle(id));
    }

    @Transactional
    public DocArticleResponse publishArticle(Long id, Long adminId, String ip) {
        var article = requireArticle(id);
        if (Integer.valueOf(1).equals(article.getIsSectionHome())) {
            validatePublishedHomeUnique(article.getLanguage(), article.getSection(), id,
                    DocPublishStatus.PUBLISHED.value(), article.getIsSectionHome());
        }
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

    private PageResult<DocArticleResponse> pageArticles(long pageNo, long pageSize, String language, String section,
                                                       Long categoryId,
                                                       Integer publishStatus, Integer isSectionHome, String keyword,
                                                       LocalDateTime startTime, LocalDateTime endTime,
                                                       List<Long> allowedCategoryIds) {
        var normalizedKeyword = normalizeNullable(keyword);
        var page = new Page<DocArticle>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getLanguage, language)
                .eq(section != null, DocArticle::getSection, section)
                .eq(categoryId != null, DocArticle::getCategoryId, categoryId)
                .in(allowedCategoryIds != null, DocArticle::getCategoryId,
                        allowedCategoryIds == null ? Collections.emptyList() : allowedCategoryIds)
                .eq(publishStatus != null, DocArticle::getPublishStatus, publishStatus)
                .eq(isSectionHome != null, DocArticle::getIsSectionHome, isSectionHome)
                .ge(startTime != null, DocArticle::getPublishedAt, startTime)
                .le(endTime != null, DocArticle::getPublishedAt, endTime)
                .and(StringUtils.hasText(normalizedKeyword), query -> query
                        .like(DocArticle::getTitle, normalizedKeyword)
                        .or()
                        .like(DocArticle::getSummary, normalizedKeyword)
                        .or()
                        .like(DocArticle::getContentMarkdown, normalizedKeyword))
                .orderByDesc(DocArticle::getIsSectionHome)
                .orderByAsc(DocArticle::getSortNo)
                .orderByDesc(DocArticle::getPublishedAt)
                .orderByDesc(DocArticle::getId);
        var result = articleMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::articleResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private DocArticleResponse publicArticleResponse(DocArticle article) {
        if (article == null || !isCategoryPathEnabled(article.getCategoryId(), article.getLanguage(), article.getSection())) {
            throw new BusinessException(ErrorCode.DOC_ARTICLE_NOT_FOUND);
        }
        articleMapper.update(null, new LambdaUpdateWrapper<DocArticle>()
                .eq(DocArticle::getId, article.getId())
                .setSql("view_count = view_count + 1"));
        article.setViewCount(article.getViewCount() == null ? 1L : article.getViewCount() + 1);
        return articleResponse(article);
    }

    private List<Long> enabledCategoryIds(String language, String section) {
        return categoryMapper.selectList(new LambdaQueryWrapper<DocCategory>()
                        .eq(DocCategory::getLanguage, language)
                        .eq(section != null, DocCategory::getSection, section)
                        .eq(DocCategory::getStatus, CommonStatus.ENABLED.value()))
                .stream()
                .map(DocCategory::getId)
                .filter(categoryId -> isCategoryPathEnabled(categoryId, language, section))
                .toList();
    }

    private boolean isCategoryPathEnabled(Long categoryId, String language, String section) {
        var currentId = categoryId;
        var expectedLanguage = language;
        var expectedSection = section;
        var visited = new HashSet<Long>();
        while (currentId != null) {
            if (!visited.add(currentId)) {
                return false;
            }
            var category = categoryMapper.selectById(currentId);
            if (category == null || !Integer.valueOf(CommonStatus.ENABLED.value()).equals(category.getStatus())) {
                return false;
            }
            if (!expectedLanguage.equals(category.getLanguage())) {
                return false;
            }
            if (expectedSection == null) {
                expectedSection = category.getSection();
            }
            if (!expectedSection.equals(category.getSection())) {
                return false;
            }
            currentId = category.getParentId();
        }
        return true;
    }

    private DocCategoryResponse categoryResponse(DocCategory category) {
        return new DocCategoryResponse(
                category.getId(),
                category.getLanguage(),
                category.getSection(),
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
                .filter(child -> category.getLanguage().equals(child.getLanguage()))
                .filter(child -> category.getSection().equals(child.getSection()))
                .map(child -> categoryResponse(child, childrenByParentId, new HashSet<>(visited)))
                .toList();
        return new DocCategoryResponse(
                category.getId(),
                category.getLanguage(),
                category.getSection(),
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
                article.getLanguage(),
                article.getSection(),
                article.getCategoryId(),
                categoryName(article.getCategoryId()),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getContentMarkdown(),
                article.getPublishStatus(),
                article.getIsSectionHome(),
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

    private void validateParent(Long id, Long parentId, String language, String section) {
        if (parentId == null) {
            return;
        }
        var parent = requireCategory(parentId);
        if (!language.equals(parent.getLanguage())) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_LANGUAGE_MISMATCH);
        }
        if (!section.equals(parent.getSection())) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_SECTION_MISMATCH);
        }
        if (id != null && createsCycle(id, parentId)) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_PARENT_CYCLE);
        }
    }

    private void validateCategoryScopeChange(DocCategory existing, String language, String section) {
        if (language.equals(existing.getLanguage()) && section.equals(existing.getSection())) {
            return;
        }
        var childCount = categoryMapper.selectCount(new LambdaQueryWrapper<DocCategory>()
                .eq(DocCategory::getParentId, existing.getId()));
        var articleCount = articleMapper.selectCount(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getCategoryId, existing.getId()));
        if (childCount > 0 || articleCount > 0) {
            if (!language.equals(existing.getLanguage())) {
                throw new BusinessException(ErrorCode.DOC_CATEGORY_LANGUAGE_MISMATCH);
            }
            throw new BusinessException(ErrorCode.DOC_CATEGORY_SECTION_MISMATCH);
        }
    }

    private void validateArticleCategoryScope(String language, String section, DocCategory category) {
        if (!language.equals(category.getLanguage())) {
            throw new BusinessException(ErrorCode.DOC_ARTICLE_LANGUAGE_MISMATCH);
        }
        if (!section.equals(category.getSection())) {
            throw new BusinessException(ErrorCode.DOC_ARTICLE_SECTION_MISMATCH);
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

    private void validateCategoryCodeUnique(String language, String section, String categoryCode, Long excludeId) {
        var count = categoryMapper.selectCount(new LambdaQueryWrapper<DocCategory>()
                .eq(DocCategory::getLanguage, language)
                .eq(DocCategory::getSection, section)
                .eq(DocCategory::getCategoryCode, categoryCode)
                .ne(excludeId != null, DocCategory::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DOC_CATEGORY_CODE_EXISTS);
        }
    }

    private void validateSlugUnique(String language, String slug, Long excludeId) {
        var count = articleMapper.selectCount(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getLanguage, language)
                .eq(DocArticle::getSlug, slug)
                .ne(excludeId != null, DocArticle::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DOC_ARTICLE_SLUG_EXISTS);
        }
    }

    private void validatePublishedHomeUnique(String language, String section, Long excludeId,
                                             Integer publishStatus, Integer isSectionHome) {
        if (!Integer.valueOf(DocPublishStatus.PUBLISHED.value()).equals(publishStatus)
                || !Integer.valueOf(1).equals(isSectionHome)) {
            return;
        }
        var count = articleMapper.selectCount(new LambdaQueryWrapper<DocArticle>()
                .eq(DocArticle::getLanguage, language)
                .eq(DocArticle::getSection, section)
                .eq(DocArticle::getPublishStatus, DocPublishStatus.PUBLISHED.value())
                .eq(DocArticle::getIsSectionHome, 1)
                .ne(excludeId != null, DocArticle::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DOC_SECTION_HOME_EXISTS);
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

    private void validateHomeFlag(Integer isSectionHome) {
        if (isSectionHome == null) {
            return;
        }
        if (!Integer.valueOf(0).equals(isSectionHome) && !Integer.valueOf(1).equals(isSectionHome)) {
            throw new BusinessException(ErrorCode.DOC_HOME_FLAG_INVALID);
        }
    }

    private Integer defaultSortNo(Integer sortNo) {
        return sortNo == null ? 0 : sortNo;
    }

    private Integer defaultHomeFlag(Integer isSectionHome) {
        validateHomeFlag(isSectionHome);
        return isSectionHome == null ? 0 : isSectionHome;
    }

    private String normalizeLanguageDefault(String value) {
        var language = normalizeNullable(value);
        return language == null ? DocLanguage.DEFAULT_VALUE : validateLanguage(language);
    }

    private String normalizeSectionRequired(String value) {
        var section = normalizeNullable(value);
        if (section == null) {
            throw new BusinessException(ErrorCode.DOC_FIELD_REQUIRED, "section is required");
        }
        return validateSection(section);
    }

    private String normalizeSectionOptional(String value) {
        var section = normalizeNullable(value);
        return section == null ? null : validateSection(section);
    }

    private String validateLanguage(String value) {
        for (var language : DocLanguage.values()) {
            if (language.value().equals(value)) {
                return value;
            }
        }
        throw new BusinessException(ErrorCode.DOC_LANGUAGE_INVALID);
    }

    private String validateSection(String value) {
        for (var section : DocSection.values()) {
            if (section.value().equals(value)) {
                return value;
            }
        }
        throw new BusinessException(ErrorCode.DOC_SECTION_INVALID);
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
