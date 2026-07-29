package com.hobbyhub.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Service
class StorageService(
    private val s3Client: S3Client,
    @Value("\${cloud.aws.s3.bucket-name}") private val bucketName: String,
    @Value("\${cloud.aws.s3.endpoint}") private val endpoint: String
) {

    fun uploadFile(file: MultipartFile, folder: String = "uploads"): String {
        if (bucketName.isBlank() || bucketName == "\${S3_BUCKET_NAME}") {
            return "http://localhost:8080/dummy/image.png" // Fallback for dev mode
        }

        val fileExtension = file.originalFilename?.substringAfterLast(".", "png") ?: "png"
        val fileName = "$folder/${UUID.randomUUID()}.$fileExtension"

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .contentType(file.contentType)
            .build()

        s3Client.putObject(
            putObjectRequest,
            RequestBody.fromInputStream(file.inputStream, file.size)
        )

        // Construct public URL. Note: Assumes bucket is public.
        // For Supabase Storage, the format is slightly different: {endpoint}/storage/v1/object/public/{bucketName}/{fileName}
        return if (endpoint.contains("supabase")) {
            "$endpoint/storage/v1/object/public/$bucketName/$fileName"
        } else {
            "https://$bucketName.s3.amazonaws.com/$fileName"
        }
    }
}
