package com.yayfolk.backend.controller;

import com.yayfolk.backend.dto.ResponseDto;
import com.yayfolk.backend.repository.ExploreConversationRepository;
import com.yayfolk.backend.repository.ExploreMessageRepository;

import com.yayfolk.backend.service.AIResourceService;
import com.yayfolk.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AIControllerTest {
    

    
    @Mock
    private AIResourceService aiResourceService;

    @Mock
    private ExploreConversationRepository exploreConversationRepository;

    @Mock
    private ExploreMessageRepository exploreMessageRepository;

    @Mock
    private UserService userService;
    
    @InjectMocks
    private AIController aiController;
    
    @Test
    public void testExploreResources() {
        Map<String, Object> request = new HashMap<>();
        request.put("userInput", "帮我规划3天北京非遗之旅");
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("intent", "ITINERARY_PLANNING");
        mockResult.put("destination", "北京");
        mockResult.put("days", 3);
        
        when(aiResourceService.exploreResources(request)).thenReturn(mockResult);
        when(aiResourceService.buildSummaryText(mockResult)).thenReturn("为您规划了3天的北京非遗之旅");
        when(aiResourceService.serializeResources(mockResult)).thenReturn("{}");
        
        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(httpRequest.getAttribute("username")).thenReturn("testuser");

        ResponseDto response = aiController.exploreResources(request, httpRequest);
        
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals("ITINERARY_PLANNING", ((Map<?, ?>) response.getData()).get("intent"));
        assertEquals("北京", ((Map<?, ?>) response.getData()).get("destination"));
        assertEquals(3, ((Map<?, ?>) response.getData()).get("days"));
    }
    
    @Test
    public void testExploreResourcesWithEmptyInput() {
        Map<String, Object> request = new HashMap<>();
        request.put("userInput", "");
        
        when(aiResourceService.exploreResources(request)).thenThrow(new RuntimeException("请输入查询内容"));
        
        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(httpRequest.getAttribute("username")).thenReturn("testuser");

        ResponseDto response = aiController.exploreResources(request, httpRequest);
        
        assertEquals(400, response.getCode());
        assertEquals("请输入查询内容", response.getMessage());
        assertNull(response.getData());
    }
}
