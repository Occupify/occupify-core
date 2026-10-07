package com.occupify.core.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportEvidenceTest {

    @Test
    void testReportEvidenceBuilderAndGetters() {
        UUID id = UUID.randomUUID();
        Report report = Report.builder().description("Report desc").build();

        ReportEvidence evidence = ReportEvidence.builder()
                .report(report)
                .fileUrl("https://example.com/screenshot.png")
                .build();
        evidence.setId(id);

        assertEquals(id, evidence.getId());
        assertEquals(report, evidence.getReport());
        assertEquals("https://example.com/screenshot.png", evidence.getFileUrl());
    }
}
