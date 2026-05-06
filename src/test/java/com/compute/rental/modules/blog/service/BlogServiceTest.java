package com.compute.rental.modules.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.BlogPublishStatus;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.modules.blog.dto.BlogCategoryRequest;
import com.compute.rental.modules.blog.dto.BlogPostRequest;
import com.compute.rental.modules.blog.dto.BlogPostTranslationRequest;
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
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogCategoryMapper categoryMapper;
    @Mock
    private BlogTagMapper tagMapper;
    @Mock
    private BlogPostMapper postMapper;
    @Mock
    private BlogPostTagMapper postTagMapper;
    @Mock
    private BlogCategoryTranslationMapper categoryTranslationMapper;
    @Mock
    private BlogTagTranslationMapper tagTranslationMapper;
    @Mock
    private BlogPostTranslationMapper postTranslationMapper;
    @Mock
    private AdminLogService adminLogService;

    private BlogService blogService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogCategory.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogTag.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogPost.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogPostTag.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogCategoryTranslation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogTagTranslation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogPostTranslation.class);
    }

    @BeforeEach
    void setUp() {
        blogService = new BlogService(categoryMapper, tagMapper, postMapper, postTagMapper, categoryTranslationMapper,
                tagTranslationMapper, postTranslationMapper, adminLogService);
    }

    @Test
    void publicCategoriesReturnOnlyEnabledRecords() {
        var category = new BlogCategory();
        category.setId(1L);
        category.setStatus(CommonStatus.ENABLED.value());
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));

        var categories = blogService.publicCategories();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).status()).isEqualTo(CommonStatus.ENABLED.value());
        verify(categoryMapper).selectList(any());
    }

    @Test
    void createCategoryShouldReturnResponseDtoAndWriteAdminLog() {
        when(categoryMapper.insert(any(BlogCategory.class))).thenAnswer(invocation -> {
            BlogCategory category = invocation.getArgument(0);
            category.setId(7L);
            return 1;
        });

        var result = blogService.createCategory(
                new BlogCategoryRequest("news", 1, null), 1L, "127.0.0.1");

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.categoryName()).isEqualTo("news");
        assertThat(result.status()).isEqualTo(CommonStatus.ENABLED.value());
        verify(adminLogService).log(eq(1L), eq("CREATE_BLOG_CATEGORY"), eq("blog_category"), eq(7L),
                isNull(), isNull(), eq("news"), eq("127.0.0.1"));
    }

    @Test
    void publicPostsReturnPublishedRecords() {
        var page = new Page<BlogPost>(1, 10);
        var post = post(BlogPublishStatus.PUBLISHED.value());
        page.setRecords(List.of(post));
        page.setTotal(1);
        when(postMapper.selectPage(any(), any())).thenReturn(page);
        when(postTagMapper.selectList(any())).thenReturn(List.of());

        var result = blogService.publicPosts(1, 10, null, null, null, null);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).publishStatus()).isEqualTo(BlogPublishStatus.PUBLISHED.value());
    }

    @Test
    void publicCategoriesUseRequestedTranslation() {
        var category = new BlogCategory();
        category.setId(1L);
        category.setCategoryName("GPU选型");
        category.setStatus(CommonStatus.ENABLED.value());
        var translation = new BlogCategoryTranslation();
        translation.setCategoryId(1L);
        translation.setLocale("en-US");
        translation.setCategoryName("GPU Selection");
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(categoryTranslationMapper.selectList(any())).thenReturn(List.of(translation));

        var categories = blogService.publicCategories("en-US");

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).categoryName()).isEqualTo("GPU Selection");
        assertThat(categories.get(0).locale()).isEqualTo("en-US");
        assertThat(categories.get(0).localeFallback()).isFalse();
    }

    @Test
    void publicPostsFallbackMissingTranslatedContent() {
        var page = new Page<BlogPost>(1, 10);
        var post = post(BlogPublishStatus.PUBLISHED.value());
        page.setRecords(List.of(post));
        page.setTotal(1);
        var translation = new BlogPostTranslation();
        translation.setPostId(9L);
        translation.setLocale("en-US");
        translation.setTitle("English title");
        translation.setSummary("English summary");
        when(postMapper.selectPage(any(), any())).thenReturn(page);
        when(postTranslationMapper.selectList(any())).thenReturn(List.of(translation));
        when(postTagMapper.selectList(any())).thenReturn(List.of());

        var result = blogService.publicPosts(1, 10, null, null, null, null, "en-US");

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).title()).isEqualTo("English title");
        assertThat(result.records().get(0).summary()).isEqualTo("English summary");
        assertThat(result.records().get(0).contentMarkdown()).isEqualTo("content");
        assertThat(result.records().get(0).locale()).isEqualTo("zh-CN");
        assertThat(result.records().get(0).requestedLocale()).isEqualTo("en-US");
        assertThat(result.records().get(0).localeFallback()).isTrue();
    }

    @Test
    void createPostWritesAdminLog() {
        when(postMapper.insert(any(BlogPost.class))).thenAnswer(invocation -> {
            BlogPost post = invocation.getArgument(0);
            post.setId(9L);
            return 1;
        });
        when(postMapper.selectById(9L)).thenReturn(post(BlogPublishStatus.DRAFT.value()));
        when(postTagMapper.selectList(any())).thenReturn(List.of());

        blogService.createPost(new BlogPostRequest(null, "title", "summary", null,
                "content", BlogPublishStatus.DRAFT.value(), 0, 0, List.of()), 1L, "127.0.0.1");

        verify(adminLogService).log(eq(1L), eq("CREATE_BLOG_POST"), eq("blog_post"), eq(9L),
                isNull(), isNull(), eq("title"), eq("127.0.0.1"));
    }

    @Test
    void updatePostTranslationCreatesEnglishTranslation() {
        when(postMapper.selectById(9L)).thenReturn(post(BlogPublishStatus.DRAFT.value()));

        var result = blogService.updatePostTranslation(9L,
                new BlogPostTranslationRequest("en-US", "English title", "English summary", "English content"),
                1L, "127.0.0.1");

        assertThat(result.postId()).isEqualTo(9L);
        assertThat(result.locale()).isEqualTo("en-US");
        assertThat(result.title()).isEqualTo("English title");
        assertThat(result.summary()).isEqualTo("English summary");
        assertThat(result.contentMarkdown()).isEqualTo("English content");

        verify(postTranslationMapper).insert(any(BlogPostTranslation.class));
        verify(adminLogService).log(eq(1L), eq("UPDATE_BLOG_POST_TRANSLATION"), eq("blog_post"), eq(9L),
                isNull(), isNull(), eq("en-US"), eq("127.0.0.1"));
    }

    private BlogPost post(Integer status) {
        var post = new BlogPost();
        post.setId(9L);
        post.setTitle("title");
        post.setContentMarkdown("content");
        post.setPublishStatus(status);
        post.setViewCount(0L);
        return post;
    }
}
