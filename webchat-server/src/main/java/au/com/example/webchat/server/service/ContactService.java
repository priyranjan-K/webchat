package au.com.example.webchat.server.service;

import au.com.example.webchat.server.dto.UserDto;
import au.com.example.webchat.server.model.Contact;
import au.com.example.webchat.server.repository.ContactRepository;
import au.com.example.webchat.server.repository.MessageRepository;
import au.com.example.webchat.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service layer for contact management.
 * <p>
 * Encapsulates all business rules around adding contacts and resolving
 * a user's contact list (explicit contacts + inferred DM partners).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;
    private final UserRepository    userRepository;

    /**
     * Adds a new contact for the given user.
     *
     * @param currentUserPhone the authenticated user's phone number
     * @param contactPhone     the phone number to add
     * @throws IllegalArgumentException if the user tries to add themselves or the contact doesn't exist
     * @throws IllegalStateException    if the contact was already added
     */
    public void addContact(String currentUserPhone, String contactPhone) {
        if (currentUserPhone.equals(contactPhone)) {
            throw new IllegalArgumentException("You cannot add yourself as a contact!");
        }

        if (!userRepository.existsByPhoneNumber(contactPhone)) {
            throw new IllegalArgumentException("User not found!");
        }

        if (contactRepository.existsByUserPhoneAndContactPhone(currentUserPhone, contactPhone)) {
            // Idempotent — already added; treat as success
            log.debug("Contact {} already added for user {}", contactPhone, currentUserPhone);
            return;
        }

        Contact contact = Contact.builder()
                .userPhone(currentUserPhone)
                .contactPhone(contactPhone)
                .build();
        contactRepository.save(contact);
        log.info("User {} added contact {}", currentUserPhone, contactPhone);
    }

    /**
     * Returns the combined contact list for a user:
     * explicitly added contacts <em>plus</em> any phone numbers they've exchanged DMs with.
     *
     * @param currentUserPhone the authenticated user's phone number
     * @return list of {@link UserDto} profiles, excluding the user themselves
     */
    public List<UserDto> getContacts(String currentUserPhone) {
        // 1. Explicitly added contacts
        Set<String> contactPhones = contactRepository.findByUserPhone(currentUserPhone).stream()
                .map(Contact::getContactPhone)
                .collect(Collectors.toCollection(HashSet::new));

        // 2. Inferred from DM history
        contactPhones.addAll(messageRepository.findDistinctChatPartners(currentUserPhone));

        // 3. Exclude self
        contactPhones.remove(currentUserPhone);

        // 4. Resolve to UserDto profiles (silently skip unknown phones)
        return contactPhones.stream()
                .map(userRepository::findByPhoneNumber)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(user -> UserDto.builder()
                        .phoneNumber(user.getPhoneNumber())
                        .displayName(user.getDisplayName())
                        .status(user.getStatus())
                        .profilePictureUrl(user.getProfilePictureUrl())
                        .lastSeen(user.getLastSeen())
                        .build())
                .collect(Collectors.toList());
    }
}
