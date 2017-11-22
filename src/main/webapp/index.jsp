<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
<body>
<h2>Hello World!</h2>

上传文件
<form name = "form1" action = "/manage/product/upload.do" method="post" enctype="multipart/form-data">
    <input type="file" name="uploadFile"/>
    <input type="submit" value="上传文件"/>
</form>

富文本图片上传
<form name = "form2" action = "/manage/product/richtext_img_upload.do" method="post" enctype="multipart/form-data">
    <input type="file" name="uploadFile"/>
    <input type="submit" value="富文本图片上传文件"/>
</form>
</body>
</html>
