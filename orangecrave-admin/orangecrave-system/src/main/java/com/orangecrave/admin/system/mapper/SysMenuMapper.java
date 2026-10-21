package com.orangecrave.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.system.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.perm_key FROM sys_menu m "
            + "JOIN sys_role_menu rm ON rm.menu_id = m.id AND rm.deleted = 0 "
            + "JOIN sys_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0 "
            + "WHERE ur.user_id = #{userId} AND m.deleted = 0 "
            + "AND m.perm_key IS NOT NULL AND m.perm_key <> ''")
    List<String> selectPermKeysByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.* FROM sys_menu m "
            + "JOIN sys_role_menu rm ON rm.menu_id = m.id AND rm.deleted = 0 "
            + "JOIN sys_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0 "
            + "WHERE ur.user_id = #{userId} AND m.deleted = 0 AND m.menu_type IN (1, 2) "
            + "ORDER BY m.sort ASC")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}
