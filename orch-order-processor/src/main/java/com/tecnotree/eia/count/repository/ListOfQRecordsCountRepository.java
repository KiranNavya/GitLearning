package com.tecnotree.eia.count.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tecnotree.eia.count.beans.ActionCountProjection;

@Repository
public interface ListOfQRecordsCountRepository {
	public List<ActionCountProjection> getActionCounts();
}