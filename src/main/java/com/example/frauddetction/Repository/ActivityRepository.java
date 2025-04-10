package com.example.frauddetction.Repository;

import com.example.frauddetction.Entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    @Query("SELECT a FROM Activity a ORDER BY a.timestamp DESC")
    List<Activity> findRecentActivities();
} 