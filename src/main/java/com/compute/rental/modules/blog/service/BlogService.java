package com.compute.rental.modules.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.BlogPublishStatus;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.blog.dto.BlogCategoryRequest;
import com.compute.rental.modules.blog.dto.BlogCategoryResponse;
import com.compute.rental.modules.blog.dto.BlogPostResponse;
import com.compute.rental.modules.blog.dto.BlogPostRequest;
import com.compute.rental.modules.blog.dto.BlogTagRequest;
import com.compute.rental.modules.blog.dto.BlogTagResponse;
import com.compute.rental.modules.blog.entity.BlogCategory;
import com.compute.rental.modules.blog.entity.BlogPost;
import com.compute.rental.modules.blog.entity.BlogPostTag;
import com.compute.rental.modules.blog.entity.BlogTag;
import com.compute.rental.modules.blog.mapper.BlogCategoryMapper;
import com.compute.rental.modules.blog.mapper.BlogPostMapper;
import com.compute.rental.modules.blog.mapper.BlogPostTagMapper;
import com.compute.rental.modules.blog.mapper.BlogTagMapper;
import com.compute.rental.modules.system.service.AdminLogService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BlogService {

    private final BlogCategoryMapper categoryMapper;
    private final BlogTagMapper tagMapper;
    private final BlogPostMapper postMapper;
    private final BlogPostTagMapper postTagMapper;
    private final AdminLogService adminLogService;

    public BlogService(
            BlogCategoryMapper categoryMapper,
            BlogTagMapper tagMapper,
            BlogPostMapper postMapper,
            BlogPostTagMapper postTagMapper,
            AdminLogService adminLogService
    ) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.postMapper = postMapper;
        this.postTagMapper = postTagMapper;
        this.adminLogService = adminLogService;
    }

    public List<BlogCategoryResponse> publicCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(BlogCategory::getSortNo)
                .orderByDesc(BlogCategory::getId))
                .stream()
                .map(this::categoryResponse)
                .toList();
    }

    public List<BlogTagResponse> publicTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getStatus, CommonStatus.ENABLED.value())
                .orderByAsc(BlogTag::getSortNo)
                .orderByDesc(BlogTag::getId))
                .stream()
                .map(this::tagResponse)
                .toList();
    }

    public PageResult<BlogPostResponse> publicPosts(long pageNo, long pageSize, Long categoryId, Long tagId,
                                                    LocalDateTime startTime, LocalDateTime endTime) {
        return pagePosts(pageNo, pageSize, categoryId, tagId, BlogPublishStatus.PUBLISHED.value(),
                startTime, endTime, true);
    }

    public BlogPostResponse publicPost(Long id) {
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
        return postResponse(post);
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

    public PageResult<BlogPostResponse> adminPosts(long pageNo, long pageSize, Long categoryId, Long tagId,
                                                   Integer publishStatus, LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        return pagePosts(pageNo, pageSize, categoryId, tagId, publishStatus, startTime, endTime, false);
    }

    public BlogPostResponse adminPost(Long id) {
        return postResponse(requirePost(id));
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
        postMapper.deleteById(id);
        log(adminId, "DELETE_BLOG_POST", "blog_post", id, "deleted", ip);
    }

    private PageResult<BlogPostResponse> pagePosts(long pageNo, long pageSize, Long categoryId, Long tagId,
                                                   Integer publishStatus, LocalDateTime startTime,
                                                   LocalDateTime endTime, boolean publicOnly) {
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
        return new PageResult<>(result.getRecords().stream().map(this::postResponse).toList(),
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
        return new BlogPostResponse(
                post.getId(),
                post.getCategoryId(),
                categoryName(post.getCategoryId()),
                post.getTitle(),
                post.getSummary(),
                post.getCoverImageUrl(),
                post.getContentMarkdown(),
                post.getPublishStatus(),
                post.getPublishedAt(),
                post.getIsTop(),
                post.getSortNo(),
                post.getViewCount(),
                post.getCreatedBy(),
                tagIds(post.getId()),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        var category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getCategoryName();
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
        return new BlogCategoryResponse(
                category.getId(),
                category.getCategoryName(),
                category.getSortNo(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private void applyTagRequest(BlogTag tag, BlogTagRequest request) {
        tag.setTagName(request.tagName());
        tag.setSortNo(request.sortNo());
        tag.setStatus(request.status());
    }

    private BlogTagResponse tagResponse(BlogTag tag) {
        return new BlogTagResponse(
                tag.getId(),
                tag.getTagName(),
                tag.getSortNo(),
                tag.getStatus(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
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
}
