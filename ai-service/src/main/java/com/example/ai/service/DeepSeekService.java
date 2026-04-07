package com.example.ai.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.ai.config.DeepSeekConfig;
import com.example.ai.dto.ChatRequest;
import com.example.ai.dto.ChatResponse;
import com.example.ai.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final String INDEX_NAME = "phone_products";

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private DeepSeekConfig config;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> chatWithRAGAndFunctionCalling(String userMessage, List<ChatRequest.Message> history) {
        // 判断是否需要检索商品（只有询问商品时才检索）
        boolean needSearch = isProductQuery(userMessage);
        
        List<Product> products = new ArrayList<>();
        if (needSearch) {
            products = searchProducts(userMessage, 5);
        }
        
        StringBuilder context = new StringBuilder();
        if (!products.isEmpty()) {
            context.append("以下是相关的商品信息：\n\n");
            for (Product p : products) {
                context.append(String.format("- %s %s: %s\n", 
                        p.getBrand(), p.getModel(), p.getContent()));
            }
            context.append("\n请基于以上商品信息回答用户问题。\n");
        }
        
        ChatRequest request = new ChatRequest();
        request.setModel(config.getModel());
        request.setTemperature(0.7);

        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(new ChatRequest.Message(
                "system",
                "你是电商平台客服。\n" +
                        (context.length() > 0 ? "以下是相关的商品信息：\n" + context.toString() + "\n\n" : "") +
                        "请回答用户问题，回答热情、礼貌。\n" +
                        "当用户明确表示要转人工、转接客服、找人工客服时，请调用 createTicket 函数。\n" +
                        "发货时间：付款后48小时内\n" +
                        "退换货：支持7天无理由\n" +
                        "快递：中通、圆通、顺丰\n" +
                        "其他问题正常回答。"
        ));
        
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        
        messages.add(new ChatRequest.Message("user", userMessage));
        request.setMessages(messages);
        
        request.setTools(List.of(createTicketTool()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        String url = config.getBaseUrl() + "/v1/chat/completions";
        ChatResponse response = restTemplate.postForObject(url, entity, ChatResponse.class);

        Map<String, Object> result = new HashMap<>();
        
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatRequest.Message responseMessage = response.getChoices().get(0).getMessage();
            String finishReason = response.getChoices().get(0).getFinishReason();
            
            if ("tool_calls".equals(finishReason) && responseMessage.getToolCalls() != null) {
                Map<String, Object> toolCall = responseMessage.getToolCalls().get(0);
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String functionName = (String) function.get("name");
                String arguments = (String) function.get("arguments");
                
                result.put("type", "function_call");
                result.put("functionName", functionName);
                result.put("arguments", arguments);
                result.put("content", "正在为您转接人工客服，请稍候...");
            } else {
                result.put("type", "text");
                result.put("content", responseMessage.getContent());
            }
        } else {
            result.put("type", "text");
            result.put("content", "抱歉，我暂时无法回答您的问题，请稍后再试。");
        }
        
        return result;
    }

    public List<Product> searchProducts(String query, int topK) {
        try {
            System.out.println("开始关键词搜索，查询词: " + query);

            // 提取关键词（去除常见词汇）
            String searchKeyword = extractSearchKeyword(query);
            System.out.println("提取的关键词: " + searchKeyword);

            // 使用通配符查询，支持模糊匹配
            SearchResponse<Product> response = esClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                        .bool(b -> b
                            .should(s1 -> s1
                                .wildcard(w -> w
                                    .field("model")
                                    .value("*" + searchKeyword + "*")
                                )
                            )
                            .should(s2 -> s2
                                .wildcard(w -> w
                                    .field("content")
                                    .value("*" + searchKeyword + "*")
                                )
                            )
                            .should(s3 -> s3
                                .match(m -> m
                                    .field("brand")
                                    .query(searchKeyword)
                                )
                            )
                        )
                    )
                    .size(topK),
                    Product.class
            );

            List<Product> products = new ArrayList<>();
            for (Hit<Product> hit : response.hits().hits()) {
                Product product = hit.source();
                if (product != null) {
                    product.setScore(hit.score());
                    products.add(product);
                }
            }
            
            System.out.println("搜索完成，找到 " + products.size() + " 个商品");
            return products;
        } catch (Exception e) {
            System.err.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 判断用户查询是否需要检索商品
     */
    private boolean isProductQuery(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String lowerMsg = message.toLowerCase();
        
        // 商品相关关键词
        String[] productKeywords = {
            // 基础商品词
            "手机", "推荐", "拍照", "游戏", "续航", "充电", "屏幕", "价格", "多少钱", "处理器", "品牌",
            // 品牌
            "iphone", "苹果", "华为", "小米", "oppo", "vivo", "一加", "三星", "荣耀", "红米", "魅族", "努比亚",
            // 型号系列
            "promax", "pro", "max", "plus", "mini", "ultra", "se",
            // 数字型号（如 16, 15, 14 等）
            "16", "15", "14", "13", "12", "11", "x", "xs", "xr",
            // 动作词
            "买", "选", "对比", "哪个好", "怎么样", "配置", "参数", "电池", "像素", "想要", "有没有",
            // 存储容量
            "128", "256", "512", "1tb",
            // 颜色
            "黑色", "白色", "金色", "蓝色", "紫色"
        };
        
        for (String keyword : productKeywords) {
            if (lowerMsg.contains(keyword)) {
                return true;
            }
        }
        
        // 额外检查：是否包含数字+字母组合（如 16promax, 15pro 等）
        if (lowerMsg.matches(".*\\d+(promax|pro|max|plus|mini).*")) {
            return true;
        }
        
        // 检查是否包含 iphone + 数字
        if (lowerMsg.matches(".*iphone\\d+.*")) {
            return true;
        }
        
        return false;
    }

    /**
     * 从用户查询中提取有效搜索关键词
     */
    private String extractSearchKeyword(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }

        String lowerQuery = query.toLowerCase();

        // 去除常见无意义词汇
        String[] stopWords = {"我", "想", "要", "买", "请问", "一下", "有没有", "推荐", "一款"};
        for (String word : stopWords) {
            lowerQuery = lowerQuery.replace(word, "");
        }

        // 提取型号数字（如 16promax -> 16, 15pro -> 15）
        if (lowerQuery.matches(".*\\d+(promax|pro|max|plus).*")) {
            // 提取数字部分
            String number = lowerQuery.replaceAll(".*?(\\d+).*", "$1");
            return number;
        }

        // 提取 iphone + 数字
        if (lowerQuery.matches(".*iphone\\d+.*")) {
            String number = lowerQuery.replaceAll(".*iphone(\\d+).*", "$1");
            return "iphone " + number;
        }

        return lowerQuery.trim();
    }


    private ChatRequest.Tool createTicketTool() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", Map.of(
            "reason", Map.of(
                "type", "string",
                "description", "用户转人工的原因"
            )
        ));
        parameters.put("required", List.of("reason"));

        return new ChatRequest.Tool(
            "function",
            new ChatRequest.Function(
                "createTicket",
                "当用户需要转人工客服时，创建工单并分配客服",
                parameters
            )
        );
    }
}