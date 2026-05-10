package com.compute.rental.modules.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.BlogPublishStatus;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.blog.dto.BlogCategoryRequest;
import com.compute.rental.modules.blog.dto.BlogCategoryResponse;
import com.compute.rental.modules.blog.dto.BlogCategoryTranslationRequest;
import com.compute.rental.modules.blog.dto.BlogCategoryTranslationResponse;
import com.compute.rental.modules.blog.dto.BlogPostResponse;
import com.compute.rental.modules.blog.dto.BlogPostRequest;
import com.compute.rental.modules.blog.dto.BlogPostTranslationRequest;
import com.compute.rental.modules.blog.dto.BlogPostTranslationResponse;
import com.compute.rental.modules.blog.dto.BlogTagRequest;
import com.compute.rental.modules.blog.dto.BlogTagResponse;
import com.compute.rental.modules.blog.dto.BlogTagTranslationRequest;
import com.compute.rental.modules.blog.dto.BlogTagTranslationResponse;
import com.compute.rental.modules.blog.entity.BlogCategory;
import com.compute.rental.modules.blog.entity.BlogCategoryTranslation;
import com.compute.rental.modules.blog.entity.BlogPost;
import com.compute.rental.modules.blog.entity.BlogPostTag;
import com.compute.rental.modules.blog.entity.BlogPostTranslation;
import com.compute.rental.modules.blog.entity.BlogTag;
import com.compute.rental.modules.blog.entity.BlogTagTranslation;
import com.compute.rental.modules.blog.mapper.BlogCategoryMapper;
import com.compute.rental.modules.blog.mapper.BlogCategoryTranslationMapper;
import com.compute.rental.modules.blog.mapper.BlogPostMapper;
import com.compute.rental.modules.blog.mapper.BlogPostTagMapper;
import com.compute.rental.modules.blog.mapper.BlogPostTranslationMapper;
import com.compute.rental.modules.blog.mapper.BlogTagMapper;
import com.compute.rental.modules.blog.mapper.BlogTagTranslationMapper;
import com.compute.rental.modules.system.service.AdminLogService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BlogService {

    private final BlogCategoryMapper categoryMapper;
    private final BlogTagMapper tagMapper;
    private final BlogPostMapper postMapper;
    private final BlogPostTagMapper postTagMapper;
    private final BlogCategoryTranslationMapper categoryTranslationMapper;
    private final BlogTagTranslationMapper tagTranslationMapper;
    private final BlogPostTranslationMapper postTranslationMapper;
    private final AdminLogService adminLogService;

    public BlogService(
            BlogCategoryMapper categoryMapper,
            BlogTagMapper tagMapper,
            BlogPostMapper postMapper,
            BlogPostTagMapper postTagMapper,
            BlogCategoryTranslationMapper categoryTranslationMapper,
            BlogTagTranslationMapper tagTranslationMapper,
            BlogPostTranslationMapper postTranslationMapper,
            AdminLogService adminLogService
    ) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.postMapper = postMapper;
        this.postTagMapper = postTagMapper;
        this.categoryTranslationMapper = categoryTranslationMapper;
        this.tagTranslationMapper = tagTranslationMapper;
        this.postTranslationMapper = postTranslationMapper;
        this.adminLogService = adminLogService;
    }

    public List<BlogCategoryResponse> publicCategories() {
        return publicCategories(LanguageResolver.DEFAULT_LANGUAGE);
    }

    public List<BlogCategoryResponse> publicCategories(String locale) {
        var categories = categoryMapper.selectList(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(BlogCategory::getSortNo)
                .orderByDesc(BlogCategory::getId));
        var translations = categoryTranslationMap(categories.stream().map(BlogCategory::getId).toList(), locale);
        return categories
                .stream()
                .map(category -> categoryResponse(category, translations.get(category.getId()), locale))
                .toList();
    }

    public List<BlogTagResponse> publicTags() {
        return publicTags(LanguageResolver.DEFAULT_LANGUAGE);
    }

    public List<BlogTagResponse> publicTags(String locale) {
        var tags = tagMapper.selectList(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(BlogTag::getSortNo)
                .orderByDesc(BlogTag::getId));
        var translations = tagTranslationMap(tags.stream().map(BlogTag::getId).toList(), locale);
        return tags
                .stream()
                .map(tag -> tagResponse(tag, translations.get(tag.getId()), locale))
                .toList();
    }

    public PageResult<BlogPostResponse> publicPosts(long pageNo, long pageSize, Long categoryId, Long tagId,
                                                    LocalDateTime startTime, LocalDateTime endTime) {
        return publicPosts(pageNo, pageSize, categoryId, tagId, startTime, endTime, LanguageResolver.DEFAULT_LANGUAGE);
    }

    public PageResult<BlogPostResponse> publicPosts(long pageNo, long pageSize, Long categoryId, Long tagId,
                                                    LocalDateTime startTime, LocalDateTime endTime, String locale) {
        return pagePosts(pageNo, pageSize, categoryId, tagId, BlogPublishStatus.PUBLISHED.value(),
                startTime, endTime, true, locale);
    }

    public BlogPostResponse publicPost(Long id) {
        return publicPost(id, LanguageResolver.DEFAULT_LANGUAGE);
    }

    public BlogPostResponse publicPost(Long id, String locale) {
        var post = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getId, id)
                .eq(BlogPost::getPublishStatus, BlogPublishStatus.PUBLISHED.value())
                .last("LIMIT 1"));
        if (post == null) {
            throw new BusinessException(ErrorCode.BLOG_POST_NOT_FOUND);
        }
        postMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                .eq(BlogPost::getId, id)
                .setSql("view_count = view_count + 1"));
        return postResponse(post, postTranslation(post.getId(), locale), locale);
    }

    public PageResult<BlogCategoryResponse> adminCategories(long pageNo, long pageSize, Integer status) {
        var page = new Page<BlogCategory>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<BlogCategory>()
                .eq(status != null, BlogCategory::getStatus, status)
                .orderByAsc(BlogCategory::getSortNo)
                .orderByDesc(BlogCategory::getId);
        var result = categoryMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::categoryResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public BlogCategoryResponse createCategory(BlogCategoryRequest request, Long adminId, String ip) {
        var now = DateTimeUtils.now();
        var category = new BlogCategory();
        applyCategoryRequest(category, request);
        category.setId(null);
        category.setStatus(defaultStatus(category.getStatus()));
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        categoryMapper.insert(category);
        log(adminId, "CREATE_BLOG_CATEGORY", "blog_category", category.getId(), category.getCategoryName(), ip);
        return categoryResponse(category);
    }

    @Transactional
    public BlogCategoryResponse updateCategory(Long id, BlogCategoryRequest request, Long adminId, String ip) {
        requireCategory(id);
        var category = new BlogCategory();
        applyCategoryRequest(category, request);
        category.setId(id);
        category.setUpdatedAt(DateTimeUtils.now());
        categoryMapper.updateById(category);
        log(adminId, "UPDATE_BLOG_CATEGORY", "blog_category", id, category.getCategoryName(), ip);
        return categoryResponse(requireCategory(id));
    }

    @Transactional
    public BlogCategoryResponse setCategoryStatus(Long id, Integer status, Long adminId, String ip) {
        requireCategory(id);
        categoryMapper.update(null, new LambdaUpdateWrapper<BlogCategory>()
                .eq(BlogCategory::getId, id)
                .set(BlogCategory::getStatus, status)
                .set(BlogCategory::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("BLOG_CATEGORY", status), "blog_category", id, "status=" + status, ip);
        return categoryResponse(requireCategory(id));
    }

    public List<BlogCategoryTranslationResponse> listCategoryTranslations(Long id) {
        var category = requireCategory(id);
        var english = categoryTranslation(id, LanguageResolver.EN_US);
        return List.of(
                new BlogCategoryTranslationResponse(id, LanguageResolver.DEFAULT_LANGUAGE,
                        category.getCategoryName(), true, category.getCreatedAt(), category.getUpdatedAt()),
                categoryTranslationResponse(id, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public BlogCategoryTranslationResponse updateCategoryTranslation(
            Long id,
            BlogCategoryTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var category = requireCategory(id);
        var locale = requireSupportedLocale(request.locale());
        var categoryName = trimToNull(request.categoryName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) && categoryName != null) {
            var now = DateTimeUtils.now();
            categoryMapper.update(null, new LambdaUpdateWrapper<BlogCategory>()
                    .eq(BlogCategory::getId, id)
                    .set(BlogCategory::getCategoryName, categoryName)
                    .set(BlogCategory::getUpdatedAt, now));
            category.setCategoryName(categoryName);
            category.setUpdatedAt(now);
        }
        var response = upsertCategoryTranslation(id, locale, category.getCategoryName(), categoryName);
        log(adminId, "UPDATE_BLOG_CATEGORY_TRANSLATION", "blog_category", id, response.locale(), ip);
        return response;
    }

    public PageResult<BlogTagResponse> adminTags(long pageNo, long pageSize, Integer status) {
        var page = new Page<BlogTag>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<BlogTag>()
                .eq(status != null, BlogTag::getStatus, status)
                .orderByAsc(BlogTag::getSortNo)
                .orderByDesc(BlogTag::getId);
        var result = tagMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::tagResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public BlogTagResponse createTag(BlogTagRequest request, Long adminId, String ip) {
        var now = DateTimeUtils.now();
        var tag = new BlogTag();
        applyTagRequest(tag, request);
        tag.setId(null);
        tag.setStatus(defaultStatus(tag.getStatus()));
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        tagMapper.insert(tag);
        log(adminId, "CREATE_BLOG_TAG", "blog_tag", tag.getId(), tag.getTagName(), ip);
        return tagResponse(tag);
    }

    @Transactional
    public BlogTagResponse updateTag(Long id, BlogTagRequest request, Long adminId, String ip) {
        requireTag(id);
        var tag = new BlogTag();
        applyTagRequest(tag, request);
        tag.setId(id);
        tag.setUpdatedAt(DateTimeUtils.now());
        tagMapper.updateById(tag);
        log(adminId, "UPDATE_BLOG_TAG", "blog_tag", id, tag.getTagName(), ip);
        return tagResponse(requireTag(id));
    }

    @Transactional
    public BlogTagResponse setTagStatus(Long id, Integer status, Long adminId, String ip) {
        requireTag(id);
        tagMapper.update(null, new LambdaUpdateWrapper<BlogTag>()
                .eq(BlogTag::getId, id)
                .set(BlogTag::getStatus, status)
                .set(BlogTag::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, statusAction("BLOG_TAG", status), "blog_tag", id, "status=" + status, ip);
        return tagResponse(requireTag(id));
    }

    public List<BlogTagTranslationResponse> listTagTranslations(Long id) {
        var tag = requireTag(id);
        var english = tagTranslation(id, LanguageResolver.EN_US);
        return List.of(
                new BlogTagTranslationResponse(id, LanguageResolver.DEFAULT_LANGUAGE, tag.getTagName(), true,
                        tag.getCreatedAt(), tag.getUpdatedAt()),
                tagTranslationResponse(id, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public BlogTagTranslationResponse updateTagTranslation(
            Long id,
            BlogTagTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var tag = requireTag(id);
        var locale = requireSupportedLocale(request.locale());
        var tagName = trimToNull(request.tagName());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) && tagName != null) {
            var now = DateTimeUtils.now();
            tagMapper.update(null, new LambdaUpdateWrapper<BlogTag>()
                    .eq(BlogTag::getId, id)
                    .set(BlogTag::getTagName, tagName)
                    .set(BlogTag::getUpdatedAt, now));
            tag.setTagName(tagName);
            tag.setUpdatedAt(now);
        }
        var response = upsertTagTranslation(id, locale, tag.getTagName(), tagName);
        log(adminId, "UPDATE_BLOG_TAG_TRANSLATION", "blog_tag", id, response.locale(), ip);
        return response;
    }

    public PageResult<BlogPostResponse> adminPosts(long pageNo, long pageSize, Long categoryId, Long tagId,
                                                   Integer publishStatus, LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        return pagePosts(pageNo, pageSize, categoryId, tagId, publishStatus, startTime, endTime, false,
                LanguageResolver.DEFAULT_LANGUAGE);
    }

    public BlogPostResponse adminPost(Long id) {
        return postResponse(requirePost(id));
    }

    public List<BlogPostTranslationResponse> listPostTranslations(Long id) {
        var post = requirePost(id);
        var english = postTranslation(id, LanguageResolver.EN_US);
        return List.of(
                new BlogPostTranslationResponse(id, LanguageResolver.DEFAULT_LANGUAGE, post.getTitle(),
                        post.getSummary(), post.getContentMarkdown(), true, post.getCreatedAt(), post.getUpdatedAt()),
                postTranslationResponse(id, LanguageResolver.EN_US, english)
        );
    }

    @Transactional
    public BlogPostResponse createPost(BlogPostRequest request, Long adminId, String ip) {
        var now = DateTimeUtils.now();
        var post = new BlogPost();
        applyPostRequest(post, request);
        post.setPublishStatus(request.publishStatus() == null ? BlogPublishStatus.DRAFT.value() : request.publishStatus());
        post.setViewCount(0L);
        post.setCreatedBy(adminId);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        if (Integer.valueOf(BlogPublishStatus.PUBLISHED.value()).equals(post.getPublishStatus())) {
            post.setPublishedAt(now);
        }
        postMapper.insert(post);
        replaceTags(post.getId(), request.tagIds());
        log(adminId, "CREATE_BLOG_POST", "blog_post", post.getId(), post.getTitle(), ip);
        return postResponse(postMapper.selectById(post.getId()));
    }

    @Transactional
    public BlogPostResponse updatePost(Long id, BlogPostRequest request, Long adminId, String ip) {
        var post = requirePost(id);
        applyPostRequest(post, request);
        post.setUpdatedAt(DateTimeUtils.now());
        postMapper.updateById(post);
        replaceTags(id, request.tagIds());
        log(adminId, "UPDATE_BLOG_POST", "blog_post", id, post.getTitle(), ip);
        return postResponse(requirePost(id));
    }

    @Transactional
    public BlogPostTranslationResponse updatePostTranslation(
            Long id,
            BlogPostTranslationRequest request,
            Long adminId,
            String ip
    ) {
        var post = requirePost(id);
        var locale = requireSupportedLocale(request.locale());
        var title = trimToNull(request.title());
        var summary = trimToNull(request.summary());
        var contentMarkdown = trimToNull(request.contentMarkdown());
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale)
                && (title != null || summary != null || contentMarkdown != null)) {
            var now = DateTimeUtils.now();
            postMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                    .eq(BlogPost::getId, id)
                    .set(BlogPost::getTitle, title == null ? post.getTitle() : title)
                    .set(BlogPost::getSummary, summary == null ? post.getSummary() : summary)
                    .set(BlogPost::getContentMarkdown,
                            contentMarkdown == null ? post.getContentMarkdown() : contentMarkdown)
                    .set(BlogPost::getUpdatedAt, now));
            post.setTitle(title == null ? post.getTitle() : title);
            post.setSummary(summary == null ? post.getSummary() : summary);
            post.setContentMarkdown(contentMarkdown == null ? post.getContentMarkdown() : contentMarkdown);
            post.setUpdatedAt(now);
        }
        var response = upsertPostTranslation(post, locale, post.getTitle(), post.getSummary(),
                post.getContentMarkdown(), title, summary, contentMarkdown);
        log(adminId, "UPDATE_BLOG_POST_TRANSLATION", "blog_post", id, response.locale(), ip);
        return response;
    }

    @Transactional
    public BlogPostResponse publishPost(Long id, Long adminId, String ip) {
        requirePost(id);
        var now = DateTimeUtils.now();
        postMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                .eq(BlogPost::getId, id)
                .set(BlogPost::getPublishStatus, BlogPublishStatus.PUBLISHED.value())
                .set(BlogPost::getPublishedAt, now)
                .set(BlogPost::getUpdatedAt, now));
        log(adminId, "PUBLISH_BLOG_POST", "blog_post", id, "published", ip);
        return postResponse(requirePost(id));
    }

    @Transactional
    public BlogPostResponse unpublishPost(Long id, Long adminId, String ip) {
        requirePost(id);
        postMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                .eq(BlogPost::getId, id)
                .set(BlogPost::getPublishStatus, BlogPublishStatus.OFFLINE.value())
                .set(BlogPost::getUpdatedAt, DateTimeUtils.now()));
        log(adminId, "UNPUBLISH_BLOG_POST", "blog_post", id, "offline", ip);
        return postResponse(requirePost(id));
    }

    @Transactional
    public void deletePost(Long id, Long adminId, String ip) {
        requirePost(id);
        postTagMapper.delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getPostId, id));
        postTranslationMapper.delete(new LambdaQueryWrapper<BlogPostTranslation>()
                .eq(BlogPostTranslation::getPostId, id));
        postMapper.deleteById(id);
        log(adminId, "DELETE_BLOG_POST", "blog_post", id, "deleted", ip);
    }

    private PageResult<BlogPostResponse> pagePosts(long pageNo, long pageSize, Long categoryId, Long tagId,
                                                   Integer publishStatus, LocalDateTime startTime,
                                                   LocalDateTime endTime, boolean publicOnly, String locale) {
        var postIds = postIdsByTag(tagId);
        if (tagId != null && postIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize);
        }
        var page = new Page<BlogPost>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<BlogPost>()
                .eq(categoryId != null, BlogPost::getCategoryId, categoryId)
                .in(tagId != null, BlogPost::getId, postIds)
                .eq(publicOnly, BlogPost::getPublishStatus, BlogPublishStatus.PUBLISHED.value())
                .eq(!publicOnly && publishStatus != null, BlogPost::getPublishStatus, publishStatus)
                .ge(startTime != null, BlogPost::getPublishedAt, startTime)
                .le(endTime != null, BlogPost::getPublishedAt, endTime)
                .orderByDesc(BlogPost::getIsTop)
                .orderByAsc(BlogPost::getSortNo)
                .orderByDesc(BlogPost::getPublishedAt)
                .orderByDesc(BlogPost::getId);
        var result = postMapper.selectPage(page, wrapper);
        var posts = result.getRecords();
        var translations = postTranslationMap(posts.stream().map(BlogPost::getId).toList(), locale);
        return new PageResult<>(posts.stream()
                .map(post -> postResponse(post, translations.get(post.getId()), locale))
                .toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private List<Long> postIdsByTag(Long tagId) {
        if (tagId == null) {
            return Collections.emptyList();
        }
        return postTagMapper.selectList(new LambdaQueryWrapper<BlogPostTag>()
                        .eq(BlogPostTag::getTagId, tagId))
                .stream()
                .map(BlogPostTag::getPostId)
                .toList();
    }

    private void applyPostRequest(BlogPost post, BlogPostRequest request) {
        post.setCategoryId(request.categoryId());
        post.setTitle(request.title());
        post.setSummary(request.summary());
        post.setCoverImageUrl(request.coverImageUrl());
        post.setContentMarkdown(request.contentMarkdown());
        if (request.publishStatus() != null) {
            post.setPublishStatus(request.publishStatus());
        }
        post.setIsTop(request.isTop() == null ? 0 : request.isTop());
        post.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
    }

    private void replaceTags(Long postId, List<Long> tagIds) {
        postTagMapper.delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getPostId, postId));
        if (tagIds == null) {
            return;
        }
        var now = DateTimeUtils.now();
        for (var tagId : tagIds.stream().distinct().toList()) {
            var relation = new BlogPostTag();
            relation.setPostId(postId);
            relation.setTagId(tagId);
            relation.setCreatedAt(now);
            postTagMapper.insert(relation);
        }
    }

    private BlogPostResponse postResponse(BlogPost post) {
        return postResponse(post, null, LanguageResolver.DEFAULT_LANGUAGE);
    }

    private BlogPostResponse postResponse(BlogPost post, BlogPostTranslation translation, String requestedLocale) {
        var title = localized(post.getTitle(), requestedLocale, translation == null ? null : translation.getTitle());
        var summary = localized(post.getSummary(), requestedLocale, translation == null ? null : translation.getSummary());
        var content = localized(post.getContentMarkdown(), requestedLocale,
                translation == null ? null : translation.getContentMarkdown());
        var categoryName = categoryName(post.getCategoryId(), requestedLocale);
        var localeFallback = title.fallback() || summary.fallback() || content.fallback() || categoryName.fallback();
        return new BlogPostResponse(
                post.getId(),
                post.getCategoryId(),
                categoryName.value(),
                title.value(),
                summary.value(),
                post.getCoverImageUrl(),
                content.value(),
                post.getPublishStatus(),
                post.getPublishedAt(),
                post.getIsTop(),
                post.getSortNo(),
                post.getViewCount(),
                post.getCreatedBy(),
                tagIds(post.getId()),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                localeFallback ? LanguageResolver.DEFAULT_LANGUAGE : requestedLocale,
                requestedLocale,
                localeFallback);
    }

    private LocalizedText categoryName(Long categoryId, String requestedLocale) {
        if (categoryId == null) {
            return LocalizedText.empty(requestedLocale);
        }
        var category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return LocalizedText.empty(requestedLocale);
        }
        return localized(category.getCategoryName(), requestedLocale, categoryTranslationName(
                categoryTranslation(categoryId, requestedLocale)));
    }

    private List<Long> tagIds(Long postId) {
        return postTagMapper.selectList(new LambdaQueryWrapper<BlogPostTag>()
                        .eq(BlogPostTag::getPostId, postId))
                .stream()
                .map(BlogPostTag::getTagId)
                .toList();
    }

    private void applyCategoryRequest(BlogCategory category, BlogCategoryRequest request) {
        category.setCategoryName(request.categoryName());
        category.setSortNo(request.sortNo());
        category.setStatus(request.status());
    }

    private BlogCategoryResponse categoryResponse(BlogCategory category) {
        return categoryResponse(category, null, LanguageResolver.DEFAULT_LANGUAGE);
    }

    private BlogCategoryResponse categoryResponse(BlogCategory category, BlogCategoryTranslation translation,
                                                  String requestedLocale) {
        var categoryName = localized(category.getCategoryName(), requestedLocale, categoryTranslationName(translation));
        return new BlogCategoryResponse(
                category.getId(),
                categoryName.value(),
                category.getSortNo(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                categoryName.locale(),
                requestedLocale,
                categoryName.fallback()
        );
    }

    private void applyTagRequest(BlogTag tag, BlogTagRequest request) {
        tag.setTagName(request.tagName());
        tag.setSortNo(request.sortNo());
        tag.setStatus(request.status());
    }

    private BlogTagResponse tagResponse(BlogTag tag) {
        return tagResponse(tag, null, LanguageResolver.DEFAULT_LANGUAGE);
    }

    private BlogTagResponse tagResponse(BlogTag tag, BlogTagTranslation translation, String requestedLocale) {
        var tagName = localized(tag.getTagName(), requestedLocale, tagTranslationName(translation));
        return new BlogTagResponse(
                tag.getId(),
                tagName.value(),
                tag.getSortNo(),
                tag.getStatus(),
                tag.getCreatedAt(),
                tag.getUpdatedAt(),
                tagName.locale(),
                requestedLocale,
                tagName.fallback()
        );
    }

    private Map<Long, BlogCategoryTranslation> categoryTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryTranslationMapper.selectList(new LambdaQueryWrapper<BlogCategoryTranslation>()
                        .in(BlogCategoryTranslation::getCategoryId, ids)
                        .eq(BlogCategoryTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(BlogCategoryTranslation::getCategoryId, Function.identity()));
    }

    private Map<Long, BlogTagTranslation> tagTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return tagTranslationMapper.selectList(new LambdaQueryWrapper<BlogTagTranslation>()
                        .in(BlogTagTranslation::getTagId, ids)
                        .eq(BlogTagTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(BlogTagTranslation::getTagId, Function.identity()));
    }

    private Map<Long, BlogPostTranslation> postTranslationMap(Collection<Long> ids, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return postTranslationMapper.selectList(new LambdaQueryWrapper<BlogPostTranslation>()
                        .in(BlogPostTranslation::getPostId, ids)
                        .eq(BlogPostTranslation::getLocale, locale))
                .stream()
                .collect(Collectors.toMap(BlogPostTranslation::getPostId, Function.identity()));
    }

    private BlogCategoryTranslation categoryTranslation(Long categoryId, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || categoryId == null) {
            return null;
        }
        return findCategoryTranslation(categoryId, locale);
    }

    private BlogTagTranslation tagTranslation(Long tagId, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || tagId == null) {
            return null;
        }
        return findTagTranslation(tagId, locale);
    }

    private BlogPostTranslation postTranslation(Long postId, String locale) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(locale) || postId == null) {
            return null;
        }
        return findPostTranslation(postId, locale);
    }

    private BlogCategoryTranslation findCategoryTranslation(Long categoryId, String locale) {
        return categoryTranslationMapper.selectOne(new LambdaQueryWrapper<BlogCategoryTranslation>()
                .eq(BlogCategoryTranslation::getCategoryId, categoryId)
                .eq(BlogCategoryTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private BlogTagTranslation findTagTranslation(Long tagId, String locale) {
        return tagTranslationMapper.selectOne(new LambdaQueryWrapper<BlogTagTranslation>()
                .eq(BlogTagTranslation::getTagId, tagId)
                .eq(BlogTagTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private BlogPostTranslation findPostTranslation(Long postId, String locale) {
        return postTranslationMapper.selectOne(new LambdaQueryWrapper<BlogPostTranslation>()
                .eq(BlogPostTranslation::getPostId, postId)
                .eq(BlogPostTranslation::getLocale, locale)
                .last("LIMIT 1"));
    }

    private BlogCategoryTranslationResponse upsertCategoryTranslation(
            Long categoryId,
            String locale,
            String defaultCategoryName,
            String categoryName
    ) {
        var now = DateTimeUtils.now();
        var translation = findCategoryTranslation(categoryId, locale);
        if (translation == null) {
            translation = new BlogCategoryTranslation();
            translation.setCategoryId(categoryId);
            translation.setLocale(locale);
            translation.setCategoryName(categoryName == null ? defaultCategoryName : categoryName);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            categoryTranslationMapper.insert(translation);
        } else {
            translation.setCategoryName(categoryName == null ? translation.getCategoryName() : categoryName);
            translation.setUpdatedAt(now);
            categoryTranslationMapper.updateById(translation);
        }
        return categoryTranslationResponse(categoryId, locale, translation);
    }

    private BlogTagTranslationResponse upsertTagTranslation(
            Long tagId,
            String locale,
            String defaultTagName,
            String tagName
    ) {
        var now = DateTimeUtils.now();
        var translation = findTagTranslation(tagId, locale);
        if (translation == null) {
            translation = new BlogTagTranslation();
            translation.setTagId(tagId);
            translation.setLocale(locale);
            translation.setTagName(tagName == null ? defaultTagName : tagName);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            tagTranslationMapper.insert(translation);
        } else {
            translation.setTagName(tagName == null ? translation.getTagName() : tagName);
            translation.setUpdatedAt(now);
            tagTranslationMapper.updateById(translation);
        }
        return tagTranslationResponse(tagId, locale, translation);
    }

    private BlogPostTranslationResponse upsertPostTranslation(
            BlogPost post,
            String locale,
            String defaultTitle,
            String defaultSummary,
            String defaultContentMarkdown,
            String title,
            String summary,
            String contentMarkdown
    ) {
        var now = DateTimeUtils.now();
        var translation = findPostTranslation(post.getId(), locale);
        if (translation == null) {
            translation = new BlogPostTranslation();
            translation.setPostId(post.getId());
            translation.setLocale(locale);
            translation.setTitle(title == null ? defaultTitle : title);
            translation.setSummary(summary == null ? defaultSummary : summary);
            translation.setContentMarkdown(contentMarkdown == null ? defaultContentMarkdown : contentMarkdown);
            translation.setCreatedAt(now);
            translation.setUpdatedAt(now);
            postTranslationMapper.insert(translation);
        } else {
            translation.setTitle(title == null ? translation.getTitle() : title);
            translation.setSummary(summary == null ? translation.getSummary() : summary);
            translation.setContentMarkdown(contentMarkdown == null ? translation.getContentMarkdown() : contentMarkdown);
            translation.setUpdatedAt(now);
            postTranslationMapper.updateById(translation);
        }
        return postTranslationResponse(post.getId(), locale, translation);
    }

    private BlogCategoryTranslationResponse categoryTranslationResponse(
            Long categoryId,
            String locale,
            BlogCategoryTranslation translation
    ) {
        return new BlogCategoryTranslationResponse(categoryId, locale,
                translation == null ? null : translation.getCategoryName(), translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
    }

    private BlogTagTranslationResponse tagTranslationResponse(
            Long tagId,
            String locale,
            BlogTagTranslation translation
    ) {
        return new BlogTagTranslationResponse(tagId, locale,
                translation == null ? null : translation.getTagName(), translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
    }

    private BlogPostTranslationResponse postTranslationResponse(
            Long postId,
            String locale,
            BlogPostTranslation translation
    ) {
        return new BlogPostTranslationResponse(postId, locale,
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getSummary(),
                translation == null ? null : translation.getContentMarkdown(),
                translation != null,
                translation == null ? null : translation.getCreatedAt(),
                translation == null ? null : translation.getUpdatedAt());
    }

    private LocalizedText localized(String defaultValue, String requestedLocale, String translatedValue) {
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(requestedLocale)) {
            return new LocalizedText(defaultValue, requestedLocale, false);
        }
        if (StringUtils.hasText(translatedValue)) {
            return new LocalizedText(translatedValue, requestedLocale, false);
        }
        if (!StringUtils.hasText(defaultValue)) {
            return new LocalizedText(defaultValue, requestedLocale, false);
        }
        return new LocalizedText(defaultValue, LanguageResolver.DEFAULT_LANGUAGE, true);
    }

    private String categoryTranslationName(BlogCategoryTranslation translation) {
        return translation == null ? null : translation.getCategoryName();
    }

    private String tagTranslationName(BlogTagTranslation translation) {
        return translation == null ? null : translation.getTagName();
    }

    private BlogCategory requireCategory(Long id) {
        var category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.BLOG_CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private BlogTag requireTag(Long id) {
        var tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.BLOG_TAG_NOT_FOUND);
        }
        return tag;
    }

    private BlogPost requirePost(Long id) {
        var post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.BLOG_POST_NOT_FOUND);
        }
        return post;
    }

    private Integer defaultStatus(Integer status) {
        return status == null ? CommonStatus.ENABLED.value() : status;
    }

    private String statusAction(String subject, Integer status) {
        return Integer.valueOf(CommonStatus.ENABLED.value()).equals(status) ? "ENABLE_" + subject : "DISABLE_" + subject;
    }

    private void log(Long adminId, String action, String table, Long targetId, String remark, String ip) {
        adminLogService.log(adminId, action, table, targetId, null, null,
                StringUtils.hasText(remark) ? remark : action, ip);
    }

    private String requireSupportedLocale(String locale) {
        var normalized = StringUtils.hasText(locale) ? locale.trim().replace('_', '-') : null;
        if (LanguageResolver.DEFAULT_LANGUAGE.equals(normalized) || LanguageResolver.EN_US.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported locale: " + locale);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record LocalizedText(String value, String locale, boolean fallback) {

        private static LocalizedText empty(String locale) {
            return new LocalizedText(null, locale, false);
        }
    }
}
