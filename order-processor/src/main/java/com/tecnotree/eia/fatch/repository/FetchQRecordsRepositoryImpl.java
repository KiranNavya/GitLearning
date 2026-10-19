package com.tecnotree.eia.fatch.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FetchQRecordsRepositoryImpl implements FetchQRecordsRepository {

	private final JdbcTemplate jdbcTemplate; // Use JdbcTemplate

	@Autowired
	public FetchQRecordsRepositoryImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<Map<String, Object>> fetchByActionCodes(String query) {
		return jdbcTemplate.queryForList(query);
	}
}