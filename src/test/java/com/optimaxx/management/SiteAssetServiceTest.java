package com.optimaxx.management;

import com.optimaxx.management.application.CloudinaryAssetClient;
import com.optimaxx.management.application.SiteAssetProperties;
import com.optimaxx.management.application.SiteAssetService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteAssetServiceTest {

    @Test
    void rejectsFileWhenContentDoesNotMatchDeclaredImageType() {
        SiteAssetService siteAssetService = new SiteAssetService(new SiteAssetProperties(), Mockito.mock(CloudinaryAssetClient.class));
        var file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "not really an image".getBytes()
        );

        assertThatThrownBy(() -> siteAssetService.upload(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("File content does not match an allowed image type");
    }

    @Test
    void uploadsValidatedImageToCloudinaryStorage() throws IOException {
        SiteAssetProperties properties = new SiteAssetProperties();
        properties.setStorage("cloudinary");
        properties.setCloudinaryUrl("cloudinary://api-key:api-secret@demo");
        CloudinaryAssetClient cloudinaryAssetClient = Mockito.mock(CloudinaryAssetClient.class);
        when(cloudinaryAssetClient.upload(any(byte[].class), eq("image/png"), any(String.class)))
                .thenReturn("https://res.cloudinary.com/demo/image/upload/v1/optimaxx/site/image.png");
        SiteAssetService siteAssetService = new SiteAssetService(properties, cloudinaryAssetClient);

        var file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                pngBytes()
        );

        var response = siteAssetService.upload(file);

        assertThat(response.url()).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/optimaxx/site/image.png");
        verify(cloudinaryAssetClient).upload(any(byte[].class), eq("image/png"), any(String.class));
    }

    @Test
    void rejectsProductionCloudinaryUploadWhenCloudinaryIsNotConfigured() throws IOException {
        SiteAssetProperties properties = new SiteAssetProperties();
        properties.setStorage("cloudinary");
        CloudinaryAssetClient cloudinaryAssetClient = Mockito.mock(CloudinaryAssetClient.class);
        SiteAssetService siteAssetService = new SiteAssetService(properties, cloudinaryAssetClient);

        var file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                pngBytes()
        );

        assertThatThrownBy(() -> siteAssetService.upload(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cloudinary upload storage is not configured");
        verify(cloudinaryAssetClient, never()).upload(any(byte[].class), any(String.class), any(String.class));
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
