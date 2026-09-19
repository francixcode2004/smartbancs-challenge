package com.tcs.SmartBancsApp.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.SmartBancsApp.model.ModelUsers;

public interface RepositoryUsers extends JpaRepository<ModelUsers, UUID> {
}