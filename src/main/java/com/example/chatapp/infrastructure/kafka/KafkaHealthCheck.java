//package com.example.chatapp.infrastructure.kafka;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.admin.AdminClient;
//import org.apache.kafka.clients.admin.DescribeClusterResult;
//import org.springframework.boot.actuator.health.Health;
//import org.springframework.boot.actuator.health.HealthIndicator;
//import org.springframework.kafka.core.KafkaAdmin;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * Kafka 클러스터 상태를 확인하는 Health Check 컴포넌트
// */
//@Component("kafka")
//@RequiredArgsConstructor
//@Slf4j
//public class KafkaHealthCheck implements HealthIndicator {
//
//    private final KafkaAdmin kafkaAdmin;
//
//    @Override
//    public Health health() {
//        try {
//            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
//                DescribeClusterResult cluster = adminClient.describeCluster();
//
//                // 클러스터 정보 조회 (타임아웃 5초)
//                String clusterId = cluster.clusterId().get(5, TimeUnit.SECONDS);
//                int nodeCount = cluster.nodes().get(5, TimeUnit.SECONDS).size();
//
//                return Health.up()
//                        .withDetail("clusterId", clusterId)
//                        .withDetail("nodeCount", nodeCount)
//                        .withDetail("status", "연결됨")
//                        .build();
//            }
//        } catch (Exception e) {
//            log.error("Kafka 헬스체크 실패: {}", e.getMessage());
//            return Health.down()
//                    .withDetail("error", e.getMessage())
//                    .withDetail("status", "연결 실패")
//                    .build();
//        }
//    }
//}
