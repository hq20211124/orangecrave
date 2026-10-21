package com.orangecrave.admin.work.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 合集入参（01 篇 §4）：作品 id 由服务端逐个校验归属与状态，不接受客户端塞集合实体。
 * 合集名要过敏感词（05 篇 §2.2 适用范围含合集名）。 */
@Data
public class WmsCollectionSaveDTO {

    private Long id;
    @NotNull(message = "缺少归属用户")
    private Long userId;
    @NotBlank(message = "合集名称不能为空")
    @Size(max = 100, message = "合集名称过长")
    private String collectionName;
    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;
    /** 0 停用 / 1 正常，与作品同一套状态口径 */
    private Integer status;
    /** 2–10 个作品成组（见 01 篇 §4） */
    @NotNull(message = "请选择合集内的作品")
    private List<Long> workIds;
}
