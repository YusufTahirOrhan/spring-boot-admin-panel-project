package com.optimaxx.management.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimaxx.management.domain.model.SitePageBlock;
import com.optimaxx.management.domain.repository.SitePageBlockRepository;
import com.optimaxx.management.interfaces.rest.dto.SitePageBlockRequest;
import com.optimaxx.management.interfaces.rest.dto.SitePageBlockResponse;
import com.optimaxx.management.interfaces.rest.dto.SitePageResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateSitePageRequest;
import com.optimaxx.management.security.StoreContext;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SitePageService {

    public static final String HOME_PAGE = "HOME";
    private static final TypeReference<Map<String, Object>> CONTENT_TYPE = new TypeReference<>() {
    };
    private static final List<String> SUPPORTED_BLOCKS = List.of(
            "hero", "services", "featuredProducts", "about", "contact", "hours", "cta"
    );

    private final SitePageBlockRepository sitePageBlockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SitePageService(SitePageBlockRepository sitePageBlockRepository) {
        this.sitePageBlockRepository = sitePageBlockRepository;
    }

    @Transactional(readOnly = true)
    public SitePageResponse getPublishedHomePage() {
        List<SitePageBlockResponse> blocks = sitePageBlockRepository
                .findByStoreIdAndPageKeyAndPublishedAndDeletedFalseOrderByOrderAsc(StoreContext.currentStoreId(), HOME_PAGE, true)
                .stream()
                .filter(SitePageBlock::isEnabled)
                .map(this::toResponse)
                .toList();

        if (blocks.isEmpty()) {
            blocks = defaultBlocks();
        }

        return new SitePageResponse(HOME_PAGE, true, blocks);
    }

    @Transactional(readOnly = true)
    public SitePageResponse getHomeDraft() {
        List<SitePageBlockResponse> draftBlocks = sitePageBlockRepository
                .findByStoreIdAndPageKeyAndPublishedAndDeletedFalseOrderByOrderAsc(StoreContext.currentStoreId(), HOME_PAGE, false)
                .stream()
                .map(this::toResponse)
                .toList();

        if (draftBlocks.isEmpty()) {
            draftBlocks = getPublishedHomePage().blocks();
        }

        return new SitePageResponse(HOME_PAGE, false, draftBlocks);
    }

    @Transactional
    public SitePageResponse updateHomeDraft(UpdateSitePageRequest request) {
        if (request == null || request.blocks() == null || request.blocks().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one page block is required");
        }

        var storeId = StoreContext.currentStoreId();
        var now = Instant.now();
        sitePageBlockRepository.findByStoreIdAndPageKeyAndPublishedAndDeletedFalseOrderByOrderAsc(storeId, HOME_PAGE, false)
                .forEach(block -> {
                    block.setDeleted(true);
                    block.setDeletedAt(now);
                });

        List<SitePageBlock> blocks = request.blocks().stream()
                .sorted(Comparator.comparing(block -> block.order() == null ? Integer.MAX_VALUE : block.order()))
                .map(blockRequest -> toDraftEntity(blockRequest, now))
                .toList();
        sitePageBlockRepository.saveAll(blocks);

        return getHomeDraft();
    }

    @Transactional
    public SitePageResponse publishHomeDraft() {
        var storeId = StoreContext.currentStoreId();
        var now = Instant.now();
        List<SitePageBlock> draftBlocks = sitePageBlockRepository
                .findByStoreIdAndPageKeyAndPublishedAndDeletedFalseOrderByOrderAsc(storeId, HOME_PAGE, false);

        if (draftBlocks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No draft blocks available to publish");
        }

        sitePageBlockRepository.findByStoreIdAndPageKeyAndPublishedAndDeletedFalseOrderByOrderAsc(storeId, HOME_PAGE, true)
                .forEach(block -> {
                    block.setDeleted(true);
                    block.setDeletedAt(now);
                });

        List<SitePageBlock> publishedBlocks = draftBlocks.stream()
                .map(draft -> copyAsPublished(draft, now))
                .toList();
        sitePageBlockRepository.saveAll(publishedBlocks);

        return getPublishedHomePage();
    }

    private SitePageBlock toDraftEntity(SitePageBlockRequest request, Instant now) {
        if (request.type() == null || !SUPPORTED_BLOCKS.contains(request.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported block type");
        }

        SitePageBlock block = new SitePageBlock();
        block.setStoreId(StoreContext.currentStoreId());
        block.setPageKey(HOME_PAGE);
        block.setType(request.type());
        block.setOrder(request.order() == null ? 0 : request.order());
        block.setEnabled(request.enabled() == null || request.enabled());
        block.setPublished(false);
        block.setContentJson(toJson(request.content() == null ? Map.of() : request.content()));
        block.setCreatedAt(now);
        block.setDeleted(false);
        return block;
    }

    private SitePageBlock copyAsPublished(SitePageBlock draft, Instant now) {
        SitePageBlock block = new SitePageBlock();
        block.setStoreId(draft.getStoreId());
        block.setPageKey(draft.getPageKey());
        block.setType(draft.getType());
        block.setOrder(draft.getOrder());
        block.setEnabled(draft.isEnabled());
        block.setPublished(true);
        block.setContentJson(draft.getContentJson());
        block.setCreatedAt(now);
        block.setDeleted(false);
        return block;
    }

    private SitePageBlockResponse toResponse(SitePageBlock block) {
        return new SitePageBlockResponse(
                block.getId(),
                block.getType(),
                block.getOrder(),
                block.isEnabled(),
                fromJson(block.getContentJson())
        );
    }

    private String toJson(Map<String, Object> content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid block content");
        }
    }

    private Map<String, Object> fromJson(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(contentJson, CONTENT_TYPE);
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private List<SitePageBlockResponse> defaultBlocks() {
        return List.of(
                new SitePageBlockResponse(null, "hero", 0, true, Map.of(
                        "title", "OptiMaxx Optik",
                        "subtitle", "Göz sağlığınız ve stiliniz için modern optik çözümler.",
                        "eyebrow", "Optik mağazası",
                        "primaryButtonLabel", "Randevu Al",
                        "primaryButtonHref", "#contact",
                        "imageUrl", ""
                )),
                new SitePageBlockResponse(null, "services", 1, true, Map.of(
                        "title", "Hizmetlerimiz",
                        "subtitle", "Mağazada sunduğumuz temel optik hizmetler.",
                        "items", List.of("Gözlük çerçevesi", "Optik cam", "Kontakt lens", "Tamir ve ayar")
                )),
                new SitePageBlockResponse(null, "contact", 2, true, Map.of(
                        "title", "Bize Ulaşın",
                        "phone", "+90 555 123 4567",
                        "email", "contact@optimaxx.com",
                        "address", "Merkez Mahallesi, Optik Caddesi No: 1"
                ))
        );
    }
}
