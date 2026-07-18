package com.yayfolk.backend.agent.query;

import java.time.LocalDate;
import java.util.Map;

/**
 * 活动结构化查询参数 DTO。
 * 替代 AIResourceService 中使用 Map<String, Object> 传递查询参数的方式。
 */
public class QueryParams {

    private String heritageType;
    private String locationCity;
    private String price;
    private Double priceMax;
    private LocalDate startDate;
    private LocalDate startDateAfter;
    private LocalDate startDateBefore;

    /**
     * 从 LLM 返回的 Map 解析查询参数。
     */
    public static QueryParams fromLLMResponse(Map<String, Object> llmJson) {
        QueryParams params = new QueryParams();

        if (llmJson == null || llmJson.isEmpty()) {
            return params;
        }

        if (llmJson.containsKey("heritage_type")) {
            params.heritageType = llmJson.get("heritage_type").toString();
        }
        if (llmJson.containsKey("location_city")) {
            params.locationCity = llmJson.get("location_city").toString();
        }
        if (llmJson.containsKey("price")) {
            params.price = llmJson.get("price").toString();
        }
        if (llmJson.containsKey("price_max")) {
            try {
                params.priceMax = Double.parseDouble(llmJson.get("price_max").toString());
            } catch (NumberFormatException ignored) {}
        }
        if (llmJson.containsKey("start_date")) {
            try {
                params.startDate = LocalDate.parse(llmJson.get("start_date").toString());
            } catch (Exception ignored) {}
        }
        if (llmJson.containsKey("start_date_after")) {
            try {
                params.startDateAfter = LocalDate.parse(llmJson.get("start_date_after").toString());
            } catch (Exception ignored) {}
        }
        if (llmJson.containsKey("start_date_before")) {
            try {
                params.startDateBefore = LocalDate.parse(llmJson.get("start_date_before").toString());
            } catch (Exception ignored) {}
        }

        return params;
    }

    /**
     * 是否有任何查询条件。
     */
    public boolean isEmpty() {
        return heritageType == null && locationCity == null && price == null
                && priceMax == null && startDate == null
                && startDateAfter == null && startDateBefore == null;
    }

    // Getters and setters

    public String getHeritageType() { return heritageType; }
    public void setHeritageType(String heritageType) { this.heritageType = heritageType; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public Double getPriceMax() { return priceMax; }
    public void setPriceMax(Double priceMax) { this.priceMax = priceMax; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getStartDateAfter() { return startDateAfter; }
    public void setStartDateAfter(LocalDate startDateAfter) { this.startDateAfter = startDateAfter; }

    public LocalDate getStartDateBefore() { return startDateBefore; }
    public void setStartDateBefore(LocalDate startDateBefore) { this.startDateBefore = startDateBefore; }
}
