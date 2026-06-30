package com.optimaxx.management.application;

import com.optimaxx.management.interfaces.rest.dto.AssetUploadResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class SiteAssetService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final SiteAssetProperties properties;
    private final CloudinaryAssetClient cloudinaryAssetClient;
    private final Path uploadDirectory;

    public SiteAssetService(SiteAssetProperties properties, CloudinaryAssetClient cloudinaryAssetClient) {
        this.properties = properties;
        this.cloudinaryAssetClient = cloudinaryAssetClient;
        this.uploadDirectory = Path.of(properties.getLocalDirectory()).toAbsolutePath().normalize();
    }

    public AssetUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be 5 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, WebP and GIF images are allowed");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!hasValidSignature(contentType, bytes)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match an allowed image type");
            }

            if (properties.isCloudinaryStorage()) {
                return uploadToCloudinary(bytes, contentType);
            }

            return uploadToLocalDisk(bytes, contentType);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File could not be uploaded");
        }
    }

    public Resource load(String filename) {
        if (filename == null || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }
        try {
            Path file = uploadDirectory.resolve(filename).normalize();
            if (!file.startsWith(uploadDirectory) || !Files.exists(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
            }
            return resource;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
        }
    }

    public String contentType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private AssetUploadResponse uploadToCloudinary(byte[] bytes, String contentType) {
        if (!properties.hasCloudinaryConfig()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloudinary upload storage is not configured");
        }

        String url;
        try {
            url = cloudinaryAssetClient.upload(bytes, contentType, UUID.randomUUID().toString());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary upload failed");
        }
        if (url == null || !url.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary did not return a secure URL");
        }
        return new AssetUploadResponse(url);
    }

    private AssetUploadResponse uploadToLocalDisk(byte[] bytes, String contentType) throws IOException {
        Files.createDirectories(uploadDirectory);
        String extension = extensionFor(contentType);
        String filename = UUID.randomUUID() + extension;
        Path target = uploadDirectory.resolve(filename).normalize();
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/public/assets/site/")
                .path(filename)
                .toUriString();
        return new AssetUploadResponse(url);
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private boolean hasValidSignature(String contentType, byte[] bytes) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/gif" -> startsWithAscii(bytes, "GIF87a") || startsWithAscii(bytes, "GIF89a");
            case "image/webp" -> bytes.length >= 12
                    && asciiEquals(bytes, 0, "RIFF")
                    && asciiEquals(bytes, 8, "WEBP");
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((bytes[index] & 0xFF) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, String prefix) {
        return asciiEquals(bytes, 0, prefix);
    }

    private boolean asciiEquals(byte[] bytes, int offset, String expected) {
        if (bytes.length < offset + expected.length()) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }
}
