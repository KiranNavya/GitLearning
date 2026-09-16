package com.tecnotree.eia.fatch.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tecnotree.eia.count.entity.CBSubsProvisioning;

@Repository
public interface FetchQRecordsRepository {
	public List<CBSubsProvisioning> fetchByActionCodes(String query);
}
