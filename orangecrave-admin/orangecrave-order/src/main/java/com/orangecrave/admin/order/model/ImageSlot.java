package com.orangecrave.admin.order.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ImageSlot {

    @NotNull(message = "图槽位序号不能为空")
    private Integer slotIndex;
    @NotBlank(message = "图稿地址不能为空")
    @Size(max = 500, message = "图稿地址过长")
    private String imageUrl;
    @Min(value = 1, message = "份数必须大于 0")
    private Integer copies;
    private Long pHash;
}
