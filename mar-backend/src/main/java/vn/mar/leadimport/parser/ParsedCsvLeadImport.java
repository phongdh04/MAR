package vn.mar.leadimport.parser;

import java.util.List;

public record ParsedCsvLeadImport(
        List<String> headers,
        List<ParsedCsvLeadImportRow> rows
) {
}
