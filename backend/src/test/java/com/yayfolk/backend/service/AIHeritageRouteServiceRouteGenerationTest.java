package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.Activity;
import com.yayfolk.backend.entity.IntangibleCulturalHeritage;
import com.yayfolk.backend.repository.ActivityRepository;
import com.yayfolk.backend.repository.IntangibleCulturalHeritageRepository;
import com.yayfolk.backend.repository.OfficialContentRepository;
import com.yayfolk.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIHeritageRouteServiceRouteGenerationTest {

    @Test
    void shouldUseHeritageAsRouteBackboneAndActivitiesAsSupplements() {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository = mock(IntangibleCulturalHeritageRepository.class);
        OfficialContentRepository officialContentRepository = mock(OfficialContentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        IntangibleCulturalHeritage kunqu = new IntangibleCulturalHeritage();
        kunqu.setId(13L);
        kunqu.setName("昆曲");
        kunqu.setCategory("传统戏剧");
        kunqu.setSubcategory("昆曲");
        kunqu.setDynasty("明代");
        kunqu.setRegion("江苏省苏州市");
        kunqu.setLevel("national");
        kunqu.setIntroduction("昆曲是中国古老戏曲艺术的重要代表，适合作为苏州非遗路线主线。");
        kunqu.setRepresentativeInheritor("王芳");
        kunqu.setIsFeatured(1);
        kunqu.setViewCount(5000);

        Activity experience = new Activity();
        experience.setId(101L);
        experience.setMerchantId(10011L);
        experience.setTitle("传统戏曲昆曲预约体验");
        experience.setContent("可预约体验传统戏曲中的昆曲身段与唱腔基础，适合作为路线中的体验点。");
        experience.setHeritageType("传统戏曲");
        experience.setActivityType("线下体验");
        experience.setLocationProvince("江苏");
        experience.setLocationCity("苏州");
        experience.setLocationDetail("姑苏区非遗体验中心");
        experience.setStatus("signup");
        experience.setAuditStatus("approved");
        experience.setStartTime(new Date());
        experience.setEndTime(new Date());
        experience.setIsRecommend(1);

        when(activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended"))
                .thenReturn(Collections.singletonList(experience));
        when(intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc())
                .thenReturn(Collections.singletonList(kunqu));
        when(officialContentRepository.findByIsPublicOrderByCreateTimeDesc(1))
                .thenReturn(Collections.emptyList());
        when(userRepository.findById(10011L)).thenReturn(Optional.empty());

        AIHeritageRouteService service = new AIHeritageRouteService(
                activityRepository,
                intangibleCulturalHeritageRepository,
                officialContentRepository,
                userRepository
        );

        Map<String, Object> response = service.generateRoute(Collections.singletonMap(
                "userInput",
                "传统戏曲非遗体验路线"
        ));

        List<Map<String, Object>> itinerary = castList(response.get("itinerary"));
        assertFalse(itinerary.isEmpty());
        List<Map<String, Object>> dayActivities = castList(itinerary.get(0).get("activities"));
        assertFalse(dayActivities.isEmpty());
        assertEquals("昆曲", dayActivities.get(0).get("name"));

        List<Map<String, Object>> highlights = castList(response.get("officialHighlights"));
        assertFalse(highlights.isEmpty());
        assertEquals("昆曲", highlights.get(0).get("title"));

        List<Map<String, Object>> activities = castList(response.get("activities"));
        assertFalse(activities.isEmpty());
        assertEquals("传统戏曲昆曲预约体验", activities.get(0).get("title"));

        String summary = String.valueOf(response.get("summary"));
        assertTrue(summary.contains("非遗项目"));
        assertTrue(summary.contains("可预约体验点"));
    }

    @Test
    void shouldTreatRequestedProjectCountAsTotalAcrossDays() {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository = mock(IntangibleCulturalHeritageRepository.class);
        OfficialContentRepository officialContentRepository = mock(OfficialContentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        List<IntangibleCulturalHeritage> heritages = Arrays.asList(
                createHeritage(1L, "景泰蓝制作技艺", "传统手工艺", "北京"),
                createHeritage(2L, "风筝制作技艺", "传统手工艺", "北京"),
                createHeritage(3L, "苏绣", "传统手工艺", "苏州"),
                createHeritage(4L, "宋锦织造技艺", "传统手工艺", "苏州")
        );
        List<Activity> activities = Arrays.asList(
                createActivity(101L, "景泰蓝点蓝体验", "北京"),
                createActivity(102L, "苏绣针法体验", "苏州"),
                createActivity(103L, "宋锦纹样体验", "苏州"),
                createActivity(104L, "风筝彩绘体验", "北京")
        );

        when(activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended"))
                .thenReturn(activities);
        when(intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc())
                .thenReturn(heritages);
        when(officialContentRepository.findByIsPublicOrderByCreateTimeDesc(1))
                .thenReturn(Collections.emptyList());

        AIHeritageRouteService service = new AIHeritageRouteService(
                activityRepository,
                intangibleCulturalHeritageRepository,
                officialContentRepository,
                userRepository
        );

        Map<String, Object> response = service.generateRoute(Collections.singletonMap(
                "userInput",
                "2天6个项目的传统手工艺路线"
        ));

        assertEquals(6, ((Number) response.get("requestedProjectCount")).intValue());
        assertEquals(6, ((Number) response.get("matchedCount")).intValue());

        List<Map<String, Object>> itinerary = castList(response.get("itinerary"));
        assertEquals(2, itinerary.size());
        assertEquals(3, castList(itinerary.get(0).get("activities")).size());
        assertEquals(3, castList(itinerary.get(1).get("activities")).size());
        assertEquals(6, countItineraryItems(itinerary));

        List<Map<String, Object>> activityCards = castList(response.get("activities"));
        assertEquals(2, activityCards.size());
    }

    @Test
    void shouldHonorExclusionTermsInGeneratedRoute() {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        IntangibleCulturalHeritageRepository intangibleCulturalHeritageRepository = mock(IntangibleCulturalHeritageRepository.class);
        OfficialContentRepository officialContentRepository = mock(OfficialContentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        List<IntangibleCulturalHeritage> heritages = Arrays.asList(
                createHeritage(11L, "唐三彩", "传统手工艺", "河南洛阳"),
                createHeritage(12L, "苏绣", "传统手工艺", "江苏苏州")
        );
        List<Activity> activities = Arrays.asList(
                createActivity(201L, "唐三彩彩绘体验", "洛阳"),
                createActivity(202L, "苏绣针法体验", "苏州")
        );

        when(activityRepository.findByAuditStatusAndStatusNotOrderByStartTimeAsc("approved", "ended"))
                .thenReturn(activities);
        when(intangibleCulturalHeritageRepository.findAllByOrderByIsFeaturedDescViewCountDescIdAsc())
                .thenReturn(heritages);
        when(officialContentRepository.findByIsPublicOrderByCreateTimeDesc(1))
                .thenReturn(Collections.emptyList());

        AIHeritageRouteService service = new AIHeritageRouteService(
                activityRepository,
                intangibleCulturalHeritageRepository,
                officialContentRepository,
                userRepository
        );

        Map<String, Object> response = service.generateRoute(Collections.singletonMap(
                "userInput",
                "两天四个项目的传统手工艺路线，排除河南的唐三彩"
        ));

        assertNoExcludedTerm(response, "唐三彩");
        assertNoExcludedTerm(response, "河南");

        List<Map<String, Object>> highlights = castList(response.get("officialHighlights"));
        assertFalse(highlights.isEmpty());
        assertEquals("苏绣", highlights.get(0).get("title"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.emptyList();
    }

    private static IntangibleCulturalHeritage createHeritage(Long id, String name, String category, String region) {
        IntangibleCulturalHeritage heritage = new IntangibleCulturalHeritage();
        heritage.setId(id);
        heritage.setName(name);
        heritage.setCategory(category);
        heritage.setSubcategory(name);
        heritage.setDynasty("宋代");
        heritage.setRegion(region);
        heritage.setLevel("national");
        heritage.setIntroduction(name + "是适合线路规划的非遗项目。");
        heritage.setIsFeatured(1);
        heritage.setViewCount(1000);
        return heritage;
    }

    private static Activity createActivity(Long id, String title, String city) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setMerchantId(10000L + id);
        activity.setTitle(title);
        activity.setContent(title + "适合作为可预约体验点。");
        activity.setHeritageType("传统手工艺");
        activity.setActivityType("线下体验");
        activity.setLocationProvince("全国");
        activity.setLocationCity(city);
        activity.setLocationDetail(city + "非遗体验中心");
        activity.setStatus("signup");
        activity.setAuditStatus("approved");
        activity.setStartTime(new Date());
        activity.setEndTime(new Date());
        activity.setIsRecommend(1);
        return activity;
    }

    private static int countItineraryItems(List<Map<String, Object>> itinerary) {
        int count = 0;
        for (Map<String, Object> day : itinerary) {
            count += castList(day.get("activities")).size();
        }
        return count;
    }

    private static void assertNoExcludedTerm(Map<String, Object> response, String excludedTerm) {
        for (Map<String, Object> day : castList(response.get("itinerary"))) {
            for (Map<String, Object> item : castList(day.get("activities"))) {
                assertFalse(String.valueOf(item.get("name")).contains(excludedTerm));
                assertFalse(String.valueOf(item.get("description")).contains(excludedTerm));
            }
        }
        for (Map<String, Object> card : castList(response.get("activities"))) {
            assertFalse(String.valueOf(card.get("title")).contains(excludedTerm));
            assertFalse(String.valueOf(card.get("description")).contains(excludedTerm));
        }
        for (Map<String, Object> highlight : castList(response.get("officialHighlights"))) {
            assertFalse(String.valueOf(highlight.get("title")).contains(excludedTerm));
            assertFalse(String.valueOf(highlight.get("summary")).contains(excludedTerm));
        }
    }
}
