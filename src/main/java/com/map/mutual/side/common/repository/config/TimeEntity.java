package com.map.mutual.side.common.repository.config;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;
/**
 * fileName       : TimeEntity
 * author         : kimjaejung
 * createDate     : 2022/03/25
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2022/03/25        kimjaejung       최초 생성
 *
 */
@MappedSuperclass
public class TimeEntity {
    @Column(name = "CREATE_DT")
    private LocalDateTime createTime;
    @Column(name = "UPDATE_DT")
    private LocalDateTime updateTime;

    @PrePersist
    public void before() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            this.createTime = now;
        }
        this.updateTime = now;
    }

    @PreUpdate
    public void always() {
        this.updateTime = LocalDateTime.now();
    }
}
