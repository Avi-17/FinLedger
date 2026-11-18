package com.finledger.api.group.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "group_members")
@IdClass(GroupMemberId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember {
    @Id @Column(name = "group_id")
    private Long groupId;

    @Id @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public enum Role { ADMIN, MEMBER }

    public GroupMember(Long groupId, Long userId, Role role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
    }

    public boolean isAdmin() { return role == Role.ADMIN; }
}
