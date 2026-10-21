package com.orangecrave.admin.order.async;

import com.orangecrave.admin.order.model.OrderSubmitDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投递到队列的下单消息。{@code requestId} 是受理时预生成的，客户端两处都对它：
 * 轮询结果用它、幂等去重也用它，因此同一笔下单不会因为重投而产生两个订单。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitMessage {

    private String requestId;
    private OrderSubmitDTO dto;
}
