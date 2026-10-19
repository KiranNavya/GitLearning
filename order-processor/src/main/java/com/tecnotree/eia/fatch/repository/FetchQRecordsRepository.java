package com.tecnotree.eia.fatch.repository;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public interface FetchQRecordsRepository {
    List<Map<String, Object>> fetchByActionCodes(String query);
}