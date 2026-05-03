package com.compute.rental.modules.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.blog.entity.BlogPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogPostMapper extends BaseMapper<BlogPost> {
}
