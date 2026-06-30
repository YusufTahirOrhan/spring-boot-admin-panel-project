package com.optimaxx.management.application;

import com.cloudinary.Cloudinary;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CloudinaryAssetUploader implements CloudinaryAssetClient {

    private final SiteAssetProperties properties;

    public CloudinaryAssetUploader(SiteAssetProperties properties) {
        this.properties = properties;
    }

    @Override
    public String upload(byte[] bytes, String contentType, String publicId) throws IOException {
        Cloudinary cloudinary = cloudinary();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("public_id", publicId);
        options.put("overwrite", false);
        if (StringUtils.hasText(properties.getFolder())) {
            options.put("folder", properties.getFolder());
        }

        Map<?, ?> result = cloudinary.uploader().upload(bytes, options);
        Object secureUrl = result.get("secure_url");
        if (secureUrl instanceof String url && StringUtils.hasText(url)) {
            return url;
        }

        throw new IOException("Cloudinary upload did not return a secure URL");
    }

    private Cloudinary cloudinary() {
        if (StringUtils.hasText(properties.getCloudinaryUrl())) {
            return new Cloudinary(properties.getCloudinaryUrl());
        }

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", properties.getCloudName());
        config.put("api_key", properties.getApiKey());
        config.put("api_secret", properties.getApiSecret());
        config.put("secure", true);
        return new Cloudinary(config);
    }
}
