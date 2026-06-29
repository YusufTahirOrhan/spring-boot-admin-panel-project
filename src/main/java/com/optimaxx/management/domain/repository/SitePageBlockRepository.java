package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.SitePageBlock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SitePageBlockRepository extends JpaRepository<SitePageBlock, UUID> {

    List<SitePageBlock> findByStoreIdAndPageKeyAndPublishedAndDeletedFalseOrderByOrderAsc(UUID storeId,
                                                                                          String pageKey,
                                                                                          boolean published);

    List<SitePageBlock> findByStoreIdAndPageKeyAndDeletedFalseOrderByPublishedAscOrderAsc(UUID storeId,
                                                                                          String pageKey);
}
