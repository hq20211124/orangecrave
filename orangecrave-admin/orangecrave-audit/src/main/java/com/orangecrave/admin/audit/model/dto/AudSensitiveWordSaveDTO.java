package com.orangecrave.admin.audit.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AudSensitiveWordSaveDTO {

    private Long id;
    @NotBlank(message = "敏感词不能为空")
    @Size(max = 100, message = "敏感词过长")
    private String word;
    @Size(max = 50, message = "分类过长")
    private String category;
    /** 替换 / 拒绝 */
    @Size(max = 20, message = "处理方式过长")
    private String action;
    private Integer status;
}
