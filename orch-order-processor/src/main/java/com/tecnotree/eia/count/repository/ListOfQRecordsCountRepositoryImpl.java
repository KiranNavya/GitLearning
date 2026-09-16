package com.tecnotree.eia.count.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.tecnotree.eia.count.beans.ActionCountProjection;
import com.tecnotree.eia.count.beans.ActionCountProjectionImpl;
import com.tecnotree.eia.count.configuration.CountQueryConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class ListOfQRecordsCountRepositoryImpl implements ListOfQRecordsCountRepository {

	@PersistenceContext
    private EntityManager entityManager;
	
	private final CountQueryConfig countQueryConfig;

    public ListOfQRecordsCountRepositoryImpl(CountQueryConfig countQueryConfig) {
        this.countQueryConfig = countQueryConfig;
    }
	
    @SuppressWarnings("unchecked")
	public List<ActionCountProjection> getActionCounts() {
    	List<Object[]> results = entityManager
                .createNativeQuery(countQueryConfig.getListOfQRecordsCount_ORCH())
                .getResultList();
        
		return results.stream().map(row -> new ActionCountProjectionImpl((String) row[0], ((Number) row[1]).intValue()))
				.collect(Collectors.toList());
	}
}