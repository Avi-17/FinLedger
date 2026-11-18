package com.finledger.api.group.repository;

import com.finledger.api.group.domain.GroupMember;
import com.finledger.api.group.domain.GroupMemberId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMember> findByGroupId(Long groupId);
    long countByGroupIdAndUserIdIn(Long groupId, Collection<Long> userIds);

    /** Member count for many groups in one query (avoids N+1 in "my groups"). */
    interface GroupCount { Long getGroupId(); Long getCount(); }

    @Query("select m.groupId as groupId, count(m) as count from GroupMember m " +
           "where m.groupId in :groupIds group by m.groupId")
    List<GroupCount> countByGroupIds(Collection<Long> groupIds);
}
