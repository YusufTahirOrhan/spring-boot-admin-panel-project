package com.optimaxx.management.interfaces.rest.dto;

public record UpdateLensPrescriptionRequest(String rightSphere,
                                            String leftSphere,
                                            String rightCylinder,
                                            String leftCylinder,
                                            String rightAxis,
                                            String leftAxis,
                                            String pd,
                                            String notes) {
}
