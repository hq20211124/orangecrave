package com.orangecrave.admin.common.storage;

import java.io.InputStream;

/**
 * 文件存储适配器（见 00 篇模块地图「支撑层」）。v1 本地实现，将来可换对象存储。  */
public interface FileStorage {

    /**
     * 保存文件，返回存储 key。      */
    String store(String originalFilename, InputStream in);

    /**
     * 按 key 读取文件。      */
    InputStream load(String key);

    /**
     * 返回可访问 URL。      */
    String urlOf(String key);
}
