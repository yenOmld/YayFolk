package com.yayfolk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.ActivityReserve;
import com.yayfolk.backend.entity.MerchantApplication;
import com.yayfolk.backend.entity.MerchantProfile;
import com.yayfolk.backend.entity.MerchantReview;
import com.yayfolk.backend.entity.Order;
import com.yayfolk.backend.entity.Product;
import com.yayfolk.backend.entity.ReserveStatusLog;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.ActivityReserveRepository;
import com.yayfolk.backend.repository.MerchantApplicationRepository;
import com.yayfolk.backend.repository.MerchantProfileRepository;
import com.yayfolk.backend.repository.MerchantReviewRepository;
import com.yayfolk.backend.repository.OrderRepository;
import com.yayfolk.backend.repository.ProductRepository;
import com.yayfolk.backend.repository.ReserveStatusLogRepository;
import com.yayfolk.backend.repository.UserRepository;
import com.yayfolk.backend.util.QrCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class MerchantService {

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantApplicationRepository applicationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityReserveRepository activityReserveRepository;
    private final ReserveStatusLogRepository reserveStatusLogRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final MerchantReviewRepository merchantReviewRepository;
    private final ObjectMapper objectMapper;

    public MerchantService(UserRepository userRepository,
                           MerchantProfileRepository merchantProfileRepository,
                           MerchantApplicationRepository applicationRepository,
                           ActivityRepository activityRepository,
                           ActivityReserveRepository activityReserveRepository,
                           ReserveStatusLogRepository reserveStatusLogRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           MerchantReviewRepository merchantReviewRepository,
                           ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.applicationRepository = applicationRepository;
        this.activityRepository = activityRepository;
        this.activityReserveRepository = activityReserveRepository;
        this.reserveStatusLogRepository = reserveStatusLogRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.merchantReviewRepository = merchantReviewRepository;
        this.objectMapper = objectMapper;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private MerchantProfile getOrCreateMerchantProfile(Long userId) {
        return merchantProfileRepository.findByUserId(userId)
                .orElseGet(new java.util.function.Supplier<MerchantProfile>() {
                    @Override
                    public MerchantProfile get() {
                        MerchantProfile profile = new MerchantProfile();
                        profile.setUserId(userId);
                        profile.setBusinessStatus("pending");
                        return merchantProfileRepository.save(profile);
                    }
                });
    }

    public Map<String, Object> applyMerchant(String username, Map<String, Object> data) {
        User user = getUser(username);

        Optional<MerchantApplication> existing = applicationRepository.findByUserId(user.getId());
        MerchantApplication app = existing.orElseGet(new java.util.function.Supplier<MerchantApplication>() {
            @Override
            public MerchantApplication get() {
                MerchantApplication application = new MerchantApplication();
                application.setUserId(user.getId());
                return application;
            }
        });

        app.setRealName(stringValue(data.get("realName")));
        app.setIdCard(stringValue(data.get("idCard")));
        app.setPhone(stringValue(data.get("phone")));
        app.setHeritageType(stringValue(data.get("heritageType")));
        app.setHeritageDescription(stringValue(data.get("heritageDescription")));
        app.setProofImages(stringValue(data.get("proofImages")));
        app.setShopName(stringValue(data.get("shopName")));
        app.setShopAddress(stringValue(data.get("shopAddress")));
        app.setProvince(stringValue(data.get("province")));
        app.setCity(stringValue(data.get("city")));
        app.setIntro(stringValue(data.get("intro")));
        app.setApplicationStatus("pending");
        app.setAuditAdminId(null);
        app.setAuditTime(null);
        app.setAuditRemark(null);
        applicationRepository.save(app);

        user.setShopStatus("pending");
        user.setShopName(stringValue(data.get("shopName")));
        userRepository.save(user);

        MerchantProfile profile = getOrCreateMerchantProfile(user.getId());
        profile.setShopName(stringValue(data.get("shopName")));
        profile.setContactName(stringValue(data.get("realName")));
        profile.setContactPhone(stringValue(data.get("phone")));
        profile.setHeritageType(stringValue(data.get("heritageType")));
        profile.setHeritageDescription(stringValue(data.get("heritageDescription")));
        profile.setProofImages(stringValue(data.get("proofImages")));
        profile.setAddress(stringValue(data.get("shopAddress")));
        profile.setProvince(stringValue(data.get("province")));
        profile.setCity(stringValue(data.get("city")));
        profile.setShopIntro(stringValue(data.get("intro")));
        profile.setBusinessStatus("pending");
        profile.setLatestApplicationId(app.getId());
        merchantProfileRepository.save(profile);

        app.setMerchantProfileId(profile.getId());
        applicationRepository.save(app);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", app.getId());
        result.put("status", "pending");
        result.put("merchantProfileId", profile.getId());
        return result;
    }

    public Map<String, Object> getMyApplication(String username) {
        User user = getUser(username);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("shopStatus", user.getShopStatus());
        result.put("isMerchant", user.getIsMerchant());

        merchantProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            result.put("merchantProfileId", profile.getId());
            result.put("businessStatus", profile.getBusinessStatus());
            result.put("shopName", profile.getShopName());
            result.put("realName", profile.getContactName());
            result.put("phone", profile.getContactPhone());
            result.put("heritageType", profile.getHeritageType());
            result.put("heritageDescription", profile.getHeritageDescription());
            result.put("shopAddress", profile.getAddress());
            result.put("province", profile.getProvince());
            result.put("city", profile.getCity());
            result.put("intro", profile.getShopIntro());
            result.put("proofImages", profile.getProofImages());
        });

        applicationRepository.findByUserId(user.getId()).ifPresent(app -> {
            result.put("applicationId", app.getId());
            result.put("applicationStatus", app.getApplicationStatus());
            result.put("realName", app.getRealName());
            result.put("idCard", app.getIdCard());
            result.put("phone", app.getPhone());
            result.put("shopName", app.getShopName());
            result.put("shopAddress", app.getShopAddress());
            result.put("heritageType", app.getHeritageType());
            result.put("heritageDescription", app.getHeritageDescription());
            result.put("province", app.getProvince());
            result.put("city", app.getCity());
            result.put("intro", app.getIntro());
            result.put("proofImages", app.getProofImages());
            result.put("auditRemark", app.getAuditRemark());
            result.put("createTime", app.getCreateTime());
            result.put("updateTime", app.getUpdateTime());
            result.put("auditTime", app.getAuditTime());
        });
        return result;
    }

    public List<Map<String, Object>> getMyActivities(String username) {
        User user = getUser(username);
        return buildMerchantActivityItems(activityRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId()));
    }

    public Map<String, Object> getMyActivitiesPage(String username, Integer page, Integer size) {
        User user = getUser(username);
        List<Map<String, Object>> items = buildMerchantActivityItems(activityRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId()));
        return paginateItems(items, page, size);
    }

    public Map<String, Object> createActivity(String username, Map<String, Object> data) {
        User user = getUser(username);
        if (!"merchant".equals(user.getRole()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("Only merchants can publish activities");
        }
        MerchantProfile profile = merchantProfileRepository.findByUserId(user.getId()).orElse(null);
        Activity activity = new Activity();
        activity.setMerchantId(user.getId());
        if (profile != null) {
            activity.setMerchantProfileId(profile.getId());
        }
        fillActivity(activity, data);
        activity.setAuditStatus("pending");
        activity.setAuditRemark(null);
        activityRepository.save(activity);
        return activityToMap(activity);
    }

    public Map<String, Object> updateActivity(String username, Long id, Map<String, Object> data) {
        User user = getUser(username);
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        if (!activity.getMerchantId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to operate on this activity");
        }
        fillActivity(activity, data);
        activity.setAuditStatus("pending");
        activity.setAuditRemark(null);
        activityRepository.save(activity);
        return activityToMap(activity);
    }

    public void deleteActivity(String username, Long id) {
        User user = getUser(username);
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        if (!activity.getMerchantId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to operate on this activity");
        }
        activityRepository.delete(activity);
    }

    private void fillActivity(Activity activity, Map<String, Object> data) {
        if (data.containsKey("categoryId")) {
            Object categoryId = data.get("categoryId");
            activity.setCategoryId(categoryId instanceof Number ? ((Number) categoryId).intValue() : null);
        }
        if (data.containsKey("title")) {
            activity.setTitle(stringValue(data.get("title")));
        }
        if (data.containsKey("subtitle")) {
            activity.setSubtitle(stringValue(data.get("subtitle")));
        }
        if (data.containsKey("content")) {
            activity.setContent(stringValue(data.get("content")));
        }
        if (data.containsKey("coverImage")) {
            activity.setCoverImage(stringValue(data.get("coverImage")));
        }
        if (data.containsKey("images")) {
            Object value = data.get("images");
            if (value instanceof String) {
                activity.setImages((String) value);
            } else {
                try {
                    activity.setImages(objectMapper.writeValueAsString(value));
                } catch (Exception ignored) {
                    activity.setImages(null);
                }
            }
        }
        if (data.containsKey("videoUrl")) {
            activity.setVideoUrl(stringValue(data.get("videoUrl")));
        }
        if (data.containsKey("videoCoverUrl")) {
            activity.setVideoCoverUrl(stringValue(data.get("videoCoverUrl")));
        }
        if (data.containsKey("heritageType")) {
            activity.setHeritageType(stringValue(data.get("heritageType")));
        }
        if (data.containsKey("activityType")) {
            activity.setActivityType(stringValue(data.get("activityType")));
        }
        if (data.containsKey("locationProvince")) {
            activity.setLocationProvince(stringValue(data.get("locationProvince")));
        }
        if (data.containsKey("locationCity")) {
            activity.setLocationCity(stringValue(data.get("locationCity")));
        }
        if (data.containsKey("locationDistrict")) {
            activity.setLocationDistrict(stringValue(data.get("locationDistrict")));
        }
        if (data.containsKey("locationDetail")) {
            activity.setLocationDetail(stringValue(data.get("locationDetail")));
        }
        if (data.containsKey("price")) {
            activity.setPrice(intValue(data.get("price"), 0));
        }
        if (data.containsKey("originalPrice")) {
            activity.setOriginalPrice(intValue(data.get("originalPrice"), null));
        }
        if (data.containsKey("maxParticipants")) {
            activity.setMaxParticipants(intValue(data.get("maxParticipants"), null));
        }
        if (data.containsKey("startTime")) {
            activity.setStartTime(parseDate(data.get("startTime"), activity.getStartTime()));
        }
        if (data.containsKey("endTime")) {
            activity.setEndTime(parseDate(data.get("endTime"), activity.getEndTime()));
        }
        if (data.containsKey("signupStartTime")) {
            activity.setSignupStartTime(parseDate(data.get("signupStartTime"), activity.getSignupStartTime()));
        }
        if (data.containsKey("signupEndTime")) {
            activity.setSignupEndTime(parseDate(data.get("signupEndTime"), activity.getSignupEndTime()));
        }
    }

    private List<Map<String, Object>> buildMerchantActivityItems(List<Activity> activities) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Activity activity : activities) {
            Map<String, Object> item = activityToMap(activity);
            Map<String, Object> bookingSummary = buildMerchantBookingSummary(activityReserveRepository.findByActivityIdOrderByCreateTimeDesc(activity.getId()));
            item.putAll(bookingSummary);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> paginateItems(List<Map<String, Object>> items, Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? items.size() : size;
        if (safeSize <= 0) {
            safeSize = 10;
        }

        int total = items.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", new ArrayList<Map<String, Object>>(items.subList(fromIndex, toIndex)));
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total);
        result.put("totalPages", totalPages);
        return result;
    }

    private Map<String, Object> activityToMap(Activity activity) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", activity.getId());
        map.put("merchantId", activity.getMerchantId());
        map.put("merchantProfileId", activity.getMerchantProfileId());
        map.put("categoryId", activity.getCategoryId());
        map.put("title", activity.getTitle());
        map.put("subtitle", activity.getSubtitle());
        map.put("coverImage", activity.getCoverImage());
        map.put("images", parseImageList(activity.getImages()));
        map.put("videoUrl", activity.getVideoUrl());
        map.put("videoCoverUrl", activity.getVideoCoverUrl());
        map.put("content", activity.getContent());
        map.put("activityType", activity.getActivityType());
        map.put("heritageType", activity.getHeritageType());
        map.put("startTime", activity.getStartTime());
        map.put("endTime", activity.getEndTime());
        map.put("signupStartTime", activity.getSignupStartTime());
        map.put("signupEndTime", activity.getSignupEndTime());
        map.put("maxParticipants", activity.getMaxParticipants());
        map.put("currentParticipants", activity.getCurrentParticipants());
        map.put("price", activity.getPrice());
        map.put("originalPrice", activity.getOriginalPrice());
        map.put("locationProvince", activity.getLocationProvince());
        map.put("locationCity", activity.getLocationCity());
        map.put("locationDistrict", activity.getLocationDistrict());
        map.put("locationDetail", activity.getLocationDetail());
        map.put("status", activity.getStatus());
        map.put("auditStatus", activity.getAuditStatus());
        map.put("auditRemark", activity.getAuditRemark());
        map.put("createTime", activity.getCreateTime());
        map.put("updateTime", activity.getUpdateTime());
        return map;
    }

    public List<Map<String, Object>> getActivityBookings(String username, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        User user = getUser(username);
        if (!Objects.equals(activity.getMerchantId(), user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to view bookings");
        }
        return buildMerchantBookingItems(activityReserveRepository.findByActivityIdOrderByCreateTimeDesc(activityId));
    }

    public Map<String, Object> getMerchantBookings(String username, Long activityId, String status, String keyword, Integer page, Integer size) {
        User user = getUser(username);
        List<ActivityReserve> source = activityReserveRepository.findByMerchantIdOrderByUpdateTimeDesc(user.getId());
        List<ActivityReserve> scoped = new ArrayList<ActivityReserve>();
        for (ActivityReserve item : source) {
            if (activityId != null && !activityId.equals(item.getActivityId())) {
                continue;
            }
            if (!matchesBookingKeyword(item, keyword)) {
                continue;
            }
            scoped.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("summary", buildMerchantBookingSummary(scoped));

        List<ActivityReserve> filtered = new ArrayList<ActivityReserve>();
        for (ActivityReserve item : scoped) {
            String currentStatus = toClientReserveStatus(item.getReserveStatus());
            if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status) && !Objects.equals(status, currentStatus)) {
                continue;
            }
            filtered.add(item);
        }

        Map<String, Object> pageResult = paginateItems(buildMerchantBookingItems(filtered), page, size);
        result.putAll(pageResult);
        return result;
    }

    public Map<String, Object> getMerchantBookingDetail(String username, Long bookingId) {
        ActivityReserve booking = requireMerchantBooking(username, bookingId);
        return buildMerchantBookingDetailMap(booking);
    }

    public Map<String, Object> lookupBookingForCheckin(String username, String code) {
        User user = getUser(username);
        String normalized = normalizeLookupCode(code);
        if (!StringUtils.hasText(normalized)) {
            throw new RuntimeException("Booking code cannot be empty");
        }

        ActivityReserve booking = null;
        if (normalized.matches("\\d+")) {
            booking = activityReserveRepository.findById(Long.valueOf(normalized)).orElse(null);
        }
        if (booking == null) {
            booking = activityReserveRepository.findFirstByMerchantIdAndReserveNoIgnoreCase(user.getId(), normalized).orElse(null);
        }
        if (booking == null) {
            throw new RuntimeException("No matching booking was found");
        }
        if (!Objects.equals(booking.getMerchantId(), user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to view this booking");
        }
        return buildMerchantBookingDetailMap(booking);
    }

    public Map<String, Object> lookupBookingForCheckinImage(String username, String imageData) {
        String decoded = QrCodeUtil.decodeFromDataUrl(imageData);
        return lookupBookingForCheckin(username, decoded);
    }
    public Map<String, Object> checkinBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityReserve booking = activityReserveRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking does not exist"));
        if (!Objects.equals(booking.getMerchantId(), user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to check in this booking");
        }
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());
        if (!"registered".equals(currentStatus)) {
            throw new RuntimeException("Only active bookings can be checked in");
        }
        if (!Integer.valueOf(1).equals(booking.getPayStatus())) {
            throw new RuntimeException("Only paid bookings can be checked in");
        }

        String oldStatus = booking.getReserveStatus();
        booking.setReserveStatus("checked_in");
        booking.setVerifyTime(new Date());
        activityReserveRepository.save(booking);
        createStatusLog(booking.getId(), oldStatus, booking.getReserveStatus(), user.getId(), "merchant", "Merchant checked in booking");
        syncActivityCurrentParticipants(booking.getActivityId());
        return buildMerchantBookingDetailMap(booking);
    }

    public Map<String, Object> rejectBooking(String username, Long bookingId) {
        ActivityReserve booking = confirmMerchantBookingRejected(username, bookingId, new Date(), "Merchant rejected booking");
        return buildMerchantBookingDetailMap(booking);
    }

    public ActivityReserve prepareMerchantBookingForReject(String username, Long bookingId) {
        ActivityReserve booking = requireMerchantBooking(username, bookingId);
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());
        if ("rejected".equals(currentStatus)) {
            return booking;
        }
        if (!"registered".equals(currentStatus)) {
            throw new RuntimeException("Only active bookings can be rejected");
        }
        return booking;
    }

    public ActivityReserve prepareMerchantBookingForRefund(String username, Long bookingId) {
        ActivityReserve booking = requireMerchantBooking(username, bookingId);
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());
        if ("registered".equals(currentStatus) && Integer.valueOf(2).equals(booking.getPayStatus())) {
            return booking;
        }
        if (!"registered".equals(currentStatus)) {
            throw new RuntimeException("Only active bookings can be refunded");
        }
        if (!Integer.valueOf(1).equals(booking.getPayStatus())) {
            throw new RuntimeException("Only paid bookings can be refunded");
        }
        if (intValue(booking.getPayAmount(), 0) <= 0) {
            throw new RuntimeException("This booking does not require a refund");
        }
        return booking;
    }

    public ActivityReserve confirmMerchantBookingRejected(String username, Long bookingId, Date operateTime, String remark) {
        User user = getUser(username);
        ActivityReserve booking = requireMerchantBooking(username, bookingId);
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());
        if ("rejected".equals(currentStatus)) {
            return booking;
        }
        if (!"registered".equals(currentStatus)) {
            throw new RuntimeException("Only active bookings can be rejected");
        }
        if (Integer.valueOf(1).equals(booking.getPayStatus()) && intValue(booking.getPayAmount(), 0) > 0) {
            throw new RuntimeException("Paid bookings must be refunded before rejection");
        }

        String oldStatus = booking.getReserveStatus();
        Date finalOperateTime = operateTime == null ? new Date() : operateTime;
        booking.setReserveStatus("rejected");
        booking.setCancelTime(finalOperateTime);
        activityReserveRepository.save(booking);
        createStatusLog(
                booking.getId(),
                oldStatus,
                booking.getReserveStatus(),
                user.getId(),
                "merchant",
                StringUtils.hasText(remark) ? remark : "Merchant rejected booking"
        );
        syncActivityCurrentParticipants(booking.getActivityId());
        return booking;
    }

    public ActivityReserve confirmMerchantBookingRefunded(String username,
                                                          Long bookingId,
                                                          Date refundTime,
                                                          String remark,
                                                          boolean keepBookingActive) {
        User user = getUser(username);
        ActivityReserve booking = requireMerchantBooking(username, bookingId);
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());

        if (keepBookingActive) {
            if ("registered".equals(currentStatus) && Integer.valueOf(2).equals(booking.getPayStatus())) {
                return booking;
            }
            if (!"registered".equals(currentStatus)) {
                throw new RuntimeException("Only active bookings can stay available after refund");
            }
            if (!Integer.valueOf(1).equals(booking.getPayStatus())) {
                throw new RuntimeException("Only paid bookings can be refunded");
            }

            String oldStatus = booking.getReserveStatus();
            booking.setReserveStatus("registered");
            booking.setCancelTime(null);
            booking.setPayStatus(2);
            activityReserveRepository.save(booking);
            createStatusLog(
                    booking.getId(),
                    oldStatus,
                    booking.getReserveStatus(),
                    user.getId(),
                    "merchant",
                    StringUtils.hasText(remark) ? remark : "Merchant refunded booking and kept it active"
            );
            syncActivityCurrentParticipants(booking.getActivityId());
            return booking;
        }

        if ("rejected".equals(currentStatus) && Integer.valueOf(2).equals(booking.getPayStatus())) {
            return booking;
        }
        if (!"registered".equals(currentStatus)) {
            throw new RuntimeException("Only active bookings can be rejected after refund");
        }
        if (!Integer.valueOf(1).equals(booking.getPayStatus())) {
            throw new RuntimeException("Only paid bookings can be refunded");
        }

        String oldStatus = booking.getReserveStatus();
        Date finalRefundTime = refundTime == null ? new Date() : refundTime;
        booking.setReserveStatus("rejected");
        booking.setCancelTime(finalRefundTime);
        booking.setPayStatus(2);
        activityReserveRepository.save(booking);
        createStatusLog(
                booking.getId(),
                oldStatus,
                booking.getReserveStatus(),
                user.getId(),
                "merchant",
                StringUtils.hasText(remark) ? remark : "Merchant rejected booking and refunded it"
        );
        syncActivityCurrentParticipants(booking.getActivityId());
        return booking;
    }

    public List<Map<String, Object>> getMyProducts(String username) {
        User user = getUser(username);
        List<Product> products = productRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Product product : products) {
            result.add(productToMap(product));
        }
        return result;
    }

    public Map<String, Object> createProduct(String username, Map<String, Object> data) {
        User user = getUser(username);
        if (!"merchant".equals(user.getRole()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("Only merchants can publish products");
        }
        Product product = new Product();
        product.setMerchantId(user.getId());
        fillProduct(product, data);
        productRepository.save(product);
        return productToMap(product);
    }

    public Map<String, Object> updateProduct(String username, Long id, Map<String, Object> data) {
        User user = getUser(username);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product does not exist"));
        if (!Objects.equals(product.getMerchantId(), user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to operate on this product");
        }
        fillProduct(product, data);
        productRepository.save(product);
        return productToMap(product);
    }

    public void deleteProduct(String username, Long id) {
        User user = getUser(username);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product does not exist"));
        if (!Objects.equals(product.getMerchantId(), user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to operate on this product");
        }
        productRepository.delete(product);
    }

    public List<Map<String, Object>> getMyOrders(String username) {
        User user = getUser(username);
        List<Order> orders = orderRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Order order : orders) {
            result.add(orderToMap(order));
        }
        return result;
    }

    public Map<String, Object> getMerchantStats(String username) {
        User user = getUser(username);
        List<Activity> activities = activityRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId());
        List<ActivityReserve> bookings = activityReserveRepository.findByMerchantIdOrderByUpdateTimeDesc(user.getId());
        List<MerchantReview> reviews = merchantReviewRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId());

        Map<Long, Activity> activityMap = buildActivityMap(activities);
        Map<Long, ActivityReserve> bookingMap = new HashMap<Long, ActivityReserve>();
        int totalRevenue = 0;
        for (ActivityReserve booking : bookings) {
            bookingMap.put(booking.getId(), booking);
            if (Integer.valueOf(1).equals(booking.getPayStatus())) {
                totalRevenue += safeInt(booking.getPayAmount());
            }
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("activityCount", activities.size());
        summary.putAll(buildMerchantBookingSummary(bookings));
        summary.put("reviewCount", reviews.size());
        summary.put("totalRevenue", totalRevenue);
        summary.put("bookingRevenue", totalRevenue);
        summary.put("averageScore", resolveAverageScore(reviews));
        summary.put("followerCount", resolveFollowerCount(user));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("summary", summary);
        result.put("bookingStatus", buildBookingStatusStats(bookings));
        result.put("salesTrend", buildSalesTrend(bookings));
        result.put("topActivities", buildTopActivities(bookings, activityMap));
        result.put("recentReviews", buildMerchantReviews(reviews, bookingMap, activityMap));
        return result;
    }

    private void fillProduct(Product product, Map<String, Object> data) {
        if (data.containsKey("name")) {
            product.setName(stringValue(data.get("name")));
        }
        if (data.containsKey("subtitle")) {
            product.setSubtitle(stringValue(data.get("subtitle")));
        }
        if (data.containsKey("description")) {
            product.setDescription(stringValue(data.get("description")));
        }
        if (data.containsKey("detail")) {
            product.setDetail(stringValue(data.get("detail")));
        }
        if (data.containsKey("mainImage")) {
            product.setMainImage(stringValue(data.get("mainImage")));
        }
        if (data.containsKey("heritageType")) {
            product.setHeritageType(stringValue(data.get("heritageType")));
        }
        if (data.containsKey("price")) {
            product.setPrice(intValue(data.get("price"), 0));
        }
        if (data.containsKey("stock")) {
            product.setStock(intValue(data.get("stock"), 0));
        }
        if (data.containsKey("status")) {
            product.setStatus(stringValue(data.get("status")));
        }
    }

    private Map<String, Object> productToMap(Product product) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", product.getId());
        map.put("merchantId", product.getMerchantId());
        map.put("name", product.getName());
        map.put("subtitle", product.getSubtitle());
        map.put("description", product.getDescription());
        map.put("detail", product.getDetail());
        map.put("mainImage", product.getMainImage());
        map.put("heritageType", product.getHeritageType());
        map.put("price", product.getPrice());
        map.put("stock", product.getStock());
        map.put("status", product.getStatus());
        map.put("createTime", product.getCreateTime());
        return map;
    }

    public Map<String, Object> bookActivity(String username, Long activityId, Map<String, Object> data) {
        User user = getUser(username);
        if ("merchant".equals(user.getRole()) || "admin".equals(user.getRole())) {
            throw new RuntimeException("Merchant and admin accounts cannot book activities");
        }
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));

        int participantCount = intValue(data.get("participantCount"), 1);
        if (participantCount <= 0) {
            participantCount = 1;
        }
        int currentParticipants = resolveCurrentParticipants(activity.getId());
        if (activity.getMaxParticipants() != null && currentParticipants + participantCount > activity.getMaxParticipants()) {
            throw new RuntimeException("This activity is already full");
        }

        Optional<ActivityReserve> existingBooking = activityReserveRepository.findFirstByActivityIdAndUserIdOrderByCreateTimeDesc(activityId, user.getId());
        if (existingBooking.isPresent()) {
            String currentStatus = toClientReserveStatus(existingBooking.get().getReserveStatus());
            if ("rejected".equals(currentStatus)) {
                throw new RuntimeException("This booking has already been rejected by the merchant");
            }
            if (!"cancelled".equals(currentStatus)) {
                throw new RuntimeException("You have already booked this activity");
            }
        }

        ActivityReserve booking = new ActivityReserve();
        booking.setReserveNo(buildReserveNo());
        booking.setActivityId(activityId);
        booking.setUserId(user.getId());
        booking.setMerchantId(activity.getMerchantId());
        booking.setActivityTitle(defaultString(activity.getTitle()));
        booking.setActivityTime(buildActivityTimeLabel(activity));
        booking.setContactName(StringUtils.hasText(stringValue(data.get("participantName"))) ? stringValue(data.get("participantName")) : displayName(user));
        booking.setContactPhone(StringUtils.hasText(stringValue(data.get("participantPhone"))) ? stringValue(data.get("participantPhone")) : defaultString(user.getPhone()));
        booking.setRemark(stringValue(data.get("remark")));
        booking.setParticipantNum(participantCount);
        int amount = intValue(activity.getPrice(), 0) * participantCount;
        booking.setTotalAmount(amount);
        booking.setPayAmount(amount);
        if (amount <= 0) {
            booking.setPayStatus(1);
            booking.setPaymentType("free");
            booking.setPaymentTime(new Date());
        } else {
            booking.setPayStatus(0);
            booking.setPaymentType("pending");
            booking.setPaymentTime(null);
        }
        booking.setReserveStatus("registered");
        activityReserveRepository.save(booking);

        createStatusLog(booking.getId(), null, booking.getReserveStatus(), user.getId(), "user", "User created booking");
        syncActivityCurrentParticipants(activity.getId());
        return userBookingToMap(booking);
    }

    public Map<String, Object> createOrder(String username, Map<String, Object> data) {
        User user = getUser(username);
        Long productId = Long.valueOf(intValue(data.get("productId"), 0));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product does not exist"));
        if (!"on_sale".equals(product.getStatus())) {
            throw new RuntimeException("Product is not available");
        }
        int quantity = intValue(data.get("quantity"), 1);
        if (quantity <= 0) {
            quantity = 1;
        }
        if (intValue(product.getStock(), 0) < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        Order order = new Order();
        order.setOrderNo(buildOrderNo());
        order.setUserId(user.getId());
        order.setMerchantId(product.getMerchantId());
        merchantProfileRepository.findByUserId(product.getMerchantId()).ifPresent(profile -> order.setMerchantProfileId(profile.getId()));
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setQuantity(quantity);
        int amount = intValue(product.getPrice(), 0) * quantity;
        order.setTotalAmount(amount);
        order.setPayAmount(amount);
        order.setReceiverName(stringValue(data.get("receiverName")));
        order.setReceiverPhone(stringValue(data.get("receiverPhone")));
        order.setReceiverProvince(stringValue(data.get("receiverProvince")));
        order.setReceiverCity(stringValue(data.get("receiverCity")));
        order.setReceiverDistrict(stringValue(data.get("receiverDistrict")));
        order.setReceiverAddress(stringValue(data.get("receiverAddress")));
        order.setRemark(stringValue(data.get("remark")));
        if (amount <= 0) {
            order.setStatus("paid");
            order.setPaymentType("free");
            order.setPaymentTime(new Date());
        } else {
            order.setStatus("pending_payment");
            order.setPaymentType("pending");
        }
        orderRepository.save(order);

        product.setStock(intValue(product.getStock(), 0) - quantity);
        productRepository.save(product);
        return orderToMap(order);
    }

    private Map<String, Object> orderToMap(Order order) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("userId", order.getUserId());
        map.put("merchantId", order.getMerchantId());
        map.put("merchantProfileId", order.getMerchantProfileId());
        map.put("productId", order.getProductId());
        map.put("productName", order.getProductName());
        map.put("quantity", order.getQuantity());
        map.put("totalAmount", order.getTotalAmount());
        map.put("payAmount", order.getPayAmount());
        map.put("status", order.getStatus());
        map.put("paymentType", order.getPaymentType());
        map.put("paymentTime", order.getPaymentTime());
        map.put("receiverName", order.getReceiverName());
        map.put("receiverPhone", order.getReceiverPhone());
        map.put("receiverProvince", order.getReceiverProvince());
        map.put("receiverCity", order.getReceiverCity());
        map.put("receiverDistrict", order.getReceiverDistrict());
        map.put("receiverAddress", order.getReceiverAddress());
        map.put("remark", order.getRemark());
        map.put("logisticsCompany", order.getLogisticsCompany());
        map.put("logisticsNo", order.getLogisticsNo());
        map.put("createTime", order.getCreateTime());
        map.put("updateTime", order.getUpdateTime());
        map.put("canPay", canPayOrder(order));
        return map;
    }

    public List<Map<String, Object>> getMyOrdersAsUser(String username) {
        User user = getUser(username);
        List<Order> orders = orderRepository.findByUserIdAndDeleteStatusOrderByCreateTimeDesc(user.getId(), 0);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Order order : orders) {
            result.add(orderToMap(order));
        }
        return result;
    }

    public Map<String, Object> getMyOrderDetail(String username, Long orderId) {
        return orderToMap(requireUserOrder(username, orderId));
    }

    public Map<String, Object> payForOrder(String username, Long orderId, Map<String, Object> data) {
        Order order = requireUserOrder(username, orderId);
        if ("paid".equalsIgnoreCase(defaultString(order.getStatus()))) {
            return orderToMap(order);
        }
        if (!canPayOrder(order)) {
            throw new RuntimeException("This order cannot be paid in its current status");
        }
        order.setStatus("paid");
        order.setPaymentTime(new Date());
        order.setPaymentType(StringUtils.hasText(stringValue(data == null ? null : data.get("paymentType"))) ? stringValue(data.get("paymentType")) : "simulated");
        orderRepository.save(order);

        if (order.getProductId() != null) {
            productRepository.findById(order.getProductId()).ifPresent(product -> {
                int quantity = intValue(order.getQuantity(), 1);
                product.setSales(intValue(product.getSales(), 0) + quantity);
                product.setSalesCount(intValue(product.getSalesCount(), 0) + quantity);
                productRepository.save(product);
            });
        }
        return orderToMap(order);
    }
    public Order prepareUserOrderForExternalPayment(String username, Long orderId, String paymentType) {
        Order order = requireUserOrder(username, orderId);
        if ("paid".equalsIgnoreCase(defaultString(order.getStatus()))) {
            return order;
        }
        if (!canPayOrder(order)) {
            throw new RuntimeException("This order cannot be paid in its current status");
        }
        if (StringUtils.hasText(paymentType)) {
            order.setPaymentType(paymentType);
            orderRepository.save(order);
        }
        return order;
    }

    public Map<String, Object> getMyActivityBookingDetail(String username, Long bookingId) {
        ActivityReserve booking = requireUserBooking(username, bookingId);
        MerchantReview review = merchantReviewRepository.findFirstByReserveIdAndUserIdOrderByCreateTimeDesc(booking.getId(), booking.getUserId()).orElse(null);
        return buildUserBookingDetailMap(booking, review);
    }

    public Map<String, Object> payForActivityBooking(String username, Long bookingId, Map<String, Object> data) {
        ActivityReserve booking = requireUserBooking(username, bookingId);
        if (!"registered".equals(toClientReserveStatus(booking.getReserveStatus()))) {
            throw new RuntimeException("Only active bookings can be paid");
        }
        if (Integer.valueOf(1).equals(booking.getPayStatus())) {
            return buildUserBookingDetailMap(booking, merchantReviewRepository.findFirstByReserveIdAndUserIdOrderByCreateTimeDesc(booking.getId(), booking.getUserId()).orElse(null));
        }
        if (intValue(booking.getPayAmount(), 0) <= 0) {
            throw new RuntimeException("This booking does not require payment");
        }

        booking.setPayStatus(1);
        booking.setPaymentTime(new Date());
        booking.setPaymentType(StringUtils.hasText(stringValue(data == null ? null : data.get("paymentType"))) ? stringValue(data.get("paymentType")) : "simulated");
        activityReserveRepository.save(booking);
        return buildUserBookingDetailMap(booking, merchantReviewRepository.findFirstByReserveIdAndUserIdOrderByCreateTimeDesc(booking.getId(), booking.getUserId()).orElse(null));
    }

    public ActivityReserve prepareUserBookingForExternalPayment(String username, Long bookingId, String paymentType) {
        ActivityReserve booking = requireUserBooking(username, bookingId);
        if (!"registered".equals(toClientReserveStatus(booking.getReserveStatus()))) {
            throw new RuntimeException("Only active bookings can be paid");
        }
        if (Integer.valueOf(1).equals(booking.getPayStatus())) {
            return booking;
        }
        if (StringUtils.hasText(paymentType)) {
            booking.setPaymentType(paymentType);
            activityReserveRepository.save(booking);
        }
        return booking;
    }

    public String getActivityBookingQrCode(String username, Long bookingId) {
        ActivityReserve booking = requireUserBooking(username, bookingId);
        if (!canUseBookingQr(booking)) {
            throw new RuntimeException("This booking does not support QR check-in yet");
        }
        return QrCodeUtil.generateDataUrl(buildBookingQrContent(booking));
    }

    public Map<String, Object> submitActivityBookingReview(String username, Long bookingId, Map<String, Object> data) {
        ActivityReserve booking = requireUserBooking(username, bookingId);
        if (!canReviewActivityBooking(booking)) {
            throw new RuntimeException("Only checked-in bookings can be reviewed");
        }

        BigDecimal score = parseReviewScore(data == null ? null : data.get("score"));
        if (score.compareTo(BigDecimal.ONE) < 0 || score.compareTo(BigDecimal.valueOf(5L)) > 0) {
            throw new RuntimeException("Review score must be between 1.0 and 5.0");
        }
        String content = stringValue(data == null ? null : data.get("content"));
        if (content.length() > 500) {
            throw new RuntimeException("Review content cannot exceed 500 characters");
        }

        MerchantReview review = merchantReviewRepository.findFirstByReserveIdAndUserIdOrderByCreateTimeDesc(booking.getId(), booking.getUserId())
                .orElseGet(new java.util.function.Supplier<MerchantReview>() {
                    @Override
                    public MerchantReview get() {
                        return new MerchantReview();
                    }
                });
        review.setMerchantId(booking.getMerchantId());
        review.setUserId(booking.getUserId());
        review.setReserveId(booking.getId());
        review.setOrderId(null);
        review.setReviewType("activity");
        review.setScore(score);
        review.setContent(content);
        MerchantReview saved = merchantReviewRepository.save(review);
        return buildUserBookingDetailMap(booking, saved);
    }

    public Map<String, Object> getMyOrderOverview(String username) {
        User user = getUser(username);
        List<Map<String, Object>> productOrders = getMyOrdersAsUser(username);
        List<ActivityReserve> bookings = activityReserveRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
        List<Map<String, Object>> activityBookings = new ArrayList<Map<String, Object>>();
        long checkedInCount = 0;
        for (ActivityReserve booking : bookings) {
            if ("checked_in".equals(toClientReserveStatus(booking.getReserveStatus()))) {
                checkedInCount++;
            }
            activityBookings.add(userBookingToMap(booking));
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("productOrderCount", productOrders.size());
        summary.put("activityBookingCount", activityBookings.size());
        summary.put("checkedInCount", checkedInCount);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("summary", summary);
        result.put("productOrders", productOrders);
        result.put("activityBookings", activityBookings);
        return result;
    }

    public void cancelOrder(String username, Long orderId) {
        User user = getUser(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order does not exist"));
        if (!Objects.equals(order.getUserId(), user.getId())) {
            throw new RuntimeException("No permission to operate on this order");
        }
        if (!"pending_payment".equals(order.getStatus())) {
            throw new RuntimeException("This order cannot be cancelled in its current status");
        }
        order.setStatus("cancelled");
        orderRepository.save(order);

        if (order.getProductId() != null) {
            productRepository.findById(order.getProductId()).ifPresent(product -> {
                product.setStock(intValue(product.getStock(), 0) + intValue(order.getQuantity(), 1));
                productRepository.save(product);
            });
        }
    }

    public Order findOrderByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new RuntimeException("Order number cannot be empty");
        }
        return orderRepository.findFirstByOrderNoIgnoreCase(orderNo.trim())
                .orElseThrow(() -> new RuntimeException("Order does not exist"));
    }

    public ActivityReserve findBookingByReserveNo(String reserveNo) {
        if (!StringUtils.hasText(reserveNo)) {
            throw new RuntimeException("Booking number cannot be empty");
        }
        return activityReserveRepository.findFirstByReserveNoIgnoreCase(reserveNo.trim())
                .orElseThrow(() -> new RuntimeException("Booking does not exist"));
    }

    public Order confirmOrderPaid(String orderNo, String paymentType, Date paymentTime) {
        Order order = findOrderByOrderNo(orderNo);
        if ("paid".equalsIgnoreCase(defaultString(order.getStatus()))) {
            return order;
        }
        if (!canPayOrder(order)) {
            throw new RuntimeException("This order cannot be marked as paid in its current status");
        }
        order.setStatus("paid");
        order.setPaymentTime(paymentTime == null ? new Date() : paymentTime);
        if (StringUtils.hasText(paymentType)) {
            order.setPaymentType(paymentType);
        }
        orderRepository.save(order);

        if (order.getProductId() != null) {
            productRepository.findById(order.getProductId()).ifPresent(product -> {
                int quantity = intValue(order.getQuantity(), 1);
                product.setSales(intValue(product.getSales(), 0) + quantity);
                product.setSalesCount(intValue(product.getSalesCount(), 0) + quantity);
                productRepository.save(product);
            });
        }
        return order;
    }

    public void confirmPaidByTradeNo(String outTradeNo, String paymentType, Date paymentTime) {
        String tradeNo = defaultString(outTradeNo).trim();
        if (!StringUtils.hasText(tradeNo)) {
            throw new RuntimeException("Trade number cannot be empty");
        }
        if (tradeNo.startsWith("YF")) {
            confirmOrderPaid(tradeNo, paymentType, paymentTime);
            return;
        }
        confirmBookingPaid(tradeNo, paymentType, paymentTime);
    }

    public Order prepareUserOrderForRefund(String username, Long orderId) {
        Order order = requireUserOrder(username, orderId);
        if ("refunded".equalsIgnoreCase(defaultString(order.getStatus()))) {
            return order;
        }
        if (!"paid".equalsIgnoreCase(defaultString(order.getStatus()))) {
            throw new RuntimeException("Only paid orders can be refunded");
        }
        if (intValue(order.getPayAmount(), 0) <= 0) {
            throw new RuntimeException("This order does not require a refund");
        }
        return order;
    }

    public Order confirmOrderRefunded(String username, Long orderId, Date refundTime) {
        Order order = requireUserOrder(username, orderId);
        if ("refunded".equalsIgnoreCase(defaultString(order.getStatus()))) {
            return order;
        }
        if (!"paid".equalsIgnoreCase(defaultString(order.getStatus()))) {
            throw new RuntimeException("Only paid orders can be refunded");
        }
        order.setStatus("refunded");
        orderRepository.save(order);

        if (order.getProductId() != null) {
            productRepository.findById(order.getProductId()).ifPresent(product -> {
                int quantity = intValue(order.getQuantity(), 1);
                product.setStock(intValue(product.getStock(), 0) + quantity);
                product.setSales(Math.max(intValue(product.getSales(), 0) - quantity, 0));
                product.setSalesCount(Math.max(intValue(product.getSalesCount(), 0) - quantity, 0));
                productRepository.save(product);
            });
        }
        return order;
    }

    public ActivityReserve prepareUserBookingForRefund(String username, Long bookingId) {
        ActivityReserve booking = requireUserBooking(username, bookingId);
        if (Integer.valueOf(2).equals(booking.getPayStatus())) {
            return booking;
        }
        if (!Integer.valueOf(1).equals(booking.getPayStatus())) {
            throw new RuntimeException("Only paid bookings can be refunded");
        }
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());
        if ("checked_in".equals(currentStatus)) {
            throw new RuntimeException("Checked-in bookings cannot be refunded");
        }
        if ("rejected".equals(currentStatus)) {
            throw new RuntimeException("Rejected bookings cannot be refunded");
        }
        if ("cancelled".equals(currentStatus)) {
            throw new RuntimeException("This booking has already been cancelled");
        }
        if (intValue(booking.getPayAmount(), 0) <= 0) {
            throw new RuntimeException("This booking does not require a refund");
        }
        return booking;
    }

    public ActivityReserve confirmBookingRefunded(String username, Long bookingId, Date refundTime, String remark) {
        User user = getUser(username);
        ActivityReserve booking = requireUserBooking(username, bookingId);
        if (Integer.valueOf(2).equals(booking.getPayStatus()) && "cancelled".equals(toClientReserveStatus(booking.getReserveStatus()))) {
            return booking;
        }
        if (Integer.valueOf(1).equals(booking.getPayStatus()) == false) {
            throw new RuntimeException("Only paid bookings can be refunded");
        }
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());
        if ("checked_in".equals(currentStatus)) {
            throw new RuntimeException("Checked-in bookings cannot be refunded");
        }
        if ("rejected".equals(currentStatus)) {
            throw new RuntimeException("Rejected bookings cannot be refunded");
        }
        if ("cancelled".equals(currentStatus)) {
            throw new RuntimeException("This booking has already been cancelled");
        }

        String oldStatus = booking.getReserveStatus();
        Date operateTime = refundTime == null ? new Date() : refundTime;
        booking.setReserveStatus("cancelled");
        booking.setCancelTime(operateTime);
        booking.setPayStatus(2);
        activityReserveRepository.save(booking);
        createStatusLog(booking.getId(), oldStatus, booking.getReserveStatus(), user.getId(), "user", StringUtils.hasText(remark) ? remark : "User refunded booking");
        syncActivityCurrentParticipants(booking.getActivityId());
        return booking;
    }

    public ActivityReserve confirmBookingPaid(String reserveNo, String paymentType, Date paymentTime) {
        ActivityReserve booking = findBookingByReserveNo(reserveNo);
        if (Integer.valueOf(1).equals(booking.getPayStatus())) {
            return booking;
        }
        if (!"registered".equals(toClientReserveStatus(booking.getReserveStatus()))) {
            throw new RuntimeException("This booking cannot be marked as paid in its current status");
        }
        booking.setPayStatus(1);
        booking.setPaymentTime(paymentTime == null ? new Date() : paymentTime);
        if (StringUtils.hasText(paymentType)) {
            booking.setPaymentType(paymentType);
        }
        activityReserveRepository.save(booking);
        return booking;
    }

    public void cancelActivityBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityReserve booking = activityReserveRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking record does not exist"));
        if (!Objects.equals(booking.getUserId(), user.getId())) {
            throw new RuntimeException("No permission to operate on this booking");
        }
        String currentStatus = toClientReserveStatus(booking.getReserveStatus());
        if ("checked_in".equals(currentStatus)) {
            throw new RuntimeException("Checked-in bookings cannot be cancelled");
        }
        if ("rejected".equals(currentStatus)) {
            throw new RuntimeException("Rejected bookings cannot be cancelled");
        }
        if ("cancelled".equals(currentStatus)) {
            throw new RuntimeException("This booking has already been cancelled");
        }

        String oldStatus = booking.getReserveStatus();
        booking.setReserveStatus("cancelled");
        booking.setCancelTime(new Date());
        if (Integer.valueOf(1).equals(booking.getPayStatus()) && intValue(booking.getPayAmount(), 0) > 0) {
            booking.setPayStatus(2);
        }
        activityReserveRepository.save(booking);
        createStatusLog(booking.getId(), oldStatus, booking.getReserveStatus(), user.getId(), "user", "User cancelled booking");
        syncActivityCurrentParticipants(booking.getActivityId());
    }

    public void deleteActivityBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityReserve booking = activityReserveRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking record does not exist"));
        if (!Objects.equals(booking.getUserId(), user.getId())) {
            throw new RuntimeException("No permission to operate on this booking");
        }
        if (!"cancelled".equals(toClientReserveStatus(booking.getReserveStatus()))) {
            throw new RuntimeException("Only cancelled bookings can be deleted");
        }
        activityReserveRepository.delete(booking);
        syncActivityCurrentParticipants(booking.getActivityId());
    }

    private Order requireUserOrder(String username, Long orderId) {
        User user = getUser(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order does not exist"));
        if (!Objects.equals(order.getUserId(), user.getId())) {
            throw new RuntimeException("No permission to access this order");
        }
        return order;
    }

    private ActivityReserve requireUserBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityReserve booking = activityReserveRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking does not exist"));
        if (!Objects.equals(booking.getUserId(), user.getId())) {
            throw new RuntimeException("No permission to access this booking");
        }
        return booking;
    }

    private ActivityReserve requireMerchantBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityReserve booking = activityReserveRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking does not exist"));
        if (!Objects.equals(booking.getMerchantId(), user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("No permission to access this booking");
        }
        return booking;
    }

    private Map<String, Object> reserveToMap(ActivityReserve booking) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", booking.getId());
        map.put("reserveNo", booking.getReserveNo());
        map.put("bookingCode", booking.getReserveNo());
        map.put("activityId", booking.getActivityId());
        map.put("userId", booking.getUserId());
        map.put("merchantId", booking.getMerchantId());
        map.put("participantName", booking.getContactName());
        map.put("participantPhone", booking.getContactPhone());
        map.put("participantCount", booking.getParticipantNum());
        map.put("status", toClientReserveStatus(booking.getReserveStatus()));
        map.put("reserveStatus", booking.getReserveStatus());
        map.put("paymentStatus", toClientPaymentStatus(booking.getPayStatus()));
        map.put("payStatus", booking.getPayStatus());
        map.put("paymentType", booking.getPaymentType());
        map.put("paymentTime", booking.getPaymentTime());
        map.put("totalAmount", booking.getTotalAmount());
        map.put("payAmount", booking.getPayAmount());
        map.put("remark", booking.getRemark());
        map.put("activityTitle", booking.getActivityTitle());
        map.put("activityTime", booking.getActivityTime());
        map.put("createTime", booking.getCreateTime());
        map.put("updateTime", booking.getUpdateTime());
        map.put("cancelTime", booking.getCancelTime());
        map.put("verificationTime", booking.getVerifyTime());
        map.put("canCheckin", "registered".equals(toClientReserveStatus(booking.getReserveStatus())) && Integer.valueOf(1).equals(booking.getPayStatus()));
        map.put("canReject", "registered".equals(toClientReserveStatus(booking.getReserveStatus())));
        map.put("canRefund", "registered".equals(toClientReserveStatus(booking.getReserveStatus()))
                && Integer.valueOf(1).equals(booking.getPayStatus())
                && intValue(booking.getPayAmount(), 0) > 0);
        map.put("canPay", canPayActivityBooking(booking));
        map.put("canOpenQr", canUseBookingQr(booking));
        return map;
    }

    private Map<String, Object> userBookingToMap(ActivityReserve booking) {
        Map<String, Object> map = reserveToMap(booking);
        Activity activity = activityRepository.findById(booking.getActivityId()).orElse(null);
        if (activity != null) {
            map.put("activityTitle", activity.getTitle());
            map.put("activitySubtitle", activity.getSubtitle());
            map.put("activityCoverImage", activity.getCoverImage());
            map.put("activityImages", parseImageList(activity.getImages()));
            map.put("detailImages", parseImageList(activity.getImages()));
            map.put("activityType", activity.getActivityType());
            map.put("heritageType", activity.getHeritageType());
            map.put("activityContent", activity.getContent());
            map.put("startTime", activity.getStartTime());
            map.put("endTime", activity.getEndTime());
            map.put("locationProvince", activity.getLocationProvince());
            map.put("locationCity", activity.getLocationCity());
            map.put("locationDistrict", activity.getLocationDistrict());
            map.put("locationDetail", activity.getLocationDetail());
            map.put("activityStatus", activity.getStatus());
        }
        userRepository.findById(booking.getMerchantId()).ifPresent(merchant -> {
            MerchantProfile profile = merchantProfileRepository.findByUserId(merchant.getId()).orElse(null);
            map.put("merchantName", preferredShopName(merchant, profile));
            map.put("merchantAvatar", merchant.getAvatar());
            map.put("merchantPhone", merchant.getPhone());
            map.put("merchantIntro", preferredShopIntro(merchant, profile));
            map.put("shopName", preferredShopName(merchant, profile));
        });
        MerchantReview review = merchantReviewRepository.findFirstByReserveIdAndUserIdOrderByCreateTimeDesc(booking.getId(), booking.getUserId()).orElse(null);
        if (review != null) {
            map.put("reviewId", review.getId());
            map.put("reviewScore", review.getScore());
            map.put("reviewContent", review.getContent());
            map.put("reviewType", review.getReviewType());
            map.put("reviewTime", review.getCreateTime());
        }
        map.put("canReview", review == null && canReviewActivityBooking(booking));
        map.put("qrContent", buildBookingQrContent(booking));
        return map;
    }

    private Map<String, Object> buildUserBookingDetailMap(ActivityReserve booking, MerchantReview review) {
        Map<String, Object> map = userBookingToMap(booking);
        map.put("timeline", buildTimeline(booking.getId()));
        map.put("participants", Collections.emptyList());
        map.put("activitySchedules", Collections.emptyList());
        if (review != null) {
            map.put("reviewId", review.getId());
            map.put("reviewScore", review.getScore());
            map.put("reviewContent", review.getContent());
            map.put("reviewType", review.getReviewType());
            map.put("reviewTime", review.getCreateTime());
            map.put("canReview", false);
        }
        return map;
    }
    private List<Map<String, Object>> buildMerchantBookingItems(List<ActivityReserve> bookings) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ActivityReserve booking : bookings) {
            result.add(buildMerchantBookingItem(booking));
        }
        return result;
    }

    private Map<String, Object> buildMerchantBookingItem(ActivityReserve booking) {
        Map<String, Object> map = reserveToMap(booking);
        Activity activity = activityRepository.findById(booking.getActivityId()).orElse(null);
        if (activity != null) {
            map.put("activityTitle", activity.getTitle());
            map.put("activitySubtitle", activity.getSubtitle());
            map.put("coverImage", activity.getCoverImage());
            map.put("activityImages", parseImageList(activity.getImages()));
            map.put("detailImages", parseImageList(activity.getImages()));
            map.put("activityContent", activity.getContent());
            map.put("startTime", activity.getStartTime());
            map.put("endTime", activity.getEndTime());
            map.put("locationProvince", activity.getLocationProvince());
            map.put("locationCity", activity.getLocationCity());
            map.put("locationDistrict", activity.getLocationDistrict());
            map.put("locationDetail", activity.getLocationDetail());
            map.put("activityTime", buildActivityTimeLabel(activity));
        }
        userRepository.findById(booking.getUserId()).ifPresent(customer -> {
            map.put("customerName", displayName(customer));
            map.put("customerUsername", customer.getUsername());
            map.put("customerAvatar", customer.getAvatar());
            map.put("customerPhone", customer.getPhone());
            map.put("customerEmail", customer.getEmail());
            map.put("customerLocation", customer.getLocation());
        });
        MerchantReview review = merchantReviewRepository.findFirstByReserveIdOrderByCreateTimeDesc(booking.getId()).orElse(null);
        if (review != null) {
            map.put("reviewScore", review.getScore());
            map.put("reviewContent", review.getContent());
            map.put("reviewType", review.getReviewType());
            map.put("reviewTime", review.getCreateTime());
        }
        map.put("timeline", buildTimeline(booking.getId()));
        return map;
    }

    private Map<String, Object> buildMerchantBookingDetailMap(ActivityReserve booking) {
        Map<String, Object> map = buildMerchantBookingItem(booking);
        map.put("participants", Collections.emptyList());
        map.put("activitySchedules", Collections.emptyList());
        return map;
    }

    private List<Map<String, Object>> buildTimeline(Long reserveId) {
        List<ReserveStatusLog> logs = reserveStatusLogRepository.findByReserveIdOrderByCreateTimeAsc(reserveId);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ReserveStatusLog log : logs) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", log.getId());
            item.put("oldStatus", toClientReserveStatus(log.getOldStatus()));
            item.put("newStatus", toClientReserveStatus(log.getNewStatus()));
            item.put("operatorId", log.getOperatorId());
            item.put("operatorType", log.getOperatorType());
            item.put("remark", log.getRemark());
            item.put("createTime", log.getCreateTime());
            userRepository.findById(log.getOperatorId()).ifPresent(operator -> item.put("operatorName", displayName(operator)));
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildMerchantBookingSummary(List<ActivityReserve> bookings) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        long bookingCount = 0;
        long pendingCheckinCount = 0;
        long checkedInCount = 0;
        long rejectedCount = 0;
        long cancelledCount = 0;
        for (ActivityReserve booking : bookings) {
            bookingCount++;
            String status = toClientReserveStatus(booking.getReserveStatus());
            if ("registered".equals(status)) {
                pendingCheckinCount++;
            } else if ("checked_in".equals(status)) {
                checkedInCount++;
            } else if ("rejected".equals(status)) {
                rejectedCount++;
            } else if ("cancelled".equals(status)) {
                cancelledCount++;
            }
        }
        summary.put("bookingCount", bookingCount);
        summary.put("pendingCheckinCount", pendingCheckinCount);
        summary.put("checkedInCount", checkedInCount);
        summary.put("rejectedCount", rejectedCount);
        summary.put("cancelledCount", cancelledCount);
        return summary;
    }

    private boolean matchesBookingKeyword(ActivityReserve booking, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return containsIgnoreCase(booking.getReserveNo(), normalized)
                || containsIgnoreCase(booking.getContactName(), normalized)
                || containsIgnoreCase(booking.getContactPhone(), normalized)
                || containsIgnoreCase(booking.getActivityTitle(), normalized)
                || String.valueOf(booking.getId()).equals(normalized);
    }

    private void createStatusLog(Long reserveId, String oldStatus, String newStatus, Long operatorId, String operatorType, String remark) {
        ReserveStatusLog log = new ReserveStatusLog();
        log.setReserveId(reserveId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperatorId(operatorId);
        log.setOperatorType(operatorType);
        log.setRemark(remark);
        reserveStatusLogRepository.save(log);
    }

    private void syncActivityCurrentParticipants(Long activityId) {
        if (activityId == null) {
            return;
        }
        activityRepository.findById(activityId).ifPresent(activity -> {
            activity.setCurrentParticipants(resolveCurrentParticipants(activityId));
            activityRepository.save(activity);
        });
    }

    private int resolveCurrentParticipants(Long activityId) {
        int total = 0;
        for (ActivityReserve booking : activityReserveRepository.findByActivityIdOrderByCreateTimeDesc(activityId)) {
            String status = toClientReserveStatus(booking.getReserveStatus());
            if ("registered".equals(status) || "checked_in".equals(status)) {
                total += intValue(booking.getParticipantNum(), 1);
            }
        }
        return total;
    }

    private String buildReserveNo() {
        return "AR" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private String buildOrderNo() {
        return "YF" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private String buildActivityTimeLabel(Activity activity) {
        String start = formatDate(activity == null ? null : activity.getStartTime());
        String end = formatDate(activity == null ? null : activity.getEndTime());
        if (!StringUtils.hasText(start) && !StringUtils.hasText(end)) {
            return "Time TBD";
        }
        if (!StringUtils.hasText(end)) {
            return start;
        }
        return start + " - " + end;
    }

    private String buildBookingQrContent(ActivityReserve booking) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "activity_booking_checkin");
        payload.put("bookingId", booking.getId());
        payload.put("reserveNo", booking.getReserveNo());
        payload.put("merchantId", booking.getMerchantId());
        payload.put("userId", booking.getUserId());
        payload.put("createTime", booking.getCreateTime() == null ? null : booking.getCreateTime().getTime());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {
            return booking.getReserveNo();
        }
    }

    private boolean canPayOrder(Order order) {
        return order != null
                && "pending_payment".equalsIgnoreCase(defaultString(order.getStatus()))
                && intValue(order.getPayAmount(), 0) > 0;
    }

    private boolean canPayActivityBooking(ActivityReserve booking) {
        return booking != null
                && "registered".equals(toClientReserveStatus(booking.getReserveStatus()))
                && !Integer.valueOf(1).equals(booking.getPayStatus())
                && intValue(booking.getPayAmount(), 0) > 0;
    }

    private boolean canUseBookingQr(ActivityReserve booking) {
        String status = toClientReserveStatus(booking.getReserveStatus());
        return !"cancelled".equals(status)
                && !"rejected".equals(status)
                && Integer.valueOf(1).equals(booking.getPayStatus());
    }

    private boolean canReviewActivityBooking(ActivityReserve booking) {
        return booking != null && "checked_in".equals(toClientReserveStatus(booking.getReserveStatus()));
    }

    private String toClientReserveStatus(String reserveStatus) {
        String status = defaultString(reserveStatus).trim();
        if (!StringUtils.hasText(status)) {
            return "registered";
        }
        if ("registered".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
            return "registered";
        }
        if ("checked_in".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
            return "checked_in";
        }
        if ("rejected".equalsIgnoreCase(status)) {
            return "rejected";
        }
        if ("cancelled".equalsIgnoreCase(status)) {
            return "cancelled";
        }
        return status;
    }

    private String toClientPaymentStatus(Integer payStatus) {
        if (payStatus == null) {
            return "unpaid";
        }
        if (payStatus == 1) {
            return "paid";
        }
        if (payStatus == 2) {
            return "refunded";
        }
        return "unpaid";
    }

    private BigDecimal parseReviewScore(Object value) {
        if (value == null) {
            throw new RuntimeException("Please provide a review score");
        }
        BigDecimal score;
        if (value instanceof Number) {
            score = BigDecimal.valueOf(((Number) value).doubleValue());
        } else {
            try {
                score = new BigDecimal(String.valueOf(value).trim());
            } catch (Exception e) {
                throw new RuntimeException("Invalid review score");
            }
        }
        return score.setScale(1, RoundingMode.HALF_UP);
    }

    private List<String> parseImageList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() { });
        } catch (Exception ignored) {
            return Collections.singletonList(raw);
        }
    }

    private String normalizeLookupCode(String code) {
        String value = defaultString(code).trim();
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith("{") && value.endsWith("}")) {
            try {
                Map<String, Object> payload = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() { });
                List<String> keys = Arrays.asList("reserveNo", "bookingId", "id", "code");
                for (String key : keys) {
                    Object candidate = payload.get(key);
                    if (candidate != null && StringUtils.hasText(String.valueOf(candidate))) {
                        return String.valueOf(candidate).trim();
                    }
                }
            } catch (Exception ignored) {
                return value;
            }
        }
        return value;
    }

    private Date parseDate(Object value, Date fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return fallback;
        }
        List<String> patterns = Arrays.asList("yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm");
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(text);
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private String formatDate(Date value) {
        if (value == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(value);
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean containsIgnoreCase(String source, String keywordLowerCase) {
        return source != null && source.toLowerCase().contains(keywordLowerCase);
    }

    private String preferredShopName(User merchant, MerchantProfile profile) {
        if (profile != null && StringUtils.hasText(profile.getShopName())) {
            return profile.getShopName();
        }
        if (merchant != null && StringUtils.hasText(merchant.getShopName())) {
            return merchant.getShopName();
        }
        if (merchant != null && StringUtils.hasText(merchant.getNickname())) {
            return merchant.getNickname();
        }
        return merchant == null ? "" : defaultString(merchant.getUsername());
    }

    private String preferredShopIntro(User merchant, MerchantProfile profile) {
        if (profile != null && StringUtils.hasText(profile.getShopIntro())) {
            return profile.getShopIntro();
        }
        return merchant == null ? "" : defaultString(merchant.getShopIntro());
    }

    private Map<Long, Activity> buildActivityMap(List<Activity> activities) {
        Map<Long, Activity> activityMap = new HashMap<Long, Activity>();
        for (Activity activity : activities) {
            if (activity.getId() != null) {
                activityMap.put(activity.getId(), activity);
            }
        }
        return activityMap;
    }
    private List<Map<String, Object>> buildMerchantReviews(List<MerchantReview> reviews,
                                                            Map<Long, ActivityReserve> bookingMap,
                                                            Map<Long, Activity> activityMap) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        int limit = Math.min(reviews.size(), 5);
        for (int index = 0; index < limit; index++) {
            MerchantReview review = reviews.get(index);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", review.getId());
            item.put("score", review.getScore());
            item.put("content", review.getContent());
            item.put("reviewType", review.getReviewType());
            item.put("createTime", review.getCreateTime());
            ActivityReserve booking = review.getReserveId() == null ? null : bookingMap.get(review.getReserveId());
            if (booking != null) {
                item.put("reserveId", booking.getId());
                item.put("activityId", booking.getActivityId());
                item.put("targetName", StringUtils.hasText(booking.getActivityTitle()) ? booking.getActivityTitle() : "Activity Review");
            }
            if (!item.containsKey("targetName") && booking != null && booking.getActivityId() != null) {
                Activity activity = activityMap.get(booking.getActivityId());
                if (activity != null && StringUtils.hasText(activity.getTitle())) {
                    item.put("targetName", activity.getTitle());
                }
            }
            if (!item.containsKey("targetName")) {
                item.put("targetName", "activity".equalsIgnoreCase(defaultString(review.getReviewType())) ? "Activity Review" : "Product Review");
            }
            userRepository.findById(review.getUserId()).ifPresent(user -> {
                item.put("userId", user.getId());
                item.put("userName", displayName(user));
                item.put("userAvatar", resolveUserAvatar(user));
            });
            result.add(item);
        }
        return result;
    }
    private List<Map<String, Object>> buildBookingStatusStats(List<ActivityReserve> bookings) {
        int registeredCount = 0;
        int checkedInCount = 0;
        int rejectedCount = 0;
        int cancelledCount = 0;
        for (ActivityReserve booking : bookings) {
            String status = toClientReserveStatus(booking.getReserveStatus());
            if ("registered".equals(status)) {
                registeredCount++;
            } else if ("checked_in".equals(status)) {
                checkedInCount++;
            } else if ("rejected".equals(status)) {
                rejectedCount++;
            } else if ("cancelled".equals(status)) {
                cancelledCount++;
            }
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        result.add(statusSummary("registered", "Active", "#1661ab", registeredCount));
        result.add(statusSummary("checked_in", "Checked In", "#1f8a70", checkedInCount));
        result.add(statusSummary("rejected", "Rejected", "#c04851", rejectedCount));
        result.add(statusSummary("cancelled", "Cancelled", "#6b7280", cancelledCount));
        return result;
    }
    private Map<String, Object> statusSummary(String key, String label, String color, int count) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("key", key);
        item.put("label", label);
        item.put("color", color);
        item.put("count", count);
        return item;
    }
    private List<Map<String, Object>> buildSalesTrend(List<ActivityReserve> bookings) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate endDate = LocalDate.now(zoneId);
        LocalDate startDate = endDate.minusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        Map<LocalDate, Map<String, Object>> bucketMap = new LinkedHashMap<LocalDate, Map<String, Object>>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            Map<String, Object> bucket = new LinkedHashMap<String, Object>();
            bucket.put("date", currentDate.toString());
            bucket.put("label", currentDate.format(formatter));
            bucket.put("bookingCount", 0);
            bucket.put("participantCount", 0);
            bucket.put("bookingRevenue", 0);
            bucketMap.put(currentDate, bucket);
        }
        for (ActivityReserve booking : bookings) {
            LocalDate bookingDate = toLocalDate(booking.getCreateTime());
            if (bookingDate != null && !bookingDate.isBefore(startDate) && !bookingDate.isAfter(endDate)) {
                Map<String, Object> bookingBucket = bucketMap.get(bookingDate);
                if (bookingBucket != null) {
                    bookingBucket.put("bookingCount", safeInt(bookingBucket.get("bookingCount")) + 1);
                    bookingBucket.put("participantCount", safeInt(bookingBucket.get("participantCount")) + safeInt(booking.getParticipantNum()));
                }
            }
            if (Integer.valueOf(1).equals(booking.getPayStatus())) {
                Date revenueSourceDate = booking.getPaymentTime() == null ? booking.getCreateTime() : booking.getPaymentTime();
                LocalDate revenueDate = toLocalDate(revenueSourceDate);
                if (revenueDate != null && !revenueDate.isBefore(startDate) && !revenueDate.isAfter(endDate)) {
                    Map<String, Object> revenueBucket = bucketMap.get(revenueDate);
                    if (revenueBucket != null) {
                        revenueBucket.put("bookingRevenue", safeInt(revenueBucket.get("bookingRevenue")) + safeInt(booking.getPayAmount()));
                    }
                }
            }
        }
        return new ArrayList<Map<String, Object>>(bucketMap.values());
    }
    private List<Map<String, Object>> buildTopActivities(List<ActivityReserve> bookings, Map<Long, Activity> activityMap) {
        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<Long, Map<String, Object>>();
        for (ActivityReserve booking : bookings) {
            Long activityId = booking.getActivityId();
            if (activityId == null) {
                continue;
            }
            Map<String, Object> item = grouped.get(activityId);
            if (item == null) {
                item = new LinkedHashMap<String, Object>();
                item.put("activityId", activityId);
                Activity activity = activityMap.get(activityId);
                String title = activity != null && StringUtils.hasText(activity.getTitle())
                        ? activity.getTitle()
                        : defaultString(booking.getActivityTitle());
                item.put("title", title);
                item.put("bookingCount", 0);
                item.put("participantCount", 0);
                item.put("revenue", 0);
                grouped.put(activityId, item);
            }
            item.put("bookingCount", safeInt(item.get("bookingCount")) + 1);
            item.put("participantCount", safeInt(item.get("participantCount")) + safeInt(booking.getParticipantNum()));
            if (Integer.valueOf(1).equals(booking.getPayStatus())) {
                item.put("revenue", safeInt(item.get("revenue")) + safeInt(booking.getPayAmount()));
            }
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(grouped.values());
        Collections.sort(result, new java.util.Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                int revenueCompare = Integer.compare(safeInt(right.get("revenue")), safeInt(left.get("revenue")));
                if (revenueCompare != 0) {
                    return revenueCompare;
                }
                int bookingCompare = Integer.compare(safeInt(right.get("bookingCount")), safeInt(left.get("bookingCount")));
                if (bookingCompare != 0) {
                    return bookingCompare;
                }
                return Integer.compare(safeInt(right.get("participantCount")), safeInt(left.get("participantCount")));
            }
        });
        if (result.size() > 5) {
            return new ArrayList<Map<String, Object>>(result.subList(0, 5));
        }
        return result;
    }
    private BigDecimal resolveAverageScore(List<MerchantReview> reviews) {
        if (reviews.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        int counted = 0;
        for (MerchantReview review : reviews) {
            if (review.getScore() == null) {
                continue;
            }
            total = total.add(review.getScore());
            counted++;
        }
        if (counted <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return total.divide(BigDecimal.valueOf(counted), 1, RoundingMode.HALF_UP);
    }
    private int resolveFollowerCount(User user) {
        if (user == null) {
            return 0;
        }
        return safeInt(user.getFollowerCount());
    }
    private String resolveUserAvatar(User user) {
        if (user == null || !StringUtils.hasText(user.getAvatar())) {
            return "/default-avatar.svg";
        }
        return user.getAvatar();
    }
    private LocalDate toLocalDate(Date value) {
        if (value == null) {
            return null;
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    private int safeInt(Object value) {
        Integer resolved = intValue(value, 0);
        return resolved == null ? 0 : resolved;
    }
    private String displayName(User user) {
        if (user == null) {
            return "";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return defaultString(user.getUsername());
    }
}
