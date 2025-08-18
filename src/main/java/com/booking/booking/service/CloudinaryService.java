package com.booking.booking.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

  private final Cloudinary cloudinary;

  public Map<String, Object> upload(MultipartFile file) {
    try {
      return cloudinary.uploader().upload(
          file.getBytes(),
          Map.of("folder", "booking")
      );
    } catch (IOException io) {
      log.error("Error uploading image to Cloudinary", io);
      throw new RuntimeException("Image upload fail");
    }
  }

  @KafkaListener(topics = "upload-image", groupId = "cloudinary-group")
  public Map<String, Object> uploadKafka(MultipartFile file) {
    try {
      return cloudinary.uploader().upload(
          file.getBytes(),
          Map.of("folder", "booking")
      );
    } catch (IOException io) {
      log.error("Error uploading image to Cloudinary", io);
      throw new RuntimeException("Image upload fail");
    }
  }

  public void delete(String publicId) {
    try {
      cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete image from Cloudinary", e);
    }
  }

  @KafkaListener(topics = "delete-image", groupId = "cloudinary-group")
  public void deleteKafka(String publicId) {
    try {
      log.info("Deleting image with publicId={}", publicId);
      cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete image from Cloudinary", e);
    }
  }
}
