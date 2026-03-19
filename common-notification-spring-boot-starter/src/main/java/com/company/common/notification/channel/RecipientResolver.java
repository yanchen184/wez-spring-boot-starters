package com.company.common.notification.channel;

/**
 * SPI for resolving recipient contact information from user ID.
 * <p>
 * Consumers MUST provide an implementation so channels know where to deliver.
 *
 * <pre>
 * {@literal @}Component
 * public class UserRecipientResolver implements RecipientResolver {
 *     public String resolveEmail(Long userId) {
 *         return userRepository.findById(userId).map(User::getEmail).orElse(null);
 *     }
 *     public String resolveDisplayName(Long userId) {
 *         return userRepository.findById(userId).map(User::getDisplayName).orElse("User#" + userId);
 *     }
 * }
 * </pre>
 */
public interface RecipientResolver {

    /**
     * Resolve email address for the given user ID.
     *
     * @return email address, or null if not available
     */
    String resolveEmail(Long userId);

    /**
     * Resolve display name for the given user ID.
     *
     * @return display name, or the userId as string if not available
     */
    default String resolveDisplayName(Long userId) {
        return "User#" + userId;
    }
}
