package com.example.ai.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.ai.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductMigrationService {

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private EmbeddingService embeddingService;

    private static final String OLD_INDEX = "phone_products";
    private static final String NEW_INDEX = "phone_products_v2";

    public void migrate() throws Exception {
        SearchResponse<Product> response = esClient.search(s -> s
            .index(OLD_INDEX)
            .size(1000),
            Product.class
        );

        List<Product> products = response.hits().hits().stream()
            .map(Hit::source)
            .collect(Collectors.toList());

        System.out.println("找到 " + products.size() + " 条商品，开始迁移...");

        int success = 0;
        int fail = 0;

        for (Product product : products) {
            try {
                String text = buildText(product);
                List<Double> embedding = embeddingService.getEmbedding(text);
                product.setEmbedding(embedding);

                esClient.index(i -> i
                    .index(NEW_INDEX)
                    .document(product)
                );

                success++;
                System.out.println("已迁移: " + product.getModel());
                Thread.sleep(200);

            } catch (Exception e) {
                fail++;
                System.err.println("失败: " + product.getModel() + " - " + e.getMessage());
            }
        }

        System.out.println("迁移完成！成功: " + success + ", 失败: " + fail);
    }

    private String buildText(Product product) {
        return String.format("品牌：%s，型号：%s，特点：%s",
            product.getBrand(),
            product.getModel(),
            product.getContent()
        );
    }
}
