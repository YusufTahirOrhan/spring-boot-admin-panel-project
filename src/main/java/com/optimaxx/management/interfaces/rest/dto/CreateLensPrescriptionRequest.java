package com.optimaxx.management.interfaces.rest.dto;

import java.util.UUID;

public record CreateLensPrescriptionRequest(UUID customerId,
                                            UUID transactionTypeId,
                                            String rightSphere,
                                            String leftSphere,
                                            String rightCylinder,
                                            String leftCylinder,
                                            String rightAxis,
                                            String leftAxis,
                                            String pd,
                                            String notes) {
}
