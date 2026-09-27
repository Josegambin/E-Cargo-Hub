package com.ecargohub.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1, 
    ports = 8085 // <- Le dice nativamente a KRaft que levante el broker en el puerto 8085
)
class ECargoHubApplicationTests {

    @Test
    void contextLoads() {
    }
}