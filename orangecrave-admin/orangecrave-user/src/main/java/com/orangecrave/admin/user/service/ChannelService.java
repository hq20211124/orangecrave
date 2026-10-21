package com.orangecrave.admin.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.user.entity.SysChannel;
import com.orangecrave.admin.user.mapper.SysChannelMapper;
import com.orangecrave.admin.user.model.dto.SysChannelSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 渠道与归因（见 04 篇 §3.6）。只做归因，不做完整渠道管理。  */
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final SysChannelMapper channelMapper;

    public List<SysChannel> list() {
        return channelMapper.selectList(new LambdaQueryWrapper<SysChannel>().eq(SysChannel::getStatus, 1));
    }

    public void save(SysChannelSaveDTO dto) {
        SysChannel channel = new SysChannel();
        channel.setId(dto.getId());
        channel.setChannelName(dto.getChannelName());
        channel.setChannelCode(dto.getChannelCode());
        // 未传 status 时 updateById 会跳过该列，保持原值
        channel.setStatus(dto.getStatus());
        if (channel.getId() == null) {
            channelMapper.insert(channel);
        } else {
            channelMapper.updateById(channel);
        }
    }

    public Long idByCode(String channelCode) {
        if (channelCode == null) {
            return null;
        }
        SysChannel channel = channelMapper.selectOne(new LambdaQueryWrapper<SysChannel>()
                .eq(SysChannel::getChannelCode, channelCode).last("limit 1"));
        return channel == null ? null : channel.getId();
    }
}
