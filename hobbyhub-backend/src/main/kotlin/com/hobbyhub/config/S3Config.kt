package com.hobbyhub.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class S3Config {

    @Value("\${cloud.aws.credentials.access-key}")
    private lateinit var accessKey: String

    @Value("\${cloud.aws.credentials.secret-key}")
    private lateinit var secretKey: String

    @Value("\${cloud.aws.region.static}")
    private lateinit var region: String

    @Value("\${cloud.aws.s3.endpoint}")
    private lateinit var endpoint: String

    @Bean
    fun s3Client(): S3Client {
        // Skip initialization if empty (e.g. running in dev mode without keys)
        if (accessKey.isBlank() || accessKey == "\${S3_ACCESS_KEY}") {
            return S3Client.builder().build() 
        }

        val credentials = AwsBasicCredentials.create(accessKey, secretKey)
        val builder = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(region))

        if (endpoint.isNotBlank() && endpoint != "\${S3_ENDPOINT}") {
            builder.endpointOverride(URI.create(endpoint))
        }

        return builder.build()
    }
}
