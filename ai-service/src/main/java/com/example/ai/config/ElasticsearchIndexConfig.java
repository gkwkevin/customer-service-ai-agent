package com.example.ai.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchIndexConfig {

    @Autowired
    private ElasticsearchClient esClient;

    @PostConstruct
    public void createIndex() {
        try {
            String indexName = "phone_products_v2";
            
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                System.out.println("索引 " + indexName + " 已存在");
                return;
            }

            CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                .index(indexName)
                .mappings(m -> m
                    .properties("brand", p -> p.keyword(k -> k))
                    .properties("model", p -> p.text(t -> t))
                    .properties("content", p -> p.text(t -> t))
                    .properties("embedding", p -> p
                        .denseVector(d -> d
                            .dims(2048)
                            .index(true)
                            .similarity(DenseVectorSimilarity.valueOf("cosine"))
                        )
                    )
                )
            );

            esClient.indices().create(request);
            System.out.println("索引 " + indexName + " 创建成功！");
        } catch (Exception e) {
            System.err.println("创建索引失败: " + e.getMessage());
        }
    }
}
