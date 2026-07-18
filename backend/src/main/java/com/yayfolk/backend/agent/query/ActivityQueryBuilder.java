package com.yayfolk.backend.agent.query;

import com.yayfolk.backend.entity.Activity;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用 JPA Criteria API 构建类型安全的活动查询。
 * 替代 AIResourceService 中的原生 SQL 拼接（消除 SQL 注入风险）。
 */
@Component
public class ActivityQueryBuilder {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 根据 QueryParams 执行活动查询，最多返回 20 条结果。
     */
    public List<Activity> query(QueryParams params) {
        return query(params, 20);
    }

    /**
     * 根据 QueryParams 执行活动查询。
     */
    public List<Activity> query(QueryParams params, int maxResults) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Activity> cq = cb.createQuery(Activity.class);
        Root<Activity> root = cq.from(Activity.class);

        List<Predicate> predicates = buildPredicates(params, cb, root);

        // 默认排除已结束的活动，只返回已审核通过的活动
        predicates.add(cb.notEqual(root.get("status"), "ended"));
        predicates.add(cb.equal(root.get("auditStatus"), "approved"));

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get("startTime")));

        TypedQuery<Activity> query = entityManager.createQuery(cq);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    private List<Predicate> buildPredicates(QueryParams params, CriteriaBuilder cb, Root<Activity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (params.getHeritageType() != null) {
            String type = "%" + params.getHeritageType() + "%";
            Predicate typePred = cb.like(root.get("heritageType"), type);
            Predicate titlePred = cb.like(root.get("title"), type);
            Predicate subtitlePred = cb.like(root.get("subtitle"), type);
            predicates.add(cb.or(typePred, titlePred, subtitlePred));
        }

        if (params.getLocationCity() != null) {
            predicates.add(cb.like(root.get("locationCity"), "%" + params.getLocationCity() + "%"));
        }

        if (params.getPrice() != null) {
            if ("免费".equals(params.getPrice()) || "0".equals(params.getPrice())) {
                predicates.add(cb.equal(root.get("price"), 0));
            }
        }

        if (params.getPriceMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), params.getPriceMax().intValue()));
        }

        if (params.getStartDate() != null) {
            predicates.add(cb.equal(root.get("startTime").as(java.sql.Date.class),
                    java.sql.Date.valueOf(params.getStartDate())));
        }

        if (params.getStartDateAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("startTime").as(java.sql.Date.class),
                    java.sql.Date.valueOf(params.getStartDateAfter())));
        }

        if (params.getStartDateBefore() != null) {
            predicates.add(cb.lessThan(root.get("startTime").as(java.sql.Date.class),
                    java.sql.Date.valueOf(params.getStartDateBefore())));
        }

        return predicates;
    }
}
