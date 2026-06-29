package com.optimaxx.management;

import com.optimaxx.management.application.SitePageService;
import com.optimaxx.management.domain.repository.SitePageBlockRepository;
import com.optimaxx.management.interfaces.rest.dto.SitePageBlockRequest;
import com.optimaxx.management.interfaces.rest.dto.UpdateSitePageRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SitePageServiceTest {

    private final SitePageBlockRepository repository = mock(SitePageBlockRepository.class);
    private final SitePageService service = new SitePageService(repository);

    @Test
    void returnsDefaultBlocksWhenPublishedContentIsEmpty() {
        when(repository.findByStoreIdAndPageKeyAndPublishedAndDeletedFalseOrderByOrderAsc(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("HOME"), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of());

        var page = service.getPublishedHomePage();

        assertThat(page.blocks()).isNotEmpty();
        assertThat(page.blocks().getFirst().type()).isEqualTo("hero");
    }

    @Test
    void rejectsUnsupportedBlockType() {
        var request = new UpdateSitePageRequest(List.of(
                new SitePageBlockRequest(null, "unknown", 0, true, Map.of())
        ));

        assertThatThrownBy(() -> service.updateHomeDraft(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void savesDraftBlocks() {
        var request = new UpdateSitePageRequest(List.of(
                new SitePageBlockRequest(null, "hero", 0, true, Map.of("title", "Yeni Başlık"))
        ));

        service.updateHomeDraft(request);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }
}
