package net.alephdev.lab3.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class MinioService {
    @Value("${minio.url}")
    private String minioUrl;
    @Value("${minio.bucket}")
    private String minioBucket;
    @Value("${minio.accessKey}")
    private String minioAccessKey;
    @Value("${minio.secretKey}")
    private String minioSecretKey;

    private MinioClient getClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    public String uploadFile(MultipartFile file) throws MinioException {
        try {
            MinioClient client = getClient();
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build());
            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }
            String filename = generateFilename(file.getOriginalFilename());
            client.putObject(
                    PutObjectArgs.builder().bucket(minioBucket).object(filename).stream(
                            file.getInputStream(), file.getSize(), -1
                    ).contentType(file.getContentType()).build());

            return filename;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MinioException("Failed to upload file to MinIO: " + ex.getMessage(), "");
        }
    }

    public void deleteFile(String filename) throws MinioException {
        try {
            getClient().removeObject(RemoveObjectArgs.builder().bucket(minioBucket).object(filename).build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new MinioException("Failed to delete file from MinIO: " + e.getMessage(), ""); // rethrow to trigger rollback
        }
    }

    public GetObjectResponse getFile(String filename) throws MinioException {
        try {
            MinioClient minioClient = getClient();
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new MinioException("Failed to get file from MinIO: " + e.getMessage(), "");
        }
    }

    public StatObjectResponse getStat(String filename) throws MinioException {
        try {
            MinioClient minioClient = getClient();
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new MinioException("Failed to get file from MinIO: " + e.getMessage(), "");
        }
    }

    public String getFileUrl(String filename) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if(filename == null) {
            return null;
        }
        return getClient().getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioBucket)
                        .object(filename)
                        .build()
        );
    }

    private String generateFilename(String originalFilename) {
        return UUID.randomUUID().toString() + "_" + originalFilename;
    }
}
