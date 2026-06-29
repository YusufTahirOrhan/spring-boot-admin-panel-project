package com.optimaxx.management.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_page_blocks")
public class SitePageBlock extends BaseEntity {

    @Column(name = "page_key", nullable = false, length = 40)
    private String pageKey;

    @Column(name = "block_type", nullable = false, length = 40)
    private String type;

    @Column(name = "display_order", nullable = false)
    private int order;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String contentJson;

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }
}
