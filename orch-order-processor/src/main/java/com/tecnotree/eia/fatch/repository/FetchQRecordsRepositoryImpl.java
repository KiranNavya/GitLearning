package com.tecnotree.eia.fatch.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tecnotree.eia.count.entity.CBSubsProvisioning;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class FetchQRecordsRepositoryImpl implements FetchQRecordsRepository {

	@PersistenceContext
    private EntityManager entityManager;
	
	
	
	/*
	 * @SuppressWarnings("unchecked") public List<ActionCountProjection>
	 * getActionCounts() { List<Object[]> results = entityManager
	 * .createNativeQuery(countQueryConfig.getListOfQRecordsCount())
	 * .getResultList();
	 * 
	 * return results.stream().map(row -> new ActionCountProjectionImpl((String)
	 * row[0], ((Number) row[1]).intValue())).collect(Collectors.toList()); }
	 * 
	 */

	@SuppressWarnings("unchecked")
	@Override
	public List<CBSubsProvisioning> fetchByActionCodes(String query) {
		// TODO Auto-generated method stub
		return entityManager
                .createNativeQuery(query,CBSubsProvisioning.class)
                .getResultList();
        
        //return results.stream().map(row -> new CBSubsProvisioning((String) row[0], ((Number) row[1]).intValue())).collect(Collectors.toList());
	}
}
