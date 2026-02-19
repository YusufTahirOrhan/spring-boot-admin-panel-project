package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.RepairStatus;

public record UpdateRepairStatusRequest(RepairStatus status) {
}
