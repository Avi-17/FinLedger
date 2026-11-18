package com.finledger.api.group.access;

import com.finledger.api.group.domain.GroupMember;
import com.finledger.api.group.domain.GroupMemberId;
import com.finledger.api.group.repository.GroupMemberRepository;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.common.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/** GroupAccessPolicy backed by the group_members table. */
@Component
@RequiredArgsConstructor
public class MembershipAccessPolicy implements GroupAccessPolicy {
    private final GroupMemberRepository members;

    @Override
    public void requireMember(Long groupId, Long userId) {
        if (!members.existsByGroupIdAndUserId(groupId, userId))
            throw new ForbiddenException("You are not a member of this group");
    }

    @Override
    public void requireAdmin(Long groupId, Long userId) {
        members.findById(new GroupMemberId(groupId, userId))
                .filter(GroupMember::isAdmin)
                .orElseThrow(() -> new ForbiddenException("Only group admins can do this"));
    }

    @Override
    public void requireAllMembers(Long groupId, Collection<Long> userIds) {
        Set<Long> distinct = new HashSet<>(userIds);
        if (members.countByGroupIdAndUserIdIn(groupId, distinct) != distinct.size())
            throw new BusinessRuleException("All participants must be members of this group");
    }
}
