package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.ActivityBooking;
import com.yayfolk.backend.entity.MerchantApplication;
import com.yayfolk.backend.entity.MerchantProfile;
import com.yayfolk.backend.entity.Order;
import com.yayfolk.backend.entity.Product;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.ActivityBookingRepository;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.MerchantApplicationRepository;
import com.yayfolk.backend.repository.MerchantProfileRepository;
import com.yayfolk.backend.repository.OrderRepository;
import com.yayfolk.backend.repository.ProductRepository;
import com.yayfolk.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class MerchantService {

    private final UserRepository userRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantApplicationRepository applicationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityBookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public MerchantService(UserRepository userRepository,
                           MerchantProfileRepository merchantProfileRepository,
                           MerchantApplicationRepository applicationRepository,
                           ActivityRepository activityRepository,
                           ActivityBookingRepository bookingRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.applicationRepository = applicationRepository;
        this.activityRepository = activityRepository;
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private MerchantProfile getOrCreateMerchantProfile(Long userId) {
        return merchantProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MerchantProfile profile = new MerchantProfile();
                    profile.setUserId(userId);
                    profile.setBusinessStatus("pending");
                    return merchantProfileRepository.save(profile);
                });
    }

    public Map<String, Object> applyMerchant(String username, Map<String, Object> data) {
        User user = getUser(username);
        String shopStatus = user.getShopStatus();

        if ("approved".equals(shopStatus) || "pending".equals(shopStatus) || "merchant".equals(user.getRole())) {
            throw new RuntimeException("You have already applied or are already a merchant");
        }

        Optional<MerchantApplication> existing = applicationRepository.findByUserId(user.getId());
        MerchantApplication app;
        if (existing.isPresent()) {
            app = existing.get();
            if (!"rejected".equals(app.getApplicationStatus())) {
                throw new RuntimeException("Current application is still being processed");
            }
        } else {
            app = new MerchantApplication();
            app.setUserId(user.getId());
        }

        app.setRealName((String) data.get("realName"));
        app.setIdCard((String) data.get("idCard"));
        app.setPhone((String) data.get("phone"));
        app.setHeritageType((String) data.get("heritageType"));
        app.setHeritageDescription((String) data.get("heritageDescription"));
        app.setShopName((String) data.get("shopName"));
        app.setShopAddress((String) data.get("shopAddress"));
        app.setProvince((String) data.get("province"));
        app.setCity((String) data.get("city"));
        app.setIntro((String) data.get("intro"));
        app.setApplicationStatus("pending");
        app.setAuditAdminId(null);
        app.setAuditTime(null);
        app.setAuditRemark(null);
        applicationRepository.save(app);

        user.setShopStatus("pending");
        user.setShopName((String) data.get("shopName"));
        userRepository.save(user);

        MerchantProfile profile = getOrCreateMerchantProfile(user.getId());
        profile.setShopName((String) data.get("shopName"));
        profile.setContactName((String) data.get("realName"));
        profile.setContactPhone((String) data.get("phone"));
        profile.setHeritageType((String) data.get("heritageType"));
        profile.setHeritageDescription((String) data.get("heritageDescription"));
        profile.setAddress((String) data.get("shopAddress"));
        profile.setProvince((String) data.get("province"));
        profile.setCity((String) data.get("city"));
        profile.setShopIntro((String) data.get("intro"));
        profile.setBusinessStatus("pending");
        profile.setLatestApplicationId(app.getId());
        merchantProfileRepository.save(profile);

        app.setMerchantProfileId(profile.getId());
        applicationRepository.save(app);

        Map<String, Object> result = new HashMap<>();
        result.put("id", app.getId());
        result.put("status", "pending");
        result.put("merchantProfileId", profile.getId());
        return result;
    }

    public Map<String, Object> getMyApplication(String username) {
        User user = getUser(username);
        Map<String, Object> result = new HashMap<>();
        result.put("shopStatus", user.getShopStatus());
        result.put("isMerchant", user.getIsMerchant());

        merchantProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            result.put("merchantProfileId", profile.getId());
            result.put("businessStatus", profile.getBusinessStatus());
            result.put("shopName", profile.getShopName());
        });

        applicationRepository.findByUserId(user.getId()).ifPresent(app -> {
            result.put("applicationId", app.getId());
            result.put("applicationStatus", app.getApplicationStatus());
            result.put("shopName", app.getShopName());
            result.put("heritageType", app.getHeritageType());
            result.put("auditRemark", app.getAuditRemark());
            result.put("createTime", app.getCreateTime());
        });
        return result;
    }

    public List<Map<String, Object>> getMyActivities(String username) {
        User user = getUser(username);
        List<Activity> activities = activityRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Activity activity : activities) {
            result.add(activityToMap(activity));
        }
        return result;
    }

    public Map<String, Object> createActivity(String username, Map<String, Object> data) {
        User user = getUser(username);
        if (!"merchant".equals(user.getRole())) {
            throw new RuntimeException("Only merchants can publish activities");
        }
        MerchantProfile profile = merchantProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Merchant profile not found"));
        Activity activity = new Activity();
        activity.setMerchantId(user.getId());
        activity.setMerchantProfileId(profile.getId());
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
        if (!activity.getMerchantId().equals(user.getId())) {
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
        if (!activity.getMerchantId().equals(user.getId())) {
            throw new RuntimeException("No permission to operate on this activity");
        }
        activityRepository.delete(activity);
    }

    private void fillActivity(Activity activity, Map<String, Object> data) {
        if (data.containsKey("categoryId")) {
            Object categoryId = data.get("categoryId");
            activity.setCategoryId(categoryId instanceof Number ? ((Number) categoryId).intValue() : null);
        }
        if (data.containsKey("title")) activity.setTitle((String) data.get("title"));
        if (data.containsKey("subtitle")) activity.setSubtitle((String) data.get("subtitle"));
        if (data.containsKey("content")) activity.setContent((String) data.get("content"));
        if (data.containsKey("coverImage")) activity.setCoverImage((String) data.get("coverImage"));
        if (data.containsKey("images")) activity.setImages((String) data.get("images"));
        if (data.containsKey("heritageType")) activity.setHeritageType((String) data.get("heritageType"));
        if (data.containsKey("activityType")) activity.setActivityType((String) data.get("activityType"));
        if (data.containsKey("locationCity")) activity.setLocationCity((String) data.get("locationCity"));
        if (data.containsKey("locationDetail")) activity.setLocationDetail((String) data.get("locationDetail"));
        if (data.containsKey("locationProvince")) activity.setLocationProvince((String) data.get("locationProvince"));
        if (data.containsKey("price")) {
            Object price = data.get("price");
            activity.setPrice(price instanceof Number ? ((Number) price).intValue() : 0);
        }
        if (data.containsKey("maxParticipants")) {
            Object maxParticipants = data.get("maxParticipants");
            activity.setMaxParticipants(maxParticipants instanceof Number ? ((Number) maxParticipants).intValue() : null);
        }
        if (data.containsKey("startTime") && data.get("startTime") != null) {
            try {
                activity.setStartTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse((String) data.get("startTime")));
            } catch (Exception ignored) {
            }
        }
        if (data.containsKey("endTime") && data.get("endTime") != null) {
            try {
                activity.setEndTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse((String) data.get("endTime")));
            } catch (Exception ignored) {
            }
        }
    }

    private Map<String, Object> activityToMap(Activity activity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", activity.getId());
        map.put("merchantId", activity.getMerchantId());
        map.put("merchantProfileId", activity.getMerchantProfileId());
        map.put("categoryId", activity.getCategoryId());
        map.put("title", activity.getTitle());
        map.put("subtitle", activity.getSubtitle());
        map.put("coverImage", activity.getCoverImage());
        map.put("images", activity.getImages());
        map.put("heritageType", activity.getHeritageType());
        map.put("activityType", activity.getActivityType());
        map.put("startTime", activity.getStartTime());
        map.put("endTime", activity.getEndTime());
        map.put("price", activity.getPrice());
        map.put("maxParticipants", activity.getMaxParticipants());
        map.put("currentParticipants", activity.getCurrentParticipants());
        map.put("locationProvince", activity.getLocationProvince());
        map.put("locationCity", activity.getLocationCity());
        map.put("locationDetail", activity.getLocationDetail());
        map.put("status", activity.getStatus());
        map.put("auditStatus", activity.getAuditStatus());
        map.put("auditRemark", activity.getAuditRemark());
        map.put("content", activity.getContent());
        map.put("createTime", activity.getCreateTime());
        return map;
    }

    public List<Map<String, Object>> getActivityBookings(String username, Long activityId) {
        User user = getUser(username);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        if (!activity.getMerchantId().equals(user.getId())) {
            throw new RuntimeException("No permission to view bookings");
        }
        List<ActivityBooking> bookings = bookingRepository.findByActivityIdOrderByCreateTimeDesc(activityId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityBooking booking : bookings) {
            result.add(bookingToMap(booking));
        }
        return result;
    }

    public Map<String, Object> checkinBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking does not exist"));
        Activity activity = activityRepository.findById(booking.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        if (!activity.getMerchantId().equals(user.getId())) {
            throw new RuntimeException("无权限核销该报名");
        }
        if ("checked_in".equals(booking.getStatus())) {
            throw new RuntimeException("该报名已核销");
        }
        if (!"registered".equals(booking.getStatus())) {
            throw new RuntimeException("仅已报名状态可核销");
        }
        booking.setStatus("checked_in");
        booking.setVerificationTime(new Date());
        booking.setVerifiedBy(user.getId());
        bookingRepository.save(booking);
        return bookingToMap(booking);
    }

    public Map<String, Object> rejectBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking does not exist"));
        Activity activity = activityRepository.findById(booking.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        if (!activity.getMerchantId().equals(user.getId())) {
            throw new RuntimeException("无权限拒绝该报名");
        }
        if (!"registered".equals(booking.getStatus())) {
            throw new RuntimeException("仅已报名状态可拒绝");
        }

        booking.setStatus("rejected");
        booking.setVerificationTime(null);
        booking.setVerifiedBy(user.getId());
        bookingRepository.save(booking);

        int current = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
        int participantCount = booking.getParticipantCount() == null ? 1 : booking.getParticipantCount();
        activity.setCurrentParticipants(Math.max(0, current - participantCount));
        activityRepository.save(activity);
        return bookingToMap(booking);
    }

    private Map<String, Object> bookingToMap(ActivityBooking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("activityId", booking.getActivityId());
        map.put("userId", booking.getUserId());
        map.put("participantName", booking.getParticipantName());
        map.put("participantPhone", booking.getParticipantPhone());
        map.put("participantCount", booking.getParticipantCount());
        map.put("status", booking.getStatus());
        map.put("paymentStatus", booking.getPaymentStatus());
        map.put("signupTime", booking.getSignupTime());
        map.put("createTime", booking.getCreateTime());
        map.put("verificationTime", booking.getVerificationTime());
        map.put("remark", booking.getRemark());
        return map;
    }

    public List<Map<String, Object>> getMyProducts(String username) {
        User user = getUser(username);
        List<Product> products = productRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product product : products) {
            result.add(productToMap(product));
        }
        return result;
    }

    public Map<String, Object> createProduct(String username, Map<String, Object> data) {
        User user = getUser(username);
        if (!"merchant".equals(user.getRole())) {
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
        if (!product.getMerchantId().equals(user.getId())) {
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
        if (!product.getMerchantId().equals(user.getId())) {
            throw new RuntimeException("No permission to operate on this product");
        }
        productRepository.delete(product);
    }

    private void fillProduct(Product product, Map<String, Object> data) {
        if (data.containsKey("name")) product.setName((String) data.get("name"));
        if (data.containsKey("subtitle")) product.setSubtitle((String) data.get("subtitle"));
        if (data.containsKey("description")) product.setDescription((String) data.get("description"));
        if (data.containsKey("detail")) product.setDetail((String) data.get("detail"));
        if (data.containsKey("mainImage")) product.setMainImage((String) data.get("mainImage"));
        if (data.containsKey("heritageType")) product.setHeritageType((String) data.get("heritageType"));
        if (data.containsKey("status")) product.setStatus((String) data.get("status"));
        if (data.containsKey("price") && data.get("price") != null) {
            product.setPrice(((Number) data.get("price")).intValue());
        }
        if (data.containsKey("originalPrice") && data.get("originalPrice") != null) {
            product.setOriginalPrice(((Number) data.get("originalPrice")).intValue());
        }
        if (data.containsKey("stock") && data.get("stock") != null) {
            product.setStock(((Number) data.get("stock")).intValue());
        }
    }

    private Map<String, Object> productToMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("subtitle", product.getSubtitle());
        map.put("description", product.getDescription());
        map.put("price", product.getPrice());
        map.put("originalPrice", product.getOriginalPrice());
        map.put("stock", product.getStock());
        map.put("sales", product.getSales());
        map.put("mainImage", product.getMainImage());
        map.put("heritageType", product.getHeritageType());
        map.put("status", product.getStatus());
        map.put("createTime", product.getCreateTime());
        return map;
    }

    public List<Map<String, Object>> getMyOrders(String username) {
        User user = getUser(username);
        List<Order> orders = orderRepository.findByMerchantIdOrderByCreateTimeDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(orderToMap(order));
        }
        return result;
    }

    private Map<String, Object> orderToMap(Order order) {
        Map<String, Object> map = new HashMap<>();
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
        return map;
    }

    public Map<String, Object> bookActivity(String username, Long activityId, Map<String, Object> data) {
        User user = getUser(username);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity does not exist"));
        int participantCount = data.get("participantCount") instanceof Number
                ? Math.max(((Number) data.get("participantCount")).intValue(), 1)
                : 1;
        int currentParticipants = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
        if (activity.getMaxParticipants() != null
                && currentParticipants + participantCount > activity.getMaxParticipants()) {
            throw new RuntimeException("This activity is already full");
        }

        Optional<ActivityBooking> existingBooking = bookingRepository.findByActivityIdAndUserId(activityId, user.getId());
        ActivityBooking booking;
        if (existingBooking.isPresent()) {
            booking = existingBooking.get();
            if ("rejected".equals(booking.getStatus())) {
                throw new RuntimeException("商家已拒绝您的报名，无法再次报名该活动");
            }
            if (!"cancelled".equals(booking.getStatus())) {
                throw new RuntimeException("您已报名该活动");
            }
        } else {
            booking = new ActivityBooking();
            booking.setActivityId(activityId);
            booking.setUserId(user.getId());
        }

        booking.setParticipantName(data.get("participantName") != null
                ? (String) data.get("participantName")
                : user.getNickname());
        booking.setParticipantPhone((String) data.get("participantPhone"));
        booking.setRemark((String) data.get("remark"));
        booking.setParticipantCount(participantCount);
        booking.setStatus("registered");
        booking.setSignupTime(new Date());
        booking.setVerificationTime(null);
        booking.setVerifiedBy(null);
        if (activity.getPrice() == null || activity.getPrice() <= 0) {
            booking.setPaymentStatus("paid");
            booking.setPaymentTime(new Date());
        } else {
            booking.setPaymentStatus("unpaid");
            booking.setPaymentTime(null);
        }
        bookingRepository.save(booking);

        activity.setCurrentParticipants(currentParticipants + participantCount);
        activityRepository.save(activity);

        return bookingToMap(booking);
    }

    public Map<String, Object> createOrder(String username, Map<String, Object> data) {
        User user = getUser(username);
        Long productId = ((Number) data.get("productId")).longValue();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product does not exist"));
        if (!"on_sale".equals(product.getStatus())) {
            throw new RuntimeException("Product is not available");
        }
        int quantity = data.containsKey("quantity") ? ((Number) data.get("quantity")).intValue() : 1;
        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        Order order = new Order();
        order.setOrderNo("YF" + System.currentTimeMillis() + (int) (Math.random() * 1000));
        order.setUserId(user.getId());
        order.setMerchantId(product.getMerchantId());
        merchantProfileRepository.findByUserId(product.getMerchantId())
                .map(MerchantProfile::getId)
                .ifPresent(order::setMerchantProfileId);
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setQuantity(quantity);
        order.setTotalAmount(product.getPrice() * quantity);
        order.setPayAmount(product.getPrice() * quantity);
        order.setReceiverName((String) data.get("receiverName"));
        order.setReceiverPhone((String) data.get("receiverPhone"));
        order.setReceiverProvince((String) data.get("receiverProvince"));
        order.setReceiverCity((String) data.get("receiverCity"));
        order.setReceiverDistrict((String) data.get("receiverDistrict"));
        order.setReceiverAddress((String) data.get("receiverAddress"));
        order.setRemark((String) data.get("remark"));
        orderRepository.save(order);

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        return orderToMap(order);
    }

    public List<Map<String, Object>> getMyOrdersAsUser(String username) {
        User user = getUser(username);
        List<Order> orders = orderRepository.findByUserIdAndDeleteStatusOrderByCreateTimeDesc(user.getId(), 0);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(orderToMap(order));
        }
        return result;
    }

    public Map<String, Object> getMyOrderOverview(String username) {
        User user = getUser(username);

        List<Map<String, Object>> productOrders = getMyOrdersAsUser(username);
        List<ActivityBooking> bookings = bookingRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
        List<Map<String, Object>> activityBookings = new ArrayList<>();
        long checkedInCount = 0;

        for (ActivityBooking booking : bookings) {
            if ("checked_in".equals(booking.getStatus())) {
                checkedInCount++;
            }
            activityBookings.add(userBookingToMap(booking));
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("productOrderCount", productOrders.size());
        summary.put("activityBookingCount", activityBookings.size());
        summary.put("checkedInCount", checkedInCount);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("productOrders", productOrders);
        result.put("activityBookings", activityBookings);
        return result;
    }

    public void cancelOrder(String username, Long orderId) {
        User user = getUser(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order does not exist"));
        if (!order.getUserId().equals(user.getId())) {
            throw new RuntimeException("No permission to operate on this order");
        }
        if (!"pending_payment".equals(order.getStatus())) {
            throw new RuntimeException("This order cannot be cancelled in its current status");
        }
        order.setStatus("cancelled");
        orderRepository.save(order);

        if (order.getProductId() != null) {
            productRepository.findById(order.getProductId()).ifPresent(product -> {
                product.setStock(product.getStock() + (order.getQuantity() == null ? 1 : order.getQuantity()));
                productRepository.save(product);
            });
        }
    }

    private Map<String, Object> userBookingToMap(ActivityBooking booking) {
        Map<String, Object> map = bookingToMap(booking);
        activityRepository.findById(booking.getActivityId()).ifPresent(activity -> {
            map.put("activityTitle", activity.getTitle());
            map.put("activitySubtitle", activity.getSubtitle());
            map.put("activityCoverImage", activity.getCoverImage());
            map.put("activityType", activity.getActivityType());
            map.put("heritageType", activity.getHeritageType());
            map.put("startTime", activity.getStartTime());
            map.put("endTime", activity.getEndTime());
            map.put("locationCity", activity.getLocationCity());
            map.put("locationDetail", activity.getLocationDetail());
            map.put("activityStatus", activity.getStatus());
            userRepository.findById(activity.getMerchantId()).ifPresent(merchant -> {
                map.put("merchantName", merchant.getNickname() != null ? merchant.getNickname() : merchant.getUsername());
                map.put("merchantPhone", merchant.getPhone());
                map.put("shopName", merchant.getShopName());
            });
        });
        return map;
    }

    public void cancelActivityBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking record does not exist"));
        if (!booking.getUserId().equals(user.getId())) {
            throw new RuntimeException("No permission to operate on this booking");
        }
        if ("checked_in".equals(booking.getStatus())) {
            throw new RuntimeException("已核销报名不可取消");
        }
        if ("rejected".equals(booking.getStatus())) {
            throw new RuntimeException("商家已拒绝的报名不可取消");
        }
        if ("cancelled".equals(booking.getStatus())) {
            throw new RuntimeException("该报名已取消");
        }

        booking.setStatus("cancelled");
        bookingRepository.save(booking);

        activityRepository.findById(booking.getActivityId()).ifPresent(activity -> {
            int current = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
            int participantCount = booking.getParticipantCount() == null ? 1 : booking.getParticipantCount();
            activity.setCurrentParticipants(Math.max(0, current - participantCount));
            activityRepository.save(activity);
        });
    }

    public void deleteActivityBooking(String username, Long bookingId) {
        User user = getUser(username);
        ActivityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking record does not exist"));
        if (!booking.getUserId().equals(user.getId())) {
            throw new RuntimeException("No permission to operate on this booking");
        }
        if (!"cancelled".equals(booking.getStatus())) {
            throw new RuntimeException("Only cancelled bookings can be deleted");
        }

        bookingRepository.delete(booking);
    }
}
