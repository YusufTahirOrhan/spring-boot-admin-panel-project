package com.optimaxx.management;

import com.optimaxx.management.application.SiteAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SiteAssetServiceTest {

    private final SiteAssetService siteAssetService = new SiteAssetService();

    @Test
    void rejectsFileWhenContentDoesNotMatchDeclaredImageType() {
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
}
