package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.heima.apis.article.IArticleClient;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Service
@Transactional
@Slf4j
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     */
    @Override
    public void autoScanWmNews(Integer id) throws InvocationTargetException, IllegalAccessException {
        // 1. 查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanService-文章不存在");
        }

        // 是否是待审核
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            // 2. 审核文本内容与图片
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);
            String content = (String) textAndImages.get("content");
            List<String> images = (List<String>) textAndImages.get("images");
            if (!handleTextScan(content, wmNews) || !handleImageScan(images, wmNews)) {
                return;
            }

            // 3. 审核成功，保存app端相关的文章数据
            ResponseResult result = saveAppArticle(wmNews);
            if (!result.getCode().equals(200)) {
                throw new RuntimeException("WmNewsAutoScanService-文章审核-保存app端相关文章数据失败");
            }

            if (wmNews.getArticleId() == null) {
                // 回填 article_id
                wmNews.setArticleId((Long) result.getData());
            }
            wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
            wmNews.setReason("审核成功");
            wmNewsMapper.updateById(wmNews);

        }

    }

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存app端相关的文章数据
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) throws InvocationTargetException, IllegalAccessException {
        ArticleDto articleDto = new ArticleDto();

        // 属性拷贝 拷贝了 content, channelId, title, labels, createdTime, publishTime
        BeanUtils.copyProperties(wmNews, articleDto);

        // 文章布局
        articleDto.setLayout(wmNews.getType());

        // 频道
        WmChannel channel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (channel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        articleDto.setChannelName(channel.getName());

        // 作者
        articleDto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        articleDto.setAuthorName(wmUser.getName());

        // 设置文章id
        if (wmNews.getArticleId() != null) {
            articleDto.setId(wmNews.getArticleId());
        }

        // 重新设置创建时间(这个时间和自媒体文章的创建时间意义不同)
        articleDto.setCreatedTime(new Date());


        return articleClient.saveArticle(articleDto);
    }

    /**
     * 1。从自媒体文章的内容中提取文本和图片
     * 2.提取文章的封面图片
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {

        //存储纯文本内容
        StringBuilder contentBuilder = new StringBuilder();

        List<String> images = new ArrayList<>();

        //1。从自媒体文章的内容中提取文本和图片
        if(StringUtils.isNotBlank(wmNews.getContent())){
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if (map.get("type").equals("text")){
                    contentBuilder.append(map.get("value"));
                }

                if (map.get("type").equals("image")){
                    images.add((String) map.get("value"));
                }
            }
        }
        //2.提取文章的封面图片
        if(StringUtils.isNotBlank(wmNews.getImages())){
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content",contentBuilder.toString());
        resultMap.put("images",images);
        return resultMap;
    }

    /**
     * 审核图片   (可使用阿里云接口) (暂时跳过，后期可以根据审核服务的接口文档实现)
     * @param images 图片内容
     * @param wmNews news对象，方便在函数中修改相关信息
     * @return true: 审核成功；false: 审核未通过
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        return true;
    }

    /**
     * 审核文本 可使用阿里云接口) (暂时跳过，后期可以根据审核服务的接口文档实现)
     * @param text 文本内容
     * @param wmNews news对象，方便在函数中修改相关信息
     * @return true: 审核成功；false: 审核未通过
     */
    private boolean handleTextScan(String text, WmNews wmNews) {
        return true;
    }
}
