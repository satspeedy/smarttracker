package com.hha.smarttracker.repository;

import com.hha.smarttracker.model.Tracking;
import com.hha.smarttracker.model.enumeration.TrackingStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TrackingRepository extends CrudRepository<Tracking, Long> {

    List<Tracking> findAllByTrackingStatus(TrackingStatus trackingStatus);

}
