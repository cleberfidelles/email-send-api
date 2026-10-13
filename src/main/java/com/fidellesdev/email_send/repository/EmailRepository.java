package com.fidellesdev.email_send.repository;

import com.fidellesdev.email_send.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends JpaRepository<Email , Long> {

}
