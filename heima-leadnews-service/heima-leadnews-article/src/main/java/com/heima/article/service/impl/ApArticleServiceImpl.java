package com.heima.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService{

    // 为什么集成了MybatisPlus还要注入mapper，因为进行了自定义查询并且是多表查询，MP针对多表不是很好，所以自己写。
    @Autowired
    private ApArticleMapper apArticleMapper;

    private static final short MAX_PAGE_SIZE = 50;

    /**
     * 加载文章列表
     *
     * @param dto
     * @param type 1:加载更多 2:加载最新
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        // 1. 参数校验
        // 分页条数校验
        Integer size = dto.getSize();
        if (size == null || size == 0) {
            size  = 10;
        }
        // 分页的值不超过50
        size = Math.min(size, MAX_PAGE_SIZE);
        dto.setSize(size);

        // 参数校验 --> type
        if (!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)) {
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }

        //  频道参数校验
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        // 3. 时间校验
        if (dto.getMaxBehotTime() == null) {
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime() == null) {
            dto.setMinBehotTime(new Date());
        }

        // 2. 查询
        List<ApArticle> articleList = apArticleMapper.loadArticleList(dto, type);

        // 3. 结果返回
        return ResponseResult.okResult(articleList);
    }
}
