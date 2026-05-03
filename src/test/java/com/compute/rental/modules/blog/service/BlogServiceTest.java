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
import com.compute.rental.modules.blog.entity.BlogCategory;
import com.compute.rental.modules.blog.entity.BlogPost;
import com.compute.rental.modules.blog.entity.BlogPostTag;
import com.compute.rental.modules.blog.entity.BlogTag;
import com.compute.rental.modules.blog.mapper.BlogCategoryMapper;
import com.compute.rental.modules.blog.mapper.BlogPostMapper;
import com.compute.rental.modules.blog.mapper.BlogPostTagMapper;
import com.compute.rental.modules.blog.mapper.BlogTagMapper;
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
    private AdminLogService adminLogService;

    private BlogService blogService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogCategory.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogTag.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogPost.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), BlogPostTag.class);
    }

    @BeforeEach
    void setUp() {
        blogService = new BlogService(categoryMapper, tagMapper, postMapper, postTagMapper, adminLogService);
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
