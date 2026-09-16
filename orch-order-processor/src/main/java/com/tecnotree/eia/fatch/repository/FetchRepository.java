package com.tecnotree.eia.fatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tecnotree.eia.count.entity.CBSubsProvisioning;

@Repository
public interface FetchRepository extends JpaRepository<CBSubsProvisioning, Long>,FetchQRecordsRepository {
	/*
	 * @Query(value = """
	 * 
	 * 
	 * SELECT * FROM ( SELECT * FROM CB_SUBS_PROVISIONING WHERE STATUS_V = 'Q' AND
	 * ACTION_CODE_V IN :actionCodes ) WHERE ROWNUM <= :totalLimit """, nativeQuery
	 * = true) List<CBSubsProvisioning> fetchByActionCodes(@Param("actionCodes")
	 * List<String> actionCodes,@Param("totalLimit") int totalLimit);
	 */

}