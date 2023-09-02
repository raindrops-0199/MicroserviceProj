package com.heima.minio.test;

import com.heima.file.service.FileStorageService;
import com.heima.minio.MinioApplication;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@SpringBootTest(classes = MinioApplication.class)
@RunWith(SpringRunner.class)
public class MinioTest {

    @Autowired
    private FileStorageService fileStorageService;

    // 把basic.html上传到minio上, 并且可以在浏览器中访问
    @Test
    public void test() throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("/Users/jason/local/temp/basic.html");
        String path = fileStorageService.uploadHtmlFile("", "basic.html", fileInputStream);
        System.out.println(path);
    }

    /**
     * 把basic.html上传到minio上, 并且可以在浏览器中访问
     */

    /*
    public static void main(String[] args) {

        try {
            FileInputStream fileInputStream = new FileInputStream("/Users/jason/local/temp/basic.html");

            // 1.获取minio链接信息， 创建一个minio客户端
            MinioClient minioClient = MinioClient.builder().credentials("minio", "12345678").endpoint("http://localhost:9000").build();

            // 2. 上传
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object("basic.html") // 文件名
                    .contentType("text/html") // 文件类型
                    .bucket("leadnews") // 桶名称， 与minio一致
                    .stream(fileInputStream, fileInputStream.available(), -1).build();

            minioClient.putObject(putObjectArgs);

            // 3. 访问路径
            System.out.println("http://localhost:9000/leadnews/basic.html");

        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidBucketNameException | InvalidKeyException | InvalidResponseException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }


    }

     */


}
