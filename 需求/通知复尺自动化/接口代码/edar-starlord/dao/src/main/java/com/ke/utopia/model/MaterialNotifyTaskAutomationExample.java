package com.ke.utopia.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MaterialNotifyTaskAutomationExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    /**
     * 分页开始
     */
    protected Integer limitStart;

    /**
     * 分页条数
     */
    protected Integer limitSize;

    public MaterialNotifyTaskAutomationExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    public void setLimitStart(Integer limitStart) {
        this.limitStart = limitStart;
    }

    public Integer getLimitStart() {
        return limitStart;
    }

    public void setLimitSize(Integer limitSize) {
        this.limitSize = limitSize;
    }

    public Integer getLimitSize() {
        return limitSize;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeIsNull() {
            addCriterion("receive_time is null");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeIsNotNull() {
            addCriterion("receive_time is not null");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeEqualTo(Date value) {
            addCriterion("receive_time =", value, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeNotEqualTo(Date value) {
            addCriterion("receive_time <>", value, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeGreaterThan(Date value) {
            addCriterion("receive_time >", value, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("receive_time >=", value, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeLessThan(Date value) {
            addCriterion("receive_time <", value, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeLessThanOrEqualTo(Date value) {
            addCriterion("receive_time <=", value, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeIn(List<Date> values) {
            addCriterion("receive_time in", values, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeNotIn(List<Date> values) {
            addCriterion("receive_time not in", values, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeBetween(Date value1, Date value2) {
            addCriterion("receive_time between", value1, value2, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andReceiveTimeNotBetween(Date value1, Date value2) {
            addCriterion("receive_time not between", value1, value2, "receiveTime");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdIsNull() {
            addCriterion("project_order_id is null");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdIsNotNull() {
            addCriterion("project_order_id is not null");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdEqualTo(Long value) {
            addCriterion("project_order_id =", value, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdNotEqualTo(Long value) {
            addCriterion("project_order_id <>", value, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdGreaterThan(Long value) {
            addCriterion("project_order_id >", value, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdGreaterThanOrEqualTo(Long value) {
            addCriterion("project_order_id >=", value, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdLessThan(Long value) {
            addCriterion("project_order_id <", value, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdLessThanOrEqualTo(Long value) {
            addCriterion("project_order_id <=", value, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdIn(List<Long> values) {
            addCriterion("project_order_id in", values, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdNotIn(List<Long> values) {
            addCriterion("project_order_id not in", values, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdBetween(Long value1, Long value2) {
            addCriterion("project_order_id between", value1, value2, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andProjectOrderIdNotBetween(Long value1, Long value2) {
            addCriterion("project_order_id not between", value1, value2, "projectOrderId");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeIsNull() {
            addCriterion("material_code is null");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeIsNotNull() {
            addCriterion("material_code is not null");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeEqualTo(String value) {
            addCriterion("material_code =", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeNotEqualTo(String value) {
            addCriterion("material_code <>", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeGreaterThan(String value) {
            addCriterion("material_code >", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeGreaterThanOrEqualTo(String value) {
            addCriterion("material_code >=", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeLessThan(String value) {
            addCriterion("material_code <", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeLessThanOrEqualTo(String value) {
            addCriterion("material_code <=", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeLike(String value) {
            addCriterion("material_code like", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeNotLike(String value) {
            addCriterion("material_code not like", value, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeIn(List<String> values) {
            addCriterion("material_code in", values, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeNotIn(List<String> values) {
            addCriterion("material_code not in", values, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeBetween(String value1, String value2) {
            addCriterion("material_code between", value1, value2, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialCodeNotBetween(String value1, String value2) {
            addCriterion("material_code not between", value1, value2, "materialCode");
            return (Criteria) this;
        }

        public Criteria andMaterialNameIsNull() {
            addCriterion("material_name is null");
            return (Criteria) this;
        }

        public Criteria andMaterialNameIsNotNull() {
            addCriterion("material_name is not null");
            return (Criteria) this;
        }

        public Criteria andMaterialNameEqualTo(String value) {
            addCriterion("material_name =", value, "materialName");
            return (Criteria) this;
        }

        public Criteria andMaterialNameNotEqualTo(String value) {
            addCriterion("material_name <>", value, "materialName");
            return (Criteria) this;
        }

        public Criteria andMaterialNameLike(String value) {
            addCriterion("material_name like", value, "materialName");
            return (Criteria) this;
        }

        public Criteria andMaterialNameNotLike(String value) {
            addCriterion("material_name not like", value, "materialName");
            return (Criteria) this;
        }

        public Criteria andMaterialNameIn(List<String> values) {
            addCriterion("material_name in", values, "materialName");
            return (Criteria) this;
        }

        public Criteria andMaterialNameNotIn(List<String> values) {
            addCriterion("material_name not in", values, "materialName");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIsNull() {
            addCriterion("task_type is null");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIsNotNull() {
            addCriterion("task_type is not null");
            return (Criteria) this;
        }

        public Criteria andTaskTypeEqualTo(Integer value) {
            addCriterion("task_type =", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotEqualTo(Integer value) {
            addCriterion("task_type <>", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeGreaterThan(Integer value) {
            addCriterion("task_type >", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeGreaterThanOrEqualTo(Integer value) {
            addCriterion("task_type >=", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLessThan(Integer value) {
            addCriterion("task_type <", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLessThanOrEqualTo(Integer value) {
            addCriterion("task_type <=", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIn(List<Integer> values) {
            addCriterion("task_type in", values, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotIn(List<Integer> values) {
            addCriterion("task_type not in", values, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeBetween(Integer value1, Integer value2) {
            addCriterion("task_type between", value1, value2, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotBetween(Integer value1, Integer value2) {
            addCriterion("task_type not between", value1, value2, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskNameIsNull() {
            addCriterion("task_name is null");
            return (Criteria) this;
        }

        public Criteria andTaskNameIsNotNull() {
            addCriterion("task_name is not null");
            return (Criteria) this;
        }

        public Criteria andTaskNameEqualTo(String value) {
            addCriterion("task_name =", value, "taskName");
            return (Criteria) this;
        }

        public Criteria andTaskNameNotEqualTo(String value) {
            addCriterion("task_name <>", value, "taskName");
            return (Criteria) this;
        }

        public Criteria andTaskNameLike(String value) {
            addCriterion("task_name like", value, "taskName");
            return (Criteria) this;
        }

        public Criteria andTaskNameNotLike(String value) {
            addCriterion("task_name not like", value, "taskName");
            return (Criteria) this;
        }

        public Criteria andTaskNameIn(List<String> values) {
            addCriterion("task_name in", values, "taskName");
            return (Criteria) this;
        }

        public Criteria andTaskNameNotIn(List<String> values) {
            addCriterion("task_name not in", values, "taskName");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusIsNull() {
            addCriterion("notify_status is null");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusIsNotNull() {
            addCriterion("notify_status is not null");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusEqualTo(Integer value) {
            addCriterion("notify_status =", value, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusNotEqualTo(Integer value) {
            addCriterion("notify_status <>", value, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusGreaterThan(Integer value) {
            addCriterion("notify_status >", value, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusGreaterThanOrEqualTo(Integer value) {
            addCriterion("notify_status >=", value, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusLessThan(Integer value) {
            addCriterion("notify_status <", value, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusLessThanOrEqualTo(Integer value) {
            addCriterion("notify_status <=", value, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusIn(List<Integer> values) {
            addCriterion("notify_status in", values, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusNotIn(List<Integer> values) {
            addCriterion("notify_status not in", values, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusBetween(Integer value1, Integer value2) {
            addCriterion("notify_status between", value1, value2, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andNotifyStatusNotBetween(Integer value1, Integer value2) {
            addCriterion("notify_status not between", value1, value2, "notifyStatus");
            return (Criteria) this;
        }

        public Criteria andJudgeResultIsNull() {
            addCriterion("judge_result is null");
            return (Criteria) this;
        }

        public Criteria andJudgeResultIsNotNull() {
            addCriterion("judge_result is not null");
            return (Criteria) this;
        }

        public Criteria andJudgeResultEqualTo(Integer value) {
            addCriterion("judge_result =", value, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultNotEqualTo(Integer value) {
            addCriterion("judge_result <>", value, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultGreaterThan(Integer value) {
            addCriterion("judge_result >", value, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultGreaterThanOrEqualTo(Integer value) {
            addCriterion("judge_result >=", value, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultLessThan(Integer value) {
            addCriterion("judge_result <", value, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultLessThanOrEqualTo(Integer value) {
            addCriterion("judge_result <=", value, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultIn(List<Integer> values) {
            addCriterion("judge_result in", values, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultNotIn(List<Integer> values) {
            addCriterion("judge_result not in", values, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultBetween(Integer value1, Integer value2) {
            addCriterion("judge_result between", value1, value2, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeResultNotBetween(Integer value1, Integer value2) {
            addCriterion("judge_result not between", value1, value2, "judgeResult");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkIsNull() {
            addCriterion("judge_remark is null");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkIsNotNull() {
            addCriterion("judge_remark is not null");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkEqualTo(String value) {
            addCriterion("judge_remark =", value, "judgeRemark");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkNotEqualTo(String value) {
            addCriterion("judge_remark <>", value, "judgeRemark");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkLike(String value) {
            addCriterion("judge_remark like", value, "judgeRemark");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkNotLike(String value) {
            addCriterion("judge_remark not like", value, "judgeRemark");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkIn(List<String> values) {
            addCriterion("judge_remark in", values, "judgeRemark");
            return (Criteria) this;
        }

        public Criteria andJudgeRemarkNotIn(List<String> values) {
            addCriterion("judge_remark not in", values, "judgeRemark");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsIsNull() {
            addCriterion("key_frame_urls is null");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsIsNotNull() {
            addCriterion("key_frame_urls is not null");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsEqualTo(String value) {
            addCriterion("key_frame_urls =", value, "keyFrameUrls");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsNotEqualTo(String value) {
            addCriterion("key_frame_urls <>", value, "keyFrameUrls");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsLike(String value) {
            addCriterion("key_frame_urls like", value, "keyFrameUrls");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsNotLike(String value) {
            addCriterion("key_frame_urls not like", value, "keyFrameUrls");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsIn(List<String> values) {
            addCriterion("key_frame_urls in", values, "keyFrameUrls");
            return (Criteria) this;
        }

        public Criteria andKeyFrameUrlsNotIn(List<String> values) {
            addCriterion("key_frame_urls not in", values, "keyFrameUrls");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeIsNull() {
            addCriterion("recommend_visit_time is null");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeIsNotNull() {
            addCriterion("recommend_visit_time is not null");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeEqualTo(Date value) {
            addCriterion("recommend_visit_time =", value, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeNotEqualTo(Date value) {
            addCriterion("recommend_visit_time <>", value, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeGreaterThan(Date value) {
            addCriterion("recommend_visit_time >", value, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("recommend_visit_time >=", value, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeLessThan(Date value) {
            addCriterion("recommend_visit_time <", value, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeLessThanOrEqualTo(Date value) {
            addCriterion("recommend_visit_time <=", value, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeIn(List<Date> values) {
            addCriterion("recommend_visit_time in", values, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeNotIn(List<Date> values) {
            addCriterion("recommend_visit_time not in", values, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeBetween(Date value1, Date value2) {
            addCriterion("recommend_visit_time between", value1, value2, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andRecommendVisitTimeNotBetween(Date value1, Date value2) {
            addCriterion("recommend_visit_time not between", value1, value2, "recommendVisitTime");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNull() {
            addCriterion("gmt_create is null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNotNull() {
            addCriterion("gmt_create is not null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateEqualTo(Date value) {
            addCriterion("gmt_create =", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotEqualTo(Date value) {
            addCriterion("gmt_create <>", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThan(Date value) {
            addCriterion("gmt_create >", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_create >=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThan(Date value) {
            addCriterion("gmt_create <", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThanOrEqualTo(Date value) {
            addCriterion("gmt_create <=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIn(List<Date> values) {
            addCriterion("gmt_create in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotIn(List<Date> values) {
            addCriterion("gmt_create not in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateBetween(Date value1, Date value2) {
            addCriterion("gmt_create between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotBetween(Date value1, Date value2) {
            addCriterion("gmt_create not between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNull() {
            addCriterion("gmt_modified is null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNotNull() {
            addCriterion("gmt_modified is not null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedEqualTo(Date value) {
            addCriterion("gmt_modified =", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotEqualTo(Date value) {
            addCriterion("gmt_modified <>", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThan(Date value) {
            addCriterion("gmt_modified >", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_modified >=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThan(Date value) {
            addCriterion("gmt_modified <", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) {
            addCriterion("gmt_modified <=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIn(List<Date> values) {
            addCriterion("gmt_modified in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotIn(List<Date> values) {
            addCriterion("gmt_modified not in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedBetween(Date value1, Date value2) {
            addCriterion("gmt_modified between", value1, value2, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotBetween(Date value1, Date value2) {
            addCriterion("gmt_modified not between", value1, value2, "gmtModified");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}