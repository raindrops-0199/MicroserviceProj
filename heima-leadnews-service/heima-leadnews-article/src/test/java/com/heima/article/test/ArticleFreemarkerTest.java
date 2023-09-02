package com.heima.article.test;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.ArticleApplication;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;


@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class ArticleFreemarkerTest {

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ApArticleService apArticleService;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Test
    public void createStaticUrlTest() throws IOException, TemplateException {

        // 将所有文章都生成静态html并上传至minio
        ArticleHomeDto dto = new ArticleHomeDto();
        dto.setSize(50);
        dto.setMaxBehotTime(new Date(0));
        dto.setMinBehotTime(new Date(20000000000000L));
        dto.setTag(ArticleConstants.DEFAULT_TAG);
        List<ApArticle> articleList = apArticleMapper.loadArticleList(dto, ArticleConstants.LOADTYPE_LOAD_MORE);


        for (ApArticle article : articleList) {
            Long id = article.getId();
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, id));

            if (apArticleContent != null && StringUtils.isNotBlank(apArticleContent.getContent())) {

                // 2. 文章内容通过freemarker生成html文件
                Template template = configuration.getTemplate("article.ftl");

                // 数据模型
                Map<String, Object> content = new HashMap<>();
                content.put("content", JSONArray.parseArray(apArticleContent.getContent()));

                //  合成
                StringWriter out = new StringWriter();
                template.process(content, out);

                // 3. 把html文件上传到minio中
                InputStream in = new ByteArrayInputStream(out.toString().getBytes());
                String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", in);

                // 4. 修改ap_article表，保存static_url字段
                apArticleService.update(Wrappers.<ApArticle>lambdaUpdate().eq(ApArticle::getId, apArticleContent.getArticleId())
                        .set(ApArticle::getStaticUrl, path));

            }
        }


        /*
        // 将某个文件生成静态html并上传至minio
        // 1. 获取文章内容 (通过文章id)
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, "1383827995813531650L"));

        if (apArticleContent != null && StringUtils.isNotBlank(apArticleContent.getContent())) {

            // 2. 文章内容通过freemarker生成html文件
            Template template = configuration.getTemplate("article.ftl");

            // 数据模型
            Map<String, Object> content = new HashMap<>();
            content.put("content", JSONArray.parseArray(apArticleContent.getContent()));

            //  合成
            StringWriter out = new StringWriter();
            template.process(content, out);

            // 3. 把html文件上传到minio中
            InputStream in = new ByteArrayInputStream(out.toString().getBytes());
            String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", in);

            // 4. 修改ap_article表，保存static_url字段
            apArticleService.update(Wrappers.<ApArticle>lambdaUpdate().eq(ApArticle::getId, apArticleContent.getArticleId())
                    .set(ApArticle::getStaticUrl, path));

        }

         */

    }
}
