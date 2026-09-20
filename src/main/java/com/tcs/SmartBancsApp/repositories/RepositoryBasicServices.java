package com.tcs.SmartBancsApp.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcs.SmartBancsApp.model.ModelBasicServices;

public interface RepositoryBasicServices extends JpaRepository<ModelBasicServices, String> {
    List<ModelBasicServices> findAllByOrderByCode();
}
