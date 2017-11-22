package com.mall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by cq on 2017/11/4.
 */
public interface IFileService {

    String upload(MultipartFile file, String path);
}
