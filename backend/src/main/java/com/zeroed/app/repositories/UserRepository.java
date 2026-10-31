package com.zeroed.app.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import com.zeroed.app.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	boolean existsbyEmail(String email);
}
