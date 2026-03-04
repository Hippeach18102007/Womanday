package com.womenday.app.service;

import com.womenday.app.model.GreetingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GreetingConfigRepository extends JpaRepository<GreetingConfig, String> {
}
