package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PmsSeriesSaveDTO {

    private Long id;
    @NotBlank(message = "系列名称不能为空")
    @Size(max = 100, message = "系列名称过长")
    private String seriesName;
    @Size(max = 255, message = "封面地址过长")
    private String cover;
    @Size(max = 100, message = "IP 名称过长")
    private String ipName;
    private Integer sort;
    private Integer status;
}
