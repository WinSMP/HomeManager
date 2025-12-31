package org.winlogon.homemanager;

import java.util.List;

public record PaginatedResult(List<String> homes, int totalPages) {}
