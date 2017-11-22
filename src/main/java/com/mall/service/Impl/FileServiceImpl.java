package com.mall.service.Impl;

import com.google.common.collect.Lists;
import com.mall.service.IFileService;
import com.mall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by cq on 2017/11/4.
 */
@Service("iFileService")
public class FileServiceImpl implements IFileService{

    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    /**
     * 上传图片文件到服务器
     * @param file 要上传的图片文件
     * @param path 上传路径
     * @return
     */
    public String upload(MultipartFile file, String path) {
        //获取原始文件名
        String fileName = file.getOriginalFilename();
        //获取文件扩展名
        //lastIndexOf:返回"."在fileName中最后一个匹配项的索引位置,即abc.jpg会返回.jpg
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".") + 1);
        //为了防止不同用户上传图片时，两张图片的文件名完全相同导致覆盖的情况，这里对文件名加上UUID防重复
        String uploadFileName = UUID.randomUUID().toString() + "." + fileExtensionName;
        //打印日志，通过{}进行占位，也就是一个占位符对应后面的一个数据，类似于c里面的printf("%c",h);
        logger.info("开始上传文件，上传文件的文件名：{}，上传的路径：{}，新文件名：{}", fileName, path, uploadFileName);

        //创建上传路径目录的文件对象
        File fileDir = new File(path);
        if(!fileDir.exists()) {
            //如果不存在
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = new File(path, uploadFileName);

        try {
            file.transferTo(targetFile);
            //文件已经上传成功了
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //已经上传到ftp服务器上
            targetFile.delete();
        } catch (IOException e) {
            logger.error("上传文件异常", e);
            return null;
        }
        return targetFile.getName();
    }

}
