package com.finledger.api.group.access;

import java.util.Collection;

/** Who may do what inside a group. Services depend on this small interface, not on GroupService. */
public interface GroupAccessPolicy {
    void requireMember(Long groupId, Long userId);
    void requireAdmin(Long groupId, Long userId);
    void requireAllMembers(Long groupId, Collection<Long> userIds);
}
