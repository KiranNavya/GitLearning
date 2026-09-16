package com.tecnotree.eia.count.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.tecnotree.eia.count.entity.CBSubsProvisioning;
import com.tecnotree.eia.count.entity.CBSubsProvisioningId;

@Repository
public interface OrderRepository extends JpaRepository<CBSubsProvisioning, CBSubsProvisioningId>, ListOfQRecordsCountRepository {

	

//	@Transactional
//	@Modifying
//	@Query("UPDATE Order o SET o.status = 'i' WHERE o.orderId = :orderId AND o.status = 'q'")
//	int lockOrder(@Param("orderId") Long orderId);
//
//	@Query("SELECT o FROM Order o WHERE o.status = 'q' ORDER BY o.orderId ASC")
//	List<Order> findTop100ByStatusPending(Pageable pageable);

	/*
	 * @Query(value = """ SELECT a.ACTION_CODE_V AS actionCode, COUNT(*) AS
	 * recordCount FROM CB_SUBS_PROVISIONING a WHERE a.STATUS_V = 'Q' GROUP BY
	 * a.ACTION_CODE_V """, nativeQuery = true) List<ActionCountProjection>
	 * getActionCounts();
	 */
	
	


}