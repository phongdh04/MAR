package vn.mar.leadimport.parser;

import java.util.Map;

public record ParsedCsvLeadImportRow(
        int rowNumber,
        Map<String, String> values
) {
}
