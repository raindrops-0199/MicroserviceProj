package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    /**
     * 条件查询文章列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        // 1. 检查参数
        dto.checkParam();
        // 2. 分页查询
        IPage<WmNews> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> wrapper = new LambdaQueryWrapper<>();

        // 状态精确查询
        if (dto.getStatus() != null) {
            wrapper.eq(WmNews::getStatus, dto.getStatus());
        }

        // 频道精确查询
        if (dto.getChannelId() != null) {
            wrapper.eq(WmNews::getChannelId, dto.getChannelId());
        }

        // 时间范围查询
        if (dto.getBeginPubDate() != null && dto.getEndPubDate() != null) {
            wrapper.between(WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        }

        // 关键字模糊查询
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            wrapper.like(WmNews::getTitle, dto.getKeyword());
        }

        // 查询当前登录人的文章
        wrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());

        // 按照发布时间倒序查询
        wrapper.orderByDesc(WmNews::getPublishTime);

        page = page(page, wrapper);

        // 3. 返回结果
        ResponseResult<List<WmNews>> responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    /**
     * 发布修改文章或保存为草稿
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        // 0. 条件判断
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 1. 保存或修改文章
        WmNews wmNews = new WmNews();

        // 属性拷贝, 名称和类型相同的拷贝
        BeanUtils.copyProperties(dto, wmNews);

        if (wmNews.getSubmitedTime() == null) {
            wmNews.setSubmitedTime(new Date());
        }

        // 封面图片，从List转为逗号分隔的字符串
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }

        //如果当前封面类型为自动 -1， 先设置为null，后期处理
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }

        saveOrUpdateWmNews(wmNews);

        // 2. 判断是否为草稿，如果是，结束当前方法
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        // 3. 如果不是，设置发布时间，保存文章内容图片与素材的关系

        // 获取到文章中的图片信息
        List<String> materials = extractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials, wmNews.getId());

        // 4. 保存文章封面图片与素材的关系，如果当前布局为自动，需要匹配封面图片
        saveRelativeInfoForCover(dto, wmNews, materials);

        // 将文章添加为task
         wmNewsTaskService.addNews2Task(wmNews.getId(), wmNews.getPublishTime());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    /**
     * 保存或修改文章
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        // 补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        if (wmNews.getCreatedTime() == null) {
            wmNews.setCreatedTime(new Date());
        }
        if (wmNews.getPublishTime() == null) {
            wmNews.setPublishTime(new Date());
        }
        wmNews.setEnable((short)1); // 默认上架

        if (wmNews.getId() == null) {
            // 保存
            save(wmNews);
        } else {
            // 修改
            // 删除文章图片与素材的关系
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

    /**
     * 提取文章内容中的图片信息
     * @param content
     * @return
     */
    private List<String> extractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if (map.get("type").equals("image")) {
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }

    /**
     * 处理文章内容图片与素材的关系
     * @param materials
     * @param newsId
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials, newsId, WemediaConstants.WM_CONTENT_REFERENCE);
    }


    @Autowired
    private WmMaterialMapper wmMaterialMapper;
    /**
     * 保存文章图片与素材关系到数据库中
     * @param materials
     * @param newsId
     * @param type 图片类型（内容图片0， 封面图片1）
     */
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        if (materials == null || materials.isEmpty()) {
            return;
        }
        // 通过图片url查询id
        List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));

        // 判断素材是否有效
        if (dbMaterials == null || dbMaterials.size() == 0 || dbMaterials.size() != materials.size()) {
            // 手动抛出异常 第一个功能：提示调用者素材失效， 第二个功能：进行数据回滚(因为在前面已经将wmNews保存在了数据库中)
            throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
        }

        List<Integer> materialIds = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

        // 批量保存
        wmNewsMaterialMapper.saveRelations(materialIds, newsId, type);
    }

    /**
     * 1. 如果当前封面类型为自动，则设置封面类型的睡觉
     *    a. 如果内容图片大一等于1，小于3，单图 type=1
     *    b. 内容图片大于等于3， type=3
     *    c. 内容无图片，无图，type=0
     * 2. 保存封面图片与素材的关系
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {

        List<String> images = dto.getImages();

        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            // 多图
            if (materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() >= 1) {
                // 单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else {
                // 无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            // 修改文章
            if (images != null && images.size() > 0) {
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }

        if (images != null && images.size() > 0) {
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }
    }
}
