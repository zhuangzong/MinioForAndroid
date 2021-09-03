package com.zz.miniodemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

public class MainActivity extends AppCompatActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }
    private static final String baseUrl = "xxx";
    private static final String accessKey = "xxx";
    private static final String secretKey = "xxx";
    private static final String bucketName = "xxx";
    /**
     * 上传实例
     * @param key  图片上传后的路径
     * @param imagePath   本地图片地址
     */
    private void upLoad(String key,String imagePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MinioClient minioClient = MinioClient.builder()
                            .endpoint(baseUrl)
                            .credentials(accessKey, secretKey)
                            .build();
                    InputStream inputStream = new FileInputStream(new File(imagePath));
                    PutObjectArgs uploadObjectArgs = PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .contentType("image/jpeg")
                            .stream(inputStream, inputStream.available(), -1)
                            .build();
                    minioClient.putObject(uploadObjectArgs);

                } catch (Exception e) {

                }
            }
        }).start();

    }
}