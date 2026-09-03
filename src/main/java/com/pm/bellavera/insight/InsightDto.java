package com.pm.bellavera.insight;

import java.math.BigDecimal;

public record InsightDto(
        InsightDomain domain,
        String code,
        String label,
        BigDecimal score,
        InsightBand band,
        BigDecimal confidence,
        String rationale) {

    static InsightDto from(Insight insight) {
        return new InsightDto(insight.getDomain(), insight.getCode(), insight.getLabel(), insight.getScore(),
                insight.getBand(), insight.getConfidence(), insight.getRationale());
    }
}
