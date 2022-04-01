package com.map.mutual.side.review.repository.dsl.impl;

import com.map.mutual.side.review.model.entity.QReviewEntity;
import com.map.mutual.side.review.model.entity.QReviewWorldMappingEntity;
import com.map.mutual.side.review.model.entity.ReviewEntity;
import com.map.mutual.side.review.repository.dsl.ReviewWorldMappingRepoDSL;
import com.map.mutual.side.world.model.dto.WorldDto;
import com.map.mutual.side.world.model.entity.QWorldEntity;
import com.map.mutual.side.world.model.entity.QWorldUserMappingEntity;
import com.map.mutual.side.world.model.entity.WorldEntity;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ReviewWorldMappingRepoDSLImpl implements ReviewWorldMappingRepoDSL {
    private final JPAQueryFactory jpaQueryFactory;

    public ReviewWorldMappingRepoDSLImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<ReviewEntity> findAllReviewsByWorldId(Long worldId) {
        List<ReviewEntity> reviewEntities = jpaQueryFactory
                .select(QReviewEntity.reviewEntity)
                .from(QReviewEntity.reviewEntity)
                .join(QReviewWorldMappingEntity.reviewWorldMappingEntity)
                .on(QReviewEntity.reviewEntity.reviewId.eq(QReviewWorldMappingEntity.reviewWorldMappingEntity.reviewEntity.reviewId))
                .where(QReviewWorldMappingEntity.reviewWorldMappingEntity.worldEntity.worldId.eq(worldId))
                .fetch();
        return reviewEntities;
    }


    //월드
    @Override
    public List<WorldDto> findAllWorldsByReviewId(Long reviewId, String suid) {

        List<WorldEntity> worldEntities = jpaQueryFactory
                .select(QWorldEntity.worldEntity)
                .from(QReviewWorldMappingEntity.reviewWorldMappingEntity)
                .innerJoin(QWorldEntity.worldEntity)
                .on(QReviewWorldMappingEntity.reviewWorldMappingEntity.reviewEntity.reviewId.eq(reviewId)
                        .and(QReviewWorldMappingEntity.reviewWorldMappingEntity.worldEntity.worldId.eq(QWorldEntity.worldEntity.worldId)))
                .fetchJoin()
                .leftJoin(QWorldUserMappingEntity.worldUserMappingEntity)
                .on(QWorldEntity.worldEntity.worldId.eq(QWorldUserMappingEntity.worldUserMappingEntity.worldId)
                        .and(QWorldUserMappingEntity.worldUserMappingEntity.userSuid.eq(suid)))
                .orderBy( new CaseBuilder().when(QWorldEntity.worldEntity.worldOwner.eq(suid)).then(1).otherwise(0).desc()
                , QWorldUserMappingEntity.worldUserMappingEntity.accessTime.desc())
                .fetch();

        List<WorldDto> worlds = new ArrayList<>();

        for(WorldEntity world : worldEntities){
            worlds.add(WorldDto.builder().worldId(world.getWorldId()).worldName(world.getWorldName()).build());
        }
        return worlds;
    }


}
