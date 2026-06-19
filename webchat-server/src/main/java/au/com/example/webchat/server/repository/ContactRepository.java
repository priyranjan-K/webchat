package au.com.example.webchat.server.repository;

import au.com.example.webchat.server.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persistence operations for {@link Contact} entities.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByUserPhone(String userPhone);

    boolean existsByUserPhoneAndContactPhone(String userPhone, String contactPhone);

    /**
     * Removes the contact relationship in <em>both</em> directions
     * (A→B and B→A) to keep the table consistent.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Contact c WHERE (c.userPhone = :u1 AND c.contactPhone = :u2) " +
           "OR (c.userPhone = :u2 AND c.contactPhone = :u1)")
    void deleteByUserPhoneAndContactPhone(@Param("u1") String u1, @Param("u2") String u2);
}
