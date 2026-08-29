package com.trsthales.ecommerce.identity.infrastructure;

import com.trsthales.ecommerce.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
}
