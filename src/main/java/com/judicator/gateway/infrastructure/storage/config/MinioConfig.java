package com.judicator.gateway.infrastructure.storage.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

  @Value("${app.minio.endpoint}")
  private String endpoint;

  @Value("${app.minio.access-key}")
  private String accessKey;

  @Value("${app.minio.secret-key}")
  private String secretKey;

  @Value("${app.minio.signing-region:minio}")
  private String signingRegion;

  @Bean
  public AmazonS3 amazonS3() {
    BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(endpoint, signingRegion))
        .withPathStyleAccessEnabled(true)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }
}
