package vn.mar.leadimport.parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;

@Component
public class CsvLeadImportParser {

    public ParsedCsvLeadImport parse(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)) {
            List<String> headers = normalizeHeaders(parser.getHeaderNames());
            validateHeaders(headers);
            List<ParsedCsvLeadImportRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(toRow(record, headers));
            }
            if (rows.isEmpty()) {
                throw invalidFile("file", "EMPTY_FILE", "CSV file must contain at least one data row");
            }
            return new ParsedCsvLeadImport(headers, rows);
        } catch (IOException | IllegalArgumentException exception) {
            throw invalidFile("file", "INVALID_CSV", "CSV file format is invalid");
        }
    }

    private List<String> normalizeHeaders(List<String> headers) {
        return headers.stream()
                .map(this::normalizeHeader)
                .toList();
    }

    private void validateHeaders(List<String> headers) {
        if (headers.isEmpty()) {
            throw invalidFile("file", "MISSING_HEADER", "CSV file must include a header row");
        }
        Set<String> seen = new HashSet<>();
        for (String header : headers) {
            if (header.isBlank()) {
                throw invalidFile("file", "BLANK_HEADER", "CSV header must not be blank");
            }
            if (!seen.add(header.toLowerCase(Locale.ROOT))) {
                throw invalidFile("file", "DUPLICATE_HEADER", "CSV header must not contain duplicate columns");
            }
        }
    }

    private ParsedCsvLeadImportRow toRow(CSVRecord record, List<String> headers) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < record.size() ? record.get(index) : "";
            values.put(headers.get(index), value == null ? "" : value.trim());
        }
        return new ParsedCsvLeadImportRow(Math.toIntExact(record.getRecordNumber() + 1), values);
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim();
    }

    private BusinessException invalidFile(String field, String code, String message) {
        return new BusinessException(
                ErrorCode.IMPORT_FILE_INVALID,
                ErrorCode.IMPORT_FILE_INVALID.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
