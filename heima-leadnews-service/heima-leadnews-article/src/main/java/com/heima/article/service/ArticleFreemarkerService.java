package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {

    /**
     * 生成静态文件，上传到minio中
     * @param apArticle 用来获取id
     * @param content 根据content生成静态页面
     */
    public void buildArticle2Minio(Long articleId, String content);
}
