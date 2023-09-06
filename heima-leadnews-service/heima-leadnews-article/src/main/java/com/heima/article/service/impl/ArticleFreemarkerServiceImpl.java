package com.heima.article.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ApArticleService apArticleService;

    /**
     * 生成静态文件，上传到minio中
     *
     * @param apArticle 用来获取id
     * @param content   根据content生成静态页面
     */
    @Async
    @Override
    public void buildArticle2Minio(Long articleId, String content) {

        if (StringUtils.isNotBlank(content)) {

            // 2. 文章内容通过freemarker生成html文件
            Template template = null;

            StringWriter out = new StringWriter();

            try {
                template = configuration.getTemplate("article.ftl");

                // 数据模型
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("content", JSONArray.parseArray(content));

                //  合成
                template.process(contentMap, out);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // 3. 把html文件上传到minio中
            InputStream in = new ByteArrayInputStream(out.toString().getBytes());
            String path = fileStorageService.uploadHtmlFile("", articleId + ".html", in);

            // 4. 修改ap_article表，保存static_url字段
            apArticleService.update(Wrappers.<ApArticle>lambdaUpdate().eq(ApArticle::getId, articleId)
                    .set(ApArticle::getStaticUrl, path));

        }
    }
}
