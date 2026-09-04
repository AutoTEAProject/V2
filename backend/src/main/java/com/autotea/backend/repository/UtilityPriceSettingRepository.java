package com.autotea.backend.repository;

import com.autotea.backend.domain.UtilityPriceSetting;
import com.autotea.backend.domain.UtilityType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilityPriceSettingRepository extends JpaRepository<UtilityPriceSetting, UtilityType> {
}
